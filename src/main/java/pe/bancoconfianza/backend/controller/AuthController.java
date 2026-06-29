package pe.bancoconfianza.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.bancoconfianza.backend.config.JwtProperties;
import pe.bancoconfianza.backend.dto.AuthResponse;
import pe.bancoconfianza.backend.dto.LoginRequest;
import pe.bancoconfianza.backend.model.AuditoriaEvento;
import pe.bancoconfianza.backend.model.Usuario;
import pe.bancoconfianza.backend.repository.UsuarioRepository;
import pe.bancoconfianza.backend.security.JwtService;
import pe.bancoconfianza.backend.service.AuditoriaService;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * AuthController — gestión de sesión y perfil del usuario autenticado.
 * El login/registro ocurre en Supabase Auth (frontend SDK).
 * Este controlador provee el perfil del usuario backend + rol.
 *
 * GET /api/auth/status              — estado público
 * GET /api/auth/me                  — perfil autenticado
 * GET /api/auth/usuarios            — lista todos los usuarios (ADMIN/GERENCIA)
 * PUT /api/auth/usuarios/{id}/rol   — cambiar rol (ADMIN)
 * PUT /api/auth/usuarios/{id}/activo— activar/desactivar (ADMIN)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthController(UsuarioRepository usuarioRepository,
                          AuditoriaService auditoriaService,
                          AuthenticationManager authenticationManager,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          JwtProperties jwtProperties) {
        this.usuarioRepository      = usuarioRepository;
        this.auditoriaService       = auditoriaService;
        this.authenticationManager  = authenticationManager;
        this.passwordEncoder        = passwordEncoder;
        this.jwtService             = jwtService;
        this.jwtProperties          = jwtProperties;
    }

    /** POST /api/auth/login — login directo con email + password */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest body) {
        // Log intent (no passwords)
        var optLog = usuarioRepository.findByEmail(body.email());
        if (optLog.isPresent()) {
            var u = optLog.get();
            String pw = u.getPassword() == null ? "<null>" : "len=" + u.getPassword().length();
            log.info("[Login] intento para: {} passwordHash={}", u.getEmail(), pw);
        } else {
            log.info("[Login] intento para usuario no existente: {}", body.email());
        }
        // Validación manual: evitamos depender del AuthenticationManager
        var optu = usuarioRepository.findByEmail(body.email());
        if (optu.isEmpty()) {
            return ResponseEntity.status(401).body(new AuthResponse("", null, null, null, null));
        }
        Usuario usuario = optu.get();
        boolean matches = usuario.getPassword() != null && passwordEncoder.matches(body.password(), usuario.getPassword());
        log.info("[Login] matches={} for {}", matches, usuario.getEmail());
        if (!matches) {
            return ResponseEntity.status(401).body(new AuthResponse("", null, null, null, null));
        }

        String token = jwtService.generateToken(usuario.getEmail(), jwtProperties.getExpirationMs());
        return ResponseEntity.ok(new AuthResponse(
            token,
            usuario.getId(),
            usuario.getNombre(),
            usuario.getEmail(),
            usuario.getRol().name()
        ));
    }

    /** POST /api/auth/login-tarjeta — login con tarjeta + clave */
    @PostMapping("/login-tarjeta")
    public ResponseEntity<?> loginTarjeta(@RequestBody Map<String, String> body) {
        try {
            String numeroTarjeta = body.get("numeroTarjeta");
            String clave = body.get("clave");
            
            System.out.println("=== [LOGIN TARJETA] ===");
            System.out.println("Tarjeta: " + numeroTarjeta);
            System.out.println("Clave: " + (clave != null ? "***" : "null"));
            
            if (numeroTarjeta == null || clave == null) {
                System.out.println("ERROR: Parámetros nulos");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "mensaje", "Parámetros incompletos"
                ));
            }
            
            System.out.println("Buscando usuario por tarjeta...");
            var optUsuario = usuarioRepository.findByNumeroTarjeta(numeroTarjeta);
            if (optUsuario.isEmpty()) {
                System.out.println("ERROR: Tarjeta no encontrada");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "mensaje", "Tarjeta no encontrada"
                ));
            }
            
            Usuario usuario = optUsuario.get();
            System.out.println("Usuario encontrado: " + usuario.getEmail());
            System.out.println("Password hash: " + (usuario.getPassword() != null ? "exists" : "null"));
            
            boolean matches = usuario.getPassword() != null && passwordEncoder.matches(clave, usuario.getPassword());
            System.out.println("Password matches: " + matches);
            
            if (!matches) {
                System.out.println("ERROR: Clave incorrecta");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "mensaje", "Clave incorrecta"
                ));
            }

            System.out.println("Generando token...");
            String token = jwtService.generateToken(usuario.getEmail(), jwtProperties.getExpirationMs());
            System.out.println("Token generado exitosamente");
            
            return ResponseEntity.ok(new AuthResponse(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol().name()
            ));
        } catch (Exception e) {
            System.out.println("=== ERROR EN LOGIN TARJETA ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "mensaje", e.getMessage(),
                "tipo", e.getClass().getSimpleName()
            ));
        }
    }

    /** Estado público — confirma que el backend está listo. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "auth", "Supabase Auth + JWT",
                "info", "BCP HomebanKing Backend — operativo"
        ));
    }

    /** GET /api/auth/me — perfil completo del usuario autenticado */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id",     usuario.getId());
        resp.put("nombre", usuario.getNombre());
        resp.put("email",  usuario.getEmail());
        resp.put("rol",    usuario.getRol().name());
        resp.put("activo", usuario.isActivo());
        resp.put("createdAt", usuario.getCreatedAt());
        return ResponseEntity.ok(resp);
    }

    /** GET /api/auth/usuarios — lista todos los usuarios (ADMIN / GERENCIA) */
    @GetMapping("/usuarios")
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA')")
    public ResponseEntity<List<Map<String, Object>>> listarUsuarios() {
        List<Map<String, Object>> lista = usuarioRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        u.getId());
            m.put("nombre",    u.getNombre());
            m.put("email",     u.getEmail());
            m.put("rol",       u.getRol().name());
            m.put("activo",    u.isActivo());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(lista);
    }

    /** PUT /api/auth/usuarios/{id}/rol — cambiar rol de un usuario (ADMIN) */
    @PutMapping("/usuarios/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cambiarRol(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails actor) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        String nuevoRol = body.get("rol");
        Usuario.Rol rol = Usuario.Rol.valueOf(nuevoRol.toUpperCase());
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(actor.getUsername(), "ADMIN",
            AuditoriaEvento.TipoAccion.USUARIO_EDICION,
            "USUARIO", "Cambio de rol a " + rol + " para " + usuario.getEmail(), id);
        return ResponseEntity.ok(Map.of("mensaje", "Rol actualizado a " + rol, "email", usuario.getEmail()));
    }

    /** PUT /api/auth/usuarios/{id}/activo — activar / desactivar usuario (ADMIN) */
    @PutMapping("/usuarios/{id}/activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleActivo(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails actor) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        boolean activo = body.getOrDefault("activo", !usuario.isActivo());
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(actor.getUsername(), "ADMIN",
            AuditoriaEvento.TipoAccion.USUARIO_EDICION,
            "USUARIO", (activo ? "Activado" : "Desactivado") + ": " + usuario.getEmail(), id);
        return ResponseEntity.ok(Map.of("activo", activo, "email", usuario.getEmail()));
    }

    /** PUT /api/auth/me/perfil — actualizar nombre propio */
    @PutMapping("/me/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> actualizarPerfil(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (body.containsKey("nombre") && !body.get("nombre").isBlank()) {
            usuario.setNombre(body.get("nombre"));
        }
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Perfil actualizado",
            "nombre",  usuario.getNombre()
        ));
    }
}
