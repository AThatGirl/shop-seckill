package cn.wolfcode.mq;

import cn.wolfcode.ws.OrderServer;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * OrderResultQueueListener
 * description:
 * 2023/4/20 12:00
 * Create by 杰瑞
 */
@Component
@RocketMQMessageListener(consumerGroup = "OrderResultGroup", topic = MQConstants.ORDER_RESULT_TOPIC)
@Slf4j
public class OrderResultQueueListener implements RocketMQListener<OrderMQResult> {


    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        //找到客户端
        Session session = null;
        int count = 3;
        //尝试3次发现客户端
        while (count --> 0){
            session = OrderServer.clients.get(orderMQResult.getToken());
            if (session != null){
                try {
                    session.getBasicRemote().sendText(JSON.toJSONString(orderMQResult));
                    log.info("成功返回给客户端消息");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            try {
                //每隔0.1s尝试一次
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.info("未找到客户端");
            }
        }
    }
}
