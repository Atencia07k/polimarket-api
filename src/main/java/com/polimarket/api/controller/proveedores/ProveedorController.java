package com.polimarket.api.controller.proveedores;

import com.polimarket.api.dto.proveedores.ActualizarContactoRequest;
import com.polimarket.api.dto.proveedores.ProveedorResponse;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.service.proveedores.ProveedorService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El identificador del proveedor es su NIT (llave natural del UML), no un id tecnico.
 */
@RestController
@RequestMapping(path = "/api/v1/proveedores", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listar(@RequestParam(required = false) String nombre,
                                                          @RequestParam(required = false) Integer productoId) {
        if (nombre != null && productoId != null) {
            throw new DatosInvalidosException("Use un solo filtro: nombre o productoId");
        }
        List<Proveedor> proveedores;
        if (nombre != null) {
            proveedores = proveedorService.buscarProveedoresPorNombre(nombre);
        } else if (productoId != null) {
            proveedores = proveedorService.consultarProveedoresPorProducto(productoId);
        } else {
            proveedores = proveedorService.listarProveedores();
        }
        return ResponseEntity.ok(proveedores.stream().map(ProveedorResponse::desde).toList());
    }

    @GetMapping("/{nit}")
    public ResponseEntity<ProveedorResponse> obtener(@PathVariable String nit) {
        return ResponseEntity.ok(ProveedorResponse.desde(proveedorService.obtenerProveedor(nit)));
    }

    @PutMapping(path = "/{nit}/contacto", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<ProveedorResponse> actualizarContacto(@PathVariable String nit,
                                                                @Valid @RequestBody ActualizarContactoRequest solicitud) {
        return ResponseEntity.ok(ProveedorResponse.desde(
                proveedorService.actualizarContacto(nit, solicitud.contacto())));
    }
}
