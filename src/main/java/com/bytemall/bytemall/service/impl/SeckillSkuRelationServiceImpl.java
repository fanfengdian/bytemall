package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.entity.SeckillSkuRelation;
import com.bytemall.bytemall.mapper.SeckillSkuRelationMapper;
import com.bytemall.bytemall.service.SeckillSkuRelationService;
import org.springframework.stereotype.Service;

@Service
public class SeckillSkuRelationServiceImpl
      extends ServiceImpl<SeckillSkuRelationMapper, SeckillSkuRelation>
      implements SeckillSkuRelationService {
}
