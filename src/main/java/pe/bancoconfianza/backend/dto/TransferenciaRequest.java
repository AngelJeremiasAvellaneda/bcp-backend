package pe.bancoconfianza.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequest(
        @NotBlank String cuentaOrigenNumero,
        @NotBlank String cuentaDestinoNumero,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        String descripcion
) {}
