package com.pilipili;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 启动程序
 * @author Li
 */
@SpringBootApplication
@MapperScan({
        "com.pilipili.mapper"
})
public class PilipiliVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PilipiliVideoApplication.class);
    }

}
