package com.bytemall.bytemall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_member")
public class Member {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    private Integer gender;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
