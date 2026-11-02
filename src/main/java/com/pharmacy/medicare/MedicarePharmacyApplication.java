package com.pharmacy.medicare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicarePharmacyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicarePharmacyApplication.class, args);
    }
}
