package com.polimarket.api.service.bodega;

import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.Inventario;
import java.util.List;

/**
 * Componente Bodega: consulta de bodegas. Las existencias se delegan en {@link InventarioService}.
 */
public interface BodegaService {

    List<Bodega> listarBodegas();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Bodega obtenerBodega(Integer idBodega);

    List<Bodega> buscarBodegasPorUbicacion(String ubicacion);

    /** Total de unidades de la bodega segun {@link Bodega#consultarDisponibilidad()}. */
    int consultarDisponibilidad(Integer idBodega);

    /** Registros de inventario (producto, stock y stock minimo) almacenados en la bodega. */
    List<Inventario> consultarProductosAlmacenados(Integer idBodega);
}
