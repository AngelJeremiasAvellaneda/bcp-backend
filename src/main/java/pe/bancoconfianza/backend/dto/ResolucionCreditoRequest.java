package pe.bancoconfianza.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Resolución de un crédito (aprobar / rechazar) por un actor del Core.
 */
public record ResolucionCreditoRequest(
    /** true = aprobado, false = rechazado */
    @NotNull boolean aprobado,

    @NotBlank(message = "Comentario obligatorio")
    String comentario,

    /** Monto aprobado (puede diferir del solicitado, solo al aprobar) */
    BigDecimal montoAprobado
) {}
