package pe.bancoconfianza.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.bancoconfianza.backend.dto.CuentaDto;
import pe.bancoconfianza.backend.dto.MovimientoDto;
import pe.bancoconfianza.backend.dto.TransferenciaRequest;
import pe.bancoconfianza.backend.model.Cuenta;
import pe.bancoconfianza.backend.model.Movimiento;
import pe.bancoconfianza.backend.model.Usuario;
import pe.bancoconfianza.backend.repository.CuentaRepository;
import pe.bancoconfianza.backend.repository.MovimientoRepository;
import pe.bancoconfianza.backend.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    public CuentaService(CuentaRepository cuentaRepository,
                         MovimientoRepository movimientoRepository,
                         UsuarioRepository usuarioRepository) {
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Devuelve todas las cuentas activas del usuario autenticado. */
    public List<CuentaDto> getCuentasDelUsuario(String email) {
        Usuario usuario = findUsuario(email);
        return cuentaRepository.findByUsuarioAndActivaTrue(usuario)
                .stream()
                .map(CuentaDto::from)
                .toList();
    }

    /** Devuelve los últimos N movimientos de una cuenta del usuario. */
    public List<MovimientoDto> getMovimientos(Long cuentaId, String email, int limit) {
        Cuenta cuenta = findCuentaDelUsuario(cuentaId, email);
        return movimientoRepository.findByCuentaOrderByCreatedAtDesc(
                cuenta, org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 100)))
                .stream()
                .map(MovimientoDto::from)
                .toList();
    }

    /** Compatibilidad backward — devuelve top 20 */
    public List<MovimientoDto> getMovimientos(Long cuentaId, String email) {
        return getMovimientos(cuentaId, email, 20);
    }

    /** Realiza una transferencia entre dos cuentas. */
    @Transactional
    public void transferir(TransferenciaRequest req, String emailOrigen) {
        Cuenta origen = cuentaRepository.findByNumeroCuenta(req.cuentaOrigenNumero())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no encontrada."));

        // Verificar que la cuenta origen pertenece al usuario autenticado
        if (!origen.getUsuario().getEmail().equals(emailOrigen)) {
            throw new SecurityException("No tienes permiso sobre esta cuenta.");
        }

        Cuenta destino = cuentaRepository.findByNumeroCuenta(req.cuentaDestinoNumero())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no encontrada."));

        if (origen.getSaldo().compareTo(req.monto()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente.");
        }

        BigDecimal saldoOrigenAntes  = origen.getSaldo();
        BigDecimal saldoDestinoAntes = destino.getSaldo();

        origen.setSaldo(saldoOrigenAntes.subtract(req.monto()));
        destino.setSaldo(saldoDestinoAntes.add(req.monto()));

        cuentaRepository.save(origen);
        cuentaRepository.save(destino);

        String desc = req.descripcion() != null ? req.descripcion() : "Transferencia";

        // Movimiento en cuenta origen
        Movimiento movOrigen = new Movimiento();
        movOrigen.setCuenta(origen);
        movOrigen.setTipo(Movimiento.TipoMovimiento.TRANSFERENCIA_ENVIADA);
        movOrigen.setMonto(req.monto());
        movOrigen.setSaldoAnterior(saldoOrigenAntes);
        movOrigen.setSaldoPosterior(origen.getSaldo());
        movOrigen.setDescripcion(desc);
        movOrigen.setCuentaDestino(destino.getNumeroCuenta());
        movimientoRepository.save(movOrigen);

        // Movimiento en cuenta destino
        Movimiento movDestino = new Movimiento();
        movDestino.setCuenta(destino);
        movDestino.setTipo(Movimiento.TipoMovimiento.TRANSFERENCIA_RECIBIDA);
        movDestino.setMonto(req.monto());
        movDestino.setSaldoAnterior(saldoDestinoAntes);
        movDestino.setSaldoPosterior(destino.getSaldo());
        movDestino.setDescripcion(desc);
        movDestino.setCuentaDestino(origen.getNumeroCuenta());
        movimientoRepository.save(movDestino);
    }

    /** Crea una cuenta de ahorros de prueba para el usuario (útil en desarrollo). */
    @Transactional
    public CuentaDto crearCuentaPrueba(String email) {
        Usuario usuario = findUsuario(email);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(generarNumeroCuenta());
        cuenta.setTipoCuenta(Cuenta.TipoCuenta.AHORROS);
        cuenta.setSaldo(new BigDecimal("1000.00"));
        cuenta.setMoneda("PEN");
        cuenta.setUsuario(usuario);

        return CuentaDto.from(cuentaRepository.save(cuenta));
    }

    /* ── Helpers ── */

    private Usuario findUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    private Cuenta findCuentaDelUsuario(Long cuentaId, String email) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada."));
        if (!cuenta.getUsuario().getEmail().equals(email)) {
            throw new SecurityException("No tienes permiso sobre esta cuenta.");
        }
        return cuenta;
    }

    private String generarNumeroCuenta() {
        Random rnd = new Random();
        String numero;
        do {
            numero = String.format("%016d", (long)(rnd.nextDouble() * 1_000_000_000_000_000L));
        } while (cuentaRepository.existsByNumeroCuenta(numero));
        return numero;
    }
}
