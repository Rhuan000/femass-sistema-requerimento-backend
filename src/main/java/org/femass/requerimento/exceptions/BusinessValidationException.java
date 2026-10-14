package org.femass.requerimento.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

public class BusinessValidationException extends RuntimeException {

    /** Mensagens por campo (chave = campo), para o front exibir junto de cada input. */
    private final Map<String, String> fieldErrors;

    public BusinessValidationException(String message) {
        this(message, Map.of());
    }

    public BusinessValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Map.of() : new LinkedHashMap<>(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
