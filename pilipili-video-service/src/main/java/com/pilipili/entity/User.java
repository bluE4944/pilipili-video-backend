package com.pilipili.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 22:23
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_user")
public class User extends BaseEntity{


    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nikeName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话
     */
    private String phone;

    /**
     * 性别
     */
    private String sex;

    /**
     * 角色
     */
    private String role;

    /**
     * 授权
     */
    private String authorization;

}
