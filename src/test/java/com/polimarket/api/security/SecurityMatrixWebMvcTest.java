package com.polimarket.api.security;

import com.polimarket.api.controller.SaludoController;
import com.polimarket.api.controller.autenticacion.AuthController;
import com.polimarket.api.controller.autenticacion.UsuarioController;
import com.polimarket.api.controller.ventas.VentaController;
import com.polimarket.api.dto.SaludoResponse;
import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.service.SaludoService;
import com.polimarket.api.service.autenticacion.AuthService;
import com.polimarket.api.service.autenticacion.UsuarioService;
import com.polimarket.api.service.impl.autenticacion.TokenServiceImpl;
import com.polimarket.api.service.ventas.VentaService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autorizacion con tokens reales firmados por el JwtEncoder del contexto y validados por el JwtDecoder.
 */
@WebMvcTest({SaludoController.class, VentaController.class, AuthController.class, UsuarioController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityMatrixWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties propiedades;

    @MockitoBean
    private SaludoService saludoService;

    @MockitoBean
    private VentaService ventaService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioService usuarioService;

    private String tokenPara(Rol rol) {
        Usuario usuario = Usuario.builder().id(7).username("usuario." + rol.name().toLowerCase()).rol(rol).build();
        return new TokenServiceImpl(jwtEncoder, propiedades).emitirAccessToken(usuario);
    }

    private String token(String issuer, String audience, Instant expira) {
        Instant emitido = expira.minus(15, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).audience(List.of(audience)).subject("intruso")
                .claim("roles", List.of("ADMIN")).issuedAt(emitido).expiresAt(expira).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void holaEsPublico() throws Exception {
        given(saludoService.obtenerSaludo()).willReturn(new SaludoResponse("Hola mundo"));

        mockMvc.perform(get("/api/v1/hola")).andExpect(status().isOk());
    }

    @Test
    void sinTokenResponde401ConProblemDetailYCabeceraBearer() throws Exception {
        mockMvc.perform(get("/api/v1/ventas").param("vendedorId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:no-autenticado"));
        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(ventaService);
    }

    @Test
    void tokenMalformadoOConFirmaAlteradaResponde401() throws Exception {
        String valido = tokenPara(Rol.ADMIN);
        String alterado = valido.substring(0, valido.length() - 4) + "AAAA";

        mockMvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer("no-es-un-jwt")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(alterado)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void tokenExpiradoOConIssuerOAudienceAjenosResponde401() throws Exception {
        Instant futuro = Instant.now().plus(10, ChronoUnit.MINUTES);
        String expirado = token(propiedades.issuer(), propiedades.audience(), Instant.now().minus(5, ChronoUnit.MINUTES));
        String otroIssuer = token("otro-emisor", propiedades.audience(), futuro);
        String otraAudiencia = token(propiedades.issuer(), "otra-api", futuro);

        for (String token : List.of(expirado, otroIssuer, otraAudiencia)) {
            mockMvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(usuarioService);
    }

    @Test
    void vendedorPuedeConsultarYRegistrarVentas() throws Exception {
        given(ventaService.consultarVentasPorVendedor(1)).willReturn(List.of());
        String token = tokenPara(Rol.VENDEDOR);

        mockMvc.perform(get("/api/v1/ventas").param("vendedorId", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        // Pasa la seguridad: el 400 viene de la validacion del cuerpo vacio
        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vendedorNoPuedeAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenPara(Rol.VENDEDOR))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:acceso-denegado"));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void adminPuedeAdministrarUsuarios() throws Exception {
        given(usuarioService.listarUsuarios()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenPara(Rol.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void meDevuelveElUsuarioDelToken() throws Exception {
        given(usuarioService.obtenerPorUsername("usuario.vendedor")).willReturn(Usuario.builder()
                .id(7).username("usuario.vendedor").rol(Rol.VENDEDOR).activo(true).fechaCreacion(Instant.now()).build());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(tokenPara(Rol.VENDEDOR))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usuario.vendedor"))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginEsPublicoYNoRequiereCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
