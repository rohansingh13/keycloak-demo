package org.example.keycloakdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class KeycloakDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeycloakDemoApplication.class, args);
    }

}
