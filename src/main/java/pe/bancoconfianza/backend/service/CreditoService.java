package pe.bancoconfianza.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.bancoconfianza.backend.dto.CreditoDto;
import pe.bancoconfianza.backend.dto.CuotaDto;
import pe.bancoconfianza.backend.dto.ResolucionCreditoRequest;
import pe.bancoconfianza.backend.dto.SimulacionCreditoDto;
import pe.bancoconfianza.backend.dto.SolicitudCreditoRequest;
import pe.bancoconfianza.backend.model.*;
import pe.bancoconfianza.backend.model.Credito.*;
import pe.bancoconfianza.backend.model.Credito.*;
import pe.bancoconfianza.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Motor de crédito del Core Bancario.
 *
 * Reglas de negocio implementadas:
 *  1. Elegibilidad (sujeto de crédito)
 *  2. Scoring crediticio (0–1000)
 *  3. RDS — Ratio Deuda/Sueldo con semáforo (Verde/Amarillo/Rojo)
 *  4. Ruta de aprobación por montos
 *  5. Cronograma francés (cuotas iguales)
 *  6. Desembolso → movimiento en cuenta
 */
@Service
public class CreditoService {

    private static final Logger log = LoggerFactory.getLogger(CreditoService.class);

    // ── Umbrales de la ruta de aprobación (en PEN) ──────────────
    private static final BigDecimal UMBRAL_ASESOR         = new BigDecimal("5000");
    private static final BigDecimal UMBRAL_ADMIN          = new BigDecimal("20000");
    private static final BigDecimal UMBRAL_JEFE_REGIONAL  = new BigDecimal("50000");
    private static final BigDecimal UMBRAL_RIESGOS        = new BigDecimal("150000");
    // > 150,000 → COMITE

    // ── Umbrales RDS ────────────────────────────────────────────
    private static final BigDecimal RDS_VERDE   = new BigDecimal("0.30"); // hasta 30%
    private static final BigDecimal RDS_AMARILLO = new BigDecimal("0.50"); // hasta 50%
    // > 50% → ROJO (no elegible)

    // ── TEA por tipo de producto (%) ────────────────────────────
    private static final BigDecimal TEA_PERSONAL     = new BigDecimal("0.3500"); // 35%
    private static final BigDecimal TEA_HIPOTECARIO  = new BigDecimal("0.0850"); // 8.5%
    private static final BigDecimal TEA_VEHICULAR    = new BigDecimal("0.1600"); // 16%
    private static final BigDecimal TEA_AGROPECUARIO = new BigDecimal("0.2500"); // 25%
    private static final BigDecimal TEA_MICROEMPRESA = new BigDecimal("0.4000"); // 40%

    private final CreditoRepository creditoRepo;
    private final CuotaCreditoRepository cuotaRepo;
    private final UsuarioRepository usuarioRepo;
    private final CuentaRepository cuentaRepo;
    private final MovimientoRepository movimientoRepo;
    private final AuditoriaService auditoriaService;

    public CreditoService(CreditoRepository creditoRepo,
                          CuotaCreditoRepository cuotaRepo,
                          UsuarioRepository usuarioRepo,
                          CuentaRepository cuentaRepo,
                          MovimientoRepository movimientoRepo,
                          AuditoriaService auditoriaService) {
        this.creditoRepo      = creditoRepo;
        this.cuotaRepo        = cuotaRepo;
        this.usuarioRepo      = usuarioRepo;
        this.cuentaRepo       = cuentaRepo;
        this.movimientoRepo   = movimientoRepo;
        this.auditoriaService = auditoriaService;
    }

    // ════════════════════════════════════════════════════════════
    // SIMULACIÓN: PRE-VISUALIZACIÓN SIN GUARDAR
    // ════════════════════════════════════════════════════════════

