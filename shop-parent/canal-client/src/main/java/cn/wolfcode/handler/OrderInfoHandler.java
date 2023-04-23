package cn.wolfcode.handler;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.redis.SeckillRedisKey;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * OrderInfoHandler
 * description:
 * 2023/4/23 11:44
 * Create by 杰瑞
 */
@Slf4j
@Component
@CanalTable(value = "t_order_info")
public class OrderInfoHandler implements EntryHandler<OrderInfo> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void insert(OrderInfo orderInfo) {
        log.info("有数据插入");

        String orderSetKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(orderInfo.getSeckillId()));
        redisTemplate.opsForSet().add(orderSetKey, String.valueOf(orderInfo.getUserId()));
        log.info("加入set集合成功");
        //创建好的订单对象，存储到redis的hash结构
        String key = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        redisTemplate.opsForHash().put(key, orderInfo.getOrderNo(), JSON.toJSONString(orderInfo));
    }

    @Override
    public void update(OrderInfo before, OrderInfo after) {
        log.info("有数据更新");
        //创建好的订单对象，存储到redis的hash结构
        String key = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        redisTemplate.opsForHash().put(key, after.getOrderNo(), JSON.toJSONString(after));

    }

    @Override
    public void delete(OrderInfo orderInfo) {
        log.info("有数据删除");
    }

}
