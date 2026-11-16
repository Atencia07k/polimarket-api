package com.polimarket.api.controller.autenticacion;

import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.exception.AutenticacionFallidaException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.security.SecurityConfig;
import com.polimarket.api.service.autenticacion.AuthService;
import com.polimarket.api.service.autenticacion.TokensEmitidos;
import com.polimarket.api.service.autenticacion.UsuarioService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UsuarioController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AutenticacionControllerWebMvcTest {

    private static final TokensEmitidos TOKENS = new TokensEmitidos("access.jwt", 900, "refresh-opaco", 604800);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioService usuarioService;

    private static Usuario usuario(int id, String username, Rol rol) {
        return Usuario.builder().id(id).username(username).passwordHash("{bcrypt}secreto").rol(rol).activo(true)
                .fechaCreacion(Instant.parse("2026-11-14T10:00:00Z")).build();
    }

    private static SimpleGrantedAuthority admin() {
        return new SimpleGrantedAuthority("ROLE_ADMIN");
    }

    @Test
    void loginExitosoDevuelveParDeTokens() throws Exception {
        given(authService.iniciarSesion("admin", "Clave.Segura1")).willReturn(TOKENS);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Clave.Segura1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("refresh-opaco"))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800));
    }

    @Test
    void loginFallidoResponde401Generico() throws Exception {
        given(authService.iniciarSesion(any(), any()))
                .willThrow(new AutenticacionFallidaException("Usuario o contrasena incorrectos"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:autenticacion-fallida"))
                .andExpect(jsonPath("$.detail").value("Usuario o contrasena incorrectos"));
    }

    @Test
    void loginSinCredencialesResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void refreshDevuelveNuevoPar() throws Exception {
        given(authService.refrescar("refresh-viejo")).willReturn(TOKENS);

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-viejo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-opaco"));
    }

    @Test
    void refreshReutilizadoResponde401() throws Exception {
        given(authService.refrescar("refresh-usado"))
                .willThrow(new AutenticacionFallidaException("El refresh token no es valido o expiro"));

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-usado\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutResponde204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-opaco\"}"))
                .andExpect(status().isNoContent());
        verify(authService).cerrarSesion("refresh-opaco");
    }

    @Test
    void adminCreaUsuarioConLocation() throws Exception {
        given(usuarioService.crearUsuario("vendedor1", "Clave.Segura1", Rol.VENDEDOR))
                .willReturn(usuario(5, "vendedor1", Rol.VENDEDOR));

        mockMvc.perform(post("/api/v1/usuarios").with(jwt().authorities(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vendedor1\",\"password\":\"Clave.Segura1\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/v1/usuarios/5")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void crearUsuarioConRolInvalidoOClaveCortaResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios").with(jwt().authorities(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vendedor1\",\"password\":\"corta\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/usuarios").with(jwt().authorities(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vendedor1\",\"password\":\"Clave.Segura1\",\"rol\":\"ROOT\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void listarYObtenerUsuarios() throws Exception {
        given(usuarioService.listarUsuarios()).willReturn(List.of(usuario(1, "admin", Rol.ADMIN)));
        given(usuarioService.obtenerUsuario(1)).willReturn(usuario(1, "admin", Rol.ADMIN));

        mockMvc.perform(get("/api/v1/usuarios").with(jwt().authorities(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/usuarios/1").with(jwt().authorities(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void desactivarUsaElSubjectDelToken() throws Exception {
        Usuario inactivo = usuario(5, "vendedor1", Rol.VENDEDOR);
        inactivo.setActivo(false);
        given(usuarioService.desactivarUsuario(5, "admin")).willReturn(inactivo);

        mockMvc.perform(patch("/api/v1/usuarios/5/desactivacion")
                        .with(jwt().jwt(j -> j.subject("admin")).authorities(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void desactivarseASiMismoResponde409() throws Exception {
        willThrow(new OperacionNoPermitidaException("Un usuario no puede desactivar su propia cuenta"))
                .given(usuarioService).desactivarUsuario(1, "admin");

        mockMvc.perform(patch("/api/v1/usuarios/1/desactivacion")
                        .with(jwt().jwt(j -> j.subject("admin")).authorities(admin())))
                .andExpect(status().isConflict());
    }
}
