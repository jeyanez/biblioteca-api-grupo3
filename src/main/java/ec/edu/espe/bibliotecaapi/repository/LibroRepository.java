package ec.edu.espe.bibliotecaapi.repository;

import ec.edu.espe.bibliotecaapi.entity.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long> {
    // Consulta personalizada 1: Libros con stock mayor a cero
    List<Libro> findByStockDisponibleGreaterThan(int stock);
}