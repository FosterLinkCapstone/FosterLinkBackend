package net.fosterlink.fosterlinkbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FosterLinkBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FosterLinkBackendApplication.class, args);
    }

}
