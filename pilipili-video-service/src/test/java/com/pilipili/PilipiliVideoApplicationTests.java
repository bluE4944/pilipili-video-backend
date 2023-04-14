package com.pilipili;

import com.pilipili.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * 保存admin
     */
    @Test
    public void testAddAdmin(){
        userService.saveAdmin(new BCryptPasswordEncoder(),password);
    }

    /**
     * 测试查找文件
     */
    @Test
    void testFindVideoFile(){
        // 路径
        String path = "D:\\迅雷下载";
        try (Stream<Path> paths = Files.walk(Paths.get(path))){
            List<Path> fileNames = paths
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            fileNames.stream().filter(path1 -> path1.toString().endsWith(".mp4")).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
