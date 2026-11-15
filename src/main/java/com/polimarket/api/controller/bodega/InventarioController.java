package com.polimarket.api.controller.bodega;

import com.polimarket.api.dto.bodega.InventarioResponse;
import com.polimarket.api.dto.bodega.StockResponse;
import com.polimarket.api.dto.bodega.StockTotalResponse;
import com.polimarket.api.service.bodega.InventarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo consultas. Las entradas y salidas de stock exigen una compra o entrega registrada, por lo que se
 * ejecutan desde /compras/{id}/recepcion y /entregas/{id}/confirmacion; no hay ajustes manuales.
 */
@RestController
@RequestMapping(path = "/api/v1/inventarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping("/stock")
    public ResponseEntity<StockResponse> stock(@RequestParam Integer productoId, @RequestParam Integer bodegaId) {
        return ResponseEntity.ok(new StockResponse(productoId, bodegaId,
                inventarioService.consultarStock(productoId, bodegaId),
                inventarioService.requiereReposicion(productoId, bodegaId)));
    }

    @GetMapping("/productos/{productoId}")
    public ResponseEntity<List<InventarioResponse>> porProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(inventarioService.consultarInventarioPorProducto(productoId).stream()
                .map(InventarioResponse::desde).toList());
    }

    @GetMapping("/productos/{productoId}/stock-total")
    public ResponseEntity<StockTotalResponse> stockTotal(@PathVariable Integer productoId) {
        return ResponseEntity.ok(new StockTotalResponse(productoId, inventarioService.consultarStockTotal(productoId)));
    }

    @GetMapping("/bodegas/{bodegaId}")
    public ResponseEntity<List<InventarioResponse>> porBodega(@PathVariable Integer bodegaId) {
        return ResponseEntity.ok(inventarioService.consultarInventarioPorBodega(bodegaId).stream()
                .map(InventarioResponse::desde).toList());
    }

    @GetMapping("/reposicion")
    public ResponseEntity<List<InventarioResponse>> reposicion() {
        return ResponseEntity.ok(inventarioService.consultarInventariosQueRequierenReposicion().stream()
                .map(InventarioResponse::desde).toList());
    }
}
