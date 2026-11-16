package com.polimarket.api.controller.bodega;

import com.polimarket.api.service.bodega.MovimientoStockService;
import com.polimarket.api.controller.ControllerTestFixtures;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.exception.VendedorNoAutorizadoException;
import com.polimarket.api.security.SecurityConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
@WebMvcTest(MovimientoStockController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
@ActiveProfiles("test")
class MovimientoStockControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovimientoStockService movimientoStockService;

    @Test
    void listarSinFiltros() throws Exception {
        given(movimientoStockService.listarMovimientos()).willReturn(List.of(movimiento("ENTRADA")));

        mockMvc.perform(get("/api/v1/movimientos-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$[0].idInventario").value(5))
                .andExpect(jsonPath("$[0].idProducto").value(3))
                .andExpect(jsonPath("$[0].idCompra").value(9))
                .andExpect(jsonPath("$[0].idEntrega").doesNotExist());
    }

    @Test
    void filtrarPorTipoYFechas() throws Exception {
        given(movimientoStockService.consultarPorTipo("SALIDA")).willReturn(List.of());
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        given(movimientoStockService.consultarPorFechas(desde, hasta)).willReturn(List.of(movimiento("ENTRADA")));

        mockMvc.perform(get("/api/v1/movimientos-stock").param("tipo", "SALIDA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/movimientos-stock").param("desde", "2026-01-01").param("hasta", "2026-12-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void tipoYFechasALaVezDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/movimientos-stock").param("tipo", "ENTRADA").param("desde", "2026-01-01"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(movimientoStockService);
    }

    @Test
    void tipoInvalidoDevuelve400() throws Exception {
        given(movimientoStockService.consultarPorTipo("OTRO")).willThrow(new DatosInvalidosException("Tipo invalido"));

        mockMvc.perform(get("/api/v1/movimientos-stock").param("tipo", "OTRO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fechaMalFormadaDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/movimientos-stock").param("desde", "14/11/2026"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(movimientoStockService);
    }

    @Test
    void consultasPorRelacion() throws Exception {
        given(movimientoStockService.obtenerMovimiento(10)).willReturn(movimiento("ENTRADA"));
        given(movimientoStockService.consultarPorInventario(5)).willReturn(List.of(movimiento("ENTRADA")));
        given(movimientoStockService.consultarPorProducto(3)).willReturn(List.of(movimiento("ENTRADA")));
        given(movimientoStockService.consultarPorCompra(9)).willReturn(List.of(movimiento("ENTRADA")));
        given(movimientoStockService.consultarPorEntrega(8)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/movimientos-stock/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
        mockMvc.perform(get("/api/v1/movimientos-stock/inventarios/5")).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/movimientos-stock/productos/3")).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/movimientos-stock/compras/9")).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/movimientos-stock/entregas/8")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        given(movimientoStockService.obtenerMovimiento(99)).willThrow(new RecursoNoEncontradoException("Movimiento", 99));

        mockMvc.perform(get("/api/v1/movimientos-stock/99")).andExpect(status().isNotFound());
    }
}
