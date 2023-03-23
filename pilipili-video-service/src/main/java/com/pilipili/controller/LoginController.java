package com.pilipili.controller;

import com.pilipili.entity.User;
import com.pilipili.entity.out.Result;
import com.pilipili.utils.RSAUtil;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;

import static com.pilipili.utils.Status.SYSTEM_ERROR;


/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 23:03
 */
@Slf4j
@RestController
@Api(value = "用户控制器",tags = "用户控制器")
@RequestMapping("/user")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LoginController {

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.build("hello 江南一点雨!");
    }

    @PostMapping("/saveUser")
    public Result<String> processRegistration(@RequestBody User user){
        //先用非对称算法RSA解密一下
        try {
            //从header里获取到参数
            String username = user.getUsername();
            log.debug("收到userId，内容为："+username);


            //这里解密，注意先用URLDecode处理了下，如果前端没有用的话，这里也不用处理
            username = RSAUtil.decrypt(URLDecoder.decode(username,"UTF-8"), RSAUtil.private_key);


            log.debug("RSA解密成功，userId为"+username);

        } catch (Exception e) {
            log.error("RSA解密失败",e);
            //如果解密失败，就返回null
            return Result.build(SYSTEM_ERROR);
        }

        return Result.build("成功");
    }
}
