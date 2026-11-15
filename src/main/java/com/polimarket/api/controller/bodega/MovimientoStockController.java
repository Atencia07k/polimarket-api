package com.polimarket.api.controller.bodega;

import com.polimarket.api.dto.bodega.MovimientoStockResponse;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.service.bodega.MovimientoStockService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo consultas: los movimientos se generan exclusivamente desde InventarioService.
 */
@RestController
@RequestMapping(path = "/api/v1/movimientos-stock", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovimientoStockController {

    private final MovimientoStockService movimientoService;

    @GetMapping
    public ResponseEntity<List<MovimientoStockResponse>> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        boolean porFechas = desde != null || hasta != null;
        if (tipo != null && porFechas) {
            throw new DatosInvalidosException("Use un solo filtro: tipo o el rango desde/hasta");
        }
        List<MovimientoStock> movimientos;
        if (tipo != null) {
            movimientos = movimientoService.consultarPorTipo(tipo);
        } else if (porFechas) {
            movimientos = movimientoService.consultarPorFechas(desde, hasta);
        } else {
            movimientos = movimientoService.listarMovimientos();
        }
        return ok(movimientos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientoStockResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(MovimientoStockResponse.desde(movimientoService.obtenerMovimiento(id)));
    }

    @GetMapping("/inventarios/{idInventario}")
    public ResponseEntity<List<MovimientoStockResponse>> porInventario(@PathVariable Integer idInventario) {
        return ok(movimientoService.consultarPorInventario(idInventario));
    }

    @GetMapping("/productos/{idProducto}")
    public ResponseEntity<List<MovimientoStockResponse>> porProducto(@PathVariable Integer idProducto) {
        return ok(movimientoService.consultarPorProducto(idProducto));
    }

    @GetMapping("/compras/{idCompra}")
    public ResponseEntity<List<MovimientoStockResponse>> porCompra(@PathVariable Integer idCompra) {
        return ok(movimientoService.consultarPorCompra(idCompra));
    }

    @GetMapping("/entregas/{idEntrega}")
    public ResponseEntity<List<MovimientoStockResponse>> porEntrega(@PathVariable Integer idEntrega) {
        return ok(movimientoService.consultarPorEntrega(idEntrega));
    }

    private static ResponseEntity<List<MovimientoStockResponse>> ok(List<MovimientoStock> movimientos) {
        return ResponseEntity.ok(movimientos.stream().map(MovimientoStockResponse::desde).toList());
    }
}
