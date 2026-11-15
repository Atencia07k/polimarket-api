package com.polimarket.api;

import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.repository.bodega.InventarioRepository;
import com.polimarket.api.repository.bodega.MovimientoStockRepository;
import com.polimarket.api.repository.bodega.ProductoRepository;
import com.polimarket.api.repository.clientes.ClienteRepository;
import com.polimarket.api.repository.entregas.EntregaRepository;
import com.polimarket.api.repository.proveedores.CompraProveedorRepository;
import com.polimarket.api.repository.proveedores.DetalleCompraRepository;
import com.polimarket.api.repository.proveedores.ProveedorRepository;
import com.polimarket.api.repository.rrhh.AutorizacionRRHHRepository;
import com.polimarket.api.repository.rrhh.VendedorRepository;
import com.polimarket.api.repository.ventas.DetalleVentaRepository;
import com.polimarket.api.repository.ventas.VentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifica que el contexto completo (servicios incluidos) arranca con el perfil test, sin base de datos.
 * Los repositorios JPA se reemplazan por mocks porque el perfil test excluye la auto-configuracion de JPA.
 */
@SpringBootTest
@ActiveProfiles("test")
@MockitoBean(types = {
        BodegaRepository.class, InventarioRepository.class, MovimientoStockRepository.class,
        ProductoRepository.class, ClienteRepository.class, EntregaRepository.class,
        CompraProveedorRepository.class, DetalleCompraRepository.class, ProveedorRepository.class,
        AutorizacionRRHHRepository.class, VendedorRepository.class, DetalleVentaRepository.class,
        VentaRepository.class})
class PolimarketApiApplicationTests {

    @Test
    void contextLoads() {
    }
}