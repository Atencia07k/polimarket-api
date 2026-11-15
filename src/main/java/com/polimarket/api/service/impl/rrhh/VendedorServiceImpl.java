package com.polimarket.api.service.impl.rrhh;

import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.rrhh.VendedorRepository;
import com.polimarket.api.service.rrhh.VendedorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendedorServiceImpl implements VendedorService {

    private final VendedorRepository vendedorRepository;

    @Override
    public List<Vendedor> listarVendedores() {
        return vendedorRepository.findAll();
    }

    @Override
    public Vendedor obtenerVendedor(Integer idVendedor) {
        if (idVendedor == null) {
            throw new DatosInvalidosException("El identificador del vendedor es obligatorio");
        }
        return vendedorRepository.findById(idVendedor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vendedor", idVendedor));
    }

    @Override
    public List<Vendedor> buscarVendedoresPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre a buscar es obligatorio");
        }
        return vendedorRepository.findByNombreContainingIgnoreCase(nombre.trim());
    }

    @Override
    public boolean existeVendedor(Integer idVendedor) {
        return idVendedor != null && vendedorRepository.existsById(idVendedor);
    }
}
