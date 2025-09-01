package com.bytemall.bytemall.controller;


import com.bytemall.bytemall.service.SeckillService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    /**
     * 手动触发秒杀商品上架。
     * 这是一个用于开发和测试的接口。
     * @return 成功信息
     */
    @GetMapping("/seckill/upload")
    public String uploadSeckillSkus() {
        seckillService.uploadSeckillSkuLatest3Days();
        return "秒杀商品上架成功";
    }


    /**
     * 执行秒杀操作
     * @param skuId 正在秒杀的商品ID
     * @return 成功信息或错误信息
     */
//    @GetMapping("/seckill/{skuId}")
//    public String seckill(@PathVariable("skuId") Long skuId) {
//        // 【重要】在真实业务中，我们需要获取当前登录的用户ID
//        // String userId = ... (从SecurityContextHolder或Token中获取)
//        // 这里为了简化测试，我们先硬编码一个用户ID
//        Long userId = 1961594574163058689L; // 假设这是 "wangwu" 的ID
//
//        try {
//            // 调用Service层执行秒杀逻辑
//            String orderSn = seckillService.kill(skuId, userId);
//            return "秒杀成功！订单号：" + orderSn;
//        } catch (Exception e) {
//            // 捕获业务异常并返回给前端
//            return "秒杀失败：" + e.getMessage();
//        }
//    }

    @GetMapping("/seckill/{skuId}")
    // 将userId改为通过@RequestParam从URL参数获取
    public String seckill(@PathVariable("skuId") Long skuId,
                          @RequestParam("userId") Long userId) {
        try {
            String orderSn = seckillService.kill(skuId, userId);
            return "秒杀成功！订单号：" + orderSn;
        } catch (Exception e) {
            return "秒杀失败：" + e.getMessage();
        }
    }

}
