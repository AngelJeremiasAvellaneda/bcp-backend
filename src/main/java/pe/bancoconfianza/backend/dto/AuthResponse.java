package pe.bancoconfianza.backend.dto;

/**
 * Respuesta del endpoint POST /api/auth/login.
 * El frontend espera: { token, id, nombre, email, rol }
 */
public record AuthResponse(
        String token,
        Long   id,
        String nombre,
        String email,
        String rol
) {}
