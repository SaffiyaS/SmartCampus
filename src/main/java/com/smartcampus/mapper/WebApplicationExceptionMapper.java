package com.smartcampus.mapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns standard JAX-RS errors (415 Unsupported Media Type, 405 Method Not
 * Allowed, etc.) into JSON. Without this, Tomcat serves its default HTML
 * error page, which breaks the "API returns JSON" contract.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        // NotFoundException is a subclass of WebApplicationException. JAX-RS
        // should pick NotFoundExceptionMapper when both are registered, but
        // if this mapper is ever chosen first, delegating here guarantees
        // 404 responses stay consistent with the dedicated mapper.
        if (exception instanceof NotFoundException) {
            return new NotFoundExceptionMapper().toResponse((NotFoundException) exception);
        }

        Response original = exception.getResponse();
        int status = original != null
                ? original.getStatus()
                : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        Response.Status reason = Response.Status.fromStatusCode(status);
        String error = reason != null ? reason.getReasonPhrase() : "Error";

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = error;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
