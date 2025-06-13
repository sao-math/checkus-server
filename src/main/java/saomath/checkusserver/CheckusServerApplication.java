package saomath.checkusserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CheckusServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckusServerApplication.class, args);
    }

}
