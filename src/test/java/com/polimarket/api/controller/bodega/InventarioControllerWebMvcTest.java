package com.polimarket.api.controller.bodega;

import com.polimarket.api.service.bodega.InventarioService;
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
@WebMvcTest(InventarioController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
@ActiveProfiles("test")
class InventarioControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventarioService inventarioService;

    @Test
    void stockDevuelveCantidadYReposicion() throws Exception {
        given(inventarioService.consultarStock(3, 4)).willReturn(8);
        given(inventarioService.requiereReposicion(3, 4)).willReturn(true);

        mockMvc.perform(get("/api/v1/inventarios/stock").param("productoId", "3").param("bodegaId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProducto").value(3))
                .andExpect(jsonPath("$.idBodega").value(4))
                .andExpect(jsonPath("$.stock").value(8))
                .andExpect(jsonPath("$.requiereReposicion").value(true));
    }

    @Test
    void stockSinParametrosDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/inventarios/stock").param("productoId", "3"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inventarioService);
    }

    @Test
    void stockInexistenteDevuelve404() throws Exception {
        given(inventarioService.consultarStock(3, 99)).willThrow(new RecursoNoEncontradoException("Inventario no existe"));

        mockMvc.perform(get("/api/v1/inventarios/stock").param("productoId", "3").param("bodegaId", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void stockTotal() throws Exception {
        given(inventarioService.consultarStockTotal(3)).willReturn(30);

        mockMvc.perform(get("/api/v1/inventarios/productos/3/stock-total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockTotal").value(30));
    }

    @Test
    void porProductoPorBodegaYReposicion() throws Exception {
        given(inventarioService.consultarInventarioPorProducto(3)).willReturn(List.of(inventario()));
        given(inventarioService.consultarInventarioPorBodega(4)).willReturn(List.of(inventario()));
        given(inventarioService.consultarInventariosQueRequierenReposicion()).willReturn(List.of(inventario()));

        mockMvc.perform(get("/api/v1/inventarios/productos/3")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idBodega").value(4));
        mockMvc.perform(get("/api/v1/inventarios/bodegas/4")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ubicacionBodega").value("Bogota Norte"));
        mockMvc.perform(get("/api/v1/inventarios/reposicion")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockMinimo").value(10));
    }

    @Test
    void noExisteEndpointDeAjusteManual() throws Exception {
        mockMvc.perform(post("/api/v1/inventarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(inventarioService);
    }
}
