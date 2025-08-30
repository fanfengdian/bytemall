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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    @Resource
    private RabbitTemplate rabbitTemplate; // 注入Spring封装好的RabbitMQ操作模板
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
        boolean result = super.save(entity);

        // --- 第三部分：新增的异步消息发送 ---
        // 5. 检查数据库操作是否成功
        if (result) {
            System.out.println("====== 用户数据已存入数据库 (ID: " + entity.getId() + ") ======");

            // 6. 发送一条"新用户注册"的消息到MQ
            // 我们需要把新生成的用户ID发送出去，以便消费者知道该为哪个用户服务
            // 注意：super.save(entity)执行后，MyBatis-Plus会自动将数据库生成的ID回填到entity对象中
            try {
                rabbitTemplate.convertAndSend("member.exchange", "member.registered", entity.getId());
                System.out.println("====== '新用户注册' 消息已发送到RabbitMQ (用户ID: " + entity.getId() + ") ======");
            } catch (Exception e) {
                // 在真实项目中，这里应该有更健壮的错误处理或重试机制
                // 比如，如果MQ发送失败，可以将消息存入数据库的一张"待发送消息表"中，由定时任务补偿发送
                System.err.println("发送MQ消息失败: " + e.getMessage());
            }
        }

        // 7. 返回数据库操作的结果
        return result;


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


    @Override
    public boolean updateMemberInfo(Long memberId, MemberUpdateDTO updateDTO) {
        // 1. 创建一个待更新的Member实体对象
        Member memberToUpdate = new Member();

        // 2. 使用Spring的工具类，将DTO中的属性值拷贝到实体对象中
        // 这是一种常见的、优雅的DTO到Entity的转换方式
        // 它会自动匹配同名、同类型的属性进行拷贝
        BeanUtils.copyProperties(updateDTO, memberToUpdate);

        // 3. 设置要更新的记录的ID
        memberToUpdate.setId(memberId);

        // 4. 调用MyBatis-Plus的updateById方法
        // 这个方法非常智能，它只会更新memberToUpdate中不为null的字段
        return this.updateById(memberToUpdate);
    }
}
