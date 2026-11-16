package com.polimarket.api.service.impl.autenticacion;

import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.autenticacion.RefreshTokenRepository;
import com.polimarket.api.repository.autenticacion.UsuarioRepository;
import com.polimarket.api.service.autenticacion.UsuarioService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {

    static final int LONGITUD_MINIMA_PASSWORD = 8;
    /** BCrypt solo considera los primeros 72 bytes. */
    static final int BYTES_MAXIMOS_PASSWORD = 72;
    private static final Pattern FORMATO_USERNAME = Pattern.compile("^[a-z0-9._-]{3,50}$");

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Usuario crearUsuario(String username, String password, Rol rol) {
        String nombre = normalizar(username);
        validarUsername(nombre);
        validarPassword(password);
        if (rol == null) {
            throw new DatosInvalidosException("El rol es obligatorio");
        }
        if (usuarioRepository.existsByUsername(nombre)) {
            throw new OperacionNoPermitidaException("Ya existe un usuario con ese nombre");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .username(nombre)
                .passwordHash(passwordEncoder.encode(password))
                .rol(rol)
                .activo(true)
                .fechaCreacion(Instant.now())
                .build());
        log.info("Usuario {} creado con rol {}", usuario.getId(), rol);
        return usuario;
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario obtenerUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            throw new DatosInvalidosException("El identificador del usuario es obligatorio");
        }
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", idUsuario));
    }

    @Override
    public Usuario obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(normalizar(username))
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    }

    @Override
    @Transactional
    public Usuario desactivarUsuario(Integer idUsuario, String usernameSolicitante) {
        Usuario usuario = obtenerUsuario(idUsuario);
        if (usuario.getUsername().equals(normalizar(usernameSolicitante))) {
            throw new OperacionNoPermitidaException("Un usuario no puede desactivar su propia cuenta");
        }
        if (!usuario.isActivo()) {
            throw new OperacionNoPermitidaException("El usuario ya esta inactivo");
        }
        usuario.setActivo(false);
        int revocados = refreshTokenRepository.revocarPorUsuario(usuario.getId());
        log.info("Usuario {} desactivado; {} refresh tokens revocados", usuario.getId(), revocados);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public boolean crearAdministradorInicial(String username, String password) {
        if (usuarioRepository.existsByRolAndActivoTrue(Rol.ADMIN)) {
            return false;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("No existe ningun ADMIN activo y no se definieron ADMIN_USERNAME / ADMIN_PASSWORD: "
                    + "no se creo el administrador inicial");
            return false;
        }
        crearUsuario(username, password, Rol.ADMIN);
        log.info("Administrador inicial creado");
        return true;
    }

    private static String normalizar(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static void validarUsername(String username) {
        if (!FORMATO_USERNAME.matcher(username).matches()) {
            throw new DatosInvalidosException(
                    "El nombre de usuario debe tener entre 3 y 50 caracteres: letras, numeros, punto, guion o guion bajo");
        }
    }

    private static void validarPassword(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA_PASSWORD
                || password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_PASSWORD) {
            throw new DatosInvalidosException("La contrasena debe tener entre 8 y 72 caracteres");
        }
    }
}
