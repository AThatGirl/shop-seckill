package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.feign.ProductFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by lanxw
 */
@Service
@Slf4j
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private ProductFeignApi productFeignApi;

    @Override
    public List<SeckillProductVo> queryByTime(Integer time) {
        //查询秒杀商品集合数据（场次查询当天的数据）
        List<SeckillProduct> seckillProducts = seckillProductMapper.queryCurrentlySeckillProduct(time);
        if (seckillProducts.size() == 0) {
            return Collections.EMPTY_LIST;
        }
        //遍历秒杀商品集合数据，获取商品id
        List<Long> productIds = new ArrayList<>();
        for (SeckillProduct seckillProduct : seckillProducts) {
            productIds.add(seckillProduct.getProductId());
        }
        //远程调用，获取商品集合
        Result<List<Product>> result = productFeignApi.queryByIds(productIds);
        if (result == null || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        List<Product> productList = result.getData();
        //定义映射关系
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : productList) {
            productMap.put(product.getId(), product);
        }
        //将商品秒杀数据集合，封装Vo并返回
        List<SeckillProductVo> seckillProductVoList = new ArrayList<>();
        for (SeckillProduct seckillProduct : seckillProducts) {

            SeckillProductVo vo = new SeckillProductVo();
            Product product = productMap.get(seckillProduct.getProductId());
            BeanUtils.copyProperties(product, vo);
            BeanUtils.copyProperties(seckillProduct, vo);
            vo.setCurrentCount(seckillProduct.getStockCount());
            seckillProductVoList.add(vo);
        }

        return seckillProductVoList;
    }

    @Override
    public SeckillProductVo find(Integer time, Long seckillId) {
        //查询秒杀商品对象
        SeckillProduct seckillProduct = seckillProductMapper.find(seckillId);
        //根据id查询Product对象
        List<Long> productIds = new ArrayList<>();
        productIds.add(seckillProduct.getProductId());
        Result<List<Product>> result = productFeignApi.queryByIds(productIds);
        if (result == null || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        Product product = result.getData().get(0);
        //将数据封装为VO对象
        SeckillProductVo vo = new SeckillProductVo();
        BeanUtils.copyProperties(product, vo);
        BeanUtils.copyProperties(seckillProduct, vo);
        vo.setCurrentCount(seckillProduct.getStockCount());
        return vo;
    }

    @Override
    public int decrStockCount(Long id) {
        return seckillProductMapper.decrStock(id);
    }

    @Override
    public List<SeckillProductVo> queryByTimeFromCache(Integer time) {
        String key = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        List<Object> objectList = redisTemplate.opsForHash().values(key);
        List<SeckillProductVo> seckillProductVoList = new ArrayList<>();
        for (Object o : objectList) {
            seckillProductVoList.add(JSON.parseObject((String) o, SeckillProductVo.class));
        }
        return seckillProductVoList;
    }

    @Override
    public SeckillProductVo findFromCache(Integer time, Long seckillId) {
        String key = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        Object strObj = redisTemplate.opsForHash().get(key, String.valueOf(seckillId));
        return JSON.parseObject((String) strObj, SeckillProductVo.class);
    }

    @Override
    public void syncStockToRedis(Integer time, Long seckillId) {
        SeckillProduct seckillProduct = seckillProductMapper.find(seckillId);
        if (seckillProduct != null && seckillProduct.getStockCount() > 0) {

            String key = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            //将数据库库存同步到redis
            redisTemplate.opsForHash().put(key, String.valueOf(seckillId), String.valueOf(seckillProduct.getStockCount()));
            log.info("数据库库存已同步,商品id{},商品库存{}", seckillProduct.getProductId(), seckillProduct.getStockCount());
        }
    }

    @Override
    public void incrStockCount(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }
}
