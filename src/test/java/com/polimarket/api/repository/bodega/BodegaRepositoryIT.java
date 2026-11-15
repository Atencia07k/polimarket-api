package com.polimarket.api.repository.bodega;

import static org.assertj.core.api.Assertions.assertThat;

import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.repository.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BodegaRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private BodegaRepository bodegaRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private MovimientoStockRepository movimientoRepository;

    @Test
    void buscaProductosPorNombreYPorProveedor() {
        Proveedor alimentos = proveedor("900123456-1", "Alimentos SA");
        Proveedor granos = proveedor("800111222-3", "Granos Ltda");
        Producto arroz = Producto.builder().nombre("Arroz Diana").precio(3500.0).build();
        arroz.agregarProveedor(alimentos);
        productoRepository.save(arroz);
        producto("Arroz Roa", granos);
        producto("Cafe", granos);
        flushAndClear();

        assertThat(productoRepository.findByNombreContainingIgnoreCase("arroz"))
                .extracting(Producto::getNombre).containsExactlyInAnyOrder("Arroz Diana", "Arroz Roa");
        assertThat(productoRepository.findByProveedoresNit("900123456-1"))
                .extracting(Producto::getId).containsExactly(arroz.getId());
        assertThat(productoRepository.findByProveedoresNit("800111222-3"))
                .extracting(Producto::getNombre).containsExactlyInAnyOrder("Arroz Roa", "Cafe");
        assertThat(productoRepository.findById(arroz.getId())).isPresent();
    }

    @Test
    void buscaBodegasPorUbicacion() {
        Bodega norte = bodegaRepository.save(Bodega.builder().ubicacion("Medellin - Norte").build());
        bodega("Bogota - Sur");
        flushAndClear();

        assertThat(bodegaRepository.findByUbicacionContainingIgnoreCase("medellin"))
                .extracting(Bodega::getId).containsExactly(norte.getId());
        assertThat(bodegaRepository.findById(norte.getId())).isPresent();
    }

    @Test
    void consultaInventarioPorProductoBodegaYReposicion() {
        Proveedor proveedor = proveedor("900123456-1", "Alimentos SA");
        Bodega norte = bodega("Norte");
        Bodega sur = bodega("Sur");
        Producto arroz = producto("Arroz", proveedor);
        Producto cafe = producto("Cafe", proveedor);
        Inventario arrozNorte = inventario(norte, arroz, 50, 10);
        Inventario arrozSur = inventario(sur, arroz, 10, 10);
        Inventario cafeNorte = inventario(norte, cafe, 3, 5);
        flushAndClear();

        assertThat(inventarioRepository.findByBodegaIdAndProductoId(norte.getId(), arroz.getId()))
                .get().extracting(Inventario::getId, Inventario::getStock)
                .containsExactly(arrozNorte.getId(), 50);
        assertThat(inventarioRepository.findByBodegaIdAndProductoId(sur.getId(), cafe.getId())).isEmpty();
        assertThat(inventarioRepository.existsByBodegaIdAndProductoId(sur.getId(), arroz.getId())).isTrue();
        assertThat(inventarioRepository.existsByBodegaIdAndProductoId(sur.getId(), cafe.getId())).isFalse();
        assertThat(inventarioRepository.findByProductoId(arroz.getId()))
                .extracting(Inventario::getId).containsExactlyInAnyOrder(arrozNorte.getId(), arrozSur.getId());
        assertThat(inventarioRepository.findByBodegaId(norte.getId()))
                .extracting(Inventario::getId).containsExactlyInAnyOrder(arrozNorte.getId(), cafeNorte.getId());

        // stock = minimo (10/10) y stock < minimo (3/5) requieren reposicion; 50/10 no
        assertThat(inventarioRepository.findQueRequierenReposicion())
                .extracting(Inventario::getId).containsExactlyInAnyOrder(arrozSur.getId(), cafeNorte.getId());
        assertThat(inventarioRepository.findQueRequierenReposicion())
                .allSatisfy(i -> assertThat(i.requiereReposicion()).isTrue());
    }

    @Test
    void consultaMovimientosPorInventarioTipoFechaEntregaYCompra() {
        Proveedor proveedor = proveedor("800111222-3", "Granos Ltda");
        Producto arroz = producto("Arroz", proveedor);
        Inventario inv = inventario(bodega("Norte"), arroz, 20, 5);
        Inventario otroInv = inventario(bodega("Sur"), producto("Cafe", proveedor), 20, 5);
        CompraProveedor compra = compra(proveedor, ESTADO_PRUEBA, HOY, arroz);
        Entrega entrega = entrega(venta(vendedor("Laura"), cliente("Ana"), ESTADO_PRUEBA, HOY, arroz),
                ESTADO_PRUEBA, HOY);
        MovimientoStock entrada = movimiento(inv, MovimientoStock.TIPO_ENTRADA, HOY.minusDays(3), null, compra);
        MovimientoStock salida = movimiento(inv, MovimientoStock.TIPO_SALIDA, HOY, entrega, null);
        MovimientoStock sinOrigen = movimiento(otroInv, MovimientoStock.TIPO_ENTRADA, HOY.minusDays(20), null, null);
        flushAndClear();

        assertThat(movimientoRepository.findByInventarioId(inv.getId()))
                .extracting(MovimientoStock::getId).containsExactlyInAnyOrder(entrada.getId(), salida.getId());
        assertThat(movimientoRepository.findByTipo(MovimientoStock.TIPO_ENTRADA))
                .extracting(MovimientoStock::getId).containsExactlyInAnyOrder(entrada.getId(), sinOrigen.getId());
        assertThat(movimientoRepository.findByFechaBetween(HOY.minusDays(3), HOY))
                .extracting(MovimientoStock::getId).containsExactlyInAnyOrder(entrada.getId(), salida.getId());
        assertThat(movimientoRepository.findByEntregaId(entrega.getId()))
                .extracting(MovimientoStock::getId).containsExactly(salida.getId());
        assertThat(movimientoRepository.findByCompraId(compra.getId()))
                .extracting(MovimientoStock::getId).containsExactly(entrada.getId());
        // Las relaciones con entrega y compra son opcionales
        assertThat(movimientoRepository.findById(sinOrigen.getId())).get()
                .satisfies(m -> {
                    assertThat(m.getEntrega()).isNull();
                    assertThat(m.getCompra()).isNull();
                });
    }
}
