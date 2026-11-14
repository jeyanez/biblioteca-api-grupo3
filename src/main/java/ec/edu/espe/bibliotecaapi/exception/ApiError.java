package ec.edu.espe.bibliotecaapi.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(int status, String error, String message, String path,
                       Instant timestamp, Map<String, String> errors) {
}
