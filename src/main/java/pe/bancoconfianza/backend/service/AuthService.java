package pe.bancoconfianza.backend.service;

import org.springframework.stereotype.Service;

/**
 * AuthService — con Supabase Auth el login/registro ocurre directamente
 * en el frontend via el SDK de Supabase. Este servicio queda como placeholder
 * para lógica adicional de negocio (ej: sincronizar perfil tras primer login).
 */
@Service
public class AuthService {
    // El login y registro se manejan en el frontend con @supabase/supabase-js.
    // El backend solo valida el JWT de Supabase en JwtAuthFilter.
}
