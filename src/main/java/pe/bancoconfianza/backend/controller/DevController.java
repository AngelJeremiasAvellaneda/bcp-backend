package pe.bancoconfianza.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.bancoconfianza.backend.model.*;
import pe.bancoconfianza.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Endpoints de desarrollo para testing masivo.
 * SOLO USAR EN DESARROLLO - Eliminar en producción.
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {

    private static final Logger log = LoggerFactory.getLogger(DevController.class);

    private final UsuarioRepository usuarioRepo;
    private final CuentaRepository cuentaRepo;
    private final CreditoRepository creditoRepo;
    private final CuotaCreditoRepository cuotaRepo;
    private final MovimientoRepository movimientoRepo;
    private final GestionCobranzaRepository gestionRepo;

    public DevController(UsuarioRepository usuarioRepo, CuentaRepository cuentaRepo, CreditoRepository creditoRepo,
                         CuotaCreditoRepository cuotaRepo, MovimientoRepository movimientoRepo,
                         GestionCobranzaRepository gestionRepo) {
        this.usuarioRepo = usuarioRepo;
        this.cuentaRepo = cuentaRepo;
        this.creditoRepo = creditoRepo;
        this.cuotaRepo = cuotaRepo;
        this.movimientoRepo = movimientoRepo;
        this.gestionRepo = gestionRepo;
    }

    /**
     * POST /api/dev/seed-masivo?force=true
     * Genera 1050 clientes con cuentas, créditos, cuotas, movimientos y gestiones de cobranza.
     * Parámetro force=true para regenerar aunque ya existan clientes
     */
    @PostMapping("/seed-masivo")
    public ResponseEntity<Map<String, Object>> seedMasivo(@RequestParam(required = false) boolean force) {
        log.info("[DevController] Iniciando seed masivo (force={})", force);

        Map<String, Object> resultado = new HashMap<>();

        try {
            // Verificar si ya hay muchos clientes
            long clienteCount = usuarioRepo.findAll().stream()
                    .filter(u -> u.getRol() == Usuario.Rol.CLIENTE)
                    .count();

            if (clienteCount > 100 && !force) {
                resultado.put("status", "skip");
                resultado.put("message", "Ya existen " + clienteCount + " clientes. Seed no ejecutado. Use ?force=true para forzar.");
                return ResponseEntity.ok(resultado);
            }

            log.info("[Seed] PASO 1: Insertando 1050 clientes...");

            // PASO 1: Insertar clientes
            List<Usuario> clientesInsertados = new ArrayList<>();
            int startIndex = force ? 2000 : 1;  // Si force=true, empezar desde cliente2000
            int clientesCreados = 0;
            
            for (int i = startIndex; i < startIndex + 1050; i++) {
                String email = "cliente" + i + "@banco.pe";

                // Si force=true, crear aunque exista. Si no, skip si existe.
                if (!force && usuarioRepo.existsByEmail(email)) {
                    continue;
                }

                // Si force=true, verificar que no exista con este email exacto
                if (force && usuarioRepo.existsByEmail(email)) {
                    continue;
                }

                Usuario cliente = new Usuario();
                cliente.setNombre("Cliente " + i + " " + getApellido(i));
                cliente.setEmail(email);
                cliente.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMye81YKJ1h9vhW4PULXDqDvvXy7IlNlpmu");
                cliente.setDni(String.format("%08d", 2000000 + i));  // DNI único también
                cliente.setRol(Usuario.Rol.CLIENTE);
                cliente.setActivo(true);

                clientesInsertados.add(usuarioRepo.save(cliente));
                clientesCreados++;

                if (clientesCreados % 200 == 0) {
                    log.info("[Seed] ... {} clientes", clientesCreados);
                }
            }

            log.info("[Seed] PASO 2: Insertando cuentas...");

            // PASO 2: Insertar cuentas
            int cuentasInsertadas = 0;
            for (Usuario cliente : clientesInsertados) {
                if (cuentaRepo.findByUsuarioAndActivaTrue(cliente).size() > 0) {
                    continue;
                }

                Cuenta cuenta = new Cuenta();
                cuenta.setNumeroCuenta(String.format("%016d", 100000000L + cliente.getId()));
                cuenta.setCci(cuenta.getNumeroCuenta() + "XXX");
                cuenta.setTipoCuenta(Cuenta.TipoCuenta.values()[cliente.getId().intValue() % 3]);
                cuenta.setSaldo(new BigDecimal((5000 + (cliente.getId() % 50000))));
                cuenta.setMoneda("PEN");
                cuenta.setActiva(true);
                cuenta.setUsuario(cliente);
                cuenta.setFechaApertura(LocalDate.now().minusDays(cliente.getId() % 365).atStartOfDay());

                cuentaRepo.save(cuenta);
                cuentasInsertadas++;

                if (cuentasInsertadas % 200 == 0) {
                    log.info("[Seed] ... {} cuentas", cuentasInsertadas);
                }
            }

            log.info("[Seed] PASO 3: Insertando créditos...");

            // PASO 3: Obtener asesores
            List<Usuario> asesores = usuarioRepo.findAll().stream()
                    .filter(u -> u.getRol() == Usuario.Rol.ASESOR)
                    .toList();

            if (asesores.isEmpty()) {
                resultado.put("status", "error");
                resultado.put("message", "No hay asesores disponibles");
                return ResponseEntity.badRequest().body(resultado);
            }

            // PASO 4: Obtener gestores para cobranza (usamos rol RIESGOS)
            List<Usuario> gestores = usuarioRepo.findAll().stream()
                    .filter(u -> u.getRol() == Usuario.Rol.RIESGOS)
                    .toList();

            // PASO 5: Insertar créditos
            int creditosInsertados = 0;
            int creditoNum = 100000;
            List<Credito> creditosCreados = new ArrayList<>();

            for (Usuario cliente : clientesInsertados) {
                var cuentas = cuentaRepo.findByUsuarioAndActivaTrue(cliente);
                if (cuentas.isEmpty()) continue;

                Cuenta cuentaDesembolso = cuentas.get(0);
                Usuario asesor = asesores.get(cliente.getId().intValue() % asesores.size());

                // Crédito 1
                Credito cred1 = crearCredito(cliente, asesor, cuentaDesembolso, creditoNum++);
                creditosCreados.add(cred1);
                creditosInsertados++;

                // Crédito 2
                Credito cred2 = crearCredito(cliente, asesor, cuentaDesembolso, creditoNum++);
                creditosCreados.add(cred2);
                creditosInsertados++;

                if (creditosInsertados % 400 == 0) {
                    log.info("[Seed] ... {} créditos", creditosInsertados);
                }
            }

            log.info("[Seed] PASO 4: Generando cuotas para créditos desembolsados...");

            // PASO 6: Generar cuotas para créditos desembolsados
            int cuotasGeneradas = 0;
            for (Credito credito : creditosCreados) {
                if (credito.getEstado() == Credito.EstadoCredito.DESEMBOLSADO && credito.getMontoAprobado() != null) {
                    generarCuotasCredito(credito);
                    cuotasGeneradas += credito.getPlazoMeses();
                    if (cuotasGeneradas % 500 == 0) {
                        log.info("[Seed] ... {} cuotas", cuotasGeneradas);
                    }
                }
            }

            log.info("[Seed] PASO 5: Generando movimientos de desembolso...");

            // PASO 7: Generar movimientos de desembolso para créditos desembolsados
            int movimientosGenerados = 0;
            for (Credito credito : creditosCreados) {
                if (credito.getEstado() == Credito.EstadoCredito.DESEMBOLSADO && 
                    credito.getCuentaDesembolso() != null && credito.getMontoAprobado() != null) {
                    
                    Cuenta cuenta = credito.getCuentaDesembolso();
                    BigDecimal saldoAnterior = cuenta.getSaldo().subtract(credito.getMontoAprobado());
                    
                    Movimiento mov = new Movimiento();
                    mov.setCuenta(cuenta);
                    mov.setTipo(Movimiento.TipoMovimiento.DEPOSITO);
                    mov.setMonto(credito.getMontoAprobado());
                    mov.setSaldoAnterior(saldoAnterior);
                    mov.setSaldoPosterior(cuenta.getSaldo());
                    mov.setDescripcion("Desembolso " + credito.getNumeroOperacion());
                    
                    movimientoRepo.save(mov);
                    movimientosGenerados++;
                }
            }

            log.info("[Seed] PASO 6: Generando gestiones de cobranza para créditos con cuotas vencidas...");

            // PASO 8: Generar gestiones de cobranza para créditos con mora
            int gestionsGeneradas = 0;
            if (!gestores.isEmpty()) {
                for (Credito credito : creditosCreados) {
                    if (credito.getEstado() == Credito.EstadoCredito.DESEMBOLSADO) {
                        var cuotas = cuotaRepo.findByCreditoOrderByNumeroCuotaAsc(credito);
                        for (CuotaCredito cuota : cuotas) {
                            // Simular algunas cuotas vencidas
                            if (cuota.getNumeroCuota() % 3 == 0 && cuota.getNumeroCuota() <= 3) {
                                cuota.setEstadoCuota(CuotaCredito.EstadoCuota.VENCIDA);
                                cuota.setDiasMora(cuota.getNumeroCuota() * 10);
                                cuota.setMontoMora(cuota.getCuotaTotal().multiply(new BigDecimal("0.10")));
                                cuotaRepo.save(cuota);

                                // Registrar gestión de cobranza
                                GestionCobranza gestion = new GestionCobranza();
                                gestion.setCredito(credito);
                                gestion.setCuota(cuota);
                                Usuario gestor = gestores.get((int)(credito.getId() % gestores.size()));
                                gestion.setGestor(gestor);
                                gestion.setTipoGestion(GestionCobranza.TipoGestion.LLAMADA_TELEFONICA);
                                gestion.setResultado(GestionCobranza.ResultadoGestion.PROMESA_PAGO);
                                gestion.setDescripcion("Gestión automática - Promesa de pago");
                                gestion.setDiasMoraAlGestionar(cuota.getDiasMora());
                                gestion.setFechaCompromisoPago(LocalDate.now().plusDays(5));

                                gestionRepo.save(gestion);
                                gestionsGeneradas++;
                            }
                        }
                    }
                }
            }

            // Estadísticas finales
            long totalClientes = usuarioRepo.findAll().stream()
                    .filter(u -> u.getRol() == Usuario.Rol.CLIENTE)
                    .count();
            long totalCuentas = cuentaRepo.count();
            long totalCreditos = creditoRepo.count();
            long totalCuotas = cuotaRepo.count();
            long totalMovimientos = movimientoRepo.count();
            long totalGestiones = gestionRepo.count();
            
            long creditosEnEvaluacion = creditoRepo.findAll().stream()
                    .filter(c -> c.getEstado() == Credito.EstadoCredito.EN_EVALUACION)
                    .count();
            long creditosDesembolsados = creditoRepo.findAll().stream()
                    .filter(c -> c.getEstado() == Credito.EstadoCredito.DESEMBOLSADO)
                    .count();

            log.info("[Seed] ══════════════════════════════════════════════════");
            log.info("[Seed] SEED COMPLETADO:");
            log.info("[Seed]   Total Clientes: {}", totalClientes);
            log.info("[Seed]   Total Cuentas: {}", totalCuentas);
            log.info("[Seed]   Total Créditos: {}", totalCreditos);
            log.info("[Seed]   Total Cuotas: {}", totalCuotas);
            log.info("[Seed]   Total Movimientos: {}", totalMovimientos);
            log.info("[Seed]   Total Gestiones Cobranza: {}", totalGestiones);
            log.info("[Seed]   - EN_EVALUACION: {} ← Asesor verá estos", creditosEnEvaluacion);
            log.info("[Seed]   - DESEMBOLSADOS: {}", creditosDesembolsados);

            resultado.put("status", "success");
            resultado.put("totalClientes", totalClientes);
            resultado.put("totalCuentas", totalCuentas);
            resultado.put("totalCreditos", totalCreditos);
            resultado.put("totalCuotas", totalCuotas);
            resultado.put("totalMovimientos", totalMovimientos);
            resultado.put("totalGestiones", totalGestiones);
            resultado.put("creditosEnEvaluacion", creditosEnEvaluacion);
            resultado.put("creditosDesembolsados", creditosDesembolsados);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("[Seed] Error durante la carga masiva", e);
            resultado.put("status", "error");
            resultado.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(resultado);
        }
    }

    private String getApellido(int i) {
        String[] apellidos = {"García", "López", "Martínez", "Rodríguez", "Fernández"};
        return apellidos[i % apellidos.length];
    }

    private Credito crearCredito(Usuario cliente, Usuario asesor, Cuenta cuentaDesembolso, int numeroCredito) {
        Credito credito = new Credito();
        credito.setNumeroOperacion(String.format("CRED-2026-%06d", numeroCredito));
        credito.setCliente(cliente);
        credito.setAsesor(asesor);
        credito.setTipoProducto(Credito.TipoProducto.values()[cliente.getId().intValue() % 5]);

        BigDecimal monto = new BigDecimal(5000 + (cliente.getId() % 195000));
        credito.setMontoSolicitado(monto);

        if (numeroCredito % 3 != 0) {
            credito.setMontoAprobado(monto);
        }

        credito.setTea(getTEA(credito.getTipoProducto()));
        credito.setPlazoMeses((int) (6 + (cliente.getId() % 60)));
        credito.setCuotaMensual(monto.multiply(new BigDecimal("0.05")).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP));
        credito.setMoneda("PEN");
        credito.setProposito(getProposito(numeroCredito));

        int score = (int) (300 + (cliente.getId() % 800));
        credito.setScoreCrediticio(score);

        credito.setIngresoMensual(new BigDecimal(2500 + (cliente.getId() % 17500)));
        credito.setDeudaTotalVigente(new BigDecimal((cliente.getId() % 10000) / 100.0));

        BigDecimal rds = new BigDecimal((cliente.getId() % 7000) / 10000.0);
        credito.setRdsRatio(rds);
        credito.setRdsSemaforo(getRDS(rds));
        credito.setEsSujetoCredito(cliente.getId() % 11 > 2);

        credito.setEstado(getEstado(numeroCredito));
        credito.setRutaAprobacion(getRuta(numeroCredito));
        credito.setCuentaDesembolso(cuentaDesembolso);

        if (credito.getEstado() == Credito.EstadoCredito.DESEMBOLSADO) {
            credito.setFechaDesembolso(LocalDate.now().minusDays(cliente.getId() % 180));
            credito.setFechaPrimeraCuota(credito.getFechaDesembolso().plusDays(30));
            credito.setFechaUltimaCuota(credito.getFechaDesembolso().plusDays(180));
            credito.setAprobadoPor(asesor);
        }

        credito.setComentarioEvaluacion(getComentario(numeroCredito));

        return creditoRepo.save(credito);
    }

    private void generarCuotasCredito(Credito credito) {
        BigDecimal montoTotal = credito.getMontoAprobado();
        int plazo = credito.getPlazoMeses();
        BigDecimal tea = credito.getTea();
        
        // Cálculo mensual simple
        BigDecimal tasaMensual = tea.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);
        BigDecimal capital = montoTotal.divide(new BigDecimal(plazo), 2, RoundingMode.HALF_UP);
        
        BigDecimal saldoCapital = montoTotal;
        LocalDate fechaCuota = credito.getFechaPrimeraCuota();
        
        for (int i = 1; i <= plazo; i++) {
            CuotaCredito cuota = new CuotaCredito();
            cuota.setCredito(credito);
            cuota.setNumeroCuota(i);
            cuota.setFechaVencimiento(fechaCuota);
            
            BigDecimal interes = saldoCapital.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cuotaTotal = capital.add(interes);
            
            cuota.setCapital(capital);
            cuota.setInteres(interes);
            cuota.setSeguro(BigDecimal.ZERO);
            cuota.setCuotaTotal(cuotaTotal);
            
            saldoCapital = saldoCapital.subtract(capital).max(BigDecimal.ZERO);
            cuota.setSaldoCapital(saldoCapital);
            
            cuota.setEstadoCuota(CuotaCredito.EstadoCuota.PENDIENTE);
            cuota.setDiasMora(0);
            cuota.setMontoMora(BigDecimal.ZERO);
            
            cuotaRepo.save(cuota);
            fechaCuota = fechaCuota.plusMonths(1);
        }
    }

    private BigDecimal getTEA(Credito.TipoProducto tipo) {
        return switch (tipo) {
            case PERSONAL -> new BigDecimal("0.3500");
            case HIPOTECARIO -> new BigDecimal("0.0850");
            case VEHICULAR -> new BigDecimal("0.1600");
            case AGROPECUARIO -> new BigDecimal("0.2500");
            case MICROEMPRESA -> new BigDecimal("0.4000");
        };
    }

    private String getProposito(int num) {
        String[] propositos = {"Inversión educativa", "Compra de propiedad", "Compra de vehículo", "Capital de trabajo", "Consumo general"};
        return propositos[num % propositos.length];
    }

    private Credito.SemaforoRds getRDS(BigDecimal rds) {
        if (rds.compareTo(new BigDecimal("0.30")) < 0) {
            return Credito.SemaforoRds.VERDE;
        } else if (rds.compareTo(new BigDecimal("0.50")) < 0) {
            return Credito.SemaforoRds.AMARILLO;
        }
        return Credito.SemaforoRds.ROJO;
    }

    private Credito.EstadoCredito getEstado(int num) {
        int mod = num % 7;
        return switch (mod) {
            case 0 -> Credito.EstadoCredito.EN_EVALUACION;
            case 1 -> Credito.EstadoCredito.APROBADO;
            case 2 -> Credito.EstadoCredito.DESEMBOLSADO;
            case 3 -> Credito.EstadoCredito.RECHAZADO;
            case 4 -> Credito.EstadoCredito.PENDIENTE_RIESGOS;
            case 5 -> Credito.EstadoCredito.PENDIENTE_JEFE_REGIONAL;
            default -> Credito.EstadoCredito.PENDIENTE_COMITE;
        };
    }

    private Credito.RutaAprobacion getRuta(int num) {
        int mod = num % 3;
        return switch (mod) {
            case 0 -> Credito.RutaAprobacion.ASESOR;
            case 1 -> Credito.RutaAprobacion.ADMIN;
            default -> Credito.RutaAprobacion.JEFE_REGIONAL;
        };
    }

    private String getComentario(int num) {
        String[] comentarios = {
            "Evaluación en progreso",
            "Aprobado - Excelente perfil",
            "Desembolsado satisfactoriamente",
            "Requiere documentación adicional",
            "En revisión por comité"
        };
        return comentarios[num % comentarios.length];
    }

    /**
     * POST /api/dev/generar-cuotas
     * Genera cuotas para créditos desembolsados existentes (sin duplicar)
     */
    @PostMapping("/generar-cuotas")
    public ResponseEntity<Map<String, Object>> generarCuotas() {
        log.info("[DevController] Generando cuotas para créditos desembolsados...");

        Map<String, Object> resultado = new HashMap<>();

        try {
            List<Credito> creditosDesembolsados = creditoRepo.findAll().stream()
                    .filter(c -> c.getEstado() == Credito.EstadoCredito.DESEMBOLSADO)
                    .filter(c -> c.getMontoAprobado() != null)
                    .filter(c -> cuotaRepo.findByCreditoOrderByNumeroCuotaAsc(c).isEmpty())
                    .toList();

            log.info("[Cuotas] Generando cuotas para {} créditos desembolsados", creditosDesembolsados.size());

            int cuotasGeneradas = 0;
            for (Credito credito : creditosDesembolsados) {
                generarCuotasCredito(credito);
                cuotasGeneradas += credito.getPlazoMeses();
            }

            long totalCuotas = cuotaRepo.count();

            resultado.put("status", "success");
            resultado.put("creditosProcesados", creditosDesembolsados.size());
            resultado.put("cuotasGeneradas", cuotasGeneradas);
            resultado.put("totalCuotas", totalCuotas);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("[Cuotas] Error generando cuotas", e);
            resultado.put("status", "error");
            resultado.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(resultado);
        }
    }

    /**
     * POST /api/dev/generar-movimientos
     * Genera movimientos de desembolso y pago para créditos
     */
    @PostMapping("/generar-movimientos")
    public ResponseEntity<Map<String, Object>> generarMovimientos() {
        log.info("[DevController] Generando movimientos de desembolso y pago...");

        Map<String, Object> resultado = new HashMap<>();

        try {
            int desembolsosGenerados = 0;
            int pagosGenerados = 0;

            // Movimientos de desembolso
            List<Credito> creditosConDesembolso = creditoRepo.findAll().stream()
                    .filter(c -> c.getEstado() == Credito.EstadoCredito.DESEMBOLSADO)
                    .filter(c -> c.getMontoAprobado() != null && c.getFechaDesembolso() != null)
                    .toList();

            for (Credito credito : creditosConDesembolso) {
                // Verificar si ya existe movimiento de desembolso
                var movExistente = movimientoRepo.findAll().stream()
                        .filter(m -> m.getDescripcion() != null)
                        .filter(m -> m.getDescripcion().contains(credito.getNumeroOperacion()))
                        .findFirst();

                if (movExistente.isEmpty() && credito.getCuentaDesembolso() != null) {
                    Movimiento mov = new Movimiento();
                    mov.setCuenta(credito.getCuentaDesembolso());
                    mov.setTipo(Movimiento.TipoMovimiento.DEPOSITO);
                    mov.setMonto(credito.getMontoAprobado());
                    BigDecimal saldoAnterior = credito.getCuentaDesembolso().getSaldo().subtract(credito.getMontoAprobado());
                    mov.setSaldoAnterior(saldoAnterior);
                    mov.setSaldoPosterior(credito.getCuentaDesembolso().getSaldo());
                    mov.setDescripcion("Desembolso " + credito.getNumeroOperacion());
                    
                    movimientoRepo.save(mov);
                    desembolsosGenerados++;
                }
            }

            // Movimientos de pago de cuotas pagadas
            var cuotasPagadas = cuotaRepo.findAll().stream()
                    .filter(cc -> cc.getEstadoCuota() == CuotaCredito.EstadoCuota.PAGADA)
                    .filter(cc -> cc.getFechaPago() != null)
                    .toList();

            for (CuotaCredito cuota : cuotasPagadas) {
                var movExistente = movimientoRepo.findAll().stream()
                        .filter(m -> m.getDescripcion() != null)
                        .filter(m -> m.getDescripcion().contains("Pago cuota " + cuota.getNumeroCuota()))
                        .findFirst();

                if (movExistente.isEmpty()) {
                    Credito credito = cuota.getCredito();
                    var cuentasCliente = cuentaRepo.findByUsuarioAndActivaTrue(credito.getCliente());
                    
                    if (!cuentasCliente.isEmpty()) {
                        Cuenta cuenta = cuentasCliente.get(0);
                        Movimiento mov = new Movimiento();
                        mov.setCuenta(cuenta);
                        mov.setTipo(Movimiento.TipoMovimiento.RETIRO);
                        mov.setMonto(cuota.getCuotaTotal());
                        BigDecimal saldoAnterior = cuenta.getSaldo().add(cuota.getCuotaTotal());
                        mov.setSaldoAnterior(saldoAnterior);
                        mov.setSaldoPosterior(cuenta.getSaldo());
                        mov.setDescripcion("Pago cuota " + cuota.getNumeroCuota() + " - " + credito.getNumeroOperacion());
                        
                        movimientoRepo.save(mov);
                        pagosGenerados++;
                    }
                }
            }

            long totalMovimientos = movimientoRepo.count();

            resultado.put("status", "success");
            resultado.put("desembolsosGenerados", desembolsosGenerados);
            resultado.put("pagosGenerados", pagosGenerados);
            resultado.put("totalMovimientos", totalMovimientos);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("[Movimientos] Error generando movimientos", e);
            resultado.put("status", "error");
            resultado.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(resultado);
        }
    }

    /**
     * POST /api/dev/generar-cuotas-movimientos-gestiones
     * Genera cuotas, movimientos y gestiones para los créditos existentes
     * Usa queries simples sin transacciones complejas
     */
    @PostMapping("/generar-cuotas-movimientos-gestiones")
    public ResponseEntity<Map<String, Object>> generarTodo() {
        log.info("[DevController] Generando cuotas, movimientos y gestiones...");

        Map<String, Object> resultado = new HashMap<>();

        try {
            log.info("[Seed] Obteniendo créditos desembolsados...");
            var creditosDesembolsados = creditoRepo.findAll().stream()
                    .filter(c -> c.getEstado() == Credito.EstadoCredito.DESEMBOLSADO)
                    .filter(c -> c.getMontoAprobado() != null && c.getFechaDesembolso() != null)
                    .toList();

            log.info("[Seed] {} créditos desembolsados encontrados", creditosDesembolsados.size());

            int cuotasGeneradas = 0;
            int movimientosGenerados = 0;

            // Generar cuotas y movimientos
            for (Credito credito : creditosDesembolsados) {
                try {
                    // Generar cuotas solo si no existen
                    var cuotasExistentes = cuotaRepo.findByCreditoOrderByNumeroCuotaAsc(credito);
                    if (cuotasExistentes.isEmpty()) {
                        generarCuotasCredito(credito);
                        cuotasGeneradas += credito.getPlazoMeses();
                        log.info("[Seed] Cuotas generadas para crédito {}", credito.getNumeroOperacion());
                    }

                    // Generar movimiento de desembolso solo si no existe
                    var movExistente = movimientoRepo.findAll().stream()
                            .filter(m -> m.getCuenta().getId().equals(credito.getCuentaDesembolso().getId()))
                            .filter(m -> m.getTipo() == Movimiento.TipoMovimiento.DEPOSITO)
                            .findFirst();

                    if (movExistente.isEmpty()) {
                        Movimiento mov = new Movimiento();
                        mov.setCuenta(credito.getCuentaDesembolso());
                        mov.setTipo(Movimiento.TipoMovimiento.DEPOSITO);
                        mov.setMonto(credito.getMontoAprobado());
                        BigDecimal saldoAnterior = credito.getCuentaDesembolso().getSaldo()
                                .subtract(credito.getMontoAprobado());
                        mov.setSaldoAnterior(saldoAnterior);
                        mov.setSaldoPosterior(credito.getCuentaDesembolso().getSaldo());
                        mov.setDescripcion("Desembolso " + credito.getNumeroOperacion());

                        movimientoRepo.save(mov);
                        movimientosGenerados++;
                        log.info("[Seed] Movimiento generado para crédito {}", credito.getNumeroOperacion());
                    }
                } catch (Exception e) {
                    log.warn("[Seed] Error procesando crédito {}: {}", credito.getNumeroOperacion(), e.getMessage());
                }
            }

            // Generar gestiones
            log.info("[Seed] Obteniendo gestores...");
            List<Usuario> gestores = usuarioRepo.findAll().stream()
                    .filter(u -> u.getRol() == Usuario.Rol.RIESGOS)
                    .toList();

            int gestionesGeneradas = 0;
            if (!gestores.isEmpty()) {
                log.info("[Seed] {} gestores encontrados", gestores.size());
                var cuotasVencidas = cuotaRepo.findAll().stream()
                        .filter(cc -> cc.getEstadoCuota() == CuotaCredito.EstadoCuota.VENCIDA)
                        .toList();

                log.info("[Seed] {} cuotas vencidas encontradas", cuotasVencidas.size());

                for (CuotaCredito cuota : cuotasVencidas) {
                    try {
                        var gestionExistente = gestionRepo.findAll().stream()
                                .filter(g -> g.getCuota() != null && g.getCuota().getId().equals(cuota.getId()))
                                .findFirst();

                        if (gestionExistente.isEmpty()) {
                            GestionCobranza gestion = new GestionCobranza();
                            gestion.setCredito(cuota.getCredito());
                            gestion.setCuota(cuota);
                            Usuario gestor = gestores.get((int)(cuota.getId() % gestores.size()));
                            gestion.setGestor(gestor);
                            gestion.setTipoGestion(GestionCobranza.TipoGestion.LLAMADA_TELEFONICA);
                            gestion.setResultado(GestionCobranza.ResultadoGestion.PROMESA_PAGO);
                            gestion.setDescripcion("Gestión automática - Promesa de pago");
                            gestion.setDiasMoraAlGestionar(cuota.getDiasMora() != null ? cuota.getDiasMora() : 0);
                            gestion.setFechaCompromisoPago(LocalDate.now().plusDays(5));

                            gestionRepo.save(gestion);
                            gestionesGeneradas++;
                        }
                    } catch (Exception e) {
                        log.warn("[Seed] Error procesando cuota {}: {}", cuota.getId(), e.getMessage());
                    }
                }
            }

            long totalCuotas = cuotaRepo.count();
            long totalMovimientos = movimientoRepo.count();
            long totalGestiones = gestionRepo.count();

            resultado.put("status", "success");
            resultado.put("cuotasGeneradas", cuotasGeneradas);
            resultado.put("movimientosGenerados", movimientosGenerados);
            resultado.put("gestionesGeneradas", gestionesGeneradas);
            resultado.put("totalCuotas", totalCuotas);
            resultado.put("totalMovimientos", totalMovimientos);
            resultado.put("totalGestiones", totalGestiones);

            log.info("[Seed] COMPLETADO - Cuotas: {}, Movimientos: {}, Gestiones: {}", 
                cuotasGeneradas, movimientosGenerados, gestionesGeneradas);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("[Seed] Error generando datos", e);
            resultado.put("status", "error");
            resultado.put("message", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(resultado);
        }
    }
}
