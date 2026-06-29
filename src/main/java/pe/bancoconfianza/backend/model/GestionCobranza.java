package pe.bancoconfianza.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * R2 — Registro de acciones de cobranza sobre una cuota o crédito en mora.
 */
@Entity
@Table(name = "gestiones_cobranza")
public class GestionCobranza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id", nullable = false)
    private Credito credito;

    /** Cuota específica gestionada (puede ser null si aplica al crédito en general) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuota_id")
    private CuotaCredito cuota;

    /** Gestor que realizó la acción */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Usuario gestor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gestion", nullable = false)
    private TipoGestion tipoGestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false)
    private ResultadoGestion resultado;

    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    /** Fecha acordada de pago (si el cliente prometió pagar) */
    @Column(name = "fecha_compromiso_pago")
    private LocalDate fechaCompromisoPago;

    /** Días de mora al momento de la gestión */
    @Column(name = "dias_mora_al_gestionar")
    private Integer diasMoraAlGestionar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TipoGestion {
        LLAMADA_TELEFONICA, SMS, EMAIL, VISITA_DOMICILIARIA,
        CARTA_NOTARIAL, ACUERDO_PAGO, OTRO
    }

    public enum ResultadoGestion {
        CONTACTO_EXITOSO, SIN_CONTACTO, PROMESA_PAGO,
        PAGO_REALIZADO, NEGATIVA_PAGO, NUMERO_INCORRECTO, OTRO
    }

    /* ── Getters / Setters ── */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Credito getCredito() { return credito; }
    public void setCredito(Credito credito) { this.credito = credito; }

    public CuotaCredito getCuota() { return cuota; }
    public void setCuota(CuotaCredito cuota) { this.cuota = cuota; }

    public Usuario getGestor() { return gestor; }
    public void setGestor(Usuario gestor) { this.gestor = gestor; }

    public TipoGestion getTipoGestion() { return tipoGestion; }
    public void setTipoGestion(TipoGestion tipo) { this.tipoGestion = tipo; }

    public ResultadoGestion getResultado() { return resultado; }
    public void setResultado(ResultadoGestion resultado) { this.resultado = resultado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String d) { this.descripcion = d; }

    public LocalDate getFechaCompromisoPago() { return fechaCompromisoPago; }
    public void setFechaCompromisoPago(LocalDate fecha) { this.fechaCompromisoPago = fecha; }

    public Integer getDiasMoraAlGestionar() { return diasMoraAlGestionar; }
    public void setDiasMoraAlGestionar(Integer dias) { this.diasMoraAlGestionar = dias; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
