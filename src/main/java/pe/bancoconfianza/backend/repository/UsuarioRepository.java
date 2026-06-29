package pe.bancoconfianza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pe.bancoconfianza.backend.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    
    Optional<Usuario> findByNumeroTarjeta(String numeroTarjeta);
    Optional<Usuario> findByNumeroTarjetaAndDni(String numeroTarjeta, String dni);
    Optional<Usuario> findByDni(String dni);
    boolean existsByDni(String dni);
    boolean existsByNumeroTarjeta(String numeroTarjeta);
    
    @Modifying
    @Transactional
    @Query(value = "UPDATE usuarios SET password = :password WHERE email = :email", nativeQuery = true)
    int updatePasswordNative(@Param("email") String email, @Param("password") String password);
}
