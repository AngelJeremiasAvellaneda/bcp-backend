package pe.bancoconfianza.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro inmutable de acciones relevantes del sistema.
 * Se crea pero nunca se modifica.
 */
@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email del actor (puede ser null si es acción del sistema) */
    @Column(name = "email_actor", length = 150)
    private String emailActor;

    /** Rol del actor en el momento de la acción */
    @Column(name = "rol_actor", length = 30)
    private String rolActor;

    /** Tipo de acción */
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false)
    private TipoAccion accion;

    /** Módulo donde ocurrió (CREDITO, CUENTA, AUTH, RECUPERACIONES) */
    @Column(name = "modulo", length = 50)
    private String modulo;

    /** Descripción legible de la acción */
    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /** ID del recurso afectado (ej. creditoId, cuentaId) */
    @Column(name = "recurso_id")
    private Long recursoId;

    /** IP de origen (capturada por el controller si está disponible) */
    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TipoAccion {
        LOGIN, LOGOUT,
        CREDITO_SOLICITUD, CREDITO_APROBACION, CREDITO_RECHAZO, CREDITO_DESEMBOLSO,
        CUENTA_TRANSFERENCIA, CUENTA_DEPOSITO, CUENTA_RETIRO,
        COBRANZA_GESTION, COBRANZA_JUDICIAL, COBRANZA_CASTIGO,
        USUARIO_CREACION, USUARIO_EDICION
    }

    /* ── Factory methods ── */
    public static AuditoriaEvento of(String email, String rol, TipoAccion accion,
                                      String modulo, String descripcion, Long recursoId, String ip) {
        AuditoriaEvento e = new AuditoriaEvento();
        e.emailActor  = email;
        e.rolActor    = rol;
        e.accion      = accion;
        e.modulo      = modulo;
        e.descripcion = descripcion;
        e.recursoId   = recursoId;
        e.ipOrigen    = ip;
        return e;
    }

    /* ── Getters ── */
    public Long getId()           { return id; }
    public String getEmailActor() { return emailActor; }
    public String getRolActor()   { return rolActor; }
    public TipoAccion getAccion() { return accion; }
    public String getModulo()     { return modulo; }
    public String getDescripcion(){ return descripcion; }
    public Long getRecursoId()    { return recursoId; }
    public String getIpOrigen()   { return ipOrigen; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
