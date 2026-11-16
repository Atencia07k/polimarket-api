package com.polimarket.api.service.impl.autenticacion;

import com.polimarket.api.entity.RefreshToken;
import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.exception.AutenticacionFallidaException;
import com.polimarket.api.repository.autenticacion.RefreshTokenRepository;
import com.polimarket.api.repository.autenticacion.UsuarioRepository;
import com.polimarket.api.security.JwtProperties;
import com.polimarket.api.service.autenticacion.TokenService;
import com.polimarket.api.service.autenticacion.TokensEmitidos;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthServiceImplTest {

    private static final PasswordEncoder ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private static final String CLAVE = "Clave.Segura1";
    private static final String HASH_CLAVE = ENCODER.encode(CLAVE);

    private UsuarioRepository usuarioRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AuthServiceImpl servicio;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        TokenService tokenService = mock(TokenService.class);
        given(tokenService.emitirAccessToken(any())).willReturn("access.jwt");
        given(tokenService.vigenciaAccessToken()).willReturn(Duration.ofMinutes(15));
        JwtProperties propiedades = new JwtProperties(null, null, null, Duration.ofDays(7), null, null, null, false);
        servicio = new AuthServiceImpl(usuarioRepository, refreshTokenRepository, tokenService, ENCODER, propiedades);
    }

    private static Usuario usuario(boolean activo) {
        return Usuario.builder().id(3).username("vendedor1").passwordHash(HASH_CLAVE).rol(Rol.VENDEDOR)
                .activo(activo).build();
    }

    private RefreshToken refreshGuardado(String valor, boolean revocado, Instant expira, Usuario usuario) {
        RefreshToken token = RefreshToken.builder().id(10L).usuario(usuario).tokenHash(AuthServiceImpl.hash(valor))
                .familia(UUID.randomUUID()).expiraEn(expira).revocado(revocado).creadoEn(Instant.now()).build();
        given(refreshTokenRepository.findConBloqueoByTokenHash(AuthServiceImpl.hash(valor)))
                .willReturn(Optional.of(token));
        return token;
    }

    @Test
    void loginNormalizaUsuarioYGuardaSoloElHashDelRefresh() {
        given(usuarioRepository.findByUsername("vendedor1")).willReturn(Optional.of(usuario(true)));

        TokensEmitidos tokens = servicio.iniciarSesion("  Vendedor1 ", CLAVE);

        assertThat(tokens.accessToken()).isEqualTo("access.jwt");
        assertThat(tokens.expiraEnSegundos()).isEqualTo(900);
        assertThat(tokens.refreshExpiraEnSegundos()).isEqualTo(Duration.ofDays(7).toSeconds());
        ArgumentCaptor<RefreshToken> guardado = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getTokenHash())
                .isEqualTo(AuthServiceImpl.hash(tokens.refreshToken()))
                .isNotEqualTo(tokens.refreshToken())
                .hasSize(64);
        assertThat(guardado.getValue().isRevocado()).isFalse();
    }

    @Test
    void loginConClaveIncorrectaUsuarioInexistenteOInactivoDaElMismoError() {
        given(usuarioRepository.findByUsername("vendedor1")).willReturn(Optional.of(usuario(true)));
        given(usuarioRepository.findByUsername("inactivo")).willReturn(Optional.of(usuario(false)));

        for (String[] intento : new String[][] {{"vendedor1", "otra-clave"}, {"fantasma", CLAVE}, {"inactivo", CLAVE}}) {
            assertThatThrownBy(() -> servicio.iniciarSesion(intento[0], intento[1]))
                    .isInstanceOf(AutenticacionFallidaException.class)
                    .hasMessage(AuthServiceImpl.CREDENCIALES_INVALIDAS);
        }
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshValidoRotaElTokenEnLaMismaFamilia() {
        RefreshToken actual = refreshGuardado("viejo", false, Instant.now().plusSeconds(60), usuario(true));

        TokensEmitidos tokens = servicio.refrescar("viejo");

        assertThat(actual.isRevocado()).isTrue();
        assertThat(tokens.refreshToken()).isNotEqualTo("viejo");
        ArgumentCaptor<RefreshToken> guardados = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(guardados.capture());
        RefreshToken nuevo = guardados.getAllValues().get(1);
        assertThat(nuevo.getFamilia()).isEqualTo(actual.getFamilia());
        assertThat(nuevo.getTokenHash()).isEqualTo(AuthServiceImpl.hash(tokens.refreshToken()));
    }

    @Test
    void reutilizarUnRefreshRevocadoRevocaTodaLaFamilia() {
        RefreshToken usado = refreshGuardado("usado", true, Instant.now().plusSeconds(60), usuario(true));

        assertThatThrownBy(() -> servicio.refrescar("usado")).isInstanceOf(AutenticacionFallidaException.class);

        verify(refreshTokenRepository).revocarFamilia(usado.getFamilia());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshExpiradoOUsuarioInactivoEsRechazado() {
        RefreshToken expirado = refreshGuardado("expirado", false, Instant.now().minusSeconds(1), usuario(true));
        RefreshToken deInactivo = refreshGuardado("inactivo", false, Instant.now().plusSeconds(60), usuario(false));

        assertThatThrownBy(() -> servicio.refrescar("expirado")).isInstanceOf(AutenticacionFallidaException.class);
        assertThatThrownBy(() -> servicio.refrescar("inactivo")).isInstanceOf(AutenticacionFallidaException.class);

        verify(refreshTokenRepository).revocarFamilia(expirado.getFamilia());
        verify(refreshTokenRepository).revocarFamilia(deInactivo.getFamilia());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshDesconocidoOVacioEsRechazado() {
        given(refreshTokenRepository.findConBloqueoByTokenHash(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.refrescar("desconocido"))
                .isInstanceOf(AutenticacionFallidaException.class)
                .hasMessage(AuthServiceImpl.REFRESH_INVALIDO);
        assertThatThrownBy(() -> servicio.refrescar("  ")).isInstanceOf(AutenticacionFallidaException.class);
    }

    @Test
    void logoutRevocaLaFamiliaYEsIdempotente() {
        RefreshToken token = refreshGuardado("activo", false, Instant.now().plusSeconds(60), usuario(true));
        given(refreshTokenRepository.findConBloqueoByTokenHash(AuthServiceImpl.hash("desconocido")))
                .willReturn(Optional.empty());

        servicio.cerrarSesion("activo");
        servicio.cerrarSesion("desconocido");
        servicio.cerrarSesion(null);

        verify(refreshTokenRepository).revocarFamilia(token.getFamilia());
    }
}
