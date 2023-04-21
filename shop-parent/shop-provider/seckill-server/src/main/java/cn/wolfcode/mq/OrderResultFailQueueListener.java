package cn.wolfcode.mq;

import cn.wolfcode.service.ISeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OrderResultFailQueueListener
 * description: 数据库库存同步到redis
 * 2023/4/21 10:42
 * Create by 杰瑞
 */
@Component
@RocketMQMessageListener(consumerGroup = "orderResultFailGroup", topic = MQConstant.ORDER_RESULT_TOPIC, selectorExpression = MQConstant.ORDER_RESULT_FAIL_TAG)
@Slf4j
public class OrderResultFailQueueListener implements RocketMQListener<OrderMQResult> {

    @Autowired
    private ISeckillProductService seckillProductService;

    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        seckillProductService.syncStockToRedis(orderMQResult.getTime(), orderMQResult.getSeckillId());
        log.info("库存同步到redis完成");
    }
}
