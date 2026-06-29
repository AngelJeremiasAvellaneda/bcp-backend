package pe.bancoconfianza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.bancoconfianza.backend.model.Credito;
import pe.bancoconfianza.backend.model.CuotaCredito;
import pe.bancoconfianza.backend.model.CuotaCredito.EstadoCuota;

import java.time.LocalDate;
import java.util.List;

public interface CuotaCreditoRepository extends JpaRepository<CuotaCredito, Long> {

    List<CuotaCredito> findByCreditoOrderByNumeroCuotaAsc(Credito credito);

    List<CuotaCredito> findByCreditoAndEstadoCuotaIn(Credito credito, List<EstadoCuota> estados);

    /** Cuotas vencidas (fecha <= hoy y no pagadas) para recalcular mora */
    @Query("SELECT cc FROM CuotaCredito cc WHERE cc.estadoCuota = pe.bancoconfianza.backend.model.CuotaCredito$EstadoCuota.PENDIENTE AND cc.fechaVencimiento < :hoy")
    List<CuotaCredito> findVencidasSinPagar(@Param("hoy") LocalDate hoy);

    /** Días de mora máximos de un crédito */
    @Query("SELECT MAX(cc.diasMora) FROM CuotaCredito cc WHERE cc.credito = :credito AND cc.estadoCuota IN (pe.bancoconfianza.backend.model.CuotaCredito$EstadoCuota.VENCIDA, pe.bancoconfianza.backend.model.CuotaCredito$EstadoCuota.PENDIENTE)")
    Integer maxDiasMora(@Param("credito") Credito credito);
}
