package pe.bancoconfianza.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "cuenta_destino", length = 20)
    private String cuentaDestino;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TipoMovimiento { DEPOSITO, RETIRO, TRANSFERENCIA_ENVIADA, TRANSFERENCIA_RECIBIDA, PAGO_SERVICIO }

    /* ── Getters / Setters ── */
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Cuenta getCuenta()                        { return cuenta; }
    public void setCuenta(Cuenta cuenta)             { this.cuenta = cuenta; }

    public TipoMovimiento getTipo()                  { return tipo; }
    public void setTipo(TipoMovimiento tipo)         { this.tipo = tipo; }

    public BigDecimal getMonto()                     { return monto; }
    public void setMonto(BigDecimal monto)           { this.monto = monto; }

    public BigDecimal getSaldoAnterior()             { return saldoAnterior; }
    public void setSaldoAnterior(BigDecimal s)       { this.saldoAnterior = s; }

    public BigDecimal getSaldoPosterior()            { return saldoPosterior; }
    public void setSaldoPosterior(BigDecimal s)      { this.saldoPosterior = s; }

    public String getDescripcion()                   { return descripcion; }
    public void setDescripcion(String descripcion)   { this.descripcion = descripcion; }

    public String getCuentaDestino()                 { return cuentaDestino; }
    public void setCuentaDestino(String c)           { this.cuentaDestino = c; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
}
