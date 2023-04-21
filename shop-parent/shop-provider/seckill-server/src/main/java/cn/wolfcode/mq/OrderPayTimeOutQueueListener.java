package cn.wolfcode.mq;

import cn.wolfcode.service.IOrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OrderPayTimeQueueListener
 * description:
 * 2023/4/21 12:00
 * Create by 杰瑞
 */
@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "OrderPayTimeOut", topic = MQConstant.ORDER_PAY_TIMEOUT_TOPIC)
public class OrderPayTimeOutQueueListener implements RocketMQListener<OrderMQResult> {

    @Autowired
    private IOrderInfoService orderInfoService;


    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        //取消订单
        orderInfoService.cancelOrder(orderMQResult.getOrderNo());
    }
}
