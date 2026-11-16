package com.polimarket.api.service.impl.autenticacion;

import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.repository.autenticacion.RefreshTokenRepository;
import com.polimarket.api.repository.autenticacion.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UsuarioServiceImplTest {

    private static final PasswordEncoder ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private UsuarioRepository usuarioRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private UsuarioServiceImpl servicio;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        given(usuarioRepository.save(any())).willAnswer(i -> i.getArgument(0));
        servicio = new UsuarioServiceImpl(usuarioRepository, refreshTokenRepository, ENCODER);
    }

    @Test
    void crearUsuarioNormalizaYGuardaBcrypt() {
        Usuario creado = servicio.crearUsuario("  Vendedor.Uno ", "Clave.Segura1", Rol.VENDEDOR);

        assertThat(creado.getUsername()).isEqualTo("vendedor.uno");
        assertThat(creado.getPasswordHash()).startsWith("{bcrypt}").doesNotContain("Clave.Segura1");
        assertThat(ENCODER.matches("Clave.Segura1", creado.getPasswordHash())).isTrue();
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getFechaCreacion()).isNotNull();
    }

    @Test
    void crearUsuarioRechazaDatosInvalidos() {
        assertThatThrownBy(() -> servicio.crearUsuario("ab", "Clave.Segura1", Rol.VENDEDOR))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.crearUsuario("con espacio", "Clave.Segura1", Rol.VENDEDOR))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.crearUsuario("valido", "corta", Rol.VENDEDOR))
                .isInstanceOf(DatosInvalidosException.class);
        // 25 caracteres de 3 bytes = 75 bytes UTF-8: BCrypt los truncaria
        assertThatThrownBy(() -> servicio.crearUsuario("valido", "€".repeat(25), Rol.VENDEDOR))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.crearUsuario("valido", "Clave.Segura1", null))
                .isInstanceOf(DatosInvalidosException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearUsuarioDuplicadoResponde409() {
        given(usuarioRepository.existsByUsername("admin")).willReturn(true);

        assertThatThrownBy(() -> servicio.crearUsuario("ADMIN", "Clave.Segura1", Rol.ADMIN))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void desactivarRevocaRefreshTokensYNoPermiteAutodesactivarse() {
        Usuario vendedor = Usuario.builder().id(5).username("vendedor1").rol(Rol.VENDEDOR).activo(true).build();
        Usuario admin = Usuario.builder().id(1).username("admin").rol(Rol.ADMIN).activo(true).build();
        given(usuarioRepository.findById(5)).willReturn(Optional.of(vendedor));
        given(usuarioRepository.findById(1)).willReturn(Optional.of(admin));

        assertThat(servicio.desactivarUsuario(5, "Admin").isActivo()).isFalse();
        verify(refreshTokenRepository).revocarPorUsuario(5);

        assertThatThrownBy(() -> servicio.desactivarUsuario(1, "ADMIN"))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> servicio.desactivarUsuario(5, "admin"))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void administradorInicialSoloSeCreaSiNoHayAdminActivoYHayCredenciales() {
        given(usuarioRepository.existsByRolAndActivoTrue(Rol.ADMIN)).willReturn(true);
        assertThat(servicio.crearAdministradorInicial("admin", "Clave.Segura1")).isFalse();

        given(usuarioRepository.existsByRolAndActivoTrue(Rol.ADMIN)).willReturn(false);
        assertThat(servicio.crearAdministradorInicial("", "")).isFalse();
        verify(usuarioRepository, never()).save(any());

        assertThat(servicio.crearAdministradorInicial("admin", "Clave.Segura1")).isTrue();
        verify(usuarioRepository).save(any());
        verify(refreshTokenRepository, never()).revocarPorUsuario(anyInt());
    }
}
