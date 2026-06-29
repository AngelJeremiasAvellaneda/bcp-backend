package pe.bancoconfianza.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.bancoconfianza.backend.dto.*;
import pe.bancoconfianza.backend.service.CreditoService;

import java.util.List;

/**
 * Endpoints del módulo de Crédito.
 *
 * Homebanking (cliente):
 *   POST   /api/creditos/solicitar          → solicitar crédito
 *   GET    /api/creditos/mis-solicitudes     → mis créditos
 *   GET    /api/creditos/{id}               → detalle de un crédito
 *   GET    /api/creditos/{id}/cronograma    → cronograma de cuotas
 *
 * Core (operadores):
 *   GET    /api/creditos/pendientes         → bandeja de pendientes por rol
 *   PUT    /api/creditos/{id}/resolver      → aprobar / rechazar
 *   POST   /api/creditos/{id}/desembolsar   → desembolso → acredita saldo
 */
@RestController
@RequestMapping("/api/creditos")
public class CreditoController {

    private final CreditoService creditoService;

    public CreditoController(CreditoService creditoService) {
        this.creditoService = creditoService;
    }

    /* ── Homebanking ── */

    @PostMapping("/simular")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SimulacionCreditoDto> simular(
            @Valid @RequestBody SolicitudCreditoRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.simularCredito(req, userDetails.getUsername()));
    }

    @PostMapping("/solicitar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<CreditoDto> solicitar(
            @Valid @RequestBody SolicitudCreditoRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.solicitarCredito(req, userDetails.getUsername()));
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<CreditoDto>> misSolicitudes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.getMisSolicitudes(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreditoDto> getCredito(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.getCredito(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/cronograma")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CuotaDto>> cronograma(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.getCronograma(id, userDetails.getUsername()));
    }

    /* ── Core ── */

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ASESOR','ADMIN','JEFE_REGIONAL','RIESGOS','COMITE','GERENCIA')")
    public ResponseEntity<List<CreditoDto>> pendientes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.getCreditosPendientes(userDetails.getUsername()));
    }

    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('ASESOR','ADMIN','JEFE_REGIONAL','RIESGOS','COMITE','GERENCIA')")
    public ResponseEntity<CreditoDto> resolver(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionCreditoRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.resolverCredito(id, req, userDetails.getUsername()));
    }

    @PostMapping("/{id}/desembolsar")
    @PreAuthorize("hasAnyRole('ADMIN','JEFE_REGIONAL','GERENCIA')")
    public ResponseEntity<CreditoDto> desembolsar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(creditoService.desembolsar(id, userDetails.getUsername()));
    }

    /**
     * GET /api/creditos/todas  — todas las solicitudes (sin filtrar por estado)
     * Para ADMIN/GERENCIA — vista general de la cartera.
     */
    @GetMapping("/todas")
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA','JEFE_REGIONAL')")
    public ResponseEntity<List<CreditoDto>> todas() {
        return ResponseEntity.ok(creditoService.getTodasLasSolicitudes());
    }
}
