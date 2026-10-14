package org.femass.requerimento.exceptions;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.femass.requerimento.dtos.ErrorDTO;

@Provider
public class AuthorizationExceptionMapper
        implements ExceptionMapper<AuthorizationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AuthorizationException exception) {

        ErrorDTO error = new ErrorDTO(
                Response.Status.FORBIDDEN.getStatusCode(),
                "Forbidden",
                exception.getMessage(),
                uriInfo.getPath()
        );

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
