package com.polimarket.api.security;

import com.polimarket.api.controller.SaludoController;
import com.polimarket.api.controller.ventas.VentaController;
import com.polimarket.api.dto.SaludoResponse;
import com.polimarket.api.service.SaludoService;
import com.polimarket.api.service.ventas.VentaService;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que solo una cadena de seguridad este activa por perfil: publica en dev/test y restrictiva en prod.
 */
class SecurityProfilesWebMvcTest {

    @Nested
    @WebMvcTest({SaludoController.class, VentaController.class})
    @Import({DevSecurityConfig.class, SecurityConfig.class})
    @ActiveProfiles("prod")
    class PerfilProd {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private SaludoService saludoService;

        @MockitoBean
        private VentaService ventaService;

        @Test
        void endpointsComercialesEstanDenegados() throws Exception {
            mockMvc.perform(get("/api/v1/ventas").param("vendedorId", "1")).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(ventaService);
        }

        @Test
        void holaSigueSiendoPublico() throws Exception {
            given(saludoService.obtenerSaludo()).willReturn(new SaludoResponse("Hola mundo"));

            mockMvc.perform(get("/api/v1/hola")).andExpect(status().isOk());
        }
    }

    @Nested
    @WebMvcTest({SaludoController.class, VentaController.class})
    @Import({DevSecurityConfig.class, SecurityConfig.class})
    @ActiveProfiles("dev")
    class PerfilDev {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private SaludoService saludoService;

        @MockitoBean
        private VentaService ventaService;

        @Test
        void endpointsComercialesSonPublicosSinCredenciales() throws Exception {
            given(ventaService.consultarVentasPorVendedor(1)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/ventas").param("vendedorId", "1")).andExpect(status().isOk());
        }

        @Test
        void postSinCsrfNoEsRechazadoPorSeguridad() throws Exception {
            mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
