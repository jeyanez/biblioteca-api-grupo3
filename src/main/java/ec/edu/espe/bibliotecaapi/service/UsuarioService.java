package ec.edu.espe.bibliotecaapi.service;

import ec.edu.espe.bibliotecaapi.dto.UsuarioRequest;
import ec.edu.espe.bibliotecaapi.dto.UsuarioResponse;
import ec.edu.espe.bibliotecaapi.entity.Usuario;
import ec.edu.espe.bibliotecaapi.exception.RecursoNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.ReglaNegocioException;
import ec.edu.espe.bibliotecaapi.repository.PrestamoRepository;
import ec.edu.espe.bibliotecaapi.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PrestamoRepository prestamoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(UsuarioService::toResponse).toList();
    }

    public UsuarioResponse obtenerUsuario(@NotNull @Positive Long id) {
        return toResponse(buscarUsuario(id));
    }

    @Transactional
    public UsuarioResponse guardarUsuario(@NotNull @Valid UsuarioRequest request) {
        Usuario usuario = new Usuario();
        aplicarDatos(usuario, request);
        return toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioResponse actualizarUsuario(@NotNull @Positive Long id, @NotNull @Valid UsuarioRequest request) {
        Usuario usuario = buscarUsuario(id);
        aplicarDatos(usuario, request);
        return toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public void eliminarUsuario(@NotNull @Positive Long id) {
        Usuario usuario = buscarUsuario(id);
        if (prestamoRepository.existsByUsuarioId(id)) {
            throw new ReglaNegocioException("No se puede eliminar un usuario que tiene prestamos registrados");
        }
        usuarioRepository.delete(usuario);
        usuarioRepository.flush();
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }

    private static void aplicarDatos(Usuario usuario, UsuarioRequest request) {
        usuario.setNombre(request.nombre().trim());
        usuario.setCedula(request.cedula().trim());
        usuario.setEmail(request.email().trim());
    }

    private static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCedula(), usuario.getEmail());
    }
}
