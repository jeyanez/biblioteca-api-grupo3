package ec.edu.espe.bibliotecaapi.exception;

public class LibroNoEncontradoException extends RecursoNoEncontradoException {
    public LibroNoEncontradoException(Long id) {
        super("No existe el libro con id " + id);
    }
}
