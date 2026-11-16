package com.polimarket.api.service.autenticacion;

import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import java.util.List;

/**
 * Administracion de los usuarios de la API.
 */
public interface UsuarioService {

    /**
     * Crea un usuario activo. El nombre se normaliza a minusculas y la contrasena se guarda con BCrypt.
     *
     * @throws com.polimarket.api.exception.DatosInvalidosException si el nombre o la contrasena no cumplen la politica
     * @throws com.polimarket.api.exception.OperacionNoPermitidaException si el nombre ya existe
     */
    Usuario crearUsuario(String username, String password, Rol rol);

    List<Usuario> listarUsuarios();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Usuario obtenerUsuario(Integer idUsuario);

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Usuario obtenerPorUsername(String username);

    /**
     * Desactiva el usuario y revoca todos sus refresh tokens. Sus access tokens vigentes expiran solos.
     *
     * @throws com.polimarket.api.exception.OperacionNoPermitidaException si intenta desactivarse a si mismo
     */
    Usuario desactivarUsuario(Integer idUsuario, String usernameSolicitante);

    /**
     * Crea el primer ADMIN solo si no existe ningun ADMIN activo.
     *
     * @return true si lo creo
     */
    boolean crearAdministradorInicial(String username, String password);
}
