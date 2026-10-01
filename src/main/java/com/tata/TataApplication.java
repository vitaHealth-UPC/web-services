package com.tata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TataApplication {

    public static void main(String[] args) {
        SpringApplication.run(TataApplication.class, args);
    }
}
