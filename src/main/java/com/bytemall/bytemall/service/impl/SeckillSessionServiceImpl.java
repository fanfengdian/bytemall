package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.entity.SeckillSession;
import com.bytemall.bytemall.mapper.SeckillSessionMapper;
import com.bytemall.bytemall.service.SeckillSessionService;
import org.springframework.stereotype.Service;

@Service
public class SeckillSessionServiceImpl
        extends ServiceImpl<SeckillSessionMapper, SeckillSession>
        implements SeckillSessionService {

}
