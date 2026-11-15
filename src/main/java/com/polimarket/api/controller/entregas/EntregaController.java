package com.polimarket.api.controller.entregas;

import com.polimarket.api.dto.entregas.ConfirmarEntregaRequest;
import com.polimarket.api.dto.entregas.EntregaResponse;
import com.polimarket.api.dto.entregas.RegistrarEntregaRequest;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.service.entregas.EntregaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Registrar una entrega la deja PENDIENTE; confirmarla descuenta el stock una sola vez (EntregaService
 * bloquea la entrega y rechaza confirmaciones repetidas).
 */
@RestController
@RequestMapping(path = "/api/v1/entregas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntregaController {

    private final EntregaService entregaService;

    // EntregaService no ofrece un listado sin filtro; se exige estado o el rango desde/hasta.
    @GetMapping
    public ResponseEntity<List<EntregaResponse>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        boolean porFechas = desde != null || hasta != null;
        if ((estado != null) == porFechas) {
            throw new DatosInvalidosException("Indique exactamente un filtro: estado o el rango desde/hasta");
        }
        List<Entrega> entregas = estado != null
                ? entregaService.consultarPorEstado(estado)
                : entregaService.consultarPorRangoFechas(desde, hasta);
        return ok(entregas);
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<EntregaResponse>> pendientes() {
        return ok(entregaService.consultarPendientes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntregaResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(EntregaResponse.desde(entregaService.consultarEntrega(id)));
    }

    @GetMapping("/ventas/{idVenta}")
    public ResponseEntity<EntregaResponse> porVenta(@PathVariable Integer idVenta) {
        return ResponseEntity.ok(EntregaResponse.desde(entregaService.consultarEntregaPorVenta(idVenta)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<EntregaResponse> registrar(@Valid @RequestBody RegistrarEntregaRequest solicitud) {
        Entrega entrega = entregaService.registrarEntrega(solicitud.idVenta(), solicitud.direccion());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(entrega.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(EntregaResponse.desde(entrega));
    }

    @PatchMapping(path = "/{id}/confirmacion", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<EntregaResponse> confirmar(@PathVariable Integer id,
                                                     @Valid @RequestBody ConfirmarEntregaRequest solicitud) {
        return ResponseEntity.ok(EntregaResponse.desde(entregaService.confirmarEntrega(id, solicitud.idBodega())));
    }

    private static ResponseEntity<List<EntregaResponse>> ok(List<Entrega> entregas) {
        return ResponseEntity.ok(entregas.stream().map(EntregaResponse::desde).toList());
    }
}
