package pe.bancoconfianza.backend.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.bancoconfianza.backend.dto.CuentaDto;
import pe.bancoconfianza.backend.dto.MovimientoDto;
import pe.bancoconfianza.backend.dto.TransferenciaRequest;
import pe.bancoconfianza.backend.service.CuentaService;
import pe.bancoconfianza.backend.service.AuditoriaService;
import pe.bancoconfianza.backend.model.AuditoriaEvento;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;
    private final AuditoriaService auditoriaService;

    public CuentaController(CuentaService cuentaService, AuditoriaService auditoriaService) {
        this.cuentaService    = cuentaService;
        this.auditoriaService = auditoriaService;
    }

    /**
     * GET /api/cuentas
     * Devuelve las cuentas activas del usuario autenticado.
     */
    @GetMapping
    public ResponseEntity<List<CuentaDto>> getMisCuentas(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cuentaService.getCuentasDelUsuario(userDetails.getUsername()));
    }

    /**
     * GET /api/cuentas/{id}/movimientos
     * Devuelve los últimos N movimientos de la cuenta indicada (por defecto 20).
     */
    @GetMapping("/{id}/movimientos")
    public ResponseEntity<List<MovimientoDto>> getMovimientos(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cuentaService.getMovimientos(id, userDetails.getUsername(), limit));
    }

    /**
     * POST /api/cuentas/transferir
     * Realiza una transferencia entre cuentas.
     */
    @PostMapping("/transferir")
    public ResponseEntity<Map<String, String>> transferir(
            @Valid @RequestBody TransferenciaRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        cuentaService.transferir(req, userDetails.getUsername());
        auditoriaService.registrar(userDetails.getUsername(), "CLIENTE",
            AuditoriaEvento.TipoAccion.CUENTA_TRANSFERENCIA,
            "CUENTA", "Transferencia S/ " + req.monto() + " a cuenta " + req.cuentaDestinoNumero(), null);
        return ResponseEntity.ok(Map.of("message", "Transferencia realizada con éxito."));
    }

    /**
     * POST /api/cuentas/prueba
     * Crea una cuenta de ahorros de prueba con S/ 1,000 (solo para desarrollo).
     */
    @PostMapping("/prueba")
    public ResponseEntity<CuentaDto> crearCuentaPrueba(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cuentaService.crearCuentaPrueba(userDetails.getUsername()));
    }
}
