package pe.bancoconfianza.backend.dto;

import pe.bancoconfianza.backend.model.Cuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuentaDto(
        Long id,
        String numeroCuenta,
        String tipo,
        BigDecimal saldo,
        String moneda,
        boolean activa,
        LocalDateTime createdAt
) {
    public static CuentaDto from(Cuenta c) {
        return new CuentaDto(
                c.getId(),
                c.getNumeroCuenta(),
                c.getTipoCuenta().name(),
                c.getSaldo(),
                c.getMoneda(),
                c.isActiva(),
                c.getCreatedAt()
        );
    }
}
