package pe.bancoconfianza.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.bancoconfianza.backend.service.AuditoriaService;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de auditoría.
 *
 * GET /api/auditoria               → últimos 50 eventos (ADMIN/GERENCIA)
 * GET /api/auditoria/mis-accesos   → mis últimos 20 eventos (cualquier autenticado)
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA')")
    public ResponseEntity<List<Map<String, Object>>> getEventosRecientes() {
        return ResponseEntity.ok(auditoriaService.getEventosRecientes());
    }

    @GetMapping("/mis-accesos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getMisAccesos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(auditoriaService.getEventosPorUsuario(userDetails.getUsername()));
    }
}
