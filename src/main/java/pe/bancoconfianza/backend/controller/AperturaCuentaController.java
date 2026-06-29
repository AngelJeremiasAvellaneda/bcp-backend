package pe.bancoconfianza.backend.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.bancoconfianza.backend.dto.AperturaCuentaRequest;
import pe.bancoconfianza.backend.dto.ValidacionTarjetaRequest;
import pe.bancoconfianza.backend.model.Cuenta;
import pe.bancoconfianza.backend.model.Usuario;
import pe.bancoconfianza.backend.service.AperturaCuentaService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/apertura-cuenta")
@CrossOrigin(origins = "*")
public class AperturaCuentaController {

    @Autowired
    private AperturaCuentaService aperturaCuentaService;

    /**
     * Endpoint para validar tarjeta y clave antes de iniciar el proceso
     */
    @PostMapping("/validar-tarjeta")
    public ResponseEntity<?> validarTarjeta(@Valid @RequestBody ValidacionTarjetaRequest request) {
        try {
            Usuario usuario = aperturaCuentaService.validarTarjetaYClave(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Validación exitosa");
            response.put("usuario", Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "dni", usuario.getDni(),
                "email", usuario.getEmail() != null ? usuario.getEmail() : ""
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Endpoint para crear la cuenta digital
     */
    @PostMapping("/crear-cuenta-digital")
    public ResponseEntity<?> crearCuentaDigital(@Valid @RequestBody AperturaCuentaRequest request) {
        try {
            System.out.println("=== DEBUG: Datos recibidos ===");
            System.out.println("DNI: " + request.getDni());
            System.out.println("Email: " + request.getEmail());
            System.out.println("Nombre: " + request.getNombreCompleto());
            System.out.println("Password: " + (request.getPassword() != null ? "***" : "null"));
            System.out.println("================================");
            
            Cuenta cuenta = aperturaCuentaService.crearCuentaDigital(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "¡Cuenta Digital BCP creada exitosamente!");
            response.put("cuenta", Map.of(
                "numeroCuenta", cuenta.getNumeroCuenta(),
                "cci", cuenta.getCci(),
                "tipoCuenta", cuenta.getTipoCuenta().toString(),
                "moneda", cuenta.getMoneda(),
                "saldo", cuenta.getSaldo(),
                "email", cuenta.getUsuario().getEmail()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== ERROR en crear-cuenta-digital ===");
            e.printStackTrace();
            System.err.println("=====================================");
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("mensaje", e.getMessage());
            error.put("tipo", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
