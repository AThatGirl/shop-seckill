package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.web.feign.IntegralFeignApi;

/**
 * IntegralFeignFallback
 * description:
 * 2023/4/22 10:51
 * Create by 杰瑞
 */
public class IntegralFeignFallback implements IntegralFeignApi {
    @Override
    public Result decrIntegral(OperateIntergralVo vo) {
        return null;
    }

    @Override
    public Result incrIntegral(OperateIntergralVo vo) {
        return null;
    }
}
