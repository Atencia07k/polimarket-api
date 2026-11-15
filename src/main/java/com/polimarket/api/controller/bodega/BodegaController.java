package com.polimarket.api.controller.bodega;

import com.polimarket.api.dto.bodega.BodegaResponse;
import com.polimarket.api.dto.bodega.DisponibilidadBodegaResponse;
import com.polimarket.api.dto.bodega.InventarioResponse;
import com.polimarket.api.entity.Bodega;
import com.polimarket.api.service.bodega.BodegaService;
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

@RestController
@RequestMapping(path = "/api/v1/bodegas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BodegaController {

    private final BodegaService bodegaService;

    @GetMapping
    public ResponseEntity<List<BodegaResponse>> listar(@RequestParam(required = false) String ubicacion) {
        List<Bodega> bodegas = ubicacion == null
                ? bodegaService.listarBodegas()
                : bodegaService.buscarBodegasPorUbicacion(ubicacion);
        return ResponseEntity.ok(bodegas.stream().map(BodegaResponse::desde).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BodegaResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(BodegaResponse.desde(bodegaService.obtenerBodega(id)));
    }

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<DisponibilidadBodegaResponse> disponibilidad(@PathVariable Integer id) {
        return ResponseEntity.ok(new DisponibilidadBodegaResponse(id, bodegaService.consultarDisponibilidad(id)));
    }

    @GetMapping("/{id}/inventarios")
    public ResponseEntity<List<InventarioResponse>> productosAlmacenados(@PathVariable Integer id) {
        return ResponseEntity.ok(bodegaService.consultarProductosAlmacenados(id).stream()
                .map(InventarioResponse::desde).toList());
    }
}
