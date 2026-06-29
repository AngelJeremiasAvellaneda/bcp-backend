package pe.bancoconfianza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.bancoconfianza.backend.model.Cuenta;
import pe.bancoconfianza.backend.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    List<Cuenta> findByUsuarioAndActivaTrue(Usuario usuario);
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    Optional<Cuenta> findByNumeroCuentaAndUsuarioIdAndActivaTrue(String numeroCuenta, Long usuarioId);
    boolean existsByNumeroCuenta(String numeroCuenta);
}
