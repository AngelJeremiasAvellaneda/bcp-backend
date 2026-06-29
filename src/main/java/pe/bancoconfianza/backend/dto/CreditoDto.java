package pe.bancoconfianza.backend.dto;

import pe.bancoconfianza.backend.model.Credito;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyección de un crédito para devolver al frontend.
 */
public record CreditoDto(
    Long id,
    String numeroOperacion,
    String tipoProducto,
    BigDecimal montoSolicitado,
    BigDecimal montoAprobado,
    BigDecimal tea,
    Integer plazoMeses,
    BigDecimal cuotaMensual,
    String moneda,
    String proposito,
    Integer scoreCrediticio,
    BigDecimal ingresoMensual,
    BigDecimal rdsRatio,
    String rdsSemaforo,
    Boolean esSujetoCredito,
    String estado,
    String rutaAprobacion,
    String comentarioEvaluacion,
    LocalDate fechaDesembolso,
    LocalDate fechaPrimeraCuota,
    LocalDate fechaUltimaCuota,
    String clienteNombre,
    String clienteEmail,
    String asesorNombre,
    LocalDateTime createdAt
) {
    public static CreditoDto from(Credito c) {
        return new CreditoDto(
            c.getId(),
            c.getNumeroOperacion(),
            c.getTipoProducto() != null ? c.getTipoProducto().name() : null,
            c.getMontoSolicitado(),
            c.getMontoAprobado(),
            c.getTea(),
            c.getPlazoMeses(),
            c.getCuotaMensual(),
            c.getMoneda(),
            c.getProposito(),
            c.getScoreCrediticio(),
            c.getIngresoMensual(),
            c.getRdsRatio(),
            c.getRdsSemaforo() != null ? c.getRdsSemaforo().name() : null,
            c.getEsSujetoCredito(),
            c.getEstado().name(),
            c.getRutaAprobacion() != null ? c.getRutaAprobacion().name() : null,
            c.getComentarioEvaluacion(),
            c.getFechaDesembolso(),
            c.getFechaPrimeraCuota(),
            c.getFechaUltimaCuota(),
            c.getCliente() != null ? c.getCliente().getNombre() : null,
            c.getCliente() != null ? c.getCliente().getEmail() : null,
            c.getAsesor() != null ? c.getAsesor().getNombre() : null,
            c.getCreatedAt()
        );
    }
}
