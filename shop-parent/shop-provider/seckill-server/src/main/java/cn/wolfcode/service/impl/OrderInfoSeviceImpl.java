package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.feign.IntegralFeignApi;
import cn.wolfcode.web.feign.PayFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by wolfcode-lanxw
 */
@Service
@Slf4j
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private RefundLogMapper refundLogMapper;

    @Autowired
    private PayFeignApi payFeignApi;

    @Autowired
    private IntegralFeignApi integralFeignApi;

    @Override
    public OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId) {
        return orderInfoMapper.findByPhoneAndSeckillId(phone, seckillId);
    }

    @Override
    @Transactional
    public OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo) {
        //扣减数据库库存
        int effectCount = seckillProductService.decrStockCount(seckillProductVo.getId());
        if (effectCount == 0) {
            //影响行数为0，说明库存为0
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }

        //在redis中设置Set集合，存储的是抢到秒杀商品用户的手机号
        //seckillOrderSet:10  -> [13913212342, xxxxxxxxxxx]
        String orderSetKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillProductVo.getId()));
        redisTemplate.opsForSet().add(orderSetKey, phone);
        log.info("加入set集合成功");
        //创建秒杀订单
        return createOrderInfo(phone, seckillProductVo);
    }

    @Override
    public OrderInfo findByOrderNo(String orderNo) {
        return orderInfoMapper.find(orderNo);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        log.info("取消的订单号:{}", orderInfo != null ? orderInfo.getOrderNo() : null);
        //判断订单是否处于超时未付款
        if (orderInfo != null && OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            //修改订单状态
            int effectCount = orderInfoMapper.updateCancelStatus(orderNo, OrderInfo.STATUS_TIMEOUT);
            if (effectCount == 0) {
                return;
            }
            //真实库存回补
            seckillProductService.incrStockCount(orderInfo.getSeckillId());
            //预库存回补
            seckillProductService.syncStockToRedis(orderInfo.getSeckillTime(), orderInfo.getSeckillId());
        }

        log.info("超时取消订单完成");
    }

    @Value("${pay.returnUrl}")
    private String returnUrl;
    @Value("${pay.notifyUrl}")
    private String notifyUrl;

    @Override
    public Result<String> payOnline(String orderNo) {
        //根据订单号，查询订单
        OrderInfo orderInfo = findByOrderNo(orderNo);
        if (OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            PayVo vo = new PayVo();

            vo.setOutTradeNo(orderNo);
            vo.setTotalAmount(String.valueOf(orderInfo.getSeckillPrice()));
            vo.setSubject(orderInfo.getProductName());
            vo.setSubject(orderInfo.getProductName());
            vo.setReturnUrl(returnUrl);
            vo.setNotifyUrl(notifyUrl);
            Result<String> result = payFeignApi.payOnline(vo);
            return result;
        }
        return Result.error(SeckillCodeMsg.PAY_STATUS_CHANGE);
    }

    @Override
    public int changePayStatus(String orderNo, Integer status, int payType) {
        return orderInfoMapper.changePayStatus(orderNo, status, payType);
    }

    @Override
    public void refundOnline(OrderInfo orderInfo) {
        RefundVo vo = new RefundVo();
        vo.setOutTradeNo(orderInfo.getOrderNo());
        vo.setRefundAmount(String.valueOf(orderInfo.getSeckillPrice()));
        vo.setRefundReason("不需要了");

        Result<Boolean> result = payFeignApi.refund(vo);
        if (result == null || result.hasError() || !result.getData()) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);

    }

    @Override
    @Transactional
    public void payIntegral(String orderNo) {
        OrderInfo orderInfo = findByOrderNo(orderNo);
        if (OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            //处于未支付状态
            PayLog log = new PayLog();
            //插入支付日志记录
            log.setOrderNo(orderNo);
            log.setPayTime(new Date());
            log.setTotalAmount(String.valueOf(orderInfo.getIntergral()));
            log.setPayType(OrderInfo.PAYTYPE_INTERGRAL);
            payLogMapper.insert(log);
            //远程调用积分支付，完成积分扣减
            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(orderInfo.getUserId());
            vo.setValue(orderInfo.getIntergral());
            //调用积分服务
            Result result = integralFeignApi.decrIntegral(vo);
            if (result == null || result.hasError()) {
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }
            //修改订单状态
            int effectCount = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_INTERGRAL);
            //如果卡在最后一刻付款，可能导致订单取消但未支付成功
            if (effectCount == 0) {
                //就要抛出异常，回滚操作
                throw new BusinessException(SeckillCodeMsg.PAY_ERROR);
            }


        }


    }

    @Override
    @Transactional
    public void refundIntegral(OrderInfo orderInfo) {

        //判断是否已支付
        if (OrderInfo.STATUS_ACCOUNT_PAID.equals(orderInfo.getStatus())) {
            //添加退款记录
            RefundLog log = new RefundLog();
            log.setOrderNo(orderInfo.getOrderNo());
            log.setRefundAmount(orderInfo.getIntergral());
            log.setRefundReason("不需要了");
            log.setRefundType(OrderInfo.PAYTYPE_INTERGRAL);
            refundLogMapper.insert(log);
            //远程调用服务，增加积分
            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(orderInfo.getUserId());
            vo.setValue(orderInfo.getIntergral());
            //调用积分服务
            Result result = integralFeignApi.incrIntegral(vo);
            if (result == null || result.hasError()) {
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }
            //修改订单状态
            int effectCount = orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);
            //如果卡在最后一刻付款，可能导致订单取消但未支付成功
            if (effectCount == 0) {
                //就要抛出异常，回滚操作
                throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
            }
        }

    }

    private OrderInfo createOrderInfo(String phone, SeckillProductVo seckillProductVo) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(Long.parseLong(phone));
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(1L);
        orderInfo.setSeckillDate(seckillProductVo.getStartDate());
        orderInfo.setSeckillTime(seckillProductVo.getTime());
        BeanUtils.copyProperties(seckillProductVo, orderInfo);
        orderInfo.setOrderNo(String.valueOf(IdGenerateUtil.get().nextId()));
        orderInfo.setSeckillId(seckillProductVo.getId());
        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }
}
