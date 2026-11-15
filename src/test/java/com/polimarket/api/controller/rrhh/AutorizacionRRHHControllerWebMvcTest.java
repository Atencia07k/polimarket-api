package com.polimarket.api.controller.rrhh;

import com.polimarket.api.service.rrhh.AutorizacionRRHHService;
import com.polimarket.api.controller.ControllerTestFixtures;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.exception.VendedorNoAutorizadoException;
import com.polimarket.api.security.DevSecurityConfig;
import com.polimarket.api.security.SecurityConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.polimarket.api.controller.ControllerTestFixtures.*;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
@WebMvcTest(AutorizacionRRHHController.class)
@Import({DevSecurityConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
class AutorizacionRRHHControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutorizacionRRHHService autorizacionRRHHService;

    @Test
    void verificarDevuelveEstado() throws Exception {
        given(autorizacionRRHHService.validarVendedor(1)).willReturn(true);

        mockMvc.perform(get("/api/v1/autorizaciones/vendedores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVendedor").value(1))
                .andExpect(jsonPath("$.autorizado").value(true));
    }

    @Test
    void historialDevuelveLista() throws Exception {
        given(autorizacionRRHHService.consultarHistorial(1)).willReturn(List.of(autorizacion("AUTORIZADA")));

        mockMvc.perform(get("/api/v1/autorizaciones/vendedores/1/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idVendedor").value(1))
                .andExpect(jsonPath("$[0].estado").value("AUTORIZADA"))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-14"));
    }

    @Test
    void autorizarDevuelve201() throws Exception {
        given(autorizacionRRHHService.autorizarVendedor(1)).willReturn(autorizacion("AUTORIZADA"));

        mockMvc.perform(post("/api/v1/autorizaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVendedor\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("AUTORIZADA"));
        verify(autorizacionRRHHService).autorizarVendedor(1);
    }

    @Test
    void autorizarSinIdDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/autorizaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.idVendedor").exists());
        verifyNoInteractions(autorizacionRRHHService);
    }

    @Test
    void autorizarRedundanteDevuelve409() throws Exception {
        given(autorizacionRRHHService.autorizarVendedor(1))
                .willThrow(new OperacionNoPermitidaException("El vendedor ya esta autorizado"));

        mockMvc.perform(post("/api/v1/autorizaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVendedor\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:operacion-no-permitida"));
    }

    @Test
    void revocarDevuelve200() throws Exception {
        given(autorizacionRRHHService.revocarAutorizacion(1)).willReturn(autorizacion("REVOCADA"));

        mockMvc.perform(patch("/api/v1/autorizaciones/vendedores/1/revocacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REVOCADA"));
    }
}
