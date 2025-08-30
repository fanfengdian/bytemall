package com.bytemall.bytemall.controller;

import com.bytemall.bytemall.dto.LoginDTO;
import com.bytemall.bytemall.dto.MemberUpdateDTO;
import com.bytemall.bytemall.entity.Member;
import com.bytemall.bytemall.service.MemberService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {


    @Resource
    private MemberService memberService;

    @GetMapping("/list")
    public List<Member> listMembers() {
        return memberService.list(); // 调用Service层的方法查询所有用户
    }

    @PostMapping("/register")
    public Member register(@RequestBody Member member) {
        // @RequestBody 注解告诉Spring，从请求的JSON体中获取数据，并转换成一个Member对象

        // 调用Service的save方法，MyBatis-Plus已经帮我们实现好了
        boolean success = memberService.save(member);

        if (success) {
            // 如果保存成功，返回刚创建的这个member对象（此时它已经有了数据库生成的id）
            return member;
        } else {
            // 实际项目中会进行更复杂的错误处理
            return null;
        }
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginDTO loginDTO) {
        return memberService.login(loginDTO);
    }

    @PutMapping("/{id}")
    public boolean updateMember(@PathVariable Long id, @RequestBody MemberUpdateDTO updateDTO) {
        // **安全提示：** 在真实项目中，应该从JWT Token中解析出用户ID
        // 来防止用户A通过接口更新用户B的信息。
        // 比如：Long currentUserId = jwtUtil.extractMemberId(token);
        // if (!id.equals(currentUserId)) { throw new AccessDeniedException("无权操作"); }

        return memberService.updateMemberInfo(id, updateDTO);
    }

    @DeleteMapping("/{id}")
    public boolean deleteMember(@PathVariable Long id) {
        // 由于配置了逻辑删除，这个removeById方法执行的不再是DELETE语句
        // 而是 UPDATE user_member SET is_deleted=1 WHERE id=? AND is_deleted=0
        return memberService.removeById(id);
    }


}
