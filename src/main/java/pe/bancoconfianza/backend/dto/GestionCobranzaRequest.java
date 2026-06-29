package pe.bancoconfianza.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GestionCobranzaRequest(
    @NotNull Long creditoId,
    Long cuotaId,
    @NotBlank String tipoGestion,
    @NotBlank String resultado,
    @NotBlank String descripcion,
    LocalDate fechaCompromisoPago
) {}
