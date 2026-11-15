package com.polimarket.api.service.impl.bodega;

import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.repository.bodega.InventarioRepository;
import com.polimarket.api.repository.bodega.MovimientoStockRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.bodega.ProductoService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las entradas y salidas bloquean el registro de inventario (SELECT ... FOR UPDATE), aplican el
 * movimiento con {@link MovimientoStock#registrar()} y lo persisten en la misma transaccion.
 * Bodega se valida con su repositorio (misma area) para no depender de BodegaService, que a su
 * vez reutiliza este servicio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final BodegaRepository bodegaRepository;
    private final ProductoService productoService;

    @Override
    public int consultarStock(Integer idProducto, Integer idBodega) {
        return obtenerInventario(idProducto, idBodega).consultarExistencia();
    }

    @Override
    public int consultarStockTotal(Integer idProducto) {
        return consultarInventarioPorProducto(idProducto).stream()
                .mapToInt(Inventario::consultarExistencia)
                .sum();
    }

    @Override
    public List<Inventario> consultarInventarioPorProducto(Integer idProducto) {
        productoService.obtenerProducto(idProducto);
        return inventarioRepository.findByProductoId(idProducto);
    }

    @Override
    public List<Inventario> consultarInventarioPorBodega(Integer idBodega) {
        validarBodega(idBodega);
        return inventarioRepository.findByBodegaId(idBodega);
    }

    @Override
    public boolean requiereReposicion(Integer idProducto, Integer idBodega) {
        return obtenerInventario(idProducto, idBodega).requiereReposicion();
    }

    @Override
    public List<Inventario> consultarInventariosQueRequierenReposicion() {
        return inventarioRepository.findQueRequierenReposicion();
    }

    @Override
    @Transactional
    public MovimientoStock registrarEntrada(Integer idProducto, Integer idBodega, int cantidad, CompraProveedor compra) {
        if (compra == null || compra.getId() == null) {
            throw new DatosInvalidosException("La entrada debe asociarse a una compra registrada");
        }
        return registrarMovimiento(MovimientoStock.TIPO_ENTRADA, idProducto, idBodega, cantidad, compra, null);
    }

    @Override
    @Transactional
    public MovimientoStock registrarSalida(Integer idProducto, Integer idBodega, int cantidad, Entrega entrega) {
        if (entrega == null || entrega.getId() == null) {
            throw new DatosInvalidosException("La salida debe asociarse a una entrega registrada");
        }
        return registrarMovimiento(MovimientoStock.TIPO_SALIDA, idProducto, idBodega, cantidad, null, entrega);
    }

    private MovimientoStock registrarMovimiento(String tipo, Integer idProducto, Integer idBodega, int cantidad,
                                                CompraProveedor compra, Entrega entrega) {
        if (cantidad <= 0) {
            throw new DatosInvalidosException("La cantidad del movimiento debe ser mayor que cero");
        }
        validarExistencia(idProducto, idBodega);
        Inventario inventario = inventarioRepository.findConBloqueoByBodegaIdAndProductoId(idBodega, idProducto)
                .orElseThrow(() -> inventarioNoEncontrado(idProducto, idBodega));

        if (MovimientoStock.TIPO_SALIDA.equals(tipo) && inventario.consultarExistencia() < cantidad) {
            throw new InventarioInsuficienteException("Stock insuficiente del producto " + idProducto
                    + " en la bodega " + idBodega + ": existencia " + inventario.consultarExistencia()
                    + ", solicitado " + cantidad);
        }

        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(tipo)
                .cantidad(cantidad)
                .fecha(LocalDate.now())
                .inventario(inventario)
                .compra(compra)
                .entrega(entrega)
                .build();
        movimiento.registrar();
        MovimientoStock guardado = movimientoStockRepository.save(movimiento);
        log.info("{} de {} unidades del producto {} en la bodega {}", tipo, cantidad, idProducto, idBodega);
        return guardado;
    }

    private Inventario obtenerInventario(Integer idProducto, Integer idBodega) {
        validarExistencia(idProducto, idBodega);
        return inventarioRepository.findByBodegaIdAndProductoId(idBodega, idProducto)
                .orElseThrow(() -> inventarioNoEncontrado(idProducto, idBodega));
    }

    private void validarExistencia(Integer idProducto, Integer idBodega) {
        productoService.obtenerProducto(idProducto);
        validarBodega(idBodega);
    }

    private void validarBodega(Integer idBodega) {
        if (idBodega == null) {
            throw new DatosInvalidosException("El identificador de la bodega es obligatorio");
        }
        if (!bodegaRepository.existsById(idBodega)) {
            throw new RecursoNoEncontradoException("Bodega", idBodega);
        }
    }

    private static RecursoNoEncontradoException inventarioNoEncontrado(Integer idProducto, Integer idBodega) {
        return new RecursoNoEncontradoException(
                "No existe inventario del producto " + idProducto + " en la bodega " + idBodega);
    }
}
