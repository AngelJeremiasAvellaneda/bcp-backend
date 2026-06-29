package pe.bancoconfianza.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una solicitud / crédito otorgado.
 * Cubre el ciclo completo: SOLICITADO → EVALUACION → APROBADO / RECHAZADO → DESEMBOLSADO.
 */
@Entity
@Table(name = "creditos")
public class Credito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número único de operación, ej. "CRED-2026-000001" */
    @Column(name = "numero_operacion", nullable = false, unique = true, length = 30)
    private String numeroOperacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    /** Asesor de crédito que registró la solicitud */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asesor_id")
    private Usuario asesor;

    /** Tipo de producto: PERSONAL, HIPOTECARIO, VEHICULAR, AGROPECUARIO, MICROEMPRESA */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false)
    private TipoProducto tipoProducto;

    @Column(name = "monto_solicitado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoSolicitado;

    @Column(name = "monto_aprobado", precision = 15, scale = 2)
    private BigDecimal montoAprobado;

    /** Tasa Efectiva Anual (%) */
    @Column(name = "tea", precision = 6, scale = 4)
    private BigDecimal tea;

    @Column(name = "plazo_meses", nullable = false)
    private Integer plazoMeses;

    @Column(name = "cuota_mensual", precision = 15, scale = 2)
    private BigDecimal cuotaMensual;

    /** Moneda: PEN, USD */
    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    /** Propósito del crédito declarado por el cliente */
    @Column(name = "proposito", length = 500)
    private String proposito;

    /* ── Evaluación crediticia ─────────────────────────────── */

    /** Score crediticio calculado (0–1000) */
    @Column(name = "score_crediticio")
    private Integer scoreCrediticio;

    /** Ingreso mensual declarado */
    @Column(name = "ingreso_mensual", precision = 15, scale = 2)
    private BigDecimal ingresoMensual;

    /** Deuda total vigente */
    @Column(name = "deuda_total_vigente", precision = 15, scale = 2)
    private BigDecimal deudaTotalVigente;

    /** Ratio Deuda/Sueldo calculado (%) — semáforo RDS */
    @Column(name = "rds_ratio", precision = 6, scale = 4)
    private BigDecimal rdsRatio;

    /** Semáforo RDS: VERDE (<30%), AMARILLO (30-50%), ROJO (>50%) */
    @Enumerated(EnumType.STRING)
    @Column(name = "rds_semaforo", length = 10)
    private SemaforoRds rdsSemaforo;

    /** Sujeto de crédito: true = elegible para crédito */
    @Column(name = "es_sujeto_credito")
    private Boolean esSujetoCredito;

    /* ── Flujo de aprobación ───────────────────────────────── */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCredito estado = EstadoCredito.SOLICITADO;

    /** Quién aprobó/rechazó */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por_id")
    private Usuario aprobadoPor;

    @Column(name = "comentario_evaluacion", length = 1000)
    private String comentarioEvaluacion;

    /** Ruta de aprobación requerida según monto */
    @Enumerated(EnumType.STRING)
    @Column(name = "ruta_aprobacion", length = 20)
    private RutaAprobacion rutaAprobacion;

    /* ── Desembolso ────────────────────────────────────────── */

    /** Cuenta destino del desembolso */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_desembolso_id")
    private Cuenta cuentaDesembolso;

    @Column(name = "fecha_desembolso")
    private LocalDate fechaDesembolso;

    @Column(name = "fecha_primera_cuota")
    private LocalDate fechaPrimeraCuota;

    @Column(name = "fecha_ultima_cuota")
    private LocalDate fechaUltimaCuota;

    /* ── Auditoría ─────────────────────────────────────────── */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /* ── Enums ─────────────────────────────────────────────── */

    public enum TipoProducto {
        PERSONAL, HIPOTECARIO, VEHICULAR, AGROPECUARIO, MICROEMPRESA
    }

    public enum EstadoCredito {
        /** Recién ingresada por el cliente o asesor */
        SOLICITADO,
        /** En evaluación por el asesor */
        EN_EVALUACION,
        /** Requiere opinión del Administrador (montos medios) */
        PENDIENTE_ADMIN,
        /** Requiere opinión del Jefe Regional */
        PENDIENTE_JEFE_REGIONAL,
        /** Requiere dictamen de Riesgos */
        PENDIENTE_RIESGOS,
        /** Requiere resolución de Comité */
        PENDIENTE_COMITE,
        /** Aprobado, pendiente de desembolso */
        APROBADO,
        /** Desembolsado — crédito activo */
        DESEMBOLSADO,
        /** Rechazado en cualquier etapa */
        RECHAZADO,
        /** Cancelado por el cliente */
        CANCELADO
    }

    public enum SemaforoRds {
        VERDE, AMARILLO, ROJO
    }

    /**
     * Ruta de aprobación según rangos de monto (en PEN):
     *  - ASESOR         : hasta  5,000
     *  - ADMIN          : hasta 20,000
     *  - JEFE_REGIONAL  : hasta 50,000
     *  - RIESGOS        : hasta 150,000
     *  - COMITE         : > 150,000
     */
    public enum RutaAprobacion {
        ASESOR, ADMIN, JEFE_REGIONAL, RIESGOS, COMITE
    }

    /* ── Getters / Setters ──────────────────────────────────── */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String n) { this.numeroOperacion = n; }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }

    public Usuario getAsesor() { return asesor; }
    public void setAsesor(Usuario asesor) { this.asesor = asesor; }

    public TipoProducto getTipoProducto() { return tipoProducto; }
    public void setTipoProducto(TipoProducto tipo) { this.tipoProducto = tipo; }

    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(BigDecimal monto) { this.montoSolicitado = monto; }

    public BigDecimal getMontoAprobado() { return montoAprobado; }
    public void setMontoAprobado(BigDecimal monto) { this.montoAprobado = monto; }

    public BigDecimal getTea() { return tea; }
    public void setTea(BigDecimal tea) { this.tea = tea; }

    public Integer getPlazoMeses() { return plazoMeses; }
    public void setPlazoMeses(Integer plazo) { this.plazoMeses = plazo; }

    public BigDecimal getCuotaMensual() { return cuotaMensual; }
    public void setCuotaMensual(BigDecimal cuota) { this.cuotaMensual = cuota; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public String getProposito() { return proposito; }
    public void setProposito(String proposito) { this.proposito = proposito; }

    public Integer getScoreCrediticio() { return scoreCrediticio; }
    public void setScoreCrediticio(Integer score) { this.scoreCrediticio = score; }

    public BigDecimal getIngresoMensual() { return ingresoMensual; }
    public void setIngresoMensual(BigDecimal ingreso) { this.ingresoMensual = ingreso; }

    public BigDecimal getDeudaTotalVigente() { return deudaTotalVigente; }
    public void setDeudaTotalVigente(BigDecimal deuda) { this.deudaTotalVigente = deuda; }

    public BigDecimal getRdsRatio() { return rdsRatio; }
    public void setRdsRatio(BigDecimal rds) { this.rdsRatio = rds; }

    public SemaforoRds getRdsSemaforo() { return rdsSemaforo; }
    public void setRdsSemaforo(SemaforoRds semaforo) { this.rdsSemaforo = semaforo; }

    public Boolean getEsSujetoCredito() { return esSujetoCredito; }
    public void setEsSujetoCredito(Boolean es) { this.esSujetoCredito = es; }

    public EstadoCredito getEstado() { return estado; }
    public void setEstado(EstadoCredito estado) { this.estado = estado; }

    public Usuario getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Usuario aprobadoPor) { this.aprobadoPor = aprobadoPor; }

    public String getComentarioEvaluacion() { return comentarioEvaluacion; }
    public void setComentarioEvaluacion(String c) { this.comentarioEvaluacion = c; }

    public RutaAprobacion getRutaAprobacion() { return rutaAprobacion; }
    public void setRutaAprobacion(RutaAprobacion ruta) { this.rutaAprobacion = ruta; }

    public Cuenta getCuentaDesembolso() { return cuentaDesembolso; }
    public void setCuentaDesembolso(Cuenta cuenta) { this.cuentaDesembolso = cuenta; }

    public LocalDate getFechaDesembolso() { return fechaDesembolso; }
    public void setFechaDesembolso(LocalDate fecha) { this.fechaDesembolso = fecha; }

    public LocalDate getFechaPrimeraCuota() { return fechaPrimeraCuota; }
    public void setFechaPrimeraCuota(LocalDate fecha) { this.fechaPrimeraCuota = fecha; }

    public LocalDate getFechaUltimaCuota() { return fechaUltimaCuota; }
    public void setFechaUltimaCuota(LocalDate fecha) { this.fechaUltimaCuota = fecha; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
