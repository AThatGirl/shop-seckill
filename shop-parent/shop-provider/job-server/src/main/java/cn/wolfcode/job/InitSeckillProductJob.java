package cn.wolfcode.job;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.redis.JobRedisKey;
import cn.wolfcode.web.feign.SeckillProductFeignApi;
import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * InitSeckillProductJob
 * description:
 * 2023/4/19 11:15
 * Create by 杰瑞
 */

@Component
@Data
public class InitSeckillProductJob implements SimpleJob {

    @Value("${jobCron.initSeckillProduct}")
    private String cron;

    @Autowired
    private SeckillProductFeignApi seckillProductFeignApi;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void execute(ShardingContext shardingContext) {

        //远程调用秒杀服务，获取秒杀列表集合
        String time = shardingContext.getShardingParameter();
        Result<List<SeckillProductVo>> result = seckillProductFeignApi.queryByTimeForJob(Integer.parseInt(shardingContext.getShardingParameter()));
        if (result == null || result.hasError()){
            //通知管理员
            return;
        }
        List<SeckillProductVo> seckillProductVoList = result.getData();
        //删除之前的数据
        //seckillProductHash:10
        String key = JobRedisKey.SECKILL_PRODUCT_HASH.getRealKey(time);
        //库存数量key
        String seckillCountKey = JobRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(time);
        redisTemplate.delete(key);
        redisTemplate.delete(seckillCountKey);
        //存储集合数据到redis中
        for (SeckillProductVo vo : seckillProductVoList) {
            redisTemplate.opsForHash().put(key, String.valueOf(vo.getId()), JSON.toJSONString(vo));
            //将库存同步到redis中
            redisTemplate.opsForHash().put(seckillCountKey, String.valueOf(vo.getId()), String.valueOf(vo.getStockCount()));
        }


    }
}
