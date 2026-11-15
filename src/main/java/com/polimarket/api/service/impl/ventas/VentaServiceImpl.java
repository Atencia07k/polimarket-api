package com.polimarket.api.service.impl.ventas;

import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.DetalleVenta;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.exception.VendedorNoAutorizadoException;
import com.polimarket.api.repository.ventas.VentaRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.bodega.ProductoService;
import com.polimarket.api.service.clientes.ClienteService;
import com.polimarket.api.service.rrhh.AutorizacionRRHHService;
import com.polimarket.api.service.rrhh.VendedorService;
import com.polimarket.api.service.ventas.VentaService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordina Clientes, Recursos Humanos y Bodega. No descuenta inventario: eso ocurre en Entregas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteService clienteService;
    private final VendedorService vendedorService;
    private final AutorizacionRRHHService autorizacionRRHHService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    @Override
    public List<Producto> listarProductos() {
        return productoService.listarProductos();
    }

    @Override
    public List<Cliente> listarClientes() {
        return clienteService.listarClientes();
    }

    @Override
    public Venta consultarVenta(Integer idVenta) {
        if (idVenta == null) {
            throw new DatosInvalidosException("El identificador de la venta es obligatorio");
        }
        return ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", idVenta));
    }

    @Override
    public List<Venta> consultarVentasPorVendedor(Integer idVendedor) {
        vendedorService.obtenerVendedor(idVendedor);
        return ventaRepository.findByVendedorId(idVendedor);
    }

    @Override
    public List<Venta> consultarVentasPorCliente(Integer idCliente) {
        clienteService.obtenerCliente(idCliente);
        return ventaRepository.findByClienteId(idCliente);
    }

    @Override
    @Transactional
    public Venta registrarVenta(Integer idVendedor, Integer idCliente, List<ItemVenta> items) {
        validarItems(items);
        Vendedor vendedor = vendedorService.obtenerVendedor(idVendedor);
        if (!autorizacionRRHHService.validarVendedor(idVendedor)) {
            throw new VendedorNoAutorizadoException(idVendedor);
        }
        Cliente cliente = clienteService.obtenerCliente(idCliente);

        Map<Integer, Producto> productos = new LinkedHashMap<>();
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (ItemVenta item : items) {
            productos.computeIfAbsent(item.idProducto(), productoService::obtenerProducto);
            cantidades.merge(item.idProducto(), item.cantidad(), Integer::sum);
        }
        cantidades.forEach((idProducto, cantidad) -> {
            int disponible = inventarioService.consultarStockTotal(idProducto);
            if (disponible < cantidad) {
                throw new InventarioInsuficienteException("Existencia insuficiente del producto " + idProducto
                        + ": disponible " + disponible + ", solicitado " + cantidad);
            }
        });

        Venta venta = Venta.builder()
                .vendedor(vendedor)
                .cliente(cliente)
                .fecha(LocalDate.now())
                .estado(ESTADO_REGISTRADA)
                .build();
        for (ItemVenta item : items) {
            Producto producto = productos.get(item.idProducto());
            venta.agregarDetalle(DetalleVenta.builder()
                    .producto(producto)
                    .cantidad(item.cantidad())
                    .precio(producto.getPrecio())
                    .build());
        }
        venta.calcularTotal();

        Venta guardada = ventaRepository.save(venta);
        log.info("Venta {} registrada por el vendedor {} para el cliente {}", guardada.getId(), idVendedor, idCliente);
        return guardada;
    }

    @Override
    @Transactional
    public Venta cancelarVenta(Integer idVenta) {
        Venta venta = consultarVenta(idVenta);
        if (!ESTADO_REGISTRADA.equals(venta.getEstado())) {
            throw new OperacionNoPermitidaException("Solo se cancelan ventas " + ESTADO_REGISTRADA
                    + "; la venta " + idVenta + " esta " + venta.getEstado());
        }
        Entrega entrega = venta.getEntrega();
        if (entrega != null && Entrega.ESTADO_ENTREGADA.equals(entrega.getEstado())) {
            throw new OperacionNoPermitidaException("La venta " + idVenta + " ya fue entregada");
        }
        venta.cancelar();
        log.info("Venta {} cancelada", idVenta);
        return ventaRepository.save(venta);
    }

    private static void validarItems(List<ItemVenta> items) {
        if (items == null || items.isEmpty()) {
            throw new DatosInvalidosException("La venta debe tener al menos un producto");
        }
        for (ItemVenta item : items) {
            if (item == null || item.idProducto() == null) {
                throw new DatosInvalidosException("Cada item debe indicar el producto");
            }
            if (item.cantidad() <= 0) {
                throw new DatosInvalidosException("La cantidad del producto " + item.idProducto() + " debe ser mayor que cero");
            }
        }
    }
}
