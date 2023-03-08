package com.pilipili;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PilipiliVideoApplicationTests {
}
