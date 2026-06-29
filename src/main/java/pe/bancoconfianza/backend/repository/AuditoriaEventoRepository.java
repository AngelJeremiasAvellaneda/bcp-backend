package pe.bancoconfianza.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.bancoconfianza.backend.model.AuditoriaEvento;

import java.util.List;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    Page<AuditoriaEvento> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditoriaEvento> findTop20ByEmailActorOrderByCreatedAtDesc(String email);

    List<AuditoriaEvento> findTop50ByOrderByCreatedAtDesc();
}
