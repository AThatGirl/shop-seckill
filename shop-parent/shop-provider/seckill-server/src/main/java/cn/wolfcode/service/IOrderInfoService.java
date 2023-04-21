package cn.wolfcode.service;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * Created by wolfcode-lanxw
 */
public interface IOrderInfoService {

    /**
     * 根据用户手机号码和秒杀商品id查询订单信息
     *
     * @param phone
     * @param seckillId
     * @return
     */
    OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId);

    /**
     * 创建秒杀订单
     *
     * @param phone
     * @param seckillProductVo
     * @return
     */
    OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo);

    /**
     * 根据订单号，查询订单对象
     *
     * @param orderNo
     * @return
     */
    OrderInfo findByOrderNo(String orderNo);

    /**
     * 根据订单号判断是否超时,超时就取消
     *
     * @param orderNo
     */
    void cancelOrder(String orderNo);

    /**
     * 获取支付服务返回的字符串
     *
     * @param orderNo
     * @return
     */
    Result<String> payOnline(String orderNo);

    /**
     * 将订单修改为支付状态
     * @param orderNo
     * @param status
     * @param payType
     * @return
     */
    int changePayStatus(String orderNo, Integer status, int payType);

    /**
     * 在线支付退款
     * @param orderInfo
     */
    void refundOnline(OrderInfo orderInfo);
}
