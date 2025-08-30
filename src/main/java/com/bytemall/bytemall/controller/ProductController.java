package com.bytemall.bytemall.controller;


import com.bytemall.bytemall.document.ProductDoc;
import com.bytemall.bytemall.entity.Product;
import com.bytemall.bytemall.repository.ProductRepository;
import com.bytemall.bytemall.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Resource
    private ProductService productService;

    @Resource
    private ElasticsearchOperations elasticsearchOperations; // 使用接口，更规范

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


    /**
     * 将MySQL中的所有商品数据导入到Elasticsearch中
     * 这是一个一次性的、用于开发测试的接口
     */
    @PostMapping("/import-es")
    public String importAllToEs() {
        // 1. 从数据库查询所有商品
        List<Product> productList = productService.list();
        if (productList == null || productList.isEmpty()) {
            return "数据库中没有商品可导入";
        }

        // 2. 将Product列表转换为ProductDoc列表
        List<ProductDoc> productDocs = productList.stream().map(product -> {
            ProductDoc doc = new ProductDoc();
            BeanUtils.copyProperties(product, doc);
            return doc;
        }).collect(Collectors.toList());

        // 3. 批量保存到ES
        elasticsearchOperations.save(productDocs);

        return "成功导入 " + productDocs.size() + " 条商品数据到ES";
    }


    @Resource
    private ProductRepository productRepository; // 注入Repository

    /**
     * 根据关键词搜索商品
     * @param keyword 搜索关键词
     * @return 匹配的商品列表
     */
    @GetMapping("/search")
    public List<ProductDoc> searchProducts(@RequestParam("keyword") String keyword) {
        // 调用Repository的方法，Spring Data会自动帮我们实现复杂的查询
        return productRepository.findByProductNameOrDescription(keyword, keyword);
    }
}
