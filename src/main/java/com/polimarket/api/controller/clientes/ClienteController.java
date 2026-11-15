package com.polimarket.api.controller.clientes;

import com.polimarket.api.dto.clientes.ActualizarClienteRequest;
import com.polimarket.api.dto.clientes.ClienteResponse;
import com.polimarket.api.entity.Cliente;
import com.polimarket.api.service.clientes.ClienteService;
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
 * Componente Clientes. Todos los controladores mapean a DTO dentro de una transaccion de solo lectura
 * (o de escritura en las operaciones que modifican datos) para poder leer relaciones LAZY con open-in-view=false.
 */
@RestController
@RequestMapping(path = "/api/v1/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar(@RequestParam(required = false) String nombre) {
        List<Cliente> clientes = nombre == null
                ? clienteService.listarClientes()
                : clienteService.buscarClientesPorNombre(nombre);
        return ResponseEntity.ok(clientes.stream().map(ClienteResponse::desde).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(ClienteResponse.desde(clienteService.obtenerCliente(id)));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody ActualizarClienteRequest solicitud) {
        Cliente cliente = clienteService.actualizarDatos(id, solicitud.nombre(), solicitud.direccion());
        return ResponseEntity.ok(ClienteResponse.desde(cliente));
    }
}
