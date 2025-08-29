package com.bytemall.bytemall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField(fill = FieldFill.INSERT) // 声明在插入时填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 声明在插入和更新时都填充
    private LocalDateTime updateTime;
}
