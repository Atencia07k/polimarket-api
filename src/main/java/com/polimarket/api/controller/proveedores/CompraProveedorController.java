package com.polimarket.api.controller.proveedores;

import com.polimarket.api.dto.proveedores.CompraResponse;
import com.polimarket.api.dto.proveedores.RecibirCompraRequest;
import com.polimarket.api.dto.proveedores.RegistrarCompraRequest;
import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.service.proveedores.CompraProveedorService;
import com.polimarket.api.service.proveedores.CompraProveedorService.ItemCompra;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Registrar una compra no mueve inventario: el ingreso ocurre al recibirla en una bodega.
 */
@RestController
@RequestMapping(path = "/api/v1/compras", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompraProveedorController {

    private final CompraProveedorService compraService;

    // CompraProveedorService no ofrece un listado sin filtro; se exige el NIT del proveedor.
    @GetMapping
    public ResponseEntity<List<CompraResponse>> porProveedor(@RequestParam String nitProveedor) {
        return ok(compraService.consultarComprasPorProveedor(nitProveedor));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<CompraResponse>> pendientes() {
        return ok(compraService.consultarComprasPendientes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(CompraResponse.desde(compraService.consultarCompra(id)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody RegistrarCompraRequest solicitud) {
        List<ItemCompra> items = solicitud.items().stream()
                .map(item -> new ItemCompra(item.idProducto(), item.cantidad(), item.precio()))
                .toList();
        CompraProveedor compra = compraService.registrarCompra(solicitud.nitProveedor(), items);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(compra.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(CompraResponse.desde(compra));
    }

    @PatchMapping(path = "/{id}/recepcion", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<CompraResponse> recibir(@PathVariable Integer id,
                                                  @Valid @RequestBody RecibirCompraRequest solicitud) {
        return ResponseEntity.ok(CompraResponse.desde(compraService.recibirCompra(id, solicitud.idBodega())));
    }

    @PatchMapping("/{id}/cancelacion")
    @Transactional
    public ResponseEntity<CompraResponse> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(CompraResponse.desde(compraService.cancelarCompra(id)));
    }

    private static ResponseEntity<List<CompraResponse>> ok(List<CompraProveedor> compras) {
        return ResponseEntity.ok(compras.stream().map(CompraResponse::desde).toList());
    }
}
