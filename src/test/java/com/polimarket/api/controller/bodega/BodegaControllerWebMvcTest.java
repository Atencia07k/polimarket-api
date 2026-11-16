package com.polimarket.api.controller.bodega;

import com.polimarket.api.service.bodega.BodegaService;
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
@WebMvcTest(BodegaController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
@ActiveProfiles("test")
class BodegaControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BodegaService bodegaService;

    @Test
    void listarDevuelve200() throws Exception {
        given(bodegaService.listarBodegas()).willReturn(List.of(bodega()));

        mockMvc.perform(get("/api/v1/bodegas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].ubicacion").value("Bogota Norte"));
    }

    @Test
    void buscarPorUbicacion() throws Exception {
        given(bodegaService.buscarBodegasPorUbicacion("bog")).willReturn(List.of(bodega()));

        mockMvc.perform(get("/api/v1/bodegas").param("ubicacion", "bog"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        given(bodegaService.obtenerBodega(99)).willThrow(new RecursoNoEncontradoException("Bodega", 99));

        mockMvc.perform(get("/api/v1/bodegas/99")).andExpect(status().isNotFound());
    }

    @Test
    void disponibilidad() throws Exception {
        given(bodegaService.consultarDisponibilidad(4)).willReturn(25);

        mockMvc.perform(get("/api/v1/bodegas/4/disponibilidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idBodega").value(4))
                .andExpect(jsonPath("$.unidadesDisponibles").value(25));
    }

    @Test
    void productosAlmacenados() throws Exception {
        given(bodegaService.consultarProductosAlmacenados(4)).willReturn(List.of(inventario()));

        mockMvc.perform(get("/api/v1/bodegas/4/inventarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProducto").value(3))
                .andExpect(jsonPath("$[0].nombreProducto").value("Cuaderno"))
                .andExpect(jsonPath("$[0].stock").value(8))
                .andExpect(jsonPath("$[0].requiereReposicion").value(true));
    }
}
