package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.web.feign.fallback.IntegralFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * IntegralFeignApi
 * description:
 * 2023/4/22 10:49
 * Create by 杰瑞
 */
@FeignClient(name = "intergral-service", fallback = IntegralFeignFallback.class)
public interface IntegralFeignApi {


    @RequestMapping("/intergral/decrIntegral")
    Result decrIntegral(@RequestBody OperateIntergralVo vo);


    @RequestMapping("/intergral/incrIntegral")
    Result incrIntegral(@RequestBody OperateIntergralVo vo);
}
