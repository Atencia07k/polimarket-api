package com.polimarket.api.controller.rrhh;

import com.polimarket.api.dto.rrhh.VendedorResponse;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.service.rrhh.VendedorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/vendedores", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendedorController {

    private final VendedorService vendedorService;

    @GetMapping
    public ResponseEntity<List<VendedorResponse>> listar(@RequestParam(required = false) String nombre) {
        List<Vendedor> vendedores = nombre == null
                ? vendedorService.listarVendedores()
                : vendedorService.buscarVendedoresPorNombre(nombre);
        return ResponseEntity.ok(vendedores.stream().map(VendedorResponse::desde).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendedorResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(VendedorResponse.desde(vendedorService.obtenerVendedor(id)));
    }
}
