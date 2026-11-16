package com.polimarket.api.controller.bodega;

import com.polimarket.api.service.bodega.ProductoService;
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
@WebMvcTest(ProductoController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
@ActiveProfiles("test")
class ProductoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @Test
    void listarDevuelveProductoConNitsDeProveedores() throws Exception {
        given(productoService.listarProductos()).willReturn(List.of(producto()));

        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].precio").value(5000.0))
                .andExpect(jsonPath("$[0].nitsProveedores[0]").value("900123456-7"))
                .andExpect(jsonPath("$[0].inventarios").doesNotExist());
    }

    @Test
    void filtrarPorProveedor() throws Exception {
        given(productoService.consultarProductosPorProveedor("900123456-7")).willReturn(List.of(producto()));

        mockMvc.perform(get("/api/v1/productos").param("nitProveedor", "900123456-7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void dosFiltrosDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/productos").param("nombre", "a").param("nitProveedor", "b"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:datos-invalidos"));
        verifyNoInteractions(productoService);
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        given(productoService.obtenerProducto(99)).willThrow(new RecursoNoEncontradoException("Producto", 99));

        mockMvc.perform(get("/api/v1/productos/99")).andExpect(status().isNotFound());
    }

    @Test
    void actualizarPrecioDevuelve200() throws Exception {
        given(productoService.actualizarPrecio(3, 6000.0)).willReturn(producto());

        mockMvc.perform(put("/api/v1/productos/3/precio").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precio\":6000.0}"))
                .andExpect(status().isOk());
        verify(productoService).actualizarPrecio(3, 6000.0);
    }

    @Test
    void precioNegativoDevuelve400() throws Exception {
        mockMvc.perform(put("/api/v1/productos/3/precio").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precio\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
        verifyNoInteractions(productoService);
    }
}
