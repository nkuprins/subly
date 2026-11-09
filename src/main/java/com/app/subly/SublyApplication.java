package com.app.subly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SublyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SublyApplication.class, args);
    }
}
