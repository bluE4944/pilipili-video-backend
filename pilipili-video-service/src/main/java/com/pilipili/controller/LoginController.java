package com.pilipili.controller;

import com.pilipili.entity.out.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 23:03
 */
@RestController("user")
public class LoginController {

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.build("hello 江南一点雨!");
    }

    public Result<String> processRegistration(){
        return Result.build();
    }
}
