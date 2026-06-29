package pe.bancoconfianza.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.bancoconfianza.backend.dto.GestionCobranzaRequest;
import pe.bancoconfianza.backend.service.RecuperacionesService;

import java.util.List;
import java.util.Map;

/**
 * Módulo Recuperaciones / Mora — R1, R2, R3.
 *
 * R1:
 *   GET  /api/recuperaciones/cartera-morosa      → consulta bandas + KPIs
 *
 * R2:
 *   POST /api/recuperaciones/gestiones            → registrar gestión
 *   GET  /api/recuperaciones/gestiones/{creditoId}→ historial de gestiones
 *
 * R3:
 *   POST /api/recuperaciones/{id}/judicial        → derivar a judicial
 *   POST /api/recuperaciones/{id}/castigar        → castigar crédito
 */
@RestController
@RequestMapping("/api/recuperaciones")
public class RecuperacionesController {

    private final RecuperacionesService recuperacionesService;

    public RecuperacionesController(RecuperacionesService recuperacionesService) {
        this.recuperacionesService = recuperacionesService;
    }

    /* ── R1 ── */

    @GetMapping("/cartera-morosa")
    @PreAuthorize("hasAnyRole('ASESOR','ADMIN','JEFE_REGIONAL','RIESGOS','COMITE','GERENCIA')")
    public ResponseEntity<Map<String, Object>> carteraMorosa() {
        return ResponseEntity.ok(recuperacionesService.getCarteraMorosa());
    }

    /* ── R2 ── */

    @PostMapping("/gestiones")
    @PreAuthorize("hasAnyRole('ASESOR','ADMIN','JEFE_REGIONAL','RIESGOS','GERENCIA')")
    public ResponseEntity<Map<String, Object>> registrarGestion(
            @Valid @RequestBody GestionCobranzaRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            recuperacionesService.registrarGestion(req, userDetails.getUsername())
        );
    }

    @GetMapping("/gestiones/{creditoId}")
    @PreAuthorize("hasAnyRole('ASESOR','ADMIN','JEFE_REGIONAL','RIESGOS','COMITE','GERENCIA')")
    public ResponseEntity<List<Map<String, Object>>> historialGestiones(
            @PathVariable Long creditoId) {
        return ResponseEntity.ok(recuperacionesService.getHistorialGestiones(creditoId));
    }

    /* ── R3 ── */

    @PostMapping("/{id}/judicial")
    @PreAuthorize("hasAnyRole('JEFE_REGIONAL','RIESGOS','GERENCIA','ADMIN')")
    public ResponseEntity<Map<String, Object>> derivarJudicial(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            recuperacionesService.derivarJudicial(id, userDetails.getUsername())
        );
    }

    @PostMapping("/{id}/castigar")
    @PreAuthorize("hasAnyRole('GERENCIA','ADMIN')")
    public ResponseEntity<Map<String, Object>> castigar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            recuperacionesService.castigarCredito(id, userDetails.getUsername())
        );
    }
}
