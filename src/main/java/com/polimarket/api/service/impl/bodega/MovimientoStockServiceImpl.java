package com.polimarket.api.service.impl.bodega;

import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.bodega.MovimientoStockRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.bodega.MovimientoStockService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solo consultas: la creacion de movimientos pertenece a InventarioService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovimientoStockServiceImpl implements MovimientoStockService {

    private final MovimientoStockRepository movimientoStockRepository;
    private final InventarioService inventarioService;

    @Override
    public List<MovimientoStock> listarMovimientos() {
        return movimientoStockRepository.findAll();
    }

    @Override
    public MovimientoStock obtenerMovimiento(Integer idMovimiento) {
        requerido(idMovimiento, "El identificador del movimiento es obligatorio");
        return movimientoStockRepository.findById(idMovimiento)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento de stock", idMovimiento));
    }

    @Override
    public List<MovimientoStock> consultarPorInventario(Integer idInventario) {
        requerido(idInventario, "El identificador del inventario es obligatorio");
        return movimientoStockRepository.findByInventarioId(idInventario);
    }

    @Override
    public List<MovimientoStock> consultarPorProducto(Integer idProducto) {
        return inventarioService.consultarInventarioPorProducto(idProducto).stream()
                .map(Inventario::getId)
                .flatMap(idInventario -> movimientoStockRepository.findByInventarioId(idInventario).stream())
                .toList();
    }

    @Override
    public List<MovimientoStock> consultarPorFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new DatosInvalidosException("El rango de fechas es invalido");
        }
        return movimientoStockRepository.findByFechaBetween(desde, hasta);
    }

    @Override
    public List<MovimientoStock> consultarPorTipo(String tipo) {
        if (!MovimientoStock.TIPO_ENTRADA.equals(tipo) && !MovimientoStock.TIPO_SALIDA.equals(tipo)) {
            throw new DatosInvalidosException("Tipo de movimiento no valido: " + tipo);
        }
        return movimientoStockRepository.findByTipo(tipo);
    }

    @Override
    public List<MovimientoStock> consultarPorCompra(Integer idCompra) {
        requerido(idCompra, "El identificador de la compra es obligatorio");
        return movimientoStockRepository.findByCompraId(idCompra);
    }

    @Override
    public List<MovimientoStock> consultarPorEntrega(Integer idEntrega) {
        requerido(idEntrega, "El identificador de la entrega es obligatorio");
        return movimientoStockRepository.findByEntregaId(idEntrega);
    }

    private static void requerido(Integer id, String mensaje) {
        if (id == null) {
            throw new DatosInvalidosException(mensaje);
        }
    }
}
