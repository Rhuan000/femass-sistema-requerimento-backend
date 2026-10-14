package org.femass.requerimento.dtos;

import java.time.Instant;
import java.util.Map;

public class ErrorDTO {

    public int status;
    public String error;
    public String message;
    public String path;
    public Instant timestamp;
    /** Mensagens por campo; ausente quando o erro não é de um campo específico. */
    public Map<String, String> fieldErrors;

    public ErrorDTO(
            int status,
            String error,
            String message,
            String path
    ) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
    }
}
