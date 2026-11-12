package ru.ifmo.soa.booking.service;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class NumericParamConverterProvider implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(
            Class<T> rawType,
            Type genericType,
            Annotation[] annotations
    ) {
        if (rawType.equals(Long.class)) {
            return (ParamConverter<T>) converter(Long::valueOf);
        }
        if (rawType.equals(Double.class)) {
            return (ParamConverter<T>) converter(Double::valueOf);
        }
        return null;
    }

    private <T> ParamConverter<T> converter(Parser<T> parser) {
        return new ParamConverter<>() {
            @Override
            public T fromString(String value) {
                try {
                    return parser.parse(value);
                } catch (NumberFormatException exception) {
                    throw new BadRequestException(
                            "Некорректный числовой параметр.",
                            exception
                    );
                }
            }

            @Override
            public String toString(T value) {
                return String.valueOf(value);
            }
        };
    }

    @FunctionalInterface
    private interface Parser<T> {
        T parse(String value);
    }
}
