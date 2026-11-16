package com.polimarket.api.repository.autenticacion;

import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Usuario}. Los nombres de usuario se guardan normalizados en minusculas.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRolAndActivoTrue(Rol rol);
}