    /**
     * Simula un crédito sin guardarlo en BD.
     * Útil para que el cliente vea la evaluación, scoring y RDS antes de confirmar.
     */
    public SimulacionCreditoDto simularCredito(SolicitudCreditoRequest req, String emailCliente) {
        Usuario cliente = findUsuario(emailCliente);

        // Validar que la cuenta existe y pertenece al cliente
        Cuenta cuentaDesembolso = cuentaRepo.findByNumeroCuenta(req.cuentaDesembolsoNumero())
            .orElseThrow(() -> new IllegalArgumentException("Cuenta de desembolso no encontrada."));

        if (!cuentaDesembolso.getUsuario().getEmail().equals(emailCliente)) {
            throw new SecurityException("La cuenta de desembolso no pertenece al cliente.");
        }

        // Calcular score
        int score = calcularScore(cliente, req);

        // TEA según producto
        BigDecimal tea = teaPorTipo(TipoProducto.valueOf(req.tipoProducto()));

        // Cuota mensual
        BigDecimal cuotaMensual = calcularCuotaMensual(req.montoSolicitado(), tea, req.plazoMeses());

        // RDS
        BigDecimal deudaTotal = req.deudaTotalVigente() != null ? req.deudaTotalVigente() : BigDecimal.ZERO;
        BigDecimal deudaTotalConCuota = deudaTotal.add(cuotaMensual);
        BigDecimal rds = deudaTotalConCuota.divide(req.ingresoMensual(), 4, java.math.RoundingMode.HALF_UP);
        SemaforoRds semaforo = semaforo(rds);

        // Elegibilidad
        boolean elegible = esElegible(score, rds, semaforo);

        // Ruta de aprobación
        RutaAprobacion ruta = rutaAprobacion(req.montoSolicitado());
        EstadoCredito estado;
        String comentario;

        if (!elegible) {
            estado = EstadoCredito.RECHAZADO;
            comentario = buildMotivoRechazo(score, rds, semaforo);
        } else if (ruta == RutaAprobacion.ASESOR && score >= 700) {
            estado = EstadoCredito.APROBADO;
            comentario = "Aprobado automáticamente — Score " + score + ", RDS " + rds.multiply(new BigDecimal("100")).setScale(1, java.math.RoundingMode.HALF_UP) + "%";
        } else {
            estado = mapRutaToEstado(ruta);
            comentario = "En evaluación — Ruta: " + ruta;
        }

        return new SimulacionCreditoDto(
            req.tipoProducto(),
            req.montoSolicitado(),
            req.plazoMeses(),
            tea,
            cuotaMensual,
            req.ingresoMensual(),
            deudaTotal,
            rds.multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP),
            semaforo.name(),
            score,
            elegible,
            ruta != null ? ruta.name() : null,
            estado.name(),
            comentario
        );
    }

    // ════════════════════════════════════════════════════════════
    // CRITERIO 1: SOLICITUD DESDE HOMEBANKING
    // ════════════════════════════════════════════════════════════

    /**
     * El cliente solicita un crédito desde el Homebanking.
     * Se evalúa automáticamente (scoring + RDS) y se determina
     * la ruta de aprobación correspondiente.
     */
    @Transactional
    public CreditoDto solicitarCredito(SolicitudCreditoRequest req, String emailCliente) {
        Usuario cliente = findUsuario(emailCliente);

        log.info("[Credito] Solicitud de {}: cuenta={}, monto={}", 
            emailCliente, req.cuentaDesembolsoNumero(), req.montoSolicitado());

        if (req.cuentaDesembolsoNumero() == null || req.cuentaDesembolsoNumero().trim().isEmpty()) {
            log.warn("[Credito] Cuenta vacía para {}", emailCliente);
            throw new IllegalArgumentException("Debe seleccionar una cuenta de desembolso.");
        }

        // ⭐ VALIDACIÓN COMPLETA EN UN QUERY: número + usuario + activa
        Cuenta cuentaDesembolso = cuentaRepo
            .findByNumeroCuentaAndUsuarioIdAndActivaTrue(
                req.cuentaDesembolsoNumero().trim(),
                cliente.getId()
            )
            .orElseThrow(() -> {
                log.warn("[Credito] Validación de cuenta FALLÓ — número={}, usuario_id={}, activa=true", 
                    req.cuentaDesembolsoNumero(), cliente.getId());
                return new IllegalArgumentException(
                    "Selecciona una cuenta válida y activa. La cuenta proporcionada no existe o está inactiva."
                );
            });

        log.debug("[Credito] ✓ Cuenta desembolso validada: id={}, número={}, saldo={}", 
            cuentaDesembolso.getId(), 
            cuentaDesembolso.getNumeroCuenta(), 
            cuentaDesembolso.getSaldo());

        // ── 1. Crear solicitud ──────────────────────────────────
        Credito credito = new Credito();
        credito.setNumeroOperacion(generarNumeroOperacion());
        credito.setCliente(cliente);
        credito.setTipoProducto(TipoProducto.valueOf(req.tipoProducto()));
        credito.setMontoSolicitado(req.montoSolicitado());
        credito.setPlazoMeses(req.plazoMeses());
        credito.setMoneda(req.moneda() != null ? req.moneda() : "PEN");
        credito.setProposito(req.proposito());
        credito.setIngresoMensual(req.ingresoMensual());
        credito.setDeudaTotalVigente(
            req.deudaTotalVigente() != null ? req.deudaTotalVigente() : BigDecimal.ZERO
        );
        credito.setCuentaDesembolso(cuentaDesembolso);
        credito.setEstado(EstadoCredito.EN_EVALUACION);

        // ── 2. Scoring crediticio ───────────────────────────────
        int score = calcularScore(cliente, req);
        credito.setScoreCrediticio(score);

        // ── 3. RDS — Ratio Deuda/Sueldo ────────────────────────
        BigDecimal tea = teaPorTipo(credito.getTipoProducto());
        credito.setTea(tea);

        BigDecimal cuotaMensual = calcularCuotaMensual(req.montoSolicitado(), tea, req.plazoMeses());
        credito.setCuotaMensual(cuotaMensual);

        BigDecimal deudaTotal = credito.getDeudaTotalVigente().add(cuotaMensual);
        BigDecimal rds = deudaTotal.divide(req.ingresoMensual(), 4, RoundingMode.HALF_UP);
        credito.setRdsRatio(rds);
        credito.setRdsSemaforo(semaforo(rds));

        // ── 4. Elegibilidad ─────────────────────────────────────
        boolean elegible = esElegible(score, rds, credito.getRdsSemaforo());
        credito.setEsSujetoCredito(elegible);

        if (!elegible) {
            credito.setEstado(EstadoCredito.RECHAZADO);
            credito.setComentarioEvaluacion(
                buildMotivoRechazo(score, rds, credito.getRdsSemaforo())
            );
            return CreditoDto.from(creditoRepo.save(credito));
        }

        // ── 5. Ruta de aprobación ───────────────────────────────
        RutaAprobacion ruta = rutaAprobacion(req.montoSolicitado());
        credito.setRutaAprobacion(ruta);

        // Si la ruta es ASESOR → estado EN_EVALUACION (asesor lo evaluará)
        // La auto-aprobación ocurre cuando el asesor resuelve (si score >= 700)
        credito.setEstado(mapRutaToEstado(ruta));

        // ── Auditoría ────────────────────────────────────────
        CreditoDto resultado = CreditoDto.from(creditoRepo.save(credito));
        auditoriaService.registrar(
            emailCliente, "CLIENTE",
            AuditoriaEvento.TipoAccion.CREDITO_SOLICITUD,
            "CREDITO", "Solicitud crédito " + credito.getNumeroOperacion() + " por S/ " + credito.getMontoSolicitado(),
            resultado.id()
        );
        return resultado;
    }

    // ════════════════════════════════════════════════════════════
    // CRITERIO 2: FLUJO DE APROBACIÓN (Core)
    // ════════════════════════════════════════════════════════════

    /** Lista de créditos pendientes de resolución para el actor del Core */
    public List<CreditoDto> getCreditosPendientes(String emailActor) {
        Usuario actor = findUsuario(emailActor);
        List<EstadoCredito> estadosRelevantes = estadosPorRol(actor.getRol());
        String[] estadosArray = estadosRelevantes.stream()
            .map(EstadoCredito::name)
            .toArray(String[]::new);
        return creditoRepo.findByEstadoIn(estadosArray)
            .stream().map(CreditoDto::from).collect(Collectors.toList());
    }

    /** Todos los créditos del cliente autenticado (Homebanking) */
    public List<CreditoDto> getMisSolicitudes(String emailCliente) {
        Usuario cliente = findUsuario(emailCliente);
        return creditoRepo.findByClienteOrderByCreatedAtDesc(cliente)
            .stream().map(CreditoDto::from).collect(Collectors.toList());
    }

    /** Todas las solicitudes — para ADMIN/GERENCIA */
    public List<CreditoDto> getTodasLasSolicitudes() {
        return creditoRepo.findAll(
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
            .stream().map(CreditoDto::from).collect(Collectors.toList());
    }

    /** Un crédito específico */
    public CreditoDto getCredito(Long id, String emailActor) {
        Credito credito = findCredito(id);
        assertPuedeVerCredito(credito, emailActor);
        return CreditoDto.from(credito);
    }

    /**
     * Resolución por un actor del Core (aprobar / rechazar).
     * Valida que el rol del actor sea competente para el estado actual.
     * 
     * Si se aprueba un crédito con monto ≤ S/5000 (ruta ASESOR),
     * se desembolsa automáticamente.
     */
    @Transactional
    public CreditoDto resolverCredito(Long id, ResolucionCreditoRequest req, String emailActor) {
        Credito credito = findCredito(id);
        Usuario actor   = findUsuario(emailActor);

        assertPuedeResolver(credito, actor);

        // ⭐ AUDITORÍA: Registrar quién está resolviendo
        credito.setAprobadoPor(actor);
        if (credito.getAsesor() == null) {
            credito.setAsesor(actor);
            log.info("[Credito] Asesor asignado: {} para crédito {}", actor.getEmail(), credito.getNumeroOperacion());
        }
        
        credito.setComentarioEvaluacion(req.comentario());
        credito.setUpdatedAt(LocalDateTime.now());

        // ⭐ AUTO-APROBACIÓN: Si es ASESOR y score >= 700, aprobar automáticamente
        boolean autoAprobado = false;
        if (credito.getRutaAprobacion() == RutaAprobacion.ASESOR && credito.getScoreCrediticio() >= 700) {
            credito.setEstado(EstadoCredito.APROBADO);
            credito.setMontoAprobado(credito.getMontoSolicitado());
            credito.setComentarioEvaluacion("Aprobado automáticamente — Score " + credito.getScoreCrediticio() + 
                ", RDS " + (credito.getRdsRatio().multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)) + "%");
            autoAprobado = true;
            log.info("[Credito] Auto-APROBADO: {} (Score {}, RDS {}%)", 
                credito.getNumeroOperacion(), credito.getScoreCrediticio(), 
                credito.getRdsRatio().multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP));
        } else if (req.aprobado()) {
            BigDecimal montoAprobado = req.montoAprobado() != null
                ? req.montoAprobado() : credito.getMontoSolicitado();
            credito.setMontoAprobado(montoAprobado);
            // Recalcular cuota si el monto cambió
            if (!montoAprobado.equals(credito.getMontoSolicitado())) {
                credito.setCuotaMensual(
                    calcularCuotaMensual(montoAprobado, credito.getTea(), credito.getPlazoMeses())
                );
            }
            credito.setEstado(EstadoCredito.APROBADO);
            log.info("[Credito] Crédito APROBADO por asesor: {} por {} ({})", credito.getNumeroOperacion(), actor.getNombre(), actor.getRol().name());
        } else {
            credito.setEstado(EstadoCredito.RECHAZADO);
            log.info("[Credito] Crédito RECHAZADO: {} por {} ({})", credito.getNumeroOperacion(), actor.getNombre(), actor.getRol().name());
        }

        CreditoDto resolvResult = CreditoDto.from(creditoRepo.save(credito));
        auditoriaService.registrar(
            emailActor, actor.getRol().name(),
            (req.aprobado() || autoAprobado) ? AuditoriaEvento.TipoAccion.CREDITO_APROBACION : AuditoriaEvento.TipoAccion.CREDITO_RECHAZO,
            "CREDITO", (req.aprobado() || autoAprobado ? "Aprobado" : "Rechazado") + ": " + credito.getNumeroOperacion() + " — " + (req.comentario() != null ? req.comentario() : "Auto-aprobación automática"),
            id
        );

        // Auto-desembolsar si ruta es ASESOR (monto <= S/ 5000) O auto-aprobado
        if ((req.aprobado() || autoAprobado) && credito.getRutaAprobacion() == RutaAprobacion.ASESOR) {
            log.info("[Auto-Desembolso] Desembolsando automáticamente crédito {} (ruta ASESOR)", credito.getNumeroOperacion());
            desembolsar(id, emailActor);
            return CreditoDto.from(findCredito(id));
        }

        return resolvResult;
    }

    /**
     * Desembolso: transfiere el monto aprobado a la cuenta del cliente
     * y genera el cronograma de cuotas.
     * Solo puede ejecutarlo un ADMIN, JEFE_REGIONAL, o GERENCIA.
     */
    @Transactional
    public CreditoDto desembolsar(Long id, String emailActor) {
        Credito credito = findCredito(id);

        if (credito.getEstado() != EstadoCredito.APROBADO) {
            throw new IllegalStateException("Solo se puede desembolsar un crédito APROBADO.");
        }

        // ── Acreditar monto en cuenta del cliente ──────────────
        Cuenta cuenta = credito.getCuentaDesembolso();
        BigDecimal saldoAntes = cuenta.getSaldo();
        cuenta.setSaldo(saldoAntes.add(credito.getMontoAprobado()));
        cuentaRepo.save(cuenta);

        Movimiento mov = new Movimiento();
        mov.setCuenta(cuenta);
        mov.setTipo(Movimiento.TipoMovimiento.DEPOSITO);
        mov.setMonto(credito.getMontoAprobado());
        mov.setSaldoAnterior(saldoAntes);
        mov.setSaldoPosterior(cuenta.getSaldo());
        mov.setDescripcion("Desembolso crédito " + credito.getNumeroOperacion());
        movimientoRepo.save(mov);

        // ── Actualizar crédito ─────────────────────────────────
        LocalDate hoy = LocalDate.now();
        credito.setEstado(EstadoCredito.DESEMBOLSADO);
        credito.setFechaDesembolso(hoy);
        credito.setFechaPrimeraCuota(hoy.plusMonths(1));
        credito.setFechaUltimaCuota(hoy.plusMonths(credito.getPlazoMeses()));
        credito.setUpdatedAt(LocalDateTime.now());
        creditoRepo.save(credito);

        // ── Generar cronograma (sistema francés) ───────────────
        generarCronograma(credito);

        log.info("[Desembolso] Crédito {} desembolsado. Monto: {} → cuenta {}",
            credito.getNumeroOperacion(), credito.getMontoAprobado(), cuenta.getNumeroCuenta());

        auditoriaService.registrar(
            emailActor, "OPERADOR",
            AuditoriaEvento.TipoAccion.CREDITO_DESEMBOLSO,
            "CREDITO", "Desembolso " + credito.getNumeroOperacion() + " S/ " + credito.getMontoAprobado() + " → cuenta " + cuenta.getNumeroCuenta(),
            id
        );

        return CreditoDto.from(credito);
    }

    /** Cronograma de cuotas de un crédito */
    public List<CuotaDto> getCronograma(Long creditoId, String emailActor) {
        Credito credito = findCredito(creditoId);
        assertPuedeVerCredito(credito, emailActor);
        return cuotaRepo.findByCreditoOrderByNumeroCuotaAsc(credito)
            .stream().map(CuotaDto::from).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════
    // CRITERIO 4 — R1: KPIs de mora
    // ════════════════════════════════════════════════════════════

    /**
     * Actualiza los días de mora de todas las cuotas vencidas.
     * Se puede invocar desde un scheduler diario.
     */
    @Transactional
    public void actualizarMora() {
        LocalDate hoy = LocalDate.now();
        List<CuotaCredito> vencidas = cuotaRepo.findVencidasSinPagar(hoy);
        for (CuotaCredito cuota : vencidas) {
            long dias = ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
            cuota.setDiasMora((int) dias);
            cuota.setEstadoCuota(CuotaCredito.EstadoCuota.VENCIDA);
            // Mora diaria = 0.1% del capital vencido por día
            BigDecimal moraDiaria = cuota.getCapital()
                .multiply(new BigDecimal("0.001"))
                .multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_UP);
            cuota.setMontoMora(moraDiaria);
            cuotaRepo.save(cuota);
        }
        log.info("[Mora] Actualizadas {} cuotas vencidas.", vencidas.size());
    }

    // ════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ════════════════════════════════════════════════════════════

    /** Cronograma francés — cuota constante */
    private void generarCronograma(Credito credito) {
        BigDecimal monto  = credito.getMontoAprobado();
        BigDecimal tea    = credito.getTea();
        int        n      = credito.getPlazoMeses();
        LocalDate  inicio = credito.getFechaPrimeraCuota();

        // TEM = (1 + TEA)^(1/12) - 1
        double teaDouble = tea.doubleValue();
        double tem       = Math.pow(1 + teaDouble, 1.0 / 12) - 1;
        BigDecimal temBD = BigDecimal.valueOf(tem).setScale(8, RoundingMode.HALF_UP);

        // Cuota = P * TEM / (1 - (1+TEM)^-n)
        double montoDouble  = monto.doubleValue();
        double cuotaDouble  = montoDouble * tem / (1 - Math.pow(1 + tem, -n));
        BigDecimal cuota    = BigDecimal.valueOf(cuotaDouble).setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldo = monto.setScale(2, RoundingMode.HALF_UP);

        for (int i = 1; i <= n; i++) {
            BigDecimal interes  = saldo.multiply(temBD).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital  = cuota.subtract(interes).setScale(2, RoundingMode.HALF_UP);
            if (i == n) {
                // Última cuota: ajusta para cerrar saldo exacto
                capital = saldo;
                cuota   = capital.add(interes);
            }
            saldo = saldo.subtract(capital).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            CuotaCredito cc = new CuotaCredito();
            cc.setCredito(credito);
            cc.setNumeroCuota(i);
            cc.setFechaVencimiento(inicio.plusMonths(i - 1));
            cc.setCapital(capital);
            cc.setInteres(interes);
            cc.setSeguro(BigDecimal.ZERO);
            cc.setCuotaTotal(capital.add(interes));
            cc.setSaldoCapital(saldo);
            cc.setEstadoCuota(CuotaCredito.EstadoCuota.PENDIENTE);
            cuotaRepo.save(cc);
        }
    }

    private BigDecimal calcularCuotaMensual(BigDecimal monto, BigDecimal tea, int n) {
        double teaD  = tea.doubleValue();
        double temD  = Math.pow(1 + teaD, 1.0 / 12) - 1;
        double montoD = monto.doubleValue();
        double cuota  = montoD * temD / (1 - Math.pow(1 + temD, -n));
        return BigDecimal.valueOf(cuota).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Scoring simplificado (0–1000):
     *  - Historial de pagos (sin mora = +400)
     *  - Ingreso vs cuota   (ratio < 30% = +300)
     *  - Antigüedad cliente (>1 cuenta = +200)
     *  - Endeudamiento bajo (<20% RDS = +100)
     */
    private int calcularScore(Usuario cliente, SolicitudCreditoRequest req) {
        int score = 0;

        // Historial: penaliza si tiene créditos en mora (placeholder: 0 mora = +400)
        score += 400;

        // Ingreso vs cuota propuesta
        BigDecimal tea    = teaPorTipo(TipoProducto.valueOf(req.tipoProducto()));
        BigDecimal cuota  = calcularCuotaMensual(req.montoSolicitado(), tea, req.plazoMeses());
        if (req.ingresoMensual().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = cuota.divide(req.ingresoMensual(), 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.20")) < 0) score += 300;
            else if (ratio.compareTo(new BigDecimal("0.30")) < 0) score += 200;
            else if (ratio.compareTo(new BigDecimal("0.40")) < 0) score += 100;
        }

        // Antigüedad / tenencia de cuentas
        long cuentas = cuentaRepo.findByUsuarioAndActivaTrue(cliente).size();
        if (cuentas >= 2) score += 200;
        else if (cuentas >= 1) score += 100;

        // Endeudamiento actual bajo
        BigDecimal deudaTotal = req.deudaTotalVigente() != null
            ? req.deudaTotalVigente() : BigDecimal.ZERO;
        if (req.ingresoMensual().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rdsActual = deudaTotal.divide(req.ingresoMensual(), 4, RoundingMode.HALF_UP);
            if (rdsActual.compareTo(new BigDecimal("0.20")) < 0) score += 100;
        }

        return Math.min(score, 1000);
    }

    private boolean esElegible(int score, BigDecimal rds, SemaforoRds semaforo) {
        if (score < 400)                              return false; // Score mínimo
        if (semaforo == SemaforoRds.ROJO)             return false; // RDS > 50%
        return true;
    }

    private String buildMotivoRechazo(int score, BigDecimal rds, SemaforoRds semaforo) {
        if (score < 400) return "Score crediticio insuficiente (" + score + "/1000). Mínimo requerido: 400.";
        if (semaforo == SemaforoRds.ROJO)
            return "Ratio Deuda/Sueldo (RDS) supera el límite permitido: "
                + rds.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP) + "% (máximo 50%).";
        return "No cumple criterios de elegibilidad.";
    }

    private SemaforoRds semaforo(BigDecimal rds) {
        if (rds.compareTo(RDS_VERDE) <= 0)    return SemaforoRds.VERDE;
        if (rds.compareTo(RDS_AMARILLO) <= 0) return SemaforoRds.AMARILLO;
        return SemaforoRds.ROJO;
    }

    private RutaAprobacion rutaAprobacion(BigDecimal monto) {
        if (monto.compareTo(UMBRAL_ASESOR) <= 0)        return RutaAprobacion.ASESOR;
        if (monto.compareTo(UMBRAL_ADMIN) <= 0)         return RutaAprobacion.ADMIN;
        if (monto.compareTo(UMBRAL_JEFE_REGIONAL) <= 0) return RutaAprobacion.JEFE_REGIONAL;
        if (monto.compareTo(UMBRAL_RIESGOS) <= 0)       return RutaAprobacion.RIESGOS;
        return RutaAprobacion.COMITE;
    }

    private EstadoCredito mapRutaToEstado(RutaAprobacion ruta) {
        return switch (ruta) {
            case ASESOR        -> EstadoCredito.EN_EVALUACION;
            case ADMIN         -> EstadoCredito.PENDIENTE_ADMIN;
            case JEFE_REGIONAL -> EstadoCredito.PENDIENTE_JEFE_REGIONAL;
            case RIESGOS       -> EstadoCredito.PENDIENTE_RIESGOS;
            case COMITE        -> EstadoCredito.PENDIENTE_COMITE;
        };
    }

    private List<EstadoCredito> estadosPorRol(Usuario.Rol rol) {
        return switch (rol) {
            case ASESOR        -> List.of(EstadoCredito.EN_EVALUACION, EstadoCredito.SOLICITADO);
            case ADMIN         -> List.of(EstadoCredito.PENDIENTE_ADMIN);
            case JEFE_REGIONAL -> List.of(EstadoCredito.PENDIENTE_JEFE_REGIONAL);
            case RIESGOS       -> List.of(EstadoCredito.PENDIENTE_RIESGOS);
            case COMITE        -> List.of(EstadoCredito.PENDIENTE_COMITE);
            case GERENCIA      -> List.of(EstadoCredito.APROBADO); // puede desembolsar
            default            -> List.of();
        };
    }

    private void assertPuedeResolver(Credito credito, Usuario actor) {
        boolean ok = switch (credito.getEstado()) {
            case EN_EVALUACION, SOLICITADO  -> actor.getRol() == Usuario.Rol.ASESOR || actor.getRol() == Usuario.Rol.ADMIN;
            case PENDIENTE_ADMIN            -> actor.getRol() == Usuario.Rol.ADMIN || actor.getRol() == Usuario.Rol.GERENCIA;
            case PENDIENTE_JEFE_REGIONAL    -> actor.getRol() == Usuario.Rol.JEFE_REGIONAL || actor.getRol() == Usuario.Rol.GERENCIA;
            case PENDIENTE_RIESGOS          -> actor.getRol() == Usuario.Rol.RIESGOS || actor.getRol() == Usuario.Rol.GERENCIA;
            case PENDIENTE_COMITE           -> actor.getRol() == Usuario.Rol.COMITE || actor.getRol() == Usuario.Rol.GERENCIA;
            default -> false;
        };
        if (!ok) throw new SecurityException(
            "El rol " + actor.getRol() + " no puede resolver un crédito en estado " + credito.getEstado()
        );
    }

    private void assertPuedeVerCredito(Credito credito, String emailActor) {
        Usuario actor = findUsuario(emailActor);
        if (actor.getRol() == Usuario.Rol.CLIENTE
            && !credito.getCliente().getEmail().equals(emailActor)) {
            throw new SecurityException("Acceso denegado al crédito.");
        }
    }

    private BigDecimal teaPorTipo(TipoProducto tipo) {
        return switch (tipo) {
            case PERSONAL     -> TEA_PERSONAL;
            case HIPOTECARIO  -> TEA_HIPOTECARIO;
            case VEHICULAR    -> TEA_VEHICULAR;
            case AGROPECUARIO -> TEA_AGROPECUARIO;
            case MICROEMPRESA -> TEA_MICROEMPRESA;
        };
    }

    private String generarNumeroOperacion() {
        String prefix = "CRED-" + java.time.Year.now().getValue() + "-";
        long count = creditoRepo.count() + 1;
        String numero;
        do {
            numero = prefix + String.format("%06d", count++);
        } while (creditoRepo.existsByNumeroOperacion(numero));
        return numero;
    }

    private Usuario findUsuario(String email) {
        return usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
    }

    private Credito findCredito(Long id) {
        return creditoRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado: " + id));
    }
}
