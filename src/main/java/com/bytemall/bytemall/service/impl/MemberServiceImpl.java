package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.dto.LoginDTO;
import com.bytemall.bytemall.dto.MemberUpdateDTO;
import com.bytemall.bytemall.entity.Member;
import com.bytemall.bytemall.mapper.MemberMapper;
import com.bytemall.bytemall.service.MemberService;
import com.bytemall.bytemall.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder; // <-- 导入
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private PasswordEncoder passwordEncoder; // <-- 恢复注入

    @Resource
    private JwtUtil jwtUtil; // <-- 恢复注入

    @Override
    public boolean save(Member entity) {
        // --- 恢复密码加密 ---
        String rawPassword = entity.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        entity.setPassword(encodedPassword);

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
    public String login(LoginDTO loginDTO) {
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Member::getUsername, loginDTO.getUsername());
        Member member = this.getOne(queryWrapper);

        if (member == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // --- 恢复密码比对 ---
        if (!passwordEncoder.matches(loginDTO.getPassword(), member.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // --- 使用注入的jwtUtil生成Token ---
        return jwtUtil.generateToken(member.getUsername(), member.getId());
    }

    @Override
    public boolean updateMemberInfo(Long memberId, MemberUpdateDTO updateDTO) {
        Member memberToUpdate = new Member();
        BeanUtils.copyProperties(updateDTO, memberToUpdate);
        memberToUpdate.setId(memberId);
        return this.updateById(memberToUpdate);
    }
}