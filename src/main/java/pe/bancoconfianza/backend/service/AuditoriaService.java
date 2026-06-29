package pe.bancoconfianza.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pe.bancoconfianza.backend.model.AuditoriaEvento;
import pe.bancoconfianza.backend.repository.AuditoriaEventoRepository;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Servicio de auditoría — registra eventos de forma asíncrona para no
 * impactar el tiempo de respuesta de las operaciones principales.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaEventoRepository repo;

    public AuditoriaService(AuditoriaEventoRepository repo) {
        this.repo = repo;
    }

    @Async
    public void registrar(String email, String rol, AuditoriaEvento.TipoAccion accion,
                          String modulo, String descripcion, Long recursoId, String ip) {
        try {
            repo.save(AuditoriaEvento.of(email, rol, accion, modulo, descripcion, recursoId, ip));
        } catch (Exception ex) {
            log.warn("[Auditoría] Error al registrar evento {}: {}", accion, ex.getMessage());
        }
    }

    /** Shortcut sin IP */
    @Async
    public void registrar(String email, String rol, AuditoriaEvento.TipoAccion accion,
                          String modulo, String descripcion, Long recursoId) {
        registrar(email, rol, accion, modulo, descripcion, recursoId, "N/A");
    }

    /** Últimos 50 eventos globales (para ADMIN/GERENCIA) */
    public List<Map<String, Object>> getEventosRecientes() {
        return repo.findTop50ByOrderByCreatedAtDesc()
            .stream()
            .map(this::toMap)
            .toList();
    }

    /** Últimos 20 eventos de un usuario específico */
    public List<Map<String, Object>> getEventosPorUsuario(String email) {
        return repo.findTop20ByEmailActorOrderByCreatedAtDesc(email)
            .stream()
            .map(this::toMap)
            .toList();
    }

    private Map<String, Object> toMap(AuditoriaEvento e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          e.getId());
        m.put("emailActor",  e.getEmailActor());
        m.put("rolActor",    e.getRolActor());
        m.put("accion",      e.getAccion().name());
        m.put("modulo",      e.getModulo());
        m.put("descripcion", e.getDescripcion());
        m.put("recursoId",   e.getRecursoId());
        m.put("ipOrigen",    e.getIpOrigen());
        m.put("createdAt",   e.getCreatedAt());
        return m;
    }
}
