package com.polimarket.api.repository.ventas;

import static org.assertj.core.api.Assertions.assertThat;

import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.DetalleVenta;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.repository.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VentasRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Test
    void consultaVentasPorVendedorClienteEstadoYFechas() {
        Producto arroz = producto("Arroz", proveedor("900123456-1", "Alimentos SA"));
        Vendedor laura = vendedor("Laura");
        Vendedor jorge = vendedor("Jorge");
        Cliente ana = cliente("Ana");
        Cliente pedro = cliente("Pedro");
        Venta v1 = venta(laura, ana, ESTADO_PRUEBA, HOY.minusDays(10), arroz);
        Venta v2 = venta(laura, pedro, Venta.ESTADO_CANCELADA, HOY.minusDays(5), arroz);
        Venta v3 = venta(jorge, ana, ESTADO_PRUEBA, HOY, arroz);
        flushAndClear();

        assertThat(ventaRepository.findByVendedorId(laura.getId()))
                .extracting(Venta::getId).containsExactlyInAnyOrder(v1.getId(), v2.getId());
        assertThat(ventaRepository.findByClienteId(ana.getId()))
                .extracting(Venta::getId).containsExactlyInAnyOrder(v1.getId(), v3.getId());
        assertThat(ventaRepository.findByEstado(Venta.ESTADO_CANCELADA))
                .extracting(Venta::getId).containsExactly(v2.getId());
        // Los extremos del rango son inclusivos
        assertThat(ventaRepository.findByFechaBetween(HOY.minusDays(10), HOY.minusDays(5)))
                .extracting(Venta::getId).containsExactlyInAnyOrder(v1.getId(), v2.getId());
        assertThat(ventaRepository.findById(v3.getId())).isPresent();
    }

    @Test
    void guardaVentaConDetallesEnCascadaYConsultaDetalles() {
        Proveedor proveedor = proveedor("900123456-1", "Alimentos SA");
        Producto arroz = producto("Arroz", proveedor);
        Producto cafe = producto("Cafe", proveedor);

        Venta venta = Venta.builder().vendedor(vendedor("Laura")).cliente(cliente("Ana"))
                .estado(ESTADO_PRUEBA).fecha(HOY).build();
        venta.agregarDetalle(DetalleVenta.builder().producto(arroz).cantidad(2).precio(1500.0).build());
        venta.agregarDetalle(DetalleVenta.builder().producto(cafe).cantidad(1).precio(9000.0).build());
        venta.calcularTotal();
        ventaRepository.save(venta);
        Venta otra = venta(vendedor("Jorge"), cliente("Pedro"), ESTADO_PRUEBA, HOY, arroz);
        flushAndClear();

        assertThat(ventaRepository.findById(venta.getId()))
                .get().extracting(Venta::getTotal).isEqualTo(12000.0);
        assertThat(detalleVentaRepository.findByVentaId(venta.getId()))
                .hasSize(2)
                .allSatisfy(d -> assertThat(d.getVenta().getId()).isEqualTo(venta.getId()))
                .extracting(d -> d.getProducto().getNombre())
                .containsExactlyInAnyOrder("Arroz", "Cafe");
        assertThat(detalleVentaRepository.findByProductoId(arroz.getId()))
                .extracting(d -> d.getVenta().getId())
                .containsExactlyInAnyOrder(venta.getId(), otra.getId());

        Integer detalleId = detalleVentaRepository.findByProductoId(cafe.getId()).getFirst().getId();
        assertThat(detalleVentaRepository.findById(detalleId)).isPresent();
    }
}
