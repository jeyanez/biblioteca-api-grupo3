package ec.edu.espe.bibliotecaapi.exception;

public class SinEjemplaresException extends ReglaNegocioException {
    public SinEjemplaresException(Long libroId) {
        super("El libro con id " + libroId + " no tiene ejemplares disponibles");
    }
}
