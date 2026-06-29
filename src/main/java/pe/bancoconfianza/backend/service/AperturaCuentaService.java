package pe.bancoconfianza.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.bancoconfianza.backend.dto.AperturaCuentaRequest;
import pe.bancoconfianza.backend.dto.ValidacionTarjetaRequest;
import pe.bancoconfianza.backend.model.Cuenta;
import pe.bancoconfianza.backend.model.Usuario;
import pe.bancoconfianza.backend.repository.CuentaRepository;
import pe.bancoconfianza.backend.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AperturaCuentaService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Valida que la tarjeta y clave sean correctos para iniciar el proceso
     */
    public Usuario validarTarjetaYClave(ValidacionTarjetaRequest request) {
        // Buscar usuario por tarjeta y DNI
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNumeroTarjetaAndDni(
            request.getNumeroTarjeta(), 
            request.getDni()
        );
        
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Tarjeta o DNI no encontrados");
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Validar la clave
        if (!passwordEncoder.matches(request.getClave(), usuario.getPassword())) {
            throw new RuntimeException("Clave incorrecta");
        }
        
        // Verificar que no tenga ya una cuenta creada
        if (usuario.isCuentaCreada()) {
            throw new RuntimeException("Este usuario ya tiene una cuenta creada");
        }
        
        return usuario;
    }

    /**
     * Crea la cuenta digital y el usuario en el sistema
     */
    @Transactional
    public Cuenta crearCuentaDigital(AperturaCuentaRequest request) {
        // Validar DNI (8 dígitos)
        if (request.getDni() == null || request.getDni().length() != 8 || !request.getDni().matches("\\d{8}")) {
            throw new RuntimeException("El DNI debe tener exactamente 8 dígitos");
        }

        // Verificar si el DNI ya existe con cuenta creada
        Optional<Usuario> usuarioExistente = usuarioRepository.findByDni(request.getDni());
        if (usuarioExistente.isPresent() && usuarioExistente.get().isCuentaCreada()) {
            throw new RuntimeException("Este DNI ya tiene una cuenta creada en el sistema");
        }

        // Verificar si el email ya existe
        Optional<Usuario> usuarioEmail = usuarioRepository.findByEmail(request.getEmail());
        if (usuarioEmail.isPresent()) {
            throw new RuntimeException("Este email ya está registrado en el sistema");
        }

        // Crear o actualizar usuario
        Usuario usuario;
        if (usuarioExistente.isPresent()) {
            usuario = usuarioExistente.get();
        } else {
            usuario = new Usuario();
        }

        // Asignar datos del usuario
        usuario.setNombre(request.getNombreCompleto());
        usuario.setEmail(request.getEmail());
        // Encriptar la contraseña automáticamente
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setDni(request.getDni());
        usuario.setNumeroTarjeta(limpiarNumeroTarjeta(request.getNumeroTarjeta()));
        usuario.setTelefono(request.getTelefono());
        usuario.setDireccion(request.getDireccion());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setOcupacion(request.getOcupacion());
        usuario.setProfesion(request.getProfesion());
        usuario.setCentroLaboral(request.getCentroLaboral());
        usuario.setRol(Usuario.Rol.CLIENTE);
        usuario.setActivo(true);
        usuario.setCuentaCreada(true);

        usuarioRepository.save(usuario);

        // Crear la cuenta digital
        Cuenta cuenta = new Cuenta();
        cuenta.setUsuario(usuario);
        cuenta.setNumeroCuenta(generarNumeroCuenta());
        cuenta.setCci(generarCCI(cuenta.getNumeroCuenta()));
        cuenta.setTipoCuenta(Cuenta.TipoCuenta.valueOf(request.getTipoCuenta().toUpperCase()));
        cuenta.setSaldo(BigDecimal.ZERO);
        cuenta.setMoneda(request.getMoneda() != null ? request.getMoneda() : "SOLES");
        cuenta.setActiva(true);
        cuenta.setFechaApertura(LocalDateTime.now());

        return cuentaRepository.save(cuenta);
    }

    /**
     * Limpia el número de tarjeta removiendo espacios y guiones
     */
    private String limpiarNumeroTarjeta(String numeroTarjeta) {
        if (numeroTarjeta == null) {
            return null;
        }
        return numeroTarjeta.replaceAll("[\\s-]", "");
    }

    /**
     * Genera un número de cuenta aleatorio
     */
    private String generarNumeroCuenta() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("251");
        for (int i = 0; i < 11; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Genera un CCI basado en el número de cuenta
     */
    private String generarCCI(String numeroCuenta) {
        return "002" + numeroCuenta + "00";
    }
}
