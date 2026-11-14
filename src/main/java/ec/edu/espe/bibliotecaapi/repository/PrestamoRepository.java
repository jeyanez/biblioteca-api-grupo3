package ec.edu.espe.bibliotecaapi.repository;

import ec.edu.espe.bibliotecaapi.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    // Consulta personalizada 2: Buscar préstamos por ID de usuario
    List<Prestamo> findByUsuarioId(Long usuarioId);

    List<Prestamo> findByEstado(String estado);
    boolean existsByLibroId(Long libroId);
    boolean existsByUsuarioId(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.id = :id")
    Optional<Prestamo> findByIdForUpdate(@Param("id") Long id);
}
