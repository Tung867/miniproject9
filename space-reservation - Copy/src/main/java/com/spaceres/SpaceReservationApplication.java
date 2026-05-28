package com.spaceres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Google Drive 자동 백업 스케줄링 활성화
public class SpaceReservationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpaceReservationApplication.class, args);
    }
}
