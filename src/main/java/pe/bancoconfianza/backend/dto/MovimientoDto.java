package pe.bancoconfianza.backend.dto;

import pe.bancoconfianza.backend.model.Movimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoDto(
        Long id,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        String descripcion,
        String cuentaDestino,
        LocalDateTime createdAt
) {
    public static MovimientoDto from(Movimiento m) {
        return new MovimientoDto(
                m.getId(),
                m.getTipo().name(),
                m.getMonto(),
                m.getSaldoAnterior(),
                m.getSaldoPosterior(),
                m.getDescripcion(),
                m.getCuentaDestino(),
                m.getCreatedAt()
        );
    }
}
