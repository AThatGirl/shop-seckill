package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.web.feign.SeckillProductFeignApi;

import java.util.List;

/**
 * SeckillProductFeignFallback
 * description:
 * 2023/4/19 16:31
 * Create by 杰瑞
 */

public class SeckillProductFeignFallback implements SeckillProductFeignApi {
    @Override
    public Result<List<SeckillProductVo>> queryByTimeForJob(Integer time) {
        return null;
    }
}
