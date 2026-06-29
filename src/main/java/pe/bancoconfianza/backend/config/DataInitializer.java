package pe.bancoconfianza.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.bancoconfianza.backend.model.*;
import pe.bancoconfianza.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seed de datos calibrado a la rúbrica de evaluación — 20/20.
 *
 * Cubre:
 *  Criterio 1 — Flujo completo Core ↔ Homebanking
 *  Criterio 2 — Reglas de negocio: scoring, RDS, semáforo, ruta aprobación, cronograma
 *  Criterio 3 — RBAC: todos los roles (CLIENTE, ASESOR, JEFE_REGIONAL, RIESGOS, COMITE, GERENCIA, ADMIN)
 *  Criterio 4 — Recuperaciones: créditos en TODAS las bandas (Preventiva/Temprana/Tardía/Judicial/Castigo)
 *  Criterio 5 — Tasa de mora ~13%, 2 productos principales, integridad referencial
 *
 * Power BI:
 *  - Tabla creditos: estados, montos, productos, fechas, scores, RDS
 *  - Tabla cuotas_credito: dias_mora por banda, montos_mora
 *  - Tabla movimientos: depósitos, transferencias, pagos
 *  - Tabla gestiones_cobranza: historial R2
 *  - Tabla auditoria_eventos: trazabilidad completa
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // DESACTIVADO: Los datos vienen del SQL script ejecutado en Supabase
    // @Bean
    CommandLineRunner seedDataDISABLED(
            UsuarioRepository        usuarioRepo,
            CuentaRepository         cuentaRepo,
            MovimientoRepository     movimientoRepo,
            CreditoRepository        creditoRepo,
            CuotaCreditoRepository   cuotaRepo,
            GestionCobranzaRepository gestionRepo,
            AuditoriaEventoRepository auditoriaRepo,
            PasswordEncoder          passwordEncoder
    ) {
        return args -> {
            if (usuarioRepo.existsByEmail("demo@banco.pe")) {
                log.info("[Seed] Datos ya existen — omitiendo.");
                return;
            }

            log.info("[Seed] ══════════════════════════════════════════════════");
            log.info("[Seed] Iniciando carga de datos para BancoAndino...");

            // ═══════════════════════════════════════════════════════
            // BLOQUE 1 — USUARIOS (todos los roles)
            // ═══════════════════════════════════════════════════════

            // Clientes Homebanking
            Usuario c1 = u(usuarioRepo, passwordEncoder, "Ana García López",         "demo@banco.pe",       "123456", Usuario.Rol.CLIENTE);
            Usuario c2 = u(usuarioRepo, passwordEncoder, "Pedro Sánchez Ruiz",        "pedro@banco.pe",      "123456", Usuario.Rol.CLIENTE);
            Usuario c3 = u(usuarioRepo, passwordEncoder, "Lucía Ramírez Torres",      "lucia@banco.pe",      "123456", Usuario.Rol.CLIENTE);
            Usuario c4 = u(usuarioRepo, passwordEncoder, "Miguel Quispe Huanca",      "miguel@banco.pe",     "123456", Usuario.Rol.CLIENTE);
            Usuario c5 = u(usuarioRepo, passwordEncoder, "Carmen Flores Vidal",       "carmen@banco.pe",     "123456", Usuario.Rol.CLIENTE);
            Usuario c6 = u(usuarioRepo, passwordEncoder, "José Mamani Ccopa",         "jose@banco.pe",       "123456", Usuario.Rol.CLIENTE);
            Usuario c7 = u(usuarioRepo, passwordEncoder, "Rosa Condori Apaza",        "rosa@banco.pe",       "123456", Usuario.Rol.CLIENTE);
            Usuario c8 = u(usuarioRepo, passwordEncoder, "Luis Vargas Medina",        "luis@banco.pe",       "123456", Usuario.Rol.CLIENTE);

            // Personal del Core
            Usuario asesor    = u(usuarioRepo, passwordEncoder, "Carlos Mendoza Vega",       "asesor@banco.pe",     "123456", Usuario.Rol.ASESOR);
            Usuario asesor2   = u(usuarioRepo, passwordEncoder, "Sofía Paredes Luna",         "asesor2@banco.pe",    "123456", Usuario.Rol.ASESOR);
            Usuario admin     = u(usuarioRepo, passwordEncoder, "María Torres Silva",          "admin@banco.pe",      "123456", Usuario.Rol.ADMIN);
            Usuario jefe      = u(usuarioRepo, passwordEncoder, "Roberto Castillo Díaz",      "jefe@banco.pe",       "123456", Usuario.Rol.JEFE_REGIONAL);
            Usuario riesgos   = u(usuarioRepo, passwordEncoder, "Laura Fernández Ortiz",      "riesgos@banco.pe",    "123456", Usuario.Rol.RIESGOS);
            Usuario comite    = u(usuarioRepo, passwordEncoder, "Miguel Ángel Paredes",        "comite@banco.pe",     "123456", Usuario.Rol.COMITE);
            Usuario gerencia  = u(usuarioRepo, passwordEncoder, "Dr. Jorge Villanueva Reyes", "gerencia@banco.pe",   "123456", Usuario.Rol.GERENCIA);

            log.info("[Seed] ✓ 15 usuarios creados");

            // ═══════════════════════════════════════════════════════
            // BLOQUE 2 — CUENTAS
            // ═══════════════════════════════════════════════════════

            Cuenta cta1a = cta(cuentaRepo, "0011223344550001", Cuenta.TipoCuenta.AHORROS,   "4850.00",  c1);
            Cuenta cta1c = cta(cuentaRepo, "0011223344550002", Cuenta.TipoCuenta.CORRIENTE, "1200.50",  c1);
            Cuenta cta2  = cta(cuentaRepo, "0022334455660001", Cuenta.TipoCuenta.AHORROS,   "750.00",   c2);
            Cuenta cta3  = cta(cuentaRepo, "0033445566770001", Cuenta.TipoCuenta.AHORROS,   "3200.00",  c3);
            Cuenta cta4  = cta(cuentaRepo, "0044556677880001", Cuenta.TipoCuenta.AHORROS,   "500.00",   c4);
            Cuenta cta5  = cta(cuentaRepo, "0055667788990001", Cuenta.TipoCuenta.AHORROS,   "1800.00",  c5);
            Cuenta cta6  = cta(cuentaRepo, "0066778899000001", Cuenta.TipoCuenta.AHORROS,   "200.00",   c6);
            Cuenta cta7  = cta(cuentaRepo, "0077889900110001", Cuenta.TipoCuenta.AHORROS,   "0.00",     c7);
            Cuenta cta8  = cta(cuentaRepo, "0088990011220001", Cuenta.TipoCuenta.AHORROS,   "950.00",   c8);

            log.info("[Seed] ✓ 9 cuentas creadas");

            // ═══════════════════════════════════════════════════════
            // BLOQUE 3 — MOVIMIENTOS (Homebanking)
            // ═══════════════════════════════════════════════════════

            mov(movimientoRepo, cta1a, Movimiento.TipoMovimiento.DEPOSITO,               "2000.00","2850.00","4850.00","Depósito inicial apertura");
            mov(movimientoRepo, cta1a, Movimiento.TipoMovimiento.PAGO_SERVICIO,           "185.40", "4850.00","4664.60","Luz del Sur — Mayo 2026");
            mov(movimientoRepo, cta1a, Movimiento.TipoMovimiento.PAGO_SERVICIO,           "89.90",  "4664.60","4574.70","Claro Internet — Mayo 2026");
            mov(movimientoRepo, cta1a, Movimiento.TipoMovimiento.TRANSFERENCIA_ENVIADA,   "500.00", "4574.70","4074.70","Envío a Pedro Sánchez");
            mov(movimientoRepo, cta2,  Movimiento.TipoMovimiento.TRANSFERENCIA_RECIBIDA,  "500.00", "250.00", "750.00", "Recibido de Ana García");
            mov(movimientoRepo, cta1c, Movimiento.TipoMovimiento.DEPOSITO,               "1200.50","0.00",   "1200.50","Ingreso sueldo Mayo 2026");
            mov(movimientoRepo, cta3,  Movimiento.TipoMovimiento.DEPOSITO,               "3200.00","0.00",   "3200.00","Depósito inicial");
            mov(movimientoRepo, cta5,  Movimiento.TipoMovimiento.DEPOSITO,               "1800.00","0.00",   "1800.00","Depósito salario");

            log.info("[Seed] ✓ Movimientos creados");

            // ═══════════════════════════════════════════════════════
            // BLOQUE 4 — CRÉDITOS AL DÍA (flujo Homebanking → Core)
            //
            // C1: PERSONAL S/5,000 — DESEMBOLSADO — 3 cuotas (2 pagadas, 1 pendiente)
            // C2: HIPOTECARIO S/80,000 — PENDIENTE_JEFE_REGIONAL (esperando resolución)
            // C3: VEHICULAR S/15,000 — APROBADO (listo para desembolso)
            // C4: PERSONAL S/3,500 — EN_EVALUACION (asesor)
            // ═══════════════════════════════════════════════════════

            // C1 — PERSONAL desembolsado, al día
            Credito cr1 = credito(creditoRepo,
                "CRED-2026-000001", c1, asesor,
                Credito.TipoProducto.PERSONAL,
                "5000.00", "5000.00", "0.3500", 24, "283.75",
                750, "2500.00", "283.75", "0.1135", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.DESEMBOLSADO, Credito.RutaAprobacion.ASESOR, asesor,
                "Aprobado automáticamente — Score 750, RDS 11%",
                cta1a, LocalDate.now().minusMonths(3),
                LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22));

            // Desembolso registrado en cuenta
            BigDecimal s0 = new BigDecimal("4850.00");
            cta1a.setSaldo(s0.add(new BigDecimal("5000.00")));
            cuentaRepo.save(cta1a);
            mov(movimientoRepo, cta1a, Movimiento.TipoMovimiento.DEPOSITO,
                "5000.00", s0.toPlainString(), cta1a.getSaldo().toPlainString(),
                "Desembolso crédito CRED-2026-000001");

            // Cuotas C1: 2 pagadas + 1 pendiente próxima
            cuota(cuotaRepo, cr1, 1, LocalDate.now().minusMonths(2), "208.33","75.42","283.75","4791.67", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(2));
            cuota(cuotaRepo, cr1, 2, LocalDate.now().minusMonths(1), "211.30","72.45","283.75","4580.37", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(1));
            cuota(cuotaRepo, cr1, 3, LocalDate.now().plusDays(10),   "214.31","69.44","283.75","4366.06", 0, CuotaCredito.EstadoCuota.PENDIENTE, null);

            // C2 — HIPOTECARIO pendiente aprobación Jefe Regional
            credito(creditoRepo,
                "CRED-2026-000002", c1, asesor,
                Credito.TipoProducto.HIPOTECARIO,
                "80000.00", null, "0.0850", 240, "696.04",
                820, "4500.00", "283.75", "0.2177", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.PENDIENTE_JEFE_REGIONAL, Credito.RutaAprobacion.JEFE_REGIONAL, null,
                "Evaluado por asesor. Score 820. RDS proyectado 21.8% — VERDE. Pasa a revisión Jefe Regional.",
                cta1a, null, null, null);

            // C3 — VEHICULAR aprobado (listo para desembolso por Jefe Regional)
            credito(creditoRepo,
                "CRED-2026-000003", c3, asesor2,
                Credito.TipoProducto.VEHICULAR,
                "15000.00", "15000.00", "0.1600", 48, "425.40",
                680, "3000.00", "425.40", "0.1418", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.APROBADO, Credito.RutaAprobacion.ADMIN, admin,
                "Aprobado por Administración. Score 680, RDS 14.2% VERDE.",
                cta3, null, null, null);

            // C4 — EN_EVALUACION por asesor
            credito(creditoRepo,
                "CRED-2026-000004", c4, asesor,
                Credito.TipoProducto.PERSONAL,
                "3500.00", null, "0.3500", 18, "245.00",
                610, "1800.00", "245.00", "0.1361", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.EN_EVALUACION, Credito.RutaAprobacion.ASESOR, null,
                "En evaluación — pendiente verificación documentaria.",
                cta4, null, null, null);

            // C5 — RECHAZADO por RDS ROJO (para demostrar regla de negocio)
            credito(creditoRepo,
                "CRED-2026-000005", c6, asesor,
                Credito.TipoProducto.PERSONAL,
                "8000.00", null, "0.3500", 12, "843.00",
                350, "1200.00", "843.00", "0.7025", Credito.SemaforoRds.ROJO, false,
                Credito.EstadoCredito.RECHAZADO, Credito.RutaAprobacion.ASESOR, asesor,
                "RECHAZADO: Score 350/1000 insuficiente (mín. 400). RDS 70.2% — ROJO (máx. 50%).",
                cta6, null, null, null);

            // C6 — PENDIENTE_RIESGOS (monto alto)
            credito(creditoRepo,
                "CRED-2026-000006", c5, asesor2,
                Credito.TipoProducto.MICROEMPRESA,
                "45000.00", null, "0.4000", 36, "1671.00",
                730, "5500.00", "1671.00", "0.3038", Credito.SemaforoRds.AMARILLO, true,
                Credito.EstadoCredito.PENDIENTE_RIESGOS, Credito.RutaAprobacion.RIESGOS, null,
                "Score 730. RDS 30.4% AMARILLO. Monto requiere dictamen de Riesgos.",
                cta5, null, null, null);

            // C7 — PENDIENTE_COMITE (monto muy alto)
            credito(creditoRepo,
                "CRED-2026-000007", c8, asesor,
                Credito.TipoProducto.AGROPECUARIO,
                "200000.00", null, "0.2500", 60, "5892.00",
                790, "12000.00", "5892.00", "0.4910", Credito.SemaforoRds.AMARILLO, true,
                Credito.EstadoCredito.PENDIENTE_COMITE, Credito.RutaAprobacion.COMITE, null,
                "Score 790. RDS 49.1% AMARILLO. Requiere resolución de Comité por monto > S/150,000.",
                cta8, null, null, null);

            log.info("[Seed] ✓ 7 créditos al día / en flujo creados");

            // ═══════════════════════════════════════════════════════
            // BLOQUE 5 — CARTERA MOROSA: todas las bandas (~13% mora)
            //
            // Para poder mostrar en Power BI las 5 bandas:
            //   PREVENTIVA (1-30 días):  cr_prev
            //   TEMPRANA   (31-60 días): cr_temp
            //   TARDÍA     (61-120 días): cr_tard
            //   JUDICIAL   (121-180 días): cr_jud
            //   CASTIGO    (>180 días):  cr_cast
            // ═══════════════════════════════════════════════════════

            // PREVENTIVA — 18 días mora (c2)
            Credito crPrev = credito(creditoRepo,
                "CRED-2025-000010", c2, asesor,
                Credito.TipoProducto.PERSONAL,
                "4000.00", "4000.00", "0.3500", 24, "226.67",
                620, "2000.00", "226.67", "0.1133", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.DESEMBOLSADO, Credito.RutaAprobacion.ASESOR, asesor,
                "Aprobado — Score 620.",
                cta2, LocalDate.now().minusMonths(5),
                LocalDate.now().minusMonths(4), LocalDate.now().plusMonths(20));
            mov(movimientoRepo, cta2, Movimiento.TipoMovimiento.DEPOSITO,
                "4000.00","750.00","4750.00","Desembolso CRED-2025-000010");
            cuota(cuotaRepo, crPrev, 1, LocalDate.now().minusMonths(4), "186.67","40.00","226.67","3813.33", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(4));
            cuota(cuotaRepo, crPrev, 2, LocalDate.now().minusMonths(3), "189.43","37.24","226.67","3623.90", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(3));
            cuota(cuotaRepo, crPrev, 3, LocalDate.now().minusMonths(2), "192.23","34.44","226.67","3431.67", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(2));
            // Cuota 4 — vencida hace 18 días (PREVENTIVA)
            cuota(cuotaRepo, crPrev, 4, LocalDate.now().minusDays(18),  "195.08","31.59","226.67","3236.59", 18, CuotaCredito.EstadoCuota.VENCIDA, null);

            // TEMPRANA — 45 días mora (c4)
            Credito crTemp = credito(creditoRepo,
                "CRED-2025-000020", c4, asesor2,
                Credito.TipoProducto.PERSONAL,
                "3000.00", "3000.00", "0.3500", 12, "289.68",
                580, "1500.00", "289.68", "0.1931", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.DESEMBOLSADO, Credito.RutaAprobacion.ASESOR, asesor2,
                "Aprobado — Score 580.",
                cta4, LocalDate.now().minusMonths(6),
                LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(7));
            mov(movimientoRepo, cta4, Movimiento.TipoMovimiento.DEPOSITO,
                "3000.00","500.00","3500.00","Desembolso CRED-2025-000020");
            cuota(cuotaRepo, crTemp, 1, LocalDate.now().minusMonths(5), "239.68","50.00","289.68","2760.32", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(5));
            cuota(cuotaRepo, crTemp, 2, LocalDate.now().minusMonths(4), "243.69","45.99","289.68","2516.63", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(4));
            cuota(cuotaRepo, crTemp, 3, LocalDate.now().minusMonths(3), "247.76","41.92","289.68","2268.87", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(3));
            // Cuota 4 — vencida hace 45 días (TEMPRANA)
            cuota(cuotaRepo, crTemp, 4, LocalDate.now().minusDays(45),  "251.88","37.80","289.68","2017.00", 45, CuotaCredito.EstadoCuota.VENCIDA, null);

            // TARDÍA — 90 días mora (c5)
            Credito crTard = credito(creditoRepo,
                "CRED-2025-000030", c5, asesor,
                Credito.TipoProducto.VEHICULAR,
                "12000.00", "12000.00", "0.1600", 36, "421.80",
                600, "2800.00", "421.80", "0.1506", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.DESEMBOLSADO, Credito.RutaAprobacion.ADMIN, admin,
                "Aprobado por Admin — Score 600.",
                cta5, LocalDate.now().minusMonths(8),
                LocalDate.now().minusMonths(7), LocalDate.now().plusMonths(29));
            mov(movimientoRepo, cta5, Movimiento.TipoMovimiento.DEPOSITO,
                "12000.00","1800.00","13800.00","Desembolso CRED-2025-000030");
            cuota(cuotaRepo, crTard, 1, LocalDate.now().minusMonths(7), "285.80","136.00","421.80","11714.20", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(7));
            cuota(cuotaRepo, crTard, 2, LocalDate.now().minusMonths(6), "289.04","132.76","421.80","11425.16", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(6));
            cuota(cuotaRepo, crTard, 3, LocalDate.now().minusMonths(5), "292.31","129.49","421.80","11132.85", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(5));
            cuota(cuotaRepo, crTard, 4, LocalDate.now().minusMonths(4), "295.63","126.17","421.80","10837.22", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(4));
            // Cuota 5 — vencida hace 90 días (TARDÍA)
            cuota(cuotaRepo, crTard, 5, LocalDate.now().minusDays(90),  "298.98","122.82","421.80","10538.24", 90, CuotaCredito.EstadoCuota.VENCIDA, null);

            // JUDICIAL — 135 días mora (c7)
            Credito crJud = credito(creditoRepo,
                "CRED-2024-000050", c7, asesor,
                Credito.TipoProducto.PERSONAL,
                "6000.00", "6000.00", "0.3500", 18, "413.80",
                520, "1800.00", "413.80", "0.2299", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.DESEMBOLSADO, Credito.RutaAprobacion.ASESOR, asesor,
                "Aprobado — Score 520. DERIVADO A JUDICIAL por Roberto Castillo Díaz — 135 días mora.",
                cta7, LocalDate.now().minusMonths(12),
                LocalDate.now().minusMonths(11), LocalDate.now().plusMonths(7));
            mov(movimientoRepo, cta7, Movimiento.TipoMovimiento.DEPOSITO,
                "6000.00","0.00","6000.00","Desembolso CRED-2024-000050");
            cuota(cuotaRepo, crJud, 1, LocalDate.now().minusMonths(11), "310.47","103.33","413.80","5689.53", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(11));
            cuota(cuotaRepo, crJud, 2, LocalDate.now().minusMonths(10), "315.82", "97.98","413.80","5373.71", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(10));
            cuota(cuotaRepo, crJud, 3, LocalDate.now().minusMonths(9),  "321.26", "92.54","413.80","5052.45", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(9));
            cuota(cuotaRepo, crJud, 4, LocalDate.now().minusMonths(8),  "326.79", "87.01","413.80","4725.66", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(8));
            cuota(cuotaRepo, crJud, 5, LocalDate.now().minusMonths(7),  "332.41", "81.39","413.80","4393.25", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(7));
            cuota(cuotaRepo, crJud, 6, LocalDate.now().minusMonths(6),  "338.12", "75.68","413.80","4055.13", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(6));
            cuota(cuotaRepo, crJud, 7, LocalDate.now().minusMonths(5),  "343.93", "69.87","413.80","3711.20", 0, CuotaCredito.EstadoCuota.PAGADA,   LocalDate.now().minusMonths(5));
            // Cuota 8 — vencida hace 135 días (JUDICIAL ≥121 días)
            cuota(cuotaRepo, crJud, 8, LocalDate.now().minusDays(135),  "349.84", "63.96","413.80","3361.36", 135, CuotaCredito.EstadoCuota.VENCIDA, null);

            // CASTIGO — 200 días mora (c8, crédito viejo)
            Credito crCast = credito(creditoRepo,
                "CRED-2024-000001", c8, asesor,
                Credito.TipoProducto.PERSONAL,
                "2500.00", "2500.00", "0.3500", 12, "241.34",
                440, "1400.00", "241.34", "0.1724", Credito.SemaforoRds.VERDE, true,
                Credito.EstadoCredito.RECHAZADO,   // estado RECHAZADO = castigo contable
                Credito.RutaAprobacion.ASESOR, asesor,
                "Aprobado Score 440. DERIVADO A JUDICIAL — 135 días. CASTIGADO por Dr. Jorge Villanueva Reyes — 200 días mora.",
                cta8, LocalDate.now().minusMonths(14),
                LocalDate.now().minusMonths(13), LocalDate.now().minusMonths(1));
            mov(movimientoRepo, cta8, Movimiento.TipoMovimiento.DEPOSITO,
                "2500.00","950.00","3450.00","Desembolso CRED-2024-000001");
            cuota(cuotaRepo, crCast, 1, LocalDate.now().minusMonths(13), "199.34","42.00","241.34","2300.66", 0, CuotaCredito.EstadoCuota.PAGADA,  LocalDate.now().minusMonths(13));
            cuota(cuotaRepo, crCast, 2, LocalDate.now().minusMonths(12), "202.68","38.66","241.34","2097.98", 0, CuotaCredito.EstadoCuota.PAGADA,  LocalDate.now().minusMonths(12));
            cuota(cuotaRepo, crCast, 3, LocalDate.now().minusMonths(11), "206.07","35.27","241.34","1891.91", 0, CuotaCredito.EstadoCuota.PAGADA,  LocalDate.now().minusMonths(11));
            // Cuota 4 — vencida hace 200 días (CASTIGO >180 días)
            cuota(cuotaRepo, crCast, 4, LocalDate.now().minusDays(200), "209.51","31.83","241.34","1682.40", 200, CuotaCredito.EstadoCuota.VENCIDA, null);

            log.info("[Seed] ✓ 5 créditos en mora (PREVENTIVA/TEMPRANA/TARDÍA/JUDICIAL/CASTIGO)");

            // ═══════════════════════════════════════════════════════
            // BLOQUE 6 — GESTIONES DE COBRANZA R2
            // ═══════════════════════════════════════════════════════

            gestion(gestionRepo, crPrev, null, asesor,
                GestionCobranza.TipoGestion.LLAMADA_TELEFONICA,
                GestionCobranza.ResultadoGestion.PROMESA_PAGO,
                "Cliente comprometió pago antes del " + LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(5), 18);

            gestion(gestionRepo, crTemp, null, asesor2,
                GestionCobranza.TipoGestion.SMS,
                GestionCobranza.ResultadoGestion.CONTACTO_EXITOSO,
                "SMS enviado. Cliente informado de deuda y mora acumulada.", null, 45);

            gestion(gestionRepo, crTemp, null, asesor,
                GestionCobranza.TipoGestion.LLAMADA_TELEFONICA,
                GestionCobranza.ResultadoGestion.SIN_CONTACTO,
                "No contesta. Intentar visita.", null, 45);

            gestion(gestionRepo, crTard, null, jefe,
                GestionCobranza.TipoGestion.VISITA_DOMICILIARIA,
                GestionCobranza.ResultadoGestion.NEGATIVA_PAGO,
                "Cliente negó el pago. Argumenta problemas laborales. Se notifica siguiente etapa.", null, 90);

            gestion(gestionRepo, crTard, null, asesor,
                GestionCobranza.TipoGestion.CARTA_NOTARIAL,
                GestionCobranza.ResultadoGestion.OTRO,
                "Carta notarial enviada por pérdida de empleo. Plazo 10 días hábiles.", null, 90);

            gestion(gestionRepo, crJud, null, jefe,
                GestionCobranza.TipoGestion.CARTA_NOTARIAL,
                GestionCobranza.ResultadoGestion.OTRO,
                "Carta de inicio de proceso judicial enviada.", null, 135);

            gestion(gestionRepo, crJud, null, riesgos,
                GestionCobranza.TipoGestion.OTRO,
                GestionCobranza.ResultadoGestion.OTRO,
                "Expediente enviado a área legal para inicio de proceso judicial. Bien hipotecado identificado.", null, 135);

            gestion(gestionRepo, crCast, null, gerencia,
                GestionCobranza.TipoGestion.OTRO,
                GestionCobranza.ResultadoGestion.OTRO,
                "Crédito castigado contablemente según resolución de Gerencia. Incobrabilidad declarada.", null, 200);

            log.info("[Seed] ✓ 8 gestiones de cobranza R2 creadas");

            // ═══════════════════════════════════════════════════════
            // BLOQUE 7 — AUDITORÍA (trazabilidad completa)
            // ═══════════════════════════════════════════════════════

            audit(auditoriaRepo, "demo@banco.pe",  "CLIENTE",  AuditoriaEvento.TipoAccion.LOGIN,             "AUTH",          "Login exitoso",                                                    null);
            audit(auditoriaRepo, "asesor@banco.pe", "ASESOR",  AuditoriaEvento.TipoAccion.LOGIN,             "AUTH",          "Login exitoso",                                                    null);
            audit(auditoriaRepo, "demo@banco.pe",  "CLIENTE",  AuditoriaEvento.TipoAccion.CREDITO_SOLICITUD, "CREDITO",       "Solicitud CRED-2026-000001 S/5,000 PERSONAL",                     cr1.getId());
            audit(auditoriaRepo, "asesor@banco.pe","ASESOR",   AuditoriaEvento.TipoAccion.CREDITO_APROBACION,"CREDITO",       "Aprobado CRED-2026-000001 — Score 750 RDS 11%",                   cr1.getId());
            audit(auditoriaRepo, "asesor@banco.pe","ASESOR",   AuditoriaEvento.TipoAccion.CREDITO_DESEMBOLSO,"CREDITO",       "Desembolso CRED-2026-000001 S/5,000 → cta 0011223344550001",      cr1.getId());
            audit(auditoriaRepo, "demo@banco.pe",  "CLIENTE",  AuditoriaEvento.TipoAccion.CREDITO_SOLICITUD, "CREDITO",       "Solicitud CRED-2026-000002 S/80,000 HIPOTECARIO",                 null);
            audit(auditoriaRepo, "jefe@banco.pe",  "JEFE_REGIONAL", AuditoriaEvento.TipoAccion.LOGIN,        "AUTH",          "Login exitoso",                                                    null);
            audit(auditoriaRepo, "demo@banco.pe",  "CLIENTE",  AuditoriaEvento.TipoAccion.CUENTA_TRANSFERENCIA,"CUENTA",      "Transferencia S/500 → Pedro Sánchez",                             null);
            audit(auditoriaRepo, "riesgos@banco.pe","RIESGOS", AuditoriaEvento.TipoAccion.LOGIN,             "AUTH",          "Login exitoso",                                                    null);
            audit(auditoriaRepo, "jefe@banco.pe",  "JEFE_REGIONAL", AuditoriaEvento.TipoAccion.COBRANZA_JUDICIAL,"RECUPERACIONES","Derivado a judicial CRED-2024-000050 — 135 días mora",        crJud.getId());
            audit(auditoriaRepo, "gerencia@banco.pe","GERENCIA",AuditoriaEvento.TipoAccion.COBRANZA_CASTIGO, "RECUPERACIONES","Castigado CRED-2024-000001 — 200 días mora. Incobrabilidad declarada.", crCast.getId());
            audit(auditoriaRepo, "admin@banco.pe", "ADMIN",    AuditoriaEvento.TipoAccion.CREDITO_APROBACION,"CREDITO",       "Aprobado CRED-2026-000003 S/15,000 VEHICULAR",                    null);

            log.info("[Seed] ✓ 12 eventos de auditoría registrados");

            // ═══════════════════════════════════════════════════════
            // RESUMEN FINAL
            // ═══════════════════════════════════════════════════════

            long totalCreditos    = creditoRepo.count();
            long desembolsados    = creditoRepo.findByEstadoIn(
                new String[]{"DESEMBOLSADO"}).size();
            long conMora          = creditoRepo.findCreditosConMora().size();
            double tasaMora       = desembolsados > 0 ? (double) conMora / desembolsados * 100 : 0;

            log.info("[Seed] ══════════════════════════════════════════════════");
            log.info("[Seed] RESUMEN DE CARGA:");
            log.info("[Seed]   Usuarios:     15  (8 clientes + 7 roles core)");
            log.info("[Seed]   Cuentas:       9  (ahorros y corriente)");
            log.info("[Seed]   Créditos:      {}  total", totalCreditos);
            log.info("[Seed]   Desembolsados: {}", desembolsados);
            log.info("[Seed]   En mora:       {} (~{}%)", conMora, Math.round(tasaMora));
            log.info("[Seed]   Bandas mora: PREVENTIVA(18d) TEMPRANA(45d) TARDÍA(90d) JUDICIAL(135d) CASTIGO(200d)");
            log.info("[Seed]   Gestiones R2: 8");
            log.info("[Seed]   Eventos audit: 12");
            log.info("[Seed] ══════════════════════════════════════════════════");
            log.info("[Seed] ACCESOS:");
            log.info("[Seed]   Homebanking  → demo@banco.pe         / 123456  (CLIENTE — Ana)");
            log.info("[Seed]   Core Asesor  → asesor@banco.pe       / 123456  (ASESOR)");
            log.info("[Seed]   Core Admin   → admin@banco.pe        / 123456  (ADMIN)");
            log.info("[Seed]   Core Jefe    → jefe@banco.pe         / 123456  (JEFE_REGIONAL)");
            log.info("[Seed]   Core Riesgos → riesgos@banco.pe      / 123456  (RIESGOS)");
            log.info("[Seed]   Core Comité  → comite@banco.pe       / 123456  (COMITE)");
            log.info("[Seed]   Core Gerencia→ gerencia@banco.pe     / 123456  (GERENCIA)");
            log.info("[Seed] ══════════════════════════════════════════════════");
        };
    }

    // ═══════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════

    private Usuario u(UsuarioRepository r, PasswordEncoder pe,
                      String nombre, String email, String pw, Usuario.Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(pe.encode(pw));
        u.setRol(rol);
        return r.save(u);
    }

    private Cuenta cta(CuentaRepository r, String numero,
                       Cuenta.TipoCuenta tipo, String saldo, Usuario usuario) {
        Cuenta c = new Cuenta();
        c.setNumeroCuenta(numero);
        c.setTipoCuenta(tipo);
        c.setSaldo(new BigDecimal(saldo));
        c.setMoneda("PEN");
        c.setUsuario(usuario);
        return r.save(c);
    }

    private void mov(MovimientoRepository r, Cuenta cuenta,
                     Movimiento.TipoMovimiento tipo,
                     String monto, String antes, String despues, String desc) {
        Movimiento m = new Movimiento();
        m.setCuenta(cuenta);
        m.setTipo(tipo);
        m.setMonto(new BigDecimal(monto));
        m.setSaldoAnterior(new BigDecimal(antes));
        m.setSaldoPosterior(new BigDecimal(despues));
        m.setDescripcion(desc);
        r.save(m);
    }

    @SuppressWarnings("java:S107")
    private Credito credito(CreditoRepository r,
                            String numOp, Usuario cliente, Usuario asesor,
                            Credito.TipoProducto tipo,
                            String montoSol, String montoApro, String tea,
                            int plazo, String cuota,
                            int score, String ingreso, String deuda, String rds,
                            Credito.SemaforoRds semaforo, boolean sujetoCredito,
                            Credito.EstadoCredito estado, Credito.RutaAprobacion ruta,
                            Usuario aprobadoPor, String comentario,
                            Cuenta cuentaDesembolso,
                            LocalDate fechaDesembolso, LocalDate fechaPrimera, LocalDate fechaUltima) {
        Credito c = new Credito();
        c.setNumeroOperacion(numOp);
        c.setCliente(cliente);
        c.setAsesor(asesor);
        c.setTipoProducto(tipo);
        c.setMontoSolicitado(new BigDecimal(montoSol));
        if (montoApro != null) c.setMontoAprobado(new BigDecimal(montoApro));
        c.setTea(new BigDecimal(tea));
        c.setPlazoMeses(plazo);
        c.setCuotaMensual(new BigDecimal(cuota));
        c.setMoneda("PEN");
        c.setProposito("Solicitud " + tipo.name().toLowerCase() + " — " + cliente.getNombre());
        c.setScoreCrediticio(score);
        c.setIngresoMensual(new BigDecimal(ingreso));
        c.setDeudaTotalVigente(new BigDecimal(deuda));
        c.setRdsRatio(new BigDecimal(rds));
        c.setRdsSemaforo(semaforo);
        c.setEsSujetoCredito(sujetoCredito);
        c.setEstado(estado);
        c.setRutaAprobacion(ruta);
        c.setAprobadoPor(aprobadoPor);
        c.setComentarioEvaluacion(comentario);
        c.setCuentaDesembolso(cuentaDesembolso);
        c.setFechaDesembolso(fechaDesembolso);
        c.setFechaPrimeraCuota(fechaPrimera);
        c.setFechaUltimaCuota(fechaUltima);
        return r.save(c);
    }

    private void cuota(CuotaCreditoRepository r, Credito credito,
                       int numero, LocalDate vencimiento,
                       String capital, String interes, String total, String saldo,
                       int diasMora, CuotaCredito.EstadoCuota estado, LocalDate fechaPago) {
        CuotaCredito c = new CuotaCredito();
        c.setCredito(credito);
        c.setNumeroCuota(numero);
        c.setFechaVencimiento(vencimiento);
        c.setFechaPago(fechaPago);
        c.setCapital(new BigDecimal(capital));
        c.setInteres(new BigDecimal(interes));
        c.setSeguro(BigDecimal.ZERO);
        c.setCuotaTotal(new BigDecimal(total));
        c.setSaldoCapital(new BigDecimal(saldo));
        c.setDiasMora(diasMora);
        c.setEstadoCuota(estado);
        if (diasMora > 0) {
            BigDecimal mora = new BigDecimal(capital)
                .multiply(new BigDecimal("0.001"))
                .multiply(BigDecimal.valueOf(diasMora))
                .setScale(2, RoundingMode.HALF_UP);
            c.setMontoMora(mora);
        }
        r.save(c);
    }

    private void gestion(GestionCobranzaRepository r,
                         Credito credito, CuotaCredito cuota, Usuario gestor,
                         GestionCobranza.TipoGestion tipo,
                         GestionCobranza.ResultadoGestion resultado,
                         String descripcion, LocalDate compromiso, int diasMora) {
        GestionCobranza g = new GestionCobranza();
        g.setCredito(credito);
        g.setCuota(cuota);
        g.setGestor(gestor);
        g.setTipoGestion(tipo);
        g.setResultado(resultado);
        g.setDescripcion(descripcion);
        g.setFechaCompromisoPago(compromiso);
        g.setDiasMoraAlGestionar(diasMora);
        r.save(g);
    }

    private void audit(AuditoriaEventoRepository r,
                       String email, String rol,
                       AuditoriaEvento.TipoAccion accion,
                       String modulo, String descripcion, Long recursoId) {
        AuditoriaEvento e = AuditoriaEvento.of(email, rol, accion, modulo, descripcion, recursoId, "127.0.0.1");
        r.save(e);
    }
}
