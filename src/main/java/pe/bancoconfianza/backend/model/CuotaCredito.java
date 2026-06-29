package pe.bancoconfianza.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cuota individual del cronograma de pagos de un crédito.
 * Al registrar un pago se actualiza estadoCuota → PAGADA.
 */
@Entity
@Table(name = "cuotas_credito")
public class CuotaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id", nullable = false)
    private Credito credito;

    /** Número de cuota (1, 2, 3 …) */
    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "capital", nullable = false, precision = 15, scale = 2)
    private BigDecimal capital;

    @Column(name = "interes", nullable = false, precision = 15, scale = 2)
    private BigDecimal interes;

    @Column(name = "seguro", precision = 15, scale = 2)
    private BigDecimal seguro = BigDecimal.ZERO;

    @Column(name = "cuota_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal cuotaTotal;

    @Column(name = "saldo_capital", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoCapital;

    /** Mora acumulada en días a la fecha de cálculo */
    @Column(name = "dias_mora")
    private Integer diasMora = 0;

    /** Monto de mora generado */
    @Column(name = "monto_mora", precision = 15, scale = 2)
    private BigDecimal montoMora = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuota", nullable = false)
    private EstadoCuota estadoCuota = EstadoCuota.PENDIENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EstadoCuota { PENDIENTE, PAGADA, VENCIDA, PARCIAL }

    /* ── Getters / Setters ── */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Credito getCredito() { return credito; }
    public void setCredito(Credito credito) { this.credito = credito; }

    public Integer getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(Integer n) { this.numeroCuota = n; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fecha) { this.fechaVencimiento = fecha; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fecha) { this.fechaPago = fecha; }

    public BigDecimal getCapital() { return capital; }
    public void setCapital(BigDecimal capital) { this.capital = capital; }

    public BigDecimal getInteres() { return interes; }
    public void setInteres(BigDecimal interes) { this.interes = interes; }

    public BigDecimal getSeguro() { return seguro; }
    public void setSeguro(BigDecimal seguro) { this.seguro = seguro; }

    public BigDecimal getCuotaTotal() { return cuotaTotal; }
    public void setCuotaTotal(BigDecimal cuota) { this.cuotaTotal = cuota; }

    public BigDecimal getSaldoCapital() { return saldoCapital; }
    public void setSaldoCapital(BigDecimal saldo) { this.saldoCapital = saldo; }

    public Integer getDiasMora() { return diasMora; }
    public void setDiasMora(Integer dias) { this.diasMora = dias; }

    public BigDecimal getMontoMora() { return montoMora; }
    public void setMontoMora(BigDecimal monto) { this.montoMora = monto; }

    public EstadoCuota getEstadoCuota() { return estadoCuota; }
    public void setEstadoCuota(EstadoCuota estado) { this.estadoCuota = estado; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
