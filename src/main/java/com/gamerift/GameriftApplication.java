package com.gamerift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Gamerift backend.
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
 * It scans this package (com.gamerift) and every sub-package, so all our @RestController,
 * @Service, @Repository and @Configuration classes are found automatically.
 */
@SpringBootApplication
public class GameriftApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameriftApplication.class, args);
    }
}
