package com.polimarket.api.service.impl.rrhh;

import com.polimarket.api.entity.AutorizacionRRHH;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.repository.rrhh.AutorizacionRRHHRepository;
import com.polimarket.api.service.rrhh.AutorizacionRRHHService;
import com.polimarket.api.service.rrhh.VendedorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacionRRHHServiceImpl implements AutorizacionRRHHService {

    private final AutorizacionRRHHRepository autorizacionRepository;
    private final VendedorService vendedorService;

    @Override
    public boolean validarVendedor(Integer idVendedor) {
        vendedorService.obtenerVendedor(idVendedor);
        return estaVigente(idVendedor);
    }

    @Override
    @Transactional
    public AutorizacionRRHH autorizarVendedor(Integer idVendedor) {
        Vendedor vendedor = vendedorService.obtenerVendedor(idVendedor);
        if (estaVigente(idVendedor)) {
            throw new OperacionNoPermitidaException("El vendedor " + idVendedor + " ya tiene una autorizacion vigente");
        }
        AutorizacionRRHH autorizacion = AutorizacionRRHH.builder().vendedor(vendedor).build();
        autorizacion.autorizar();
        log.info("Vendedor {} autorizado por RRHH", idVendedor);
        return autorizacionRepository.save(autorizacion);
    }

    @Override
    @Transactional
    public AutorizacionRRHH revocarAutorizacion(Integer idVendedor) {
        Vendedor vendedor = vendedorService.obtenerVendedor(idVendedor);
        if (!estaVigente(idVendedor)) {
            throw new OperacionNoPermitidaException("El vendedor " + idVendedor + " no tiene una autorizacion vigente");
        }
        AutorizacionRRHH revocacion = AutorizacionRRHH.builder().vendedor(vendedor).build();
        revocacion.revocar();
        log.info("Autorizacion del vendedor {} revocada por RRHH", idVendedor);
        return autorizacionRepository.save(revocacion);
    }

    @Override
    public List<AutorizacionRRHH> consultarHistorial(Integer idVendedor) {
        vendedorService.obtenerVendedor(idVendedor);
        return autorizacionRepository.findByVendedorIdOrderByFechaDescIdDesc(idVendedor);
    }

    private boolean estaVigente(Integer idVendedor) {
        return autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(idVendedor)
                .map(AutorizacionRRHH::estaVigente)
                .orElse(false);
    }
}
