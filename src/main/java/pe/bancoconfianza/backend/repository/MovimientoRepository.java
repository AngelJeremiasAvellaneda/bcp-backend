package pe.bancoconfianza.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.bancoconfianza.backend.model.Cuenta;
import pe.bancoconfianza.backend.model.Movimiento;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    Page<Movimiento> findByCuentaOrderByCreatedAtDesc(Cuenta cuenta, Pageable pageable);
    List<Movimiento> findTop10ByCuentaOrderByCreatedAtDesc(Cuenta cuenta);
}
