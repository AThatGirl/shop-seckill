package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;

/**
 * Created by lanxw
 */
public interface ISeckillProductService {
    /**
     * 查询秒杀列表的数据（当天指定的时间）
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTime(Integer time);

    /**
     * 根据秒杀场次和秒杀商品id查询秒杀商品的VO对象
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo find(Integer time, Long seckillId);

    /**
     * 扣减库存
     * @param id
     */
    void decrStockCount(Long id);

    /**
     *  从缓存中获取秒杀商品
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTimeFromCache(Integer time);

    /**
     * 从缓存中获取秒杀商品详情
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo findFromCache(Integer time, Long seckillId);
}
