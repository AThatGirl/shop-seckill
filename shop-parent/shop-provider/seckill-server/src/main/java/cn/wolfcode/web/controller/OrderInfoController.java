package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.DateUtil;
import cn.wolfcode.util.UserUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;

    @RequestMapping("/doSeckill")
    @RequireLogin
    public Result<String> doSeckill(Integer time, Long seckillId, HttpServletRequest request){
        //判断是否处于抢购的时间
        SeckillProductVo seckillProductVo = seckillProductService.find(time, seckillId);
        boolean isLegalTime = DateUtil.isLegalTime(seckillProductVo.getStartDate(), seckillProductVo.getTime());
        if (!isLegalTime){
            return Result.error(CommonCodeMsg.ILLEGAL_OPERATION);
        }
        //一个用户只能抢购一个商品
        //获取token信息
        String token = request.getHeader(CommonConstants.TOKEN_NAME);
        //根据token从redis中获取手机号
        String phone = UserUtil.getUserPhone(redisTemplate, token);
        OrderInfo orderInfo = orderInfoService.findByPhoneAndSeckillId(phone, seckillId);
        if(orderInfo != null){
            //说明已经抢购过，提示重复下单
            return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
        }
        //保证库存数量足够
        if(seckillProductVo.getStockCount() <= 0){
            //提示库存不足
            return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }

        orderInfo = orderInfoService.doSeckill(phone, seckillProductVo);
        return Result.success(orderInfo.getOrderNo());
    }

    @RequestMapping("/find")
    @RequireLogin
    public Result<OrderInfo> find(String orderNo, HttpServletRequest request){
        OrderInfo orderInfo = orderInfoService.findByOrderNo(orderNo);
        //防止查看别人的订单号
        String token = request.getHeader(CommonConstants.TOKEN_NAME);
        String phone = UserUtil.getUserPhone(redisTemplate, token);
        if (!phone.equals(String.valueOf(orderInfo.getUserId()))) {
            return Result.error(CommonCodeMsg.ILLEGAL_OPERATION);
        }
        return Result.success(orderInfo);
    }



}
