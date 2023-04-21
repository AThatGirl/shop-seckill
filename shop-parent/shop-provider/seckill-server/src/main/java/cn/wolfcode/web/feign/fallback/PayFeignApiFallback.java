package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import cn.wolfcode.web.feign.PayFeignApi;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PayFeignApiFallback
 * description:
 * 2023/4/21 20:18
 * Create by 杰瑞
 */
public class PayFeignApiFallback implements PayFeignApi {
    @Override
    public Result<String> payOnline(PayVo vo) {
        return null;
    }

    @Override
    public Result<Boolean> rsaCheckV1(Map<String, String> params) {
        return null;
    }

    @Override
    public Result<Boolean> refund(RefundVo vo) {
        return null;
    }
}
