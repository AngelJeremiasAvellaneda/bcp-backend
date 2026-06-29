package pe.bancoconfianza.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.bancoconfianza.backend.repository.UsuarioRepository;
import pe.bancoconfianza.backend.security.SupabaseUserDetails;

/**
 * UserDetailsService que soporta dos modos:
 * 1. Usuario existe en BD local → devuelve el Usuario (con roles propios)
 * 2. Usuario autenticado via Supabase Auth → devuelve SupabaseUserDetails
 *    (no requiere tabla de usuarios propia)
 */
@Service
public class UsuarioService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("UsuarioService.loadUserByUsername: buscando usuario {}", email);
        var opt = usuarioRepository.findByEmail(email);
        if (opt.isPresent()) {
            var u = opt.get();
            // Evitar imprimir contraseñas reales; mostramos longitud del hash para diagnóstico
            String pw = u.getPassword() == null ? "<null>" : "len=" + u.getPassword().length();
            log.info("Usuario encontrado: {} - passwordHash {} rol={}", u.getEmail(), pw, u.getRol());
            return u;
        }
        log.info("Usuario no encontrado en BD local: {}, devolviendo SupabaseUserDetails", email);
        return new SupabaseUserDetails(email);
    }
}
