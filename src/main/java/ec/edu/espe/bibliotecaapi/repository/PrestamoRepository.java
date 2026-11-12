package ec.edu.espe.bibliotecaapi.repository;

import ec.edu.espe.bibliotecaapi.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    // Consulta personalizada 2: Buscar préstamos por ID de usuario
    List<Prestamo> findByUsuarioId(Long usuarioId);
}
