package com.polimarket.api.controller.proveedores;

import com.polimarket.api.service.proveedores.CompraProveedorService;
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
@WebMvcTest(CompraProveedorController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
@ActiveProfiles("test")
class CompraProveedorControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompraProveedorService compraProveedorService;

    private static final String COMPRA_JSON =
            "{\"nitProveedor\":\"900123456-7\",\"items\":[{\"idProducto\":3,\"cantidad\":10,\"precio\":3000.0}]}";

    @Test
    void registrarDevuelve201ConLocationYMapeaItems() throws Exception {
        given(compraProveedorService.registrarCompra(eq("900123456-7"), anyList())).willReturn(compra("PENDIENTE"));

        mockMvc.perform(post("/api/v1/compras").contentType(MediaType.APPLICATION_JSON).content(COMPRA_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/compras/9")))
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.nitProveedor").value("900123456-7"))
                .andExpect(jsonPath("$.total").value(30000.0))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(30000.0));
        verify(compraProveedorService).registrarCompra("900123456-7",
                List.of(new CompraProveedorService.ItemCompra(3, 10, 3000.0)));
    }

    @Test
    void registrarSinItemsDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/compras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nitProveedor\":\"900\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.items").exists());
        verifyNoInteractions(compraProveedorService);
    }

    @Test
    void registrarConCantidadInvalidaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/compras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nitProveedor\":\"900\",\"items\":[{\"idProducto\":3,\"cantidad\":0,\"precio\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['items[0].cantidad']").exists());
        verifyNoInteractions(compraProveedorService);
    }

    @Test
    void productoNoSuministradoDevuelve400() throws Exception {
        given(compraProveedorService.registrarCompra(anyString(), anyList()))
                .willThrow(new DatosInvalidosException("El proveedor no suministra el producto 3"));

        mockMvc.perform(post("/api/v1/compras").contentType(MediaType.APPLICATION_JSON).content(COMPRA_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El proveedor no suministra el producto 3"));
    }

    @Test
    void consultas() throws Exception {
        given(compraProveedorService.consultarComprasPorProveedor("900123456-7")).willReturn(List.of(compra("PENDIENTE")));
        given(compraProveedorService.consultarComprasPendientes()).willReturn(List.of(compra("PENDIENTE")));
        given(compraProveedorService.consultarCompra(9)).willReturn(compra("PENDIENTE"));

        mockMvc.perform(get("/api/v1/compras").param("nitProveedor", "900123456-7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/compras/pendientes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
        mockMvc.perform(get("/api/v1/compras/9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombreProveedor").value("Distribuidora Andina"));
    }

    @Test
    void listarSinNitDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/compras")).andExpect(status().isBadRequest());
        verifyNoInteractions(compraProveedorService);
    }

    @Test
    void recibirDevuelve200() throws Exception {
        given(compraProveedorService.recibirCompra(9, 4)).willReturn(compra("RECIBIDA"));

        mockMvc.perform(patch("/api/v1/compras/9/recepcion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBodega\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"));
    }

    @Test
    void recibirDosVecesDevuelve409() throws Exception {
        given(compraProveedorService.recibirCompra(9, 4))
                .willThrow(new OperacionNoPermitidaException("La compra no esta PENDIENTE"));

        mockMvc.perform(patch("/api/v1/compras/9/recepcion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBodega\":4}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarDevuelve200() throws Exception {
        given(compraProveedorService.cancelarCompra(9)).willReturn(compra("CANCELADA"));

        mockMvc.perform(patch("/api/v1/compras/9/cancelacion"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
}
