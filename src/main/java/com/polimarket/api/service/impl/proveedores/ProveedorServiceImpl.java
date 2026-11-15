package com.polimarket.api.service.impl.proveedores;

import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.proveedores.ProveedorRepository;
import com.polimarket.api.service.proveedores.ProveedorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Override
    public List<Proveedor> listarProveedores() {
        return proveedorRepository.findAll();
    }

    @Override
    public Proveedor obtenerProveedor(String nit) {
        if (nit == null || nit.isBlank()) {
            throw new DatosInvalidosException("El NIT del proveedor es obligatorio");
        }
        return proveedorRepository.findById(nit.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", nit));
    }

    @Override
    public List<Proveedor> buscarProveedoresPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre a buscar es obligatorio");
        }
        return proveedorRepository.findByNombreContainingIgnoreCase(nombre.trim());
    }

    @Override
    public List<Proveedor> consultarProveedoresPorProducto(Integer idProducto) {
        if (idProducto == null) {
            throw new DatosInvalidosException("El identificador del producto es obligatorio");
        }
        return proveedorRepository.findByProductosId(idProducto);
    }

    @Override
    @Transactional
    public Proveedor actualizarContacto(String nit, String contacto) {
        if (contacto == null || contacto.isBlank()) {
            throw new DatosInvalidosException("Los datos de contacto son obligatorios");
        }
        Proveedor proveedor = obtenerProveedor(nit);
        proveedor.actualizarContacto(contacto.trim());
        log.info("Contacto del proveedor {} actualizado", proveedor.getNit());
        return proveedorRepository.save(proveedor);
    }
}
