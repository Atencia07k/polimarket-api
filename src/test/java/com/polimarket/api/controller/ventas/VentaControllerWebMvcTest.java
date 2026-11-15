package com.polimarket.api.controller.ventas;

import com.polimarket.api.service.ventas.VentaService;
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
@WebMvcTest(VentaController.class)
@Import({DevSecurityConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
class VentaControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentaService ventaService;

    private static final String VENTA_JSON =
            "{\"idVendedor\":1,\"idCliente\":2,\"items\":[{\"idProducto\":3,\"cantidad\":2}]}";

    @Test
    void registrarDevuelve201ConLocation() throws Exception {
        given(ventaService.registrarVenta(eq(1), eq(2), anyList())).willReturn(venta("REGISTRADA"));

        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/ventas/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.total").value(10000.0))
                .andExpect(jsonPath("$.fecha").value("2026-11-14"))
                .andExpect(jsonPath("$.idVendedor").value(1))
                .andExpect(jsonPath("$.nombreCliente").value("Carlos Cliente"))
                .andExpect(jsonPath("$.detalles[0].idProducto").value(3))
                .andExpect(jsonPath("$.detalles[0].precio").value(5000.0))
                .andExpect(jsonPath("$.idEntrega").doesNotExist())
                .andExpect(jsonPath("$.vendedor").doesNotExist());
        verify(ventaService).registrarVenta(1, 2, List.of(new VentaService.ItemVenta(3, 2)));
    }

    @Test
    void precioEnviadoPorElClienteSeIgnora() throws Exception {
        given(ventaService.registrarVenta(eq(1), eq(2), anyList())).willReturn(venta("REGISTRADA"));

        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVendedor\":1,\"idCliente\":2,\"total\":1,\"items\":[{\"idProducto\":3,\"cantidad\":2,\"precio\":1}]}"))
                .andExpect(status().isCreated());
        verify(ventaService).registrarVenta(1, 2, List.of(new VentaService.ItemVenta(3, 2)));
    }

    @Test
    void vendedorNoAutorizadoDevuelve409() throws Exception {
        given(ventaService.registrarVenta(eq(1), eq(2), anyList())).willThrow(new VendedorNoAutorizadoException(1));

        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:vendedor-no-autorizado"));
    }

    @Test
    void inventarioInsuficienteDevuelve409() throws Exception {
        given(ventaService.registrarVenta(eq(1), eq(2), anyList()))
                .willThrow(new InventarioInsuficienteException("Stock insuficiente para el producto 3"));

        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:inventario-insuficiente"));
    }

    @Test
    void clienteInexistenteDevuelve404() throws Exception {
        given(ventaService.registrarVenta(eq(1), eq(2), anyList()))
                .willThrow(new RecursoNoEncontradoException("Cliente", 2));

        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarInvalidoDevuelve400SinInvocarServicio() throws Exception {
        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\":-2,\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.idVendedor").exists())
                .andExpect(jsonPath("$.errores.idCliente").exists())
                .andExpect(jsonPath("$.errores.items").exists());
        verifyNoInteractions(ventaService);
    }

    @Test
    void listarPorVendedorOCliente() throws Exception {
        given(ventaService.consultarVentasPorVendedor(1)).willReturn(List.of(venta("REGISTRADA")));
        given(ventaService.consultarVentasPorCliente(2)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/ventas").param("vendedorId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/ventas").param("clienteId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listarSinFiltroOConDosFiltrosDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/ventas")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/ventas").param("vendedorId", "1").param("clienteId", "2"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ventaService);
    }

    @Test
    void obtenerConEntregaIncluyeIdEntrega() throws Exception {
        given(ventaService.consultarVenta(7)).willReturn(entrega("PENDIENTE").getVenta());

        mockMvc.perform(get("/api/v1/ventas/7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idEntrega").value(8));
    }

    @Test
    void cancelarYaEntregadaDevuelve409() throws Exception {
        given(ventaService.cancelarVenta(7)).willThrow(new OperacionNoPermitidaException("La venta ya fue entregada"));

        mockMvc.perform(patch("/api/v1/ventas/7/cancelacion")).andExpect(status().isConflict());
    }

    @Test
    void cancelarDevuelve200() throws Exception {
        given(ventaService.cancelarVenta(7)).willReturn(venta("CANCELADA"));

        mockMvc.perform(patch("/api/v1/ventas/7/cancelacion"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
}
