package com.polimarket.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Respuestas 401/403 de la cadena de seguridad con el mismo formato ProblemDetail (RFC 9457) del resto de la API.
 * Conserva la cabecera {@code WWW-Authenticate: Bearer ...} (RFC 6750) y nunca expone detalles del token.
 */
final class ProblemDetailSecurityHandlers {

    private static final String NO_AUTENTICADO = """
            {"type":"urn:polimarket:error:no-autenticado","title":"No autenticado","status":401,\
            "detail":"Se requiere un token de acceso valido"}""";

    private static final String ACCESO_DENEGADO = """
            {"type":"urn:polimarket:error:acceso-denegado","title":"Acceso denegado","status":403,\
            "detail":"No tiene permisos para esta operacion"}""";

    private ProblemDetailSecurityHandlers() {
    }

    static AuthenticationEntryPoint entryPoint() {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            bearer.commence(request, response, ex);
            escribir(response, NO_AUTENTICADO);
        };
    }

    static AccessDeniedHandler accessDeniedHandler() {
        BearerTokenAccessDeniedHandler bearer = new BearerTokenAccessDeniedHandler();
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) -> {
            bearer.handle(request, response, ex);
            escribir(response, ACCESO_DENEGADO);
        };
    }

    private static void escribir(HttpServletResponse response, String cuerpo) throws IOException {
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(cuerpo);
    }
}
