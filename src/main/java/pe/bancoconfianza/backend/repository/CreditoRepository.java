package pe.bancoconfianza.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.bancoconfianza.backend.model.Credito;
import pe.bancoconfianza.backend.model.Credito.EstadoCredito;
import pe.bancoconfianza.backend.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface CreditoRepository extends JpaRepository<Credito, Long> {

    Optional<Credito> findByNumeroOperacion(String numeroOperacion);

    List<Credito> findByClienteOrderByCreatedAtDesc(Usuario cliente);

    Page<Credito> findByEstadoOrderByCreatedAtAsc(EstadoCredito estado, Pageable pageable);

    @Query(value = "SELECT * FROM creditos WHERE estado::text = ANY(CAST(:estados AS text[]))", nativeQuery = true)
    List<Credito> findByEstadoIn(@org.springframework.data.repository.query.Param("estados") String[] estados);

    boolean existsByNumeroOperacion(String numeroOperacion);

    /** Créditos DESEMBOLSADOS que tienen al menos una cuota vencida */
    @Query(value = """
        SELECT DISTINCT cr.* FROM creditos cr
        JOIN cuotas_credito cc ON cc.credito_id = cr.id
        WHERE cr.estado = 'DESEMBOLSADO'
          AND cc.estado_cuota = 'VENCIDA'
        """, nativeQuery = true)
    List<Credito> findCreditosConMora();

    List<Credito> findByAsesorAndEstadoIn(Usuario asesor, List<EstadoCredito> estados);
}
