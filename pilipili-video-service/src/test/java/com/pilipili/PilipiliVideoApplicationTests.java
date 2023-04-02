package com.pilipili;

import com.pilipili.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 测试程序
 * @author Li
 * @version 1.0
 * @ClassName PilipiliVideoApplicationTests
 * @Description
 * @since 2023/3/8 17:53
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PilipiliVideoApplication.class)
@Slf4j
public class PilipiliVideoApplicationTests {
    @Autowired
    private UserService userService;

    @Value("${pilipili.user.admin.password}")
    private String password;

    @Test
    public void testAddAdmin(){
        userService.saveAdmin(password);
    }
}
