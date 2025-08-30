package com.bytemall.bytemall.dto;

import lombok.Data;

@Data
public class MemberUpdateDTO {
    // 假设我们只允许用户更新这几个字段
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    private Integer gender;
}
