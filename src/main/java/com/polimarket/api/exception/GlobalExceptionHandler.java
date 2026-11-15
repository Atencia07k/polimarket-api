package com.polimarket.api.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a respuestas HTTP con formato ProblemDetail (RFC 9457).
 * <p>
 * Los errores de Spring MVC (JSON mal formado, tipos invalidos, parametros faltantes, rutas inexistentes)
 * los resuelve {@link ResponseEntityExceptionHandler}. Nunca se exponen trazas, SQL ni detalles de Hibernate.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), "recurso-no-encontrado");
    }

    @ExceptionHandler(DatosInvalidosException.class)
    ProblemDetail datosInvalidos(DatosInvalidosException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Datos invalidos", ex.getMessage(), "datos-invalidos");
    }

    @ExceptionHandler(VendedorNoAutorizadoException.class)
    ProblemDetail vendedorNoAutorizado(VendedorNoAutorizadoException ex) {
        return problema(HttpStatus.CONFLICT, "Vendedor no autorizado por Recursos Humanos", ex.getMessage(),
                "vendedor-no-autorizado");
    }

    @ExceptionHandler(InventarioInsuficienteException.class)
    ProblemDetail inventarioInsuficiente(InventarioInsuficienteException ex) {
        return problema(HttpStatus.CONFLICT, "Inventario insuficiente", ex.getMessage(), "inventario-insuficiente");
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    ProblemDetail operacionNoPermitida(OperacionNoPermitidaException ex) {
        return problema(HttpStatus.CONFLICT, "Operacion no permitida", ex.getMessage(), "operacion-no-permitida");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridadDeDatos(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operacion entra en conflicto con datos existentes", "conflicto-datos");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail bloqueo(PessimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "El registro esta siendo modificado por otra operacion; intente de nuevo", "conflicto-concurrencia");
    }

    // Soporte para la etapa de seguridad JWT: no convertir 401/403 en 500.
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail noAutenticado(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", "Se requiere autenticacion", "no-autenticado");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesoDenegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", "No tiene permisos para esta operacion",
                "acceso-denegado");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail errorInesperado(Exception ex) {
        log.error("Error interno no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrio un error inesperado; intente mas tarde", "error-interno");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Solicitud invalida",
                "Uno o mas campos no son validos", "validacion");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle, String tipo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create("urn:polimarket:error:" + tipo));
        return problema;
    }
}
