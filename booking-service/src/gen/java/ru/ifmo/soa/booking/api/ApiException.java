package ru.ifmo.soa.booking.api;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaResteasyServerCodegen", date = "2026-09-29T15:09:58.593749+03:00[Europe/Moscow]")
public class ApiException extends Exception{
    private int code;
    public ApiException (int code, String msg) {
        super(msg);
        this.code = code;
    }
}
