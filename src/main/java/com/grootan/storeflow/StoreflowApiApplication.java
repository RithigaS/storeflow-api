package com.grootan.storeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StoreflowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreflowApiApplication.class, args);
    }

}
