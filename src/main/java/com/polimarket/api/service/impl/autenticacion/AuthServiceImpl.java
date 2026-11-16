package com.polimarket.api.service.impl.autenticacion;

import com.polimarket.api.entity.RefreshToken;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.exception.AutenticacionFallidaException;
import com.polimarket.api.repository.autenticacion.RefreshTokenRepository;
import com.polimarket.api.repository.autenticacion.UsuarioRepository;
import com.polimarket.api.security.JwtProperties;
import com.polimarket.api.service.autenticacion.AuthService;
import com.polimarket.api.service.autenticacion.TokenService;
import com.polimarket.api.service.autenticacion.TokensEmitidos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    static final String CREDENCIALES_INVALIDAS = "Usuario o contrasena incorrectos";
    static final String REFRESH_INVALIDO = "El refresh token no es valido o expiro";
    private static final int BYTES_REFRESH_TOKEN = 32;

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties propiedades;
    private final SecureRandom aleatorio = new SecureRandom();
    /** Hash de referencia para comparar aunque el usuario no exista (evita enumeracion por tiempo). */
    private final String hashSenuelo;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
                           TokenService tokenService, PasswordEncoder passwordEncoder, JwtProperties propiedades) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.propiedades = propiedades;
        this.hashSenuelo = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public TokensEmitidos iniciarSesion(String username, String password) {
        String nombre = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String clave = password == null ? "" : password;
        Optional<Usuario> encontrado = usuarioRepository.findByUsername(nombre);
        boolean coincide = passwordEncoder.matches(clave,
                encontrado.map(Usuario::getPasswordHash).orElse(hashSenuelo));
        if (encontrado.isEmpty() || !coincide || !encontrado.get().isActivo()) {
            log.info("Inicio de sesion rechazado");
            throw new AutenticacionFallidaException(CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = encontrado.get();
        log.info("Inicio de sesion del usuario {}", usuario.getId());
        return emitir(usuario, UUID.randomUUID());
    }

    @Override
    @Transactional(noRollbackFor = AutenticacionFallidaException.class)
    public TokensEmitidos refrescar(String refreshToken) {
        RefreshToken actual = buscar(refreshToken)
                .orElseThrow(() -> new AutenticacionFallidaException(REFRESH_INVALIDO));
        Usuario usuario = actual.getUsuario();
        if (actual.isRevocado()) {
            int revocados = refreshTokenRepository.revocarFamilia(actual.getFamilia());
            log.warn("Reutilizacion de refresh token del usuario {}: familia revocada ({} tokens)",
                    usuario.getId(), revocados);
            throw new AutenticacionFallidaException(REFRESH_INVALIDO);
        }
        if (actual.estaExpirado(Instant.now()) || !usuario.isActivo()) {
            refreshTokenRepository.revocarFamilia(actual.getFamilia());
            throw new AutenticacionFallidaException(REFRESH_INVALIDO);
        }
        actual.setRevocado(true);
        refreshTokenRepository.save(actual);
        return emitir(usuario, actual.getFamilia());
    }

    @Override
    public void cerrarSesion(String refreshToken) {
        buscar(refreshToken).ifPresent(token -> {
            refreshTokenRepository.revocarFamilia(token.getFamilia());
            log.info("Cierre de sesion del usuario {}", token.getUsuario().getId());
        });
    }

    private Optional<RefreshToken> buscar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findConBloqueoByTokenHash(hash(refreshToken.trim()));
    }

    private TokensEmitidos emitir(Usuario usuario, UUID familia) {
        byte[] bytes = new byte[BYTES_REFRESH_TOKEN];
        aleatorio.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ahora = Instant.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hash(refresh))
                .familia(familia)
                .expiraEn(ahora.plus(propiedades.refreshTokenTtl()))
                .revocado(false)
                .creadoEn(ahora)
                .build());
        return new TokensEmitidos(tokenService.emitirAccessToken(usuario),
                tokenService.vigenciaAccessToken().toSeconds(),
                refresh, propiedades.refreshTokenTtl().toSeconds());
    }

    static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
