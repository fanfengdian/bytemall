package com.bytemall.bytemall.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.math.BigDecimal;

@Data
@Document(indexName = "bytemall_product") // 定义ES中的索引名叫 "bytemall_product"
public class ProductDoc {

    @Id
    private Long id; // 文档ID，对应商品ID

    // productName字段用于全文检索，需要分词
    // 我们先指定使用ik_max_word分词器，下一步就去安装它
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String productName;

    // description字段也用于全文检索
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    // price和stock不用于全文检索，主要用于精确过滤、排序或聚合
    // FieldType.Double 和 FieldType.Integer 是合适的类型
    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;
}
