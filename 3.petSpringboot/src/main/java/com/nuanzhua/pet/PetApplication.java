package com.nuanzhua.pet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PetApplication {
    /** 启动Spring Boot应用。 */
    public static void main(String[] args) { SpringApplication.run(PetApplication.class, args); }
}
