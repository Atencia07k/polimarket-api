package com.polimarket.api.controller.entregas;

import com.polimarket.api.service.entregas.EntregaService;
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
@WebMvcTest(EntregaController.class)
@Import({DevSecurityConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
class EntregaControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntregaService entregaService;

    @Test
    void registrarDevuelve201ConLocation() throws Exception {
        given(entregaService.registrarEntrega(7, "Calle 1 # 2-3")).willReturn(entrega("PENDIENTE"));

        mockMvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVenta\":7,\"direccion\":\"Calle 1 # 2-3\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/entregas/8")))
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.idVenta").value(7))
                .andExpect(jsonPath("$.venta").doesNotExist());
    }

    @Test
    void registrarSinDireccionDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON).content("{\"idVenta\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.direccion").exists());
        verifyNoInteractions(entregaService);
    }

    @Test
    void registrarDuplicadaDevuelve409() throws Exception {
        given(entregaService.registrarEntrega(7, "Calle"))
                .willThrow(new OperacionNoPermitidaException("La venta ya tiene una entrega"));

        mockMvc.perform(post("/api/v1/entregas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVenta\":7,\"direccion\":\"Calle\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarDevuelve200() throws Exception {
        given(entregaService.confirmarEntrega(8, 4)).willReturn(entrega("ENTREGADA"));

        mockMvc.perform(patch("/api/v1/entregas/8/confirmacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBodega\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"));
        verify(entregaService).confirmarEntrega(8, 4);
    }

    @Test
    void confirmarDosVecesDevuelve409() throws Exception {
        given(entregaService.confirmarEntrega(8, 4))
                .willThrow(new OperacionNoPermitidaException("La entrega no esta PENDIENTE"));

        mockMvc.perform(patch("/api/v1/entregas/8/confirmacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBodega\":4}"))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarSinBodegaDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/entregas/8/confirmacion").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(entregaService);
    }

    @Test
    void consultas() throws Exception {
        given(entregaService.consultarPendientes()).willReturn(List.of(entrega("PENDIENTE")));
        given(entregaService.consultarEntrega(8)).willReturn(entrega("PENDIENTE"));
        given(entregaService.consultarEntregaPorVenta(7)).willReturn(entrega("PENDIENTE"));
        given(entregaService.consultarPorEstado("ENTREGADA")).willReturn(List.of());
        given(entregaService.consultarPorRangoFechas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .willReturn(List.of(entrega("PENDIENTE")));

        mockMvc.perform(get("/api/v1/entregas/pendientes")).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/entregas/8")).andExpect(jsonPath("$.direccion").value("Calle 1 # 2-3"));
        mockMvc.perform(get("/api/v1/entregas/ventas/7")).andExpect(jsonPath("$.id").value(8));
        mockMvc.perform(get("/api/v1/entregas").param("estado", "ENTREGADA")).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/entregas").param("desde", "2026-01-01").param("hasta", "2026-12-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void listarSinFiltroDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/entregas")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/entregas").param("estado", "PENDIENTE").param("desde", "2026-01-01"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(entregaService);
    }

    @Test
    void entregaDeVentaInexistenteDevuelve404() throws Exception {
        given(entregaService.consultarEntregaPorVenta(99)).willThrow(new RecursoNoEncontradoException("Entrega", 99));

        mockMvc.perform(get("/api/v1/entregas/ventas/99")).andExpect(status().isNotFound());
    }
}
