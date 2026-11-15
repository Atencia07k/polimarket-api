package com.polimarket.api.service.impl.proveedores;

import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.DetalleCompra;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.proveedores.CompraProveedorRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.bodega.ProductoService;
import com.polimarket.api.service.proveedores.CompraProveedorService;
import com.polimarket.api.service.proveedores.ProveedorService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompraProveedorServiceImpl implements CompraProveedorService {

    private final CompraProveedorRepository compraProveedorRepository;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    @Override
    @Transactional
    public CompraProveedor registrarCompra(String nitProveedor, List<ItemCompra> items) {
        validarItems(items);
        Proveedor proveedor = proveedorService.obtenerProveedor(nitProveedor);

        CompraProveedor compra = CompraProveedor.builder()
                .proveedor(proveedor)
                .fecha(LocalDate.now())
                .estado(ESTADO_PENDIENTE)
                .build();
        for (ItemCompra item : items) {
            Producto producto = productoService.obtenerProducto(item.idProducto());
            boolean suministrado = producto.getProveedores().stream()
                    .anyMatch(p -> proveedor.getNit().equals(p.getNit()));
            if (!suministrado) {
                throw new DatosInvalidosException("El producto " + item.idProducto()
                        + " no es suministrado por el proveedor " + proveedor.getNit());
            }
            compra.agregarDetalle(DetalleCompra.builder()
                    .producto(producto)
                    .cantidad(item.cantidad())
                    .precio(item.precio())
                    .build());
        }

        CompraProveedor guardada = compraProveedorRepository.save(compra);
        log.info("Compra {} registrada al proveedor {} por {}", guardada.getId(), proveedor.getNit(),
                guardada.calcularTotal());
        return guardada;
    }

    @Override
    public CompraProveedor consultarCompra(Integer idCompra) {
        if (idCompra == null) {
            throw new DatosInvalidosException("El identificador de la compra es obligatorio");
        }
        return compraProveedorRepository.findById(idCompra)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra", idCompra));
    }

    @Override
    public List<CompraProveedor> consultarComprasPorProveedor(String nitProveedor) {
        Proveedor proveedor = proveedorService.obtenerProveedor(nitProveedor);
        return compraProveedorRepository.findByProveedorNit(proveedor.getNit());
    }

    @Override
    public List<CompraProveedor> consultarComprasPendientes() {
        return compraProveedorRepository.findByEstado(ESTADO_PENDIENTE);
    }

    @Override
    public double calcularTotal(Integer idCompra) {
        return consultarCompra(idCompra).calcularTotal();
    }

    @Override
    @Transactional
    public CompraProveedor recibirCompra(Integer idCompra, Integer idBodega) {
        CompraProveedor compra = obtenerPendienteConBloqueo(idCompra);

        // Orden ascendente por producto: todas las transacciones bloquean inventarios en el mismo orden.
        Map<Integer, Integer> cantidades = new TreeMap<>();
        for (DetalleCompra detalle : compra.getDetalles()) {
            cantidades.merge(detalle.getProducto().getId(), detalle.getCantidad(), Integer::sum);
        }
        cantidades.forEach((idProducto, cantidad) ->
                inventarioService.registrarEntrada(idProducto, idBodega, cantidad, compra));

        compra.setEstado(ESTADO_RECIBIDA);
        log.info("Compra {} recibida en la bodega {}", idCompra, idBodega);
        return compraProveedorRepository.save(compra);
    }

    @Override
    @Transactional
    public CompraProveedor cancelarCompra(Integer idCompra) {
        CompraProveedor compra = obtenerPendienteConBloqueo(idCompra);
        compra.cancelar();
        log.info("Compra {} cancelada", idCompra);
        return compraProveedorRepository.save(compra);
    }

    private CompraProveedor obtenerPendienteConBloqueo(Integer idCompra) {
        if (idCompra == null) {
            throw new DatosInvalidosException("El identificador de la compra es obligatorio");
        }
        CompraProveedor compra = compraProveedorRepository.findConBloqueoById(idCompra)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra", idCompra));
        if (!ESTADO_PENDIENTE.equals(compra.getEstado())) {
            throw new OperacionNoPermitidaException("La compra " + idCompra + " esta en estado "
                    + compra.getEstado() + "; solo se permite sobre compras " + ESTADO_PENDIENTE);
        }
        return compra;
    }

    private static void validarItems(List<ItemCompra> items) {
        if (items == null || items.isEmpty()) {
            throw new DatosInvalidosException("La compra debe tener al menos un producto");
        }
        for (ItemCompra item : items) {
            if (item == null || item.idProducto() == null) {
                throw new DatosInvalidosException("Cada item debe indicar el producto");
            }
            if (item.cantidad() <= 0) {
                throw new DatosInvalidosException("La cantidad del producto " + item.idProducto() + " debe ser mayor que cero");
            }
            if (Double.isNaN(item.precio()) || Double.isInfinite(item.precio()) || item.precio() < 0) {
                throw new DatosInvalidosException("El precio del producto " + item.idProducto() + " es invalido");
            }
        }
    }
}
