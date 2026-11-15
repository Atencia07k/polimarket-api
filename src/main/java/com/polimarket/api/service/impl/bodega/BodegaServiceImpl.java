package com.polimarket.api.service.impl.bodega;

import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.service.bodega.BodegaService;
import com.polimarket.api.service.bodega.InventarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BodegaServiceImpl implements BodegaService {

    private final BodegaRepository bodegaRepository;
    private final InventarioService inventarioService;

    @Override
    public List<Bodega> listarBodegas() {
        return bodegaRepository.findAll();
    }

    @Override
    public Bodega obtenerBodega(Integer idBodega) {
        if (idBodega == null) {
            throw new DatosInvalidosException("El identificador de la bodega es obligatorio");
        }
        return bodegaRepository.findById(idBodega)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bodega", idBodega));
    }

    @Override
    public List<Bodega> buscarBodegasPorUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank()) {
            throw new DatosInvalidosException("La ubicacion a buscar es obligatoria");
        }
        return bodegaRepository.findByUbicacionContainingIgnoreCase(ubicacion.trim());
    }

    @Override
    public int consultarDisponibilidad(Integer idBodega) {
        return obtenerBodega(idBodega).consultarDisponibilidad();
    }

    @Override
    public List<Inventario> consultarProductosAlmacenados(Integer idBodega) {
        return inventarioService.consultarInventarioPorBodega(idBodega);
    }
}
