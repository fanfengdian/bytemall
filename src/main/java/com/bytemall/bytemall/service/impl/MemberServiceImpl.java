package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.entity.Member;
import com.bytemall.bytemall.mapper.MemberMapper;
import com.bytemall.bytemall.service.MemberService;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

}
