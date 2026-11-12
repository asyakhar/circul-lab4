package ru.ifmo.soa.booking;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;
import ru.ifmo.soa.booking.api.PersonApi;
import ru.ifmo.soa.booking.api.SellApi;
import ru.ifmo.soa.booking.service.BadRequestExceptionMapper;
import ru.ifmo.soa.booking.service.ConstraintViolationExceptionMapper;
import ru.ifmo.soa.booking.service.NumericParamConverterProvider;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(PersonApi.class);
        register(SellApi.class);
        register(BadRequestExceptionMapper.class);
        register(ConstraintViolationExceptionMapper.class);
        register(NumericParamConverterProvider.class);
        register(JacksonConfig.class);
    }
}
