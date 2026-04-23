package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", UNPROCESSABLE_ENTITY);
        body.put("error", "Unprocessable Entity");
        body.put("message", exception.getMessage());
        return Response.status(UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
