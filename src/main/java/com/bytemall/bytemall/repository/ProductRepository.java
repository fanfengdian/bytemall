package com.bytemall.bytemall.repository;

import com.bytemall.bytemall.document.ProductDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface ProductRepository extends ElasticsearchRepository<ProductDoc, Long> {
    // Spring Data Elasticsearch会根据这个方法名，自动生成查询逻辑
    // 它会创建一个查询，在productName字段或description字段中，寻找匹配keyword的文档
    List<ProductDoc> findByProductNameOrDescription(String productName, String description);
}
