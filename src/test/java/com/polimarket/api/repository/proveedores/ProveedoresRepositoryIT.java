package com.polimarket.api.repository.proveedores;

import static org.assertj.core.api.Assertions.assertThat;

import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.DetalleCompra;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.repository.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProveedoresRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private CompraProveedorRepository compraRepository;

    @Autowired
    private DetalleCompraRepository detalleCompraRepository;

    @Test
    void consultaProveedorPorNitNombreYProducto() {
        Proveedor alimentos = proveedorRepository.save(
                Proveedor.builder().nit("900123456-1").nombre("Alimentos SA").contacto("ventas@alimentos.co").build());
        Proveedor granos = proveedor("800111222-3", "Granos Ltda");
        Producto arroz = producto("Arroz", alimentos);
        arroz.agregarProveedor(granos);
        Producto cafe = producto("Cafe", granos);
        flushAndClear();

        assertThat(proveedorRepository.findById("900123456-1"))
                .get().extracting(Proveedor::getNombre).isEqualTo("Alimentos SA");
        assertThat(proveedorRepository.existsById("000000000-0")).isFalse();
        assertThat(proveedorRepository.findByNombreContainingIgnoreCase("granos"))
                .extracting(Proveedor::getNit).containsExactly("800111222-3");
        assertThat(proveedorRepository.findByProductosId(arroz.getId()))
                .extracting(Proveedor::getNit).containsExactlyInAnyOrder("900123456-1", "800111222-3");
        assertThat(proveedorRepository.findByProductosId(cafe.getId()))
                .extracting(Proveedor::getNit).containsExactly("800111222-3");
    }

    @Test
    void guardarConNitExistenteActualizaElProveedor() {
        proveedor("900123456-1", "Alimentos SA");
        flushAndClear();

        proveedorRepository.save(
                Proveedor.builder().nit("900123456-1").nombre("Alimentos SAS").contacto("nuevo@alimentos.co").build());
        flushAndClear();

        assertThat(proveedorRepository.count()).isEqualTo(1);
        assertThat(proveedorRepository.findById("900123456-1"))
                .get().extracting(Proveedor::getNombre).isEqualTo("Alimentos SAS");
    }

    @Test
    void consultaComprasPorProveedorEstadoYFechasConSusDetalles() {
        Proveedor alimentos = proveedor("900123456-1", "Alimentos SA");
        Proveedor granos = proveedor("800111222-3", "Granos Ltda");
        Producto arroz = producto("Arroz", alimentos);
        Producto cafe = producto("Cafe", alimentos);

        CompraProveedor c1 = CompraProveedor.builder()
                .proveedor(alimentos).estado(ESTADO_PRUEBA).fecha(HOY.minusDays(7)).build();
        c1.agregarDetalle(DetalleCompra.builder().producto(arroz).cantidad(100).precio(2800.0).build());
        c1.agregarDetalle(DetalleCompra.builder().producto(cafe).cantidad(20).precio(7000.0).build());
        compraRepository.save(c1);
        CompraProveedor c2 = CompraProveedor.builder()
                .proveedor(granos).estado(CompraProveedor.ESTADO_CANCELADA).fecha(HOY).build();
        c2.agregarDetalle(DetalleCompra.builder().producto(arroz).cantidad(50).precio(2700.0).build());
        compraRepository.save(c2);
        flushAndClear();

        assertThat(compraRepository.findByProveedorNit("900123456-1"))
                .extracting(CompraProveedor::getId).containsExactly(c1.getId());
        assertThat(compraRepository.findByEstado(CompraProveedor.ESTADO_CANCELADA))
                .extracting(CompraProveedor::getId).containsExactly(c2.getId());
        assertThat(compraRepository.findByFechaBetween(HOY.minusDays(7), HOY.minusDays(1)))
                .extracting(CompraProveedor::getId).containsExactly(c1.getId());
        assertThat(compraRepository.findById(c2.getId())).isPresent();

        assertThat(detalleCompraRepository.findByCompraId(c1.getId()))
                .hasSize(2)
                .allSatisfy(d -> assertThat(d.getCompra().getId()).isEqualTo(c1.getId()))
                .extracting(d -> d.getProducto().getNombre())
                .containsExactlyInAnyOrder("Arroz", "Cafe");
        assertThat(detalleCompraRepository.findByProductoId(arroz.getId()))
                .extracting(DetalleCompra::getCantidad).containsExactlyInAnyOrder(100, 50);

        Integer detalleId = detalleCompraRepository.findByProductoId(cafe.getId()).getFirst().getId();
        assertThat(detalleCompraRepository.findById(detalleId)).isPresent();
    }
}
