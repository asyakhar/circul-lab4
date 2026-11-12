package ru.ifmo.soa.ticket.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.ifmo.soa.ticket.model.TicketPatch;

@Component
public class TicketPatchConverter
        implements Converter<String, TicketPatch> {

    private final ObjectReader reader;

    public TicketPatchConverter(ObjectMapper objectMapper) {
        this.reader = objectMapper
                .readerFor(TicketPatch.class)
                .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    @Override
    public TicketPatch convert(String source) {
        try {
            return reader.readValue(source);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Параметр ticket содержит неразбираемый JSON.",
                    exception
            );
        }
    }
}