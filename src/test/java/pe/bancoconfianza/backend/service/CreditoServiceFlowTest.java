package pe.bancoconfianza.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pe.bancoconfianza.backend.dto.CreditoDto;
import pe.bancoconfianza.backend.dto.SolicitudCreditoRequest;
import pe.bancoconfianza.backend.model.Cuenta;
import pe.bancoconfianza.backend.model.Credito;
import pe.bancoconfianza.backend.model.Usuario;
import pe.bancoconfianza.backend.repository.CreditoRepository;
import pe.bancoconfianza.backend.repository.CuentaRepository;
import pe.bancoconfianza.backend.repository.UsuarioRepository;
import pe.bancoconfianza.backend.repository.AuditoriaEventoRepository;
import pe.bancoconfianza.backend.repository.CuotaCreditoRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite para el flujo completo de solicitud de crédito.
 * Valida: cuenta válida, cuenta inactiva, pertenencia, etc.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({CreditoService.class, CuentaService.class})
@DisplayName("Flujo Completo de Solicitud de Crédito")
public class CreditoServiceFlowTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CreditoService creditoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private CreditoRepository creditoRepository;

    private Usuario cliente;
    private Usuario otroCliente;
    private Cuenta cuentaActiva;
    private Cuenta cuentaInactiva;

    @BeforeEach
    void setUp() {
        // Crear cliente Angel
        cliente = new Usuario();
        cliente.setNombre("Angel Jeremias");
        cliente.setEmail("angel@banco.pe");
        cliente.setPassword("hashedPassword");
        cliente.setRol(Usuario.Rol.CLIENTE);
        cliente.setActivo(true);
        cliente = usuarioRepository.save(cliente);

        // Crear otro cliente (para validar seguridad)
        otroCliente = new Usuario();
        otroCliente.setNombre("Otro Usuario");
        otroCliente.setEmail("otro@banco.pe");
        otroCliente.setPassword("hashedPassword");
        otroCliente.setRol(Usuario.Rol.CLIENTE);
        otroCliente.setActivo(true);
        otroCliente = usuarioRepository.save(otroCliente);

        // Crear cuenta activa de Angel
        cuentaActiva = new Cuenta();
        cuentaActiva.setNumeroCuenta("0099887766550001");
        cuentaActiva.setTipoCuenta(Cuenta.TipoCuenta.DIGITAL);
        cuentaActiva.setSaldo(BigDecimal.ZERO);
        cuentaActiva.setMoneda("PEN");
        cuentaActiva.setActiva(true);
        cuentaActiva.setUsuario(cliente);
        cuentaActiva = cuentaRepository.save(cuentaActiva);

        // Crear cuenta inactiva de Angel
        cuentaInactiva = new Cuenta();
        cuentaInactiva.setNumeroCuenta("0099887766550002");
        cuentaInactiva.setTipoCuenta(Cuenta.TipoCuenta.AHORROS);
        cuentaInactiva.setSaldo(new BigDecimal("500.00"));
        cuentaInactiva.setMoneda("PEN");
        cuentaInactiva.setActiva(false);  // ← INACTIVA
        cuentaInactiva.setUsuario(cliente);
        cuentaInactiva = cuentaRepository.save(cuentaInactiva);

        entityManager.flush();
    }

    @Test
    @DisplayName("Caso 1: Cliente con cuenta activa y saldo 0 → Solicitud exitosa")
    void testSolicitudExitosaConCuentaActivaSaldoCero() {
        // Arrange
        SolicitudCreditoRequest request = new SolicitudCreditoRequest(
            "PERSONAL",
            new BigDecimal("3000.00"),
            6,
            "PEN",
            "Universidad",
            new BigDecimal("1200.00"),
            BigDecimal.ZERO,
            "0099887766550001"  // Cuenta activa, saldo 0
        );

        // Act
        CreditoDto resultado = creditoService.solicitarCredito(request, cliente.getEmail());

        // Assert
        assertNotNull(resultado);
        assertNotNull(resultado.id());
        assertNotNull(resultado.numeroOperacion());
        assertEquals("EN_EVALUACION", resultado.estado());
        assertEquals(new BigDecimal("3000.00"), resultado.montoSolicitado());
        assertNotNull(resultado.scoreCrediticio());
        assertNotNull(resultado.rdsRatio());
        assertNotNull(resultado.rdsSemaforo());

        // Verificar que se guardó en BD
        Credito creditoGuardado = creditoRepository.findByNumeroOperacion(resultado.numeroOperacion()).orElse(null);
        assertNotNull(creditoGuardado);
        assertEquals(cliente.getId(), creditoGuardado.getCliente().getId());
        assertEquals(cuentaActiva.getId(), creditoGuardado.getCuentaDesembolso().getId());
    }

    @Test
    @DisplayName("Caso 2: Cliente sin cuenta activa → Bloqueo")
    void testBloqueoSinCuentaActiva() {
        // Arrange
        SolicitudCreditoRequest request = new SolicitudCreditoRequest(
            "PERSONAL",
            new BigDecimal("2000.00"),
            12,
            "PEN",
            "Consumo",
            new BigDecimal("1500.00"),
            BigDecimal.ZERO,
            "0099887766550002"  // Cuenta INACTIVA
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditoService.solicitarCredito(request, cliente.getEmail())
        );

        assertTrue(exception.getMessage().contains("válida") || 
                   exception.getMessage().contains("activa") ||
                   exception.getMessage().contains("inactiva"));
    }

    @Test
    @DisplayName("Caso 3: Cuenta vacía → Error claro")
    void testErrorCuentaVacia() {
        // Arrange
        SolicitudCreditoRequest request = new SolicitudCreditoRequest(
            "PERSONAL",
            new BigDecimal("2000.00"),
            12,
            "PEN",
            "Consumo",
            new BigDecimal("1500.00"),
            BigDecimal.ZERO,
            ""  // Cuenta VACÍA
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditoService.solicitarCredito(request, cliente.getEmail())
        );

        assertTrue(exception.getMessage().contains("seleccionar") || 
                   exception.getMessage().contains("obligatoria"));
    }

    @Test
    @DisplayName("Caso 4: Numero de cuenta válido pero de otro usuario → Seguridad")
    void testSeguridad_CuentaDeOtroUsuario() {
        // Arrange: Crear cuenta de otro usuario
        Cuenta cuentaDelOtro = new Cuenta();
        cuentaDelOtro.setNumeroCuenta("9999999999999999");
        cuentaDelOtro.setTipoCuenta(Cuenta.TipoCuenta.AHORROS);
        cuentaDelOtro.setSaldo(new BigDecimal("5000.00"));
        cuentaDelOtro.setMoneda("PEN");
        cuentaDelOtro.setActiva(true);
        cuentaDelOtro.setUsuario(otroCliente);
        cuentaDelOtro = cuentaRepository.save(cuentaDelOtro);

        SolicitudCreditoRequest request = new SolicitudCreditoRequest(
            "PERSONAL",
            new BigDecimal("3000.00"),
            6,
            "PEN",
            "Universidad",
            new BigDecimal("1200.00"),
            BigDecimal.ZERO,
            "9999999999999999"  // Cuenta de OTRO usuario
        );

        // Act & Assert: Angel NO debería poder usar cuenta de Otro
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> creditoService.solicitarCredito(request, cliente.getEmail())
        );

        assertTrue(exception.getMessage().contains("válida") || 
                   exception.getMessage().contains("no existe"));
    }

    @Test
    @DisplayName("Caso 5: Query correcta con filtro usuario_id + activa")
    void testQueryCuentaConFiltros() {
        // Arrange
        String numeroCuenta = cuentaActiva.getNumeroCuenta();
        Long usuarioId = cliente.getId();

        // Act: Buscar con 3 filtros
        Optional<Cuenta> cuentaEncontrada = cuentaRepository
            .findByNumeroCuentaAndUsuarioIdAndActivaTrue(numeroCuenta, usuarioId);

        // Assert
        assertTrue(cuentaEncontrada.isPresent());
        assertEquals(cuentaActiva.getId(), cuentaEncontrada.get().getId());
        assertTrue(cuentaEncontrada.get().isActiva());
        assertEquals(usuarioId, cuentaEncontrada.get().getUsuario().getId());
    }

    @Test
    @DisplayName("Caso 6: Query no devuelve cuenta inactiva aunque el número sea correcto")
    void testQueryNoDevolverCuentaInactiva() {
        // Arrange
        String numeroCuenta = cuentaInactiva.getNumeroCuenta();
        Long usuarioId = cliente.getId();

        // Act
        Optional<Cuenta> cuentaEncontrada = cuentaRepository
            .findByNumeroCuentaAndUsuarioIdAndActivaTrue(numeroCuenta, usuarioId);

        // Assert
        assertTrue(cuentaEncontrada.isEmpty(), "No debería devolver cuenta inactiva");
    }

    @Test
    @DisplayName("Caso 7: Validaciones de campos del request")
    void testValidacionesRequest() {
        // Arrange - montos fuera de rango
        SolicitudCreditoRequest requestMontoBajo = new SolicitudCreditoRequest(
            "PERSONAL",
            new BigDecimal("100.00"),  // < 500 mínimo
            6,
            "PEN",
            "Consumo",
            new BigDecimal("1200.00"),
            BigDecimal.ZERO,
            "0099887766550001"
        );

        // Act & Assert: Validación @DecimalMin
        assertThrows(Exception.class,
            () -> creditoService.solicitarCredito(requestMontoBajo, cliente.getEmail())
        );
    }
}
