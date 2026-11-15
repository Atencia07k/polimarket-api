package com.polimarket.api.controller.bodega;

import com.polimarket.api.dto.bodega.ActualizarPrecioRequest;
import com.polimarket.api.dto.bodega.ProductoResponse;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.service.bodega.ProductoService;
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

@RestController
@RequestMapping(path = "/api/v1/productos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(@RequestParam(required = false) String nombre,
                                                         @RequestParam(required = false) String nitProveedor) {
        if (nombre != null && nitProveedor != null) {
            throw new DatosInvalidosException("Use un solo filtro: nombre o nitProveedor");
        }
        List<Producto> productos;
        if (nombre != null) {
            productos = productoService.buscarProductosPorNombre(nombre);
        } else if (nitProveedor != null) {
            productos = productoService.consultarProductosPorProveedor(nitProveedor);
        } else {
            productos = productoService.listarProductos();
        }
        return ResponseEntity.ok(productos.stream().map(ProductoResponse::desde).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(ProductoResponse.desde(productoService.obtenerProducto(id)));
    }

    @PutMapping(path = "/{id}/precio", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<ProductoResponse> actualizarPrecio(@PathVariable Integer id,
                                                             @Valid @RequestBody ActualizarPrecioRequest solicitud) {
        return ResponseEntity.ok(ProductoResponse.desde(productoService.actualizarPrecio(id, solicitud.precio())));
    }
}
