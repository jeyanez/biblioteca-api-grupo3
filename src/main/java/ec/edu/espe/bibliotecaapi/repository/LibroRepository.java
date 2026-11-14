package ec.edu.espe.bibliotecaapi.repository;

import ec.edu.espe.bibliotecaapi.entity.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long> {
    // Consulta personalizada 1: Libros con stock mayor a cero
    List<Libro> findByStockDisponibleGreaterThan(int stock);

    boolean existsByIsbn(String isbn);
    boolean existsByIsbnAndIdNot(String isbn, Long id);
    boolean existsByAutorId(Long autorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id = :id")
    Optional<Libro> findByIdForUpdate(@Param("id") Long id);
}
