package com.polimarket.api.service.impl.bodega;

import com.polimarket.api.entity.Producto;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.bodega.ProductoRepository;
import com.polimarket.api.service.bodega.ProductoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    @Override
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @Override
    public Producto obtenerProducto(Integer idProducto) {
        if (idProducto == null) {
            throw new DatosInvalidosException("El identificador del producto es obligatorio");
        }
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", idProducto));
    }

    @Override
    public List<Producto> buscarProductosPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre a buscar es obligatorio");
        }
        return productoRepository.findByNombreContainingIgnoreCase(nombre.trim());
    }

    @Override
    @Transactional
    public Producto actualizarPrecio(Integer idProducto, double precio) {
        if (Double.isNaN(precio) || Double.isInfinite(precio) || precio < 0) {
            throw new DatosInvalidosException("El precio debe ser un numero mayor o igual a cero");
        }
        Producto producto = obtenerProducto(idProducto);
        producto.actualizarPrecio(precio);
        log.info("Precio del producto {} actualizado", idProducto);
        return productoRepository.save(producto);
    }

    @Override
    public List<Producto> consultarProductosPorProveedor(String nitProveedor) {
        if (nitProveedor == null || nitProveedor.isBlank()) {
            throw new DatosInvalidosException("El NIT del proveedor es obligatorio");
        }
        return productoRepository.findByProveedoresNit(nitProveedor.trim());
    }

    @Override
    public boolean existeProducto(Integer idProducto) {
        return idProducto != null && productoRepository.existsById(idProducto);
    }
}
