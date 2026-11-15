package com.polimarket.api.controller.proveedores;

import com.polimarket.api.service.proveedores.ProveedorService;
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
@WebMvcTest(ProveedorController.class)
@Import({DevSecurityConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
class ProveedorControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProveedorService proveedorService;

    @Test
    void listarDevuelve200() throws Exception {
        given(proveedorService.listarProveedores()).willReturn(List.of(proveedor()));

        mockMvc.perform(get("/api/v1/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nit").value("900123456-7"))
                .andExpect(jsonPath("$[0].nombre").value("Distribuidora Andina"))
                .andExpect(jsonPath("$[0].contacto").value("ventas@andina.co"))
                .andExpect(jsonPath("$[0].productos").doesNotExist());
    }

    @Test
    void filtrarPorProducto() throws Exception {
        given(proveedorService.consultarProveedoresPorProducto(3)).willReturn(List.of(proveedor()));

        mockMvc.perform(get("/api/v1/proveedores").param("productoId", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void dosFiltrosDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/proveedores").param("nombre", "a").param("productoId", "3"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(proveedorService);
    }

    @Test
    void obtenerPorNit() throws Exception {
        given(proveedorService.obtenerProveedor("900123456-7")).willReturn(proveedor());

        mockMvc.perform(get("/api/v1/proveedores/900123456-7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nit").value("900123456-7"));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        given(proveedorService.obtenerProveedor("000")).willThrow(new RecursoNoEncontradoException("Proveedor", "000"));

        mockMvc.perform(get("/api/v1/proveedores/000")).andExpect(status().isNotFound());
    }

    @Test
    void actualizarContacto() throws Exception {
        given(proveedorService.actualizarContacto("900123456-7", "nuevo@andina.co")).willReturn(proveedor());

        mockMvc.perform(put("/api/v1/proveedores/900123456-7/contacto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contacto\":\"nuevo@andina.co\"}"))
                .andExpect(status().isOk());
        verify(proveedorService).actualizarContacto("900123456-7", "nuevo@andina.co");
    }

    @Test
    void contactoVacioDevuelve400() throws Exception {
        mockMvc.perform(put("/api/v1/proveedores/900123456-7/contacto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contacto\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.contacto").exists());
        verifyNoInteractions(proveedorService);
    }
}
