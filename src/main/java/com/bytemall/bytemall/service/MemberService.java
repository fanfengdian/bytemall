package com.bytemall.bytemall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bytemall.bytemall.dto.LoginDTO;
import com.bytemall.bytemall.entity.Member;

public interface MemberService extends IService<Member> {

    // 返回登录成功的Member对象，如果失败则可以抛出异常或返回null
    Member login(LoginDTO loginDTO);

}
