package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.entity.Product;
import com.bytemall.bytemall.mapper.ProductMapper;
import com.bytemall.bytemall.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Product getById(Serializable id) {
        // 1. 定义缓存key
        String redisKey = "product:" + id;

        // 2. 查Redis
        String productJson = stringRedisTemplate.opsForValue().get(redisKey);

        // 3. 判断缓存是否命中
        if (productJson != null && !productJson.isEmpty()) {
            System.out.println("====== 缓存命中！从Redis读取商品信息 (ID: " + id + ") ======");
            try {
                return MAPPER.readValue(productJson, Product.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // 4. 未命中，查数据库
        System.out.println("====== 缓存未命中！查询MySQL数据库 (ID: " + id + ") ======");
        Product productFromDb = super.getById(id);

        // 5. 结果写入Redis
        if (productFromDb != null) {
            try {
                String json = MAPPER.writeValueAsString(productFromDb);
                stringRedisTemplate.opsForValue().set(redisKey, json, 10, TimeUnit.MINUTES);
                System.out.println("====== 商品信息已写入Redis缓存 (ID: " + id + ") ======");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return productFromDb;
    }

}
