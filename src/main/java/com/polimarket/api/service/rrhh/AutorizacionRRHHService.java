package com.polimarket.api.service.rrhh;

import com.polimarket.api.entity.AutorizacionRRHH;
import java.util.List;

/**
 * Componente Recursos Humanos: autorizacion comercial de vendedores (independiente de JWT).
 * <p>
 * Regla aprobada: un vendedor esta autorizado cuando su registro mas reciente (fecha descendente
 * y, ante empate, id descendente) esta vigente ({@link AutorizacionRRHH#estaVigente()}).
 * Autorizar y revocar agregan un registro nuevo al historial; nunca modifican los anteriores.
 */
public interface AutorizacionRRHHService {

    /**
     * @return true si el vendedor tiene una autorizacion vigente
     * @throws com.polimarket.api.exception.RecursoNoEncontradoException si el vendedor no existe
     */
    boolean validarVendedor(Integer idVendedor);

    /**
     * @throws com.polimarket.api.exception.OperacionNoPermitidaException si ya esta autorizado
     */
    AutorizacionRRHH autorizarVendedor(Integer idVendedor);

    /**
     * @throws com.polimarket.api.exception.OperacionNoPermitidaException si no tiene autorizacion vigente
     */
    AutorizacionRRHH revocarAutorizacion(Integer idVendedor);

    /** Historial del vendedor, del mas reciente al mas antiguo. */
    List<AutorizacionRRHH> consultarHistorial(Integer idVendedor);
}
