package pe.bancoconfianza.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint público de salud — usado por el frontend.
 * No requiere autenticación (permitido en SecurityConfig bajo /api/public/**)
 * 
 * Endpoints:
 *  • GET /api/public/health                 → Health check genérico
 *  • GET /                                   → Mensaje de bienvenida del backend
 *  • GET /api/core                         → Status CORE Financiero
 */
@RestController
public class HealthController {

    public HealthController() {
    }

    /**
     * GET /api/public/health
     * Health check genérico sin exponer información sensible
     */
    @GetMapping("/api/public/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status",    "UP",
                "service",   "BancoConfianza API"
        ));
    }

    /**
     * GET /
     * Mensaje de bienvenida del backend
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> welcome() {
        return ResponseEntity.ok(Map.of(
                "nombre",           "Homebanking BCP",
                "estado",           "✅ Backend funcionando correctamente",
                "version",          "1.0.0",
                "ambiente",         "Desarrollo",
                "mensaje",          "Bienvenido al backend del Banco de Crédito del Perú",
                "endpoints",        Map.of(
                    "health",       "/api/public/health",
                    "core",         "/api/core",
                    "auth",         "/api/auth/login",
                    "creditos",     "/api/creditos"
                ),
                "timestamp",        System.currentTimeMillis()
        ));
    }

    /**
     * GET /api/core
     * Status del CORE Financiero
     */
    @GetMapping("/api/core")
    public ResponseEntity<Map<String, Object>> coreStatus() {
        return ResponseEntity.ok(Map.of(
                "nombre",           "CORE Financiero",
                "estado",           "✅ CORE funcionando correctamente",
                "version",          "1.0.0",
                "ambiente",         "Desarrollo",
                "mensaje",          "Plataforma de Gestión Crediticia BCP",
                "modulos",          Map.of(
                    "colocaciones",     "✅ Activo",
                    "recuperaciones",   "✅ Activo",
                    "administracion",   "✅ Activo",
                    "reportes",         "✅ Activo"
                ),
                "roles_permitidos",  new String[]{
                    "ASESOR",
                    "JEFE_REGIONAL",
                    "RIESGOS",
                    "COMITE",
                    "GERENCIA",
                    "ADMIN"
                },
                "timestamp",        System.currentTimeMillis()
        ));
    }
}
