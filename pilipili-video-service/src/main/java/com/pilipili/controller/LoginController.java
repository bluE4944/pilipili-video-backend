package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.out.Result;
import com.pilipili.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.web.servlet.oauth2.login.OAuth2LoginSecurityMarker;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;


/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 23:03
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LoginController {

    private final UserService userService;

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.build("hello 江南一点雨!");
    }

    @PostMapping("/login")
    public Result<String> login(String username, String password, HttpServletRequest req) {
        try {
            UserDetails userDetails = userService.loadUserByUsername(username);
            if(userDetails.getPassword().equals(password)){
            }

        }catch (Exception e){
            e.printStackTrace();

        }

        return Result.build("hello 江南一点雨!");
    }
    @PostMapping("/doLogin")
    public Result<String> doLogin() {
        return Result.build("hello 江南一点雨!");
    }

    public Result<String> processRegistration(){
        return Result.build();
    }
}
