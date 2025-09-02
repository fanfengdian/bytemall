package com.bytemall.bytemall.controller;

import com.bytemall.bytemall.entity.Member;
import com.bytemall.bytemall.service.MemberService;
import com.bytemall.bytemall.service.SeckillService;
import com.bytemall.bytemall.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    // 我们可能需要MemberService来根据用户名查ID
    @Resource
    private MemberService memberService;

    @Resource
    private JwtUtil jwtUtil; // 我们暂时用它来解析ID，更优方案是直接在Token里存ID

    @GetMapping("/seckill/upload")
    public String uploadSeckillSkus() {
        seckillService.uploadSeckillSkuLatest3Days();
        return "秒杀商品上架成功";
    }

    @GetMapping("/seckill/{skuId}")
    public String seckill(@PathVariable("skuId") Long skuId) {

        // --- 恢复从安全上下文获取用户信息 ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "秒杀失败：请先登录";
        }
        String username = authentication.getName();

        // 【关键逻辑】通过用户名查询用户ID
        // 注意：这会产生一次额外的数据库查询，是性能瓶颈。
        // 更优的方案是：1. 在JWT的payload里直接存放userId。2. 在JwtAuthenticationFilter里把userId作为principal。
        // 我们先实现功能，后续再优化。
        Member member = memberService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.bytemall.bytemall.entity.Member>().eq(com.bytemall.bytemall.entity.Member::getUsername, username));
        if (member == null) {
            return "秒杀失败：用户不存在";
        }
        Long userId = member.getId();

        try {
            String orderSn = seckillService.kill(skuId, userId);
            return "秒杀成功！订单号：" + orderSn;
        } catch (Exception e) {
            return "秒杀失败：" + e.getMessage();
        }
    }
}
