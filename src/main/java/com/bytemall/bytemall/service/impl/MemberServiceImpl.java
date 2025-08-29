package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.dto.LoginDTO;
import com.bytemall.bytemall.entity.Member;
import com.bytemall.bytemall.mapper.MemberMapper;
import com.bytemall.bytemall.service.MemberService;
import com.bytemall.bytemall.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Resource
    private PasswordEncoder passwordEncoder; // 注入我们在SecurityConfig中定义的加密器

    @Override
    public boolean save(Member entity) {
        // 1. 从实体中获取原始密码
        String rawPassword = entity.getPassword();
        // 2. 使用加密器对密码进行加密
        String encodedPassword = passwordEncoder.encode(rawPassword);
        // 3. 将加密后的密码设置回实体
        entity.setPassword(encodedPassword);

        // 4. 调用父类的save方法，将包含加密密码的实体存入数据库
        return super.save(entity);
    }

    @Resource // <-- 新增！注入JwtUtil的实例
    private JwtUtil jwtUtil;

    @Override
    public String login(LoginDTO loginDTO) {
        // 1. 根据用户名查询数据库中的用户
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Member::getUsername, loginDTO.getUsername());
        Member member = this.getOne(queryWrapper); // this.getOne是IService提供的方法

        // 2. 判断用户是否存在
        if (member == null) {
            // 实际项目中应抛出自定义异常，例如 "用户不存在"
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 关键：比对密码
        // 第一个参数是用户输入的原始密码，第二个参数是数据库中存储的加密密码
        boolean isMatch = passwordEncoder.matches(loginDTO.getPassword(), member.getPassword());

        // 4. 判断密码是否匹配
        if (!isMatch) {
            // 实际项目中应抛出自定义异常，例如 "密码错误"
            throw new RuntimeException("用户名或密码错误");
        }

        // 5. 登录成功，返回用户信息（注意：返回前应该脱敏，比如把密码设为null）
        member.setPassword(null);
        return jwtUtil.generateToken(member.getUsername(), member.getId());
    }
}
