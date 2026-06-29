package pe.bancoconfianza.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.bancoconfianza.backend.service.EstadisticasService;

import java.util.List;
import java.util.Map;

/**
 * EstadisticasController — Datos de tendencias para dashboards ejecutivos.
 *
 * GET /api/estadisticas/creditos-mensuales   → desembolsos + cartera + mora (12 meses)
 * GET /api/estadisticas/recuperaciones       → efectividad de gestiones de cobranza
 * GET /api/estadisticas/actividad-usuarios   → operaciones por hora del día
 * GET /api/estadisticas/kpis-ejecutivos      → ROA, ROE, clientes activos (Sprint 5)
 * GET /api/estadisticas/rankings             → Top asesores y regiones (Sprint 5)
 * GET /api/estadisticas/cumplimiento-metas   → Meta vs realizado mensual (Sprint 5)
 */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    /**
     * Tendencias mensuales de créditos (año en curso).
     * Roles: GERENCIA, ADMIN, JEFE_REGIONAL.
     *
     * Respuesta:
     * [
     *   { mes:"Ene", mesNumero:1, desembolsos:12, montoDesembolsado:480000,
     *     carteraAcumulada:480, moraPct:5.2, solicitudes:18, rechazados:3 },
     *   ...
     * ]
     */
    @GetMapping("/creditos-mensuales")
    @PreAuthorize("hasAnyRole('GERENCIA','ADMIN','JEFE_REGIONAL','RIESGOS')")
    public ResponseEntity<List<Map<String, Object>>> creditosMensuales() {
        return ResponseEntity.ok(estadisticasService.getCreditosMensuales());
    }

    /**
     * Estadísticas de recuperaciones / cobranza.
     * Roles: GERENCIA, ADMIN, JEFE_REGIONAL, RIESGOS.
     *
     * Respuesta:
     * {
     *   totalGestiones: 284,
     *   pagosRealizados: 42,
     *   promesas: 68,
     *   contactabilidadPct: 71.5,
     *   efectividadPorTipo: [
     *     { tipo:"Llamada", tipoRaw:"LLAMADA_TELEFONICA", total:120, exitosos:48, pct:40.0 },
     *     ...
     *   ]
     * }
     */
    @GetMapping("/recuperaciones")
    @PreAuthorize("hasAnyRole('GERENCIA','ADMIN','JEFE_REGIONAL','RIESGOS','COBRANZA')")
    public ResponseEntity<Map<String, Object>> recuperaciones() {
        return ResponseEntity.ok(estadisticasService.getEstadisticasRecuperaciones());
    }

    /**
     * Actividad de operaciones por hora del día (0–23h).
     * Roles: ADMIN, GERENCIA.
     *
     * Respuesta:
     * {
     *   porHora: [ { hora:"08h", horaNum:8, eventos:35 }, ... ],
     *   totalOps: 748,
     *   esDemoData: false
     * }
     */
    @GetMapping("/actividad-usuarios")
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA')")
    public ResponseEntity<Map<String, Object>> actividadUsuarios() {
        return ResponseEntity.ok(estadisticasService.getActividadPorHora());
    }

    /**
     * KPIs ejecutivos avanzados.
     * Roles: GERENCIA, ADMIN.
     *
     * Respuesta:
     * {
     *   roa: 2.8,
     *   roe: 18.5,
     *   clientesActivos: 1240,
     *   carteraTotal: 12500000,
     *   resultadoNeto: 1500000,
     *   patrimonio: 1875000
     * }
     */
    @GetMapping("/kpis-ejecutivos")
    @PreAuthorize("hasAnyRole('GERENCIA','ADMIN')")
    public ResponseEntity<Map<String, Object>> kpisEjecutivos() {
        return ResponseEntity.ok(estadisticasService.getKpisEjecutivos());
    }

    /**
     * Rankings de desempeño.
     * Roles: GERENCIA, ADMIN, JEFE_REGIONAL.
     *
     * Respuesta:
     * {
     *   topAsesores: [
     *     { nombre:"Carlos Ruiz", creditos:48, montoTotal:890000 },
     *     ...
     *   ],
     *   topRegiones: [
     *     { region:"Lima Centro", cartera:4200000, creditos:180 },
     *     ...
     *   ]
     * }
     */
    @GetMapping("/rankings")
    @PreAuthorize("hasAnyRole('GERENCIA','ADMIN','JEFE_REGIONAL')")
    public ResponseEntity<Map<String, Object>> rankings() {
        return ResponseEntity.ok(estadisticasService.getRankings());
    }

    /**
     * Cumplimiento de metas mensuales.
     * Roles: GERENCIA, ADMIN, JEFE_REGIONAL.
     *
     * Respuesta:
     * {
     *   metaCantidad: 50,
     *   metaMonto: 2000000,
     *   colocacionesDelMes: 43,
     *   montoColocado: 1740000,
     *   cumplimientoCantidad: 86.0,
     *   cumplimientoMonto: 87.0,
     *   mesActual: "JUNE"
     * }
     */
    @GetMapping("/cumplimiento-metas")
    @PreAuthorize("hasAnyRole('GERENCIA','ADMIN','JEFE_REGIONAL')")
    public ResponseEntity<Map<String, Object>> cumplimientoMetas() {
        return ResponseEntity.ok(estadisticasService.getCumplimientoMetas());
    }

    /**
     * Cartera detallada por asesor (con drill-down).
     * Roles: GERENCIA, ADMIN, JEFE_REGIONAL.
     *
     * Query params:
     *  - producto (opcional): filtrar por tipo de producto
     *
     * Respuesta:
     * [
     *   { nombre:"Carlos Ruiz", creditos:48, montoTotal:890000, productos:{...} },
     *   ...
     * ]
     */
    @GetMapping("/cartera-por-asesor")
    @PreAuthorize("hasAnyRole('GERENCIA','ADMIN','JEFE_REGIONAL')")
    public ResponseEntity<List<Map<String, Object>>> carteraPorAsesor(
        @RequestParam(required = false) String producto
    ) {
        return ResponseEntity.ok(estadisticasService.getCarteraPorAsesor(producto));
    }
}
