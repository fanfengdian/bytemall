package com.bytemall.bytemall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bytemall.bytemall")
@MapperScan("com.bytemall.bytemall.mapper")
public class BytemallApplication {

    public static void main(String[] args) {

        SpringApplication.run(BytemallApplication.class, args);
    }

}
