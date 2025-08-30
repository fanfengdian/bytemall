package com.bytemall.bytemall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("pms_product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productName;
    private String description;
    private BigDecimal price;
    private Integer stock;

    @TableLogic
    private Integer isDeleted;
}
