package com.bytemall.bytemall.service;

public interface SeckillService {

    /**
     * 将未来3天内需要参与秒杀的商品上架到Redis缓存
     */
    void uploadSeckillSkuLatest3Days();

    //返回订单号
    String kill(Long skuId, Long userId) throws Exception;

}
