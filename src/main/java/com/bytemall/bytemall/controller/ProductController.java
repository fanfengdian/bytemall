package com.bytemall.bytemall.controller;


import com.bytemall.bytemall.entity.Product;
import com.bytemall.bytemall.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Resource
    private ProductService productService;

    /**
     * 根据ID查询商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getById(id);
    }

    /**
     * 创建新商品（用于我们测试造数据）
     * @param product 商品信息
     * @return 是否成功
     */
    @PostMapping
    public boolean createProduct(@RequestBody Product product) {
        return productService.save(product);
    }
}
