package ru.ifmo.soa.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = {
        "ru.ifmo.soa.ticket.service",
        "ru.ifmo.soa.ticket.soap"
})
public class OpenAPISpringBoot extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(OpenAPISpringBoot.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(OpenAPISpringBoot.class, args);
    }
}
