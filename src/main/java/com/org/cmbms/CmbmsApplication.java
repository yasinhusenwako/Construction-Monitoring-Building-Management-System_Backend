
package com.org.cmbms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CmbmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmbmsApplication.class, args);
    }
}
