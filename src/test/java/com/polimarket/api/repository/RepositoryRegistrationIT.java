package com.polimarket.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.polimarket.api.entity.AutorizacionRRHH;
import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.DetalleCompra;
import com.polimarket.api.entity.DetalleVenta;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.entity.RefreshToken;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.entity.Venta;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.support.Repositories;

/**
 * Verifica que Spring Data JPA detecte los 15 repositorios, cada uno en su subpaquete
 * funcional y con el tipo de identificador real de su entidad.
 */
class RepositoryRegistrationIT extends AbstractRepositoryIT {

    private static final String BASE = "com.polimarket.api.repository.";

    @Autowired
    private ApplicationContext context;

    @Test
    void registraLos15RepositoriosConSuPaqueteEIdentificador() {
        Repositories repositories = new Repositories(context);
        Map<Class<?>, Object[]> esperado = Map.ofEntries(
                Map.entry(Cliente.class, new Object[] {"clientes", Integer.class}),
                Map.entry(Venta.class, new Object[] {"ventas", Integer.class}),
                Map.entry(DetalleVenta.class, new Object[] {"ventas", Integer.class}),
                Map.entry(Vendedor.class, new Object[] {"rrhh", Integer.class}),
                Map.entry(AutorizacionRRHH.class, new Object[] {"rrhh", Integer.class}),
                Map.entry(Producto.class, new Object[] {"bodega", Integer.class}),
                Map.entry(Bodega.class, new Object[] {"bodega", Integer.class}),
                Map.entry(Inventario.class, new Object[] {"bodega", Integer.class}),
                Map.entry(MovimientoStock.class, new Object[] {"bodega", Integer.class}),
                Map.entry(Proveedor.class, new Object[] {"proveedores", String.class}),
                Map.entry(CompraProveedor.class, new Object[] {"proveedores", Integer.class}),
                Map.entry(DetalleCompra.class, new Object[] {"proveedores", Integer.class}),
                Map.entry(Entrega.class, new Object[] {"entregas", Integer.class}),
                Map.entry(Usuario.class, new Object[] {"autenticacion", Integer.class}),
                Map.entry(RefreshToken.class, new Object[] {"autenticacion", Long.class}));

        assertThat(repositories).hasSize(15);
        esperado.forEach((entidad, datos) -> {
            var info = repositories.getRepositoryInformationFor(entidad).orElseThrow();
            assertThat(info.getRepositoryInterface().getPackageName()).isEqualTo(BASE + datos[0]);
            assertThat(info.getRepositoryInterface().getSimpleName()).isEqualTo(entidad.getSimpleName() + "Repository");
            assertThat(info.getIdType()).isEqualTo(datos[1]);
        });
    }
}
