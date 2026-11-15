package com.polimarket.api.controller.clientes;

import com.polimarket.api.service.clientes.ClienteService;
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
@WebMvcTest(ClienteController.class)
@Import({DevSecurityConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
class ClienteControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void listarDevuelve200ConDtos() throws Exception {
        given(clienteService.listarClientes()).willReturn(List.of(cliente()));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Carlos Cliente"))
                .andExpect(jsonPath("$[0].direccion").value("Calle 1 # 2-3"))
                .andExpect(jsonPath("$[0].ventas").doesNotExist());
    }

    @Test
    void listarConNombreUsaBusqueda() throws Exception {
        given(clienteService.buscarClientesPorNombre("car")).willReturn(List.of(cliente()));

        mockMvc.perform(get("/api/v1/clientes").param("nombre", "car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        verify(clienteService, never()).listarClientes();
    }

    @Test
    void obtenerDevuelve200() throws Exception {
        given(clienteService.obtenerCliente(2)).willReturn(cliente());

        mockMvc.perform(get("/api/v1/clientes/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos Cliente"));
    }

    @Test
    void obtenerInexistenteDevuelve404ProblemDetail() throws Exception {
        given(clienteService.obtenerCliente(99)).willThrow(new RecursoNoEncontradoException("Cliente", 99));

        mockMvc.perform(get("/api/v1/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:recurso-no-encontrado"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void idNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/clientes/abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clienteService);
    }

    @Test
    void actualizarDevuelve200EInvocaServicio() throws Exception {
        given(clienteService.actualizarDatos(2, "Nuevo", "Nueva dir")).willReturn(cliente());

        mockMvc.perform(put("/api/v1/clientes/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"direccion\":\"Nueva dir\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
        verify(clienteService).actualizarDatos(2, "Nuevo", "Nueva dir");
    }

    @Test
    void actualizarConNombreVacioDevuelve400ConErrores() throws Exception {
        mockMvc.perform(put("/api/v1/clientes/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"direccion\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:polimarket:error:validacion"))
                .andExpect(jsonPath("$.errores.nombre").exists());
        verifyNoInteractions(clienteService);
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(put("/api/v1/clientes/2").contentType(MediaType.APPLICATION_JSON).content("{nombre"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(clienteService);
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        given(clienteService.listarClientes()).willThrow(new IllegalStateException("SQL secreto"));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrio un error inesperado; intente mas tarde"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL secreto"))));
    }
}
