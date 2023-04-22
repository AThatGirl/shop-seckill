package cn.wolfcode.web.controller;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.feign.PayFeignApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/orderPay")
@RefreshScope
@Slf4j
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private PayFeignApi payFeignApi;


    @RequestMapping("/pay")
    public Result<String> pay(String orderNo, Integer type) {

        if (OrderInfo.PAYTYPE_ONLINE.equals(type)) {
            //在线支付
            Result<String> result = orderInfoService.payOnline(orderNo);
            log.info("在线支付完成");
            return result;
        } else {
            //积分支付
            orderInfoService.payIntegral(orderNo);
            log.info("积分支付完成");
            return Result.success();
        }
    }

    @RequestMapping("/refund")
    public Result<String> refund(@RequestParam String orderNo){
        OrderInfo orderInfo = orderInfoService.findByOrderNo(orderNo);
        if (OrderInfo.PAYTYPE_ONLINE.equals(orderInfo.getPayType())){
            //在线支付退款
            orderInfoService.refundOnline(orderInfo);
            log.info("在线支付退款完成");
        }else {
            //积分支付退款
            orderInfoService.refundIntegral(orderInfo);
            log.info("积分退款完成");
        }
        return Result.success();
    }


    //异步回调
    @RequestMapping("/notifyUrl")
    public String notifyUrl(@RequestParam Map<String, String> params) {
        log.info("异步回调执行");
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError() || !result.getData()) {
            return "fail";
        }
        boolean signVerified = result.getData();//调用SDK验证签名
        if (signVerified) {
            //验签成功
            String orderNo = params.get("out_trade_no");
            //修改订单状态
            int effectCount = orderInfoService.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_ONLINE);
            if (effectCount == 0) {
                //说明支付成功，但是回调的时候，已经被其他请求更新支付状态(超时取消)
                //发消息通知客服，走退款流程
            }
        }

        return "success";
    }

    @Value("${pay.errorUrl}")
    private String errorUrl;
    @Value("${pay.frontEndPayUrl}")
    private String frontEndPayUrl;

    @RequestMapping("/returnUrl")
    public void returnUrl(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        log.info("同步回调执行");
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError()) {
            response.sendRedirect(errorUrl);
            return;
        }
        String orderNo = params.get("out_trade_no");
        response.sendRedirect(frontEndPayUrl + orderNo);
    }


}
