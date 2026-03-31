package com.foodya.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodyaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodyaBackendApplication.class, args);
    }
}
