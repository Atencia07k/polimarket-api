package com.polimarket.api.service.impl.clientes;

import com.polimarket.api.entity.Cliente;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.clientes.ClienteRepository;
import com.polimarket.api.service.clientes.ClienteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    @Override
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @Override
    public Cliente obtenerCliente(Integer idCliente) {
        if (idCliente == null) {
            throw new DatosInvalidosException("El identificador del cliente es obligatorio");
        }
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", idCliente));
    }

    @Override
    public List<Cliente> buscarClientesPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre a buscar es obligatorio");
        }
        return clienteRepository.findByNombreContainingIgnoreCase(nombre.trim());
    }

    @Override
    @Transactional
    public Cliente actualizarDatos(Integer idCliente, String nombre, String direccion) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre del cliente es obligatorio");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new DatosInvalidosException("La direccion del cliente es obligatoria");
        }
        Cliente cliente = obtenerCliente(idCliente);
        cliente.setNombre(nombre.trim());
        cliente.setDireccion(direccion.trim());
        log.info("Cliente {} actualizado", idCliente);
        return clienteRepository.save(cliente);
    }

    @Override
    public boolean existeCliente(Integer idCliente) {
        return idCliente != null && clienteRepository.existsById(idCliente);
    }
}
