package ec.edu.espe.bibliotecaapi.repository;

import ec.edu.espe.bibliotecaapi.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}