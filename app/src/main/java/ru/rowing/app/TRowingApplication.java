package ru.rowing.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа веб-приложения системы управления соревнованиями по гребле. */
@SpringBootApplication
public class TRowingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TRowingApplication.class, args);
    }
}
