package com.polimarket.api.service.impl.entregas;

import com.polimarket.api.entity.DetalleVenta;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.entregas.EntregaRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.entregas.EntregaService;
import com.polimarket.api.service.ventas.VentaService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordina Ventas y Bodega. La confirmacion bloquea la entrega y cada inventario afectado;
 * cualquier error revierte las salidas ya aplicadas y el cambio de estado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository entregaRepository;
    private final VentaService ventaService;
    private final InventarioService inventarioService;

    @Override
    public List<Entrega> consultarPendientes() {
        return entregaRepository.findByEstado(ESTADO_PENDIENTE);
    }

    @Override
    public Entrega consultarEntrega(Integer idEntrega) {
        if (idEntrega == null) {
            throw new DatosInvalidosException("El identificador de la entrega es obligatorio");
        }
        return entregaRepository.findById(idEntrega)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega", idEntrega));
    }

    @Override
    public Entrega consultarEntregaPorVenta(Integer idVenta) {
        ventaService.consultarVenta(idVenta);
        return entregaRepository.findByVentaId(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La venta " + idVenta + " no tiene entrega"));
    }

    @Override
    public List<Entrega> consultarPorEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new DatosInvalidosException("El estado es obligatorio");
        }
        return entregaRepository.findByEstado(estado.trim());
    }

    @Override
    public List<Entrega> consultarPorRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new DatosInvalidosException("El rango de fechas es invalido");
        }
        return entregaRepository.findByFechaBetween(desde, hasta);
    }

    @Override
    @Transactional
    public Entrega registrarEntrega(Integer idVenta, String direccion) {
        if (direccion == null || direccion.isBlank()) {
            throw new DatosInvalidosException("La direccion de entrega es obligatoria");
        }
        Venta venta = ventaService.consultarVenta(idVenta);
        validarVentaRegistrada(venta);
        if (entregaRepository.findByVentaId(idVenta).isPresent()) {
            throw new OperacionNoPermitidaException("La venta " + idVenta + " ya tiene una entrega registrada");
        }
        Entrega entrega = Entrega.builder()
                .venta(venta)
                .direccion(direccion.trim())
                .fecha(LocalDate.now())
                .estado(ESTADO_PENDIENTE)
                .build();
        Entrega guardada = entregaRepository.save(entrega);
        log.info("Entrega {} registrada para la venta {}", guardada.getId(), idVenta);
        return guardada;
    }

    @Override
    @Transactional
    public Entrega confirmarEntrega(Integer idEntrega, Integer idBodega) {
        if (idEntrega == null) {
            throw new DatosInvalidosException("El identificador de la entrega es obligatorio");
        }
        if (idBodega == null) {
            throw new DatosInvalidosException("El identificador de la bodega es obligatorio");
        }
        // El bloqueo serializa confirmaciones concurrentes: la segunda ve el estado ENTREGADA y se rechaza.
        Entrega entrega = entregaRepository.findConBloqueoById(idEntrega)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega", idEntrega));
        if (!ESTADO_PENDIENTE.equals(entrega.getEstado())) {
            throw new OperacionNoPermitidaException("La entrega " + idEntrega + " esta en estado "
                    + entrega.getEstado() + "; solo se confirman entregas " + ESTADO_PENDIENTE);
        }
        Venta venta = entrega.getVenta();
        validarVentaRegistrada(venta);

        // Orden ascendente por producto: todas las transacciones bloquean inventarios en el mismo orden.
        Map<Integer, Integer> cantidades = new TreeMap<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            cantidades.merge(detalle.getProducto().getId(), detalle.getCantidad(), Integer::sum);
        }
        cantidades.forEach((idProducto, cantidad) ->
                inventarioService.registrarSalida(idProducto, idBodega, cantidad, entrega));

        entrega.registrarEntrega();
        log.info("Entrega {} confirmada desde la bodega {}", idEntrega, idBodega);
        return entregaRepository.save(entrega);
    }

    private static void validarVentaRegistrada(Venta venta) {
        if (!VentaService.ESTADO_REGISTRADA.equals(venta.getEstado())) {
            throw new OperacionNoPermitidaException("La venta " + venta.getId() + " esta en estado "
                    + venta.getEstado() + "; solo se entregan ventas " + VentaService.ESTADO_REGISTRADA);
        }
    }
}
