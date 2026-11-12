package ru.ifmo.soa.booking;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ifmo.soa.booking.api.ApiOriginFilter;

@Configuration
public class WebConfig {

    @Bean
    FilterRegistrationBean<ApiOriginFilter> apiOriginFilter() {
        FilterRegistrationBean<ApiOriginFilter> registration =
                new FilterRegistrationBean<>(new ApiOriginFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}
