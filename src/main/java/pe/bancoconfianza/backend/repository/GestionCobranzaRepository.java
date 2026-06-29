package pe.bancoconfianza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.bancoconfianza.backend.model.Credito;
import pe.bancoconfianza.backend.model.GestionCobranza;

import java.util.List;

public interface GestionCobranzaRepository extends JpaRepository<GestionCobranza, Long> {

    List<GestionCobranza> findByCreditoOrderByCreatedAtDesc(Credito credito);
}
