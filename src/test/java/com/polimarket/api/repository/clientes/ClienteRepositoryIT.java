package com.polimarket.api.repository.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import com.polimarket.api.entity.Cliente;
import com.polimarket.api.repository.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ClienteRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void guardaYConsultaPorIdentificador() {
        Cliente guardado = clienteRepository.save(
                Cliente.builder().nombre("Ana Torres").direccion("Calle 10 # 5-20").build());
        flushAndClear();

        assertThat(guardado.getId()).isNotNull();
        assertThat(clienteRepository.findById(guardado.getId()))
                .get().extracting(Cliente::getNombre).isEqualTo("Ana Torres");
        assertThat(clienteRepository.existsById(guardado.getId())).isTrue();
        assertThat(clienteRepository.existsById(-1)).isFalse();
        assertThat(clienteRepository.findById(-1)).isEmpty();
    }

    @Test
    void buscaPorNombreSinDistinguirMayusculas() {
        cliente("Ana Torres");
        cliente("Mariana Ruiz");
        cliente("Pedro Gomez");
        flushAndClear();

        assertThat(clienteRepository.findByNombreContainingIgnoreCase("ANA"))
                .extracting(Cliente::getNombre)
                .containsExactlyInAnyOrder("Ana Torres", "Mariana Ruiz");
        assertThat(clienteRepository.findByNombreContainingIgnoreCase("inexistente")).isEmpty();
    }
}
