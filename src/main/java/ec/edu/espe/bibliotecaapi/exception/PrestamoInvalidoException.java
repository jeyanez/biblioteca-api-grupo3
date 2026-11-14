package ec.edu.espe.bibliotecaapi.exception;

public class PrestamoInvalidoException extends ReglaNegocioException {
    public PrestamoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
