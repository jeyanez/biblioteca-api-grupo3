package ec.edu.espe.bibliotecaapi.exception;

public class IsbnDuplicadoException extends ReglaNegocioException {
    public IsbnDuplicadoException(String isbn) {
        super("Ya existe un libro con ISBN " + isbn);
    }
}
