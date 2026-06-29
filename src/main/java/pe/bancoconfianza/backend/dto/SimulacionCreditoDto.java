package pe.bancoconfianza.backend.dto;

import java.math.BigDecimal;

/**
 * Simulación de crédito (pre-visualización sin guardar en BD).
 * Se usa cuando el cliente quiere ver qué pasaría con parámetros específicos
 * antes de confirmar la solicitud.
 */
public record SimulacionCreditoDto(
    String tipoProducto,
    BigDecimal montoSolicitado,
    Integer plazoMeses,
    BigDecimal tea,
    BigDecimal cuotaMensual,
    BigDecimal ingresoMensual,
    BigDecimal deudaTotalVigente,
    BigDecimal rdsRatio,
    String rdsSemaforo,
    Integer scoreCrediticio,
    Boolean esSujetoCredito,
    String rutaAprobacion,
    String estado,
    String comentarioEvaluacion
) {}
