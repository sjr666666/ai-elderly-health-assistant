package com.example.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionUsesBusinessCodeAndDoesNotExposeStackTrace() {
        ResponseEntity<ResponseResult<Void>> response = handler.handleBusiness(
                new BusinessException(ResponseCode.PARAM_ERROR, "invalid request"));

        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        assertEquals("invalid request", response.getBody().getMessage());
    }

    @Test
    void unexpectedExceptionUsesGenericMessage() {
        ResponseEntity<ResponseResult<Void>> response = handler.handleUnexpected(
                new IllegalStateException("database password"));

        assertEquals(500, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(ResponseCode.INTERNAL_ERROR.getMessage(), response.getBody().getMessage());
    }
}
