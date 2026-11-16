package com.polimarket.api.config;

import com.polimarket.api.service.autenticacion.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer ADMIN al arrancar si no existe ninguno, usando ADMIN_USERNAME / ADMIN_PASSWORD
 * (variables de entorno o archivo .env en dev). No hay credenciales predeterminadas en el codigo.
 */
@Component
@RequiredArgsConstructor
public class AdministradorInicialRunner implements ApplicationRunner {

    private final UsuarioService usuarioService;

    @Value("${polimarket.seguridad.admin-inicial.username:}")
    private String username;

    @Value("${polimarket.seguridad.admin-inicial.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        usuarioService.crearAdministradorInicial(username, password);
    }
}
