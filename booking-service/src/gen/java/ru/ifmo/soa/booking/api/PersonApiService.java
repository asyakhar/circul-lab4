package ru.ifmo.soa.booking.api;

import ru.ifmo.soa.booking.api.*;
import ru.ifmo.soa.booking.model.*;

import ru.ifmo.soa.booking.model.ErrorResponse;

import java.util.List;
import java.util.Map;
import ru.ifmo.soa.booking.api.NotFoundException;

import java.io.InputStream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaResteasyServerCodegen", date = "2026-09-29T15:09:58.593749+03:00[Europe/Moscow]")
public interface PersonApiService {
      Response cancelPersonBookings(Long personId,SecurityContext securityContext) throws NotFoundException;
}
