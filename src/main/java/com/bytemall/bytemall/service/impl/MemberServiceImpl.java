package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.dto.LoginDTO;
import com.bytemall.bytemall.dto.MemberUpdateDTO;
import com.bytemall.bytemall.entity.Member;
import com.bytemall.bytemall.mapper.MemberMapper;
import com.bytemall.bytemall.service.MemberService;
import com.bytemall.bytemall.utils.JwtUtil; // 导入JwtUtil
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    // 我们暂时禁用了Security，所以也不能注入JwtUtil了，需要手动创建
    // @Resource
    // private JwtUtil jwtUtil;

    @Override
    public boolean save(Member entity) {
        // 直接保存原始密码
        boolean result = super.save(entity);
        if (result) {
            try {
                rabbitTemplate.convertAndSend("member.exchange", "member.registered", entity.getId());
                System.out.println("====== '新用户注册' 消息已发送到RabbitMQ (用户ID: " + entity.getId() + ") ======");
            } catch (Exception e) {
                System.err.println("发送MQ消息失败: " + e.getMessage());
            }
        }
        return result;
    }

    @Override
    public String login(LoginDTO loginDTO) { // <-- 返回值修改为 String
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Member::getUsername, loginDTO.getUsername());
        Member member = this.getOne(queryWrapper);

        if (member == null || !loginDTO.getPassword().equals(member.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // --- 临时方案：手动new一个JwtUtil来生成Token ---
        // 注意：这里的密钥和过期时间是硬编码的，后续恢复Security时要改回从配置读取
        JwtUtil tempJwtUtil = new JwtUtil("ByteMallSecretKeyForJwtTokenGenerationAndValidationSpecial", 86400000L);
        return tempJwtUtil.generateToken(member.getUsername(), member.getId());
    }

    @Override // <-- 新增！实现updateMemberInfo方法
    public boolean updateMemberInfo(Long memberId, MemberUpdateDTO updateDTO) {
        Member memberToUpdate = new Member();
        BeanUtils.copyProperties(updateDTO, memberToUpdate);
        memberToUpdate.setId(memberId);
        return this.updateById(memberToUpdate);
    }
}