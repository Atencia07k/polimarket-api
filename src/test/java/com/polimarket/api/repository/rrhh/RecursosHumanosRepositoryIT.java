package com.polimarket.api.repository.rrhh;

import static org.assertj.core.api.Assertions.assertThat;

import com.polimarket.api.entity.AutorizacionRRHH;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.repository.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecursosHumanosRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private VendedorRepository vendedorRepository;

    @Autowired
    private AutorizacionRRHHRepository autorizacionRepository;

    @Test
    void guardaYBuscaVendedores() {
        Vendedor laura = vendedorRepository.save(Vendedor.builder().nombre("Laura Mejia").build());
        vendedor("Jorge Diaz");
        flushAndClear();

        assertThat(vendedorRepository.existsById(laura.getId())).isTrue();
        assertThat(vendedorRepository.findById(laura.getId())).isPresent();
        assertThat(vendedorRepository.findByNombreContainingIgnoreCase("mejia"))
                .extracting(Vendedor::getNombre).containsExactly("Laura Mejia");
    }

    @Test
    void historialOrdenaPorFechaDescendenteYDesempataPorId() {
        Vendedor laura = vendedor("Laura");
        Vendedor jorge = vendedor("Jorge");
        AutorizacionRRHH antigua = autorizacion(laura, AutorizacionRRHH.ESTADO_AUTORIZADA, HOY.minusDays(30));
        AutorizacionRRHH mismaFecha1 = autorizacion(laura, AutorizacionRRHH.ESTADO_AUTORIZADA, HOY);
        AutorizacionRRHH mismaFecha2 = autorizacion(laura, AutorizacionRRHH.ESTADO_REVOCADA, HOY);
        AutorizacionRRHH deJorge = autorizacion(jorge, AutorizacionRRHH.ESTADO_REVOCADA, HOY.minusDays(1));
        flushAndClear();

        assertThat(autorizacionRepository.findByVendedorIdOrderByFechaDescIdDesc(laura.getId()))
                .extracting(AutorizacionRRHH::getId)
                .containsExactly(mismaFecha2.getId(), mismaFecha1.getId(), antigua.getId());

        // El mas reciente se recupera tal cual, aunque su estado sea REVOCADA
        assertThat(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(laura.getId()))
                .get().extracting(AutorizacionRRHH::getId, AutorizacionRRHH::getEstado)
                .containsExactly(mismaFecha2.getId(), AutorizacionRRHH.ESTADO_REVOCADA);

        assertThat(autorizacionRepository.findByVendedorId(jorge.getId()))
                .extracting(AutorizacionRRHH::getId).containsExactly(deJorge.getId());
        assertThat(autorizacionRepository.findByEstado(AutorizacionRRHH.ESTADO_REVOCADA))
                .extracting(AutorizacionRRHH::getId)
                .containsExactlyInAnyOrder(mismaFecha2.getId(), deJorge.getId());
    }

    @Test
    void vendedorSinAutorizacionesNoTieneRegistroMasReciente() {
        Vendedor nuevo = vendedor("Sin historial");
        flushAndClear();

        assertThat(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(nuevo.getId())).isEmpty();
        assertThat(autorizacionRepository.findByVendedorIdOrderByFechaDescIdDesc(nuevo.getId())).isEmpty();
    }
}
