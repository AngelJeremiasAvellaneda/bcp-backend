package pe.bancoconfianza.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Datos que el cliente envía desde el Homebanking al solicitar un crédito.
 */
public record SolicitudCreditoRequest(

    @NotNull(message = "Tipo de producto es obligatorio")
    String tipoProducto,

    @NotNull @DecimalMin(value = "500.00", message = "Monto mínimo S/ 500")
    BigDecimal montoSolicitado,

    @NotNull @Min(value = 3, message = "Mínimo 3 meses") @Max(value = 360, message = "Máximo 360 meses")
    Integer plazoMeses,

    String moneda,

    @NotBlank(message = "Propósito es obligatorio")
    @Size(max = 500)
    String proposito,

    /** Ingreso mensual declarado por el cliente */
    @NotNull @DecimalMin(value = "0.01", message = "Ingreso debe ser positivo")
    BigDecimal ingresoMensual,

    /** Deuda vigente total declarada */
    @DecimalMin("0.00")
    BigDecimal deudaTotalVigente,

    /** Número de cuenta para recibir el desembolso */
    @NotBlank(message = "Cuenta de desembolso es obligatoria")
    String cuentaDesembolsoNumero
) {}
