package pe.bancoconfianza.backend.dto;

import pe.bancoconfianza.backend.model.CuotaCredito;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaDto(
    Long id,
    Integer numeroCuota,
    LocalDate fechaVencimiento,
    LocalDate fechaPago,
    BigDecimal capital,
    BigDecimal interes,
    BigDecimal seguro,
    BigDecimal cuotaTotal,
    BigDecimal saldoCapital,
    Integer diasMora,
    BigDecimal montoMora,
    String estadoCuota
) {
    public static CuotaDto from(CuotaCredito c) {
        return new CuotaDto(
            c.getId(),
            c.getNumeroCuota(),
            c.getFechaVencimiento(),
            c.getFechaPago(),
            c.getCapital(),
            c.getInteres(),
            c.getSeguro(),
            c.getCuotaTotal(),
            c.getSaldoCapital(),
            c.getDiasMora(),
            c.getMontoMora(),
            c.getEstadoCuota().name()
        );
    }
}
