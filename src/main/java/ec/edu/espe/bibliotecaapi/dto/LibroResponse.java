package ec.edu.espe.bibliotecaapi.dto;

public record LibroResponse(Long id, String titulo, String isbn, Integer stockDisponible, Long autorId) {
}
