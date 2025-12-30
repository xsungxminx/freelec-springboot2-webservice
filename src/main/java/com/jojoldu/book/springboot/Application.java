package com.jojoldu.book.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing      // JPA Auditing 활성화
//@SpringBootApplication
@SpringBootApplication(scanBasePackages = {"com.jojoldu.book.springboot", "egovframework"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}