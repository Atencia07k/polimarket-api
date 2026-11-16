package com.polimarket.api.controller.rrhh;

import com.polimarket.api.service.rrhh.VendedorService;
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
@WebMvcTest(VendedorController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
@ActiveProfiles("test")
class VendedorControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendedorService vendedorService;

    @Test
    void listarDevuelve200() throws Exception {
        given(vendedorService.listarVendedores()).willReturn(List.of(vendedor()));

        mockMvc.perform(get("/api/v1/vendedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana Vendedora"))
                .andExpect(jsonPath("$[0].autorizaciones").doesNotExist());
    }

    @Test
    void buscarPorNombre() throws Exception {
        given(vendedorService.buscarVendedoresPorNombre("ana")).willReturn(List.of(vendedor()));

        mockMvc.perform(get("/api/v1/vendedores").param("nombre", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        given(vendedorService.obtenerVendedor(99)).willThrow(new RecursoNoEncontradoException("Vendedor", 99));

        mockMvc.perform(get("/api/v1/vendedores/99"))
                .andExpect(status().isNotFound());
    }
}
