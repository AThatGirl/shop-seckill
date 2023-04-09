package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.ProductFeignApi;

import java.util.List;

public class ProductFeignFallback implements ProductFeignApi {

    @Override
    public Result<List<Product>> queryByIds(List<Long> productIds) {
        //返回兜底数据
        return null;
    }
}
