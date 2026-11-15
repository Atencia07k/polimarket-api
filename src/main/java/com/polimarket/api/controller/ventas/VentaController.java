package com.polimarket.api.controller.ventas;

import com.polimarket.api.dto.ventas.RegistrarVentaRequest;
import com.polimarket.api.dto.ventas.VentaResponse;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.service.ventas.VentaService;
import com.polimarket.api.service.ventas.VentaService.ItemVenta;
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
 * Registrar una venta valida RRHH, cliente, productos y existencias en VentaService; no descuenta stock
 * (el descuento ocurre al confirmar la entrega). El cliente HTTP nunca envia precios ni totales.
 */
@RestController
@RequestMapping(path = "/api/v1/ventas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaController {

    private final VentaService ventaService;

    // VentaService no ofrece un listado sin filtro; se exige vendedorId o clienteId.
    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar(@RequestParam(required = false) Integer vendedorId,
                                                      @RequestParam(required = false) Integer clienteId) {
        if ((vendedorId == null) == (clienteId == null)) {
            throw new DatosInvalidosException("Indique exactamente un filtro: vendedorId o clienteId");
        }
        List<Venta> ventas = vendedorId != null
                ? ventaService.consultarVentasPorVendedor(vendedorId)
                : ventaService.consultarVentasPorCliente(clienteId);
        return ResponseEntity.ok(ventas.stream().map(VentaResponse::desde).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(VentaResponse.desde(ventaService.consultarVenta(id)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody RegistrarVentaRequest solicitud) {
        List<ItemVenta> items = solicitud.items().stream()
                .map(item -> new ItemVenta(item.idProducto(), item.cantidad()))
                .toList();
        Venta venta = ventaService.registrarVenta(solicitud.idVendedor(), solicitud.idCliente(), items);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(venta.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(VentaResponse.desde(venta));
    }

    @PatchMapping("/{id}/cancelacion")
    @Transactional
    public ResponseEntity<VentaResponse> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(VentaResponse.desde(ventaService.cancelarVenta(id)));
    }
}
