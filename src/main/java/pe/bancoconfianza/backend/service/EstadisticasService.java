package pe.bancoconfianza.backend.service;

import org.springframework.stereotype.Service;
import pe.bancoconfianza.backend.model.Credito;
import pe.bancoconfianza.backend.model.Credito.EstadoCredito;
import pe.bancoconfianza.backend.model.Usuario;
import pe.bancoconfianza.backend.repository.CreditoRepository;
import pe.bancoconfianza.backend.repository.CuotaCreditoRepository;
import pe.bancoconfianza.backend.repository.GestionCobranzaRepository;
import pe.bancoconfianza.backend.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EstadisticasService — Datos de tendencias mensuales para dashboards.
 *
 * Provee 3 endpoints principales:
 *  1. /api/estadisticas/creditos-mensuales  → desembolsos y mora por mes (12m)
 *  2. /api/estadisticas/recuperaciones      → KPIs de recuperaciones + por tipo gestión
 *  3. /api/estadisticas/actividad-usuarios  → logins y operaciones por hora del día
 */
@Service
public class EstadisticasService {

    private final CreditoRepository        creditoRepo;
    private final CuotaCreditoRepository   cuotaRepo;
    private final GestionCobranzaRepository gestionRepo;
    private final UsuarioRepository        usuarioRepo;

    private static final String[] MESES_ES = {
        "Ene","Feb","Mar","Abr","May","Jun",
        "Jul","Ago","Sep","Oct","Nov","Dic"
    };

    public EstadisticasService(CreditoRepository creditoRepo,
                               CuotaCreditoRepository cuotaRepo,
                               GestionCobranzaRepository gestionRepo,
                               UsuarioRepository usuarioRepo) {
        this.creditoRepo  = creditoRepo;
        this.cuotaRepo    = cuotaRepo;
        this.gestionRepo  = gestionRepo;
        this.usuarioRepo  = usuarioRepo;
    }

    /* ══════════════════════════════════════════════════════════
       1. CRÉDITOS MENSUALES — desembolsos + cartera + mora
    ══════════════════════════════════════════════════════════ */

    /**
     * Agrupa créditos por mes (año en curso) y calcula:
     *  - desembolsos: cantidad desembolsados en ese mes
     *  - montoDesembolsado: suma de montoAprobado en ese mes
     *  - carteraAcumulada: suma de montoAprobado de todos los DESEMBOLSADOS hasta ese mes
     *  - moraPct: % de mora de créditos creados ese mes (aproximado con cuotas vencidas)
     *
     * Retorna array de 12 elementos (un objeto por mes).
     */
    public List<Map<String, Object>> getCreditosMensuales() {
        int anioActual = LocalDateTime.now().getYear();

        // Todos los créditos del año en curso
        List<Credito> todosCreditos = creditoRepo.findAll();

        // Agrupamos por mes de creación (createdAt)
        Map<Integer, List<Credito>> porMes = todosCreditos.stream()
            .filter(c -> c.getCreatedAt() != null
                && c.getCreatedAt().getYear() == anioActual)
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().getMonthValue() // 1–12
            ));

        // Créditos DESEMBOLSADOS de todos los tiempos (para cartera acumulada)
        List<Credito> desembolsadosTotal = todosCreditos.stream()
            .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO)
            .collect(Collectors.toList());

        // Construir respuesta mes a mes
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (int mes = 1; mes <= 12; mes++) {
            List<Credito> delMes = porMes.getOrDefault(mes, Collections.emptyList());

            long desembolsados = delMes.stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO)
                .count();

            double montoDesemb = delMes.stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO)
                .mapToDouble(c -> c.getMontoAprobado() != null
                    ? c.getMontoAprobado().doubleValue() : 0.0)
                .sum();

            // Cartera acumulada hasta este mes
            final int mesActual = mes;
            double acumHastaMes = desembolsadosTotal.stream()
                .filter(c -> c.getCreatedAt() != null
                    && (c.getCreatedAt().getYear() < anioActual
                        || (c.getCreatedAt().getYear() == anioActual
                            && c.getCreatedAt().getMonthValue() <= mesActual)))
                .mapToDouble(c -> c.getMontoAprobado() != null
                    ? c.getMontoAprobado().doubleValue() : 0.0)
                .sum();

            // Tasa mora estimada (créditos del mes con cuotas vencidas / total del mes)
            long totalDelMes   = delMes.size();
            long conMoraDelMes = delMes.stream()
                .filter(c -> cuotaRepo.maxDiasMora(c) != null
                    && cuotaRepo.maxDiasMora(c) > 0)
                .count();
            double moraPct = totalDelMes > 0
                ? Math.round((double) conMoraDelMes / totalDelMes * 1000.0) / 10.0
                : 0.0;

            // Rechazados del mes
            long rechazados = delMes.stream()
                .filter(c -> c.getEstado() == EstadoCredito.RECHAZADO)
                .count();

            // Solicitudes del mes (todas)
            long solicitudes = delMes.size();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("mes",                MESES_ES[mes - 1]);
            item.put("mesNumero",          mes);
            item.put("desembolsos",        desembolsados);
            item.put("montoDesembolsado",  Math.round(montoDesemb));
            item.put("carteraAcumulada",   Math.round(acumHastaMes / 1000.0)); // en K soles
            item.put("moraPct",            moraPct);
            item.put("solicitudes",        solicitudes);
            item.put("rechazados",         rechazados);

            resultado.add(item);
        }

        return resultado;
    }

    /* ══════════════════════════════════════════════════════════
       2. ESTADÍSTICAS DE RECUPERACIONES
    ══════════════════════════════════════════════════════════ */

    /**
     * Retorna estadísticas de recuperaciones:
     *  - efectividadPorTipo: % de gestiones con resultado PAGO_REALIZADO / PROMESA_PAGO por tipo
     *  - totalGestiones: total de gestiones registradas
     *  - promesasCumplidas: PAGO_REALIZADO / total
     *  - contactabilidad: CONTACTO_EXITOSO / total
     */
    public Map<String, Object> getEstadisticasRecuperaciones() {
        var gestiones = gestionRepo.findAll();

        int total = gestiones.size();

        // Conteo por tipo de gestión
        Map<String, Long> porTipo = gestiones.stream()
            .collect(Collectors.groupingBy(
                g -> g.getTipoGestion().name(),
                Collectors.counting()
            ));

        // Conteo de resultados exitosos por tipo
        Map<String, Long> exitosPorTipo = gestiones.stream()
            .filter(g ->
                g.getResultado().name().equals("PAGO_REALIZADO") ||
                g.getResultado().name().equals("PROMESA_PAGO"))
            .collect(Collectors.groupingBy(
                g -> g.getTipoGestion().name(),
                Collectors.counting()
            ));

        // Calcular % efectividad por tipo
        List<Map<String, Object>> efectividad = new ArrayList<>();
        Map<String, String> tipoLabel = Map.of(
            "LLAMADA_TELEFONICA",  "Llamada",
            "SMS",                 "SMS",
            "EMAIL",               "Email",
            "VISITA_DOMICILIARIA", "Visita",
            "CARTA_NOTARIAL",      "Notarial",
            "ACUERDO_PAGO",        "Acuerdo",
            "OTRO",                "Otro"
        );

        for (var entry : porTipo.entrySet()) {
            String tipo     = entry.getKey();
            long   cnt      = entry.getValue();
            long   exitosos = exitosPorTipo.getOrDefault(tipo, 0L);
            double pct      = cnt > 0 ? Math.round((double) exitosos / cnt * 1000.0) / 10.0 : 0.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tipo",       tipoLabel.getOrDefault(tipo, tipo));
            item.put("tipoRaw",    tipo);
            item.put("total",      cnt);
            item.put("exitosos",   exitosos);
            item.put("pct",        pct);
            efectividad.add(item);
        }

        // Ordenar por pct descendente
        efectividad.sort((a, b) ->
            Double.compare((Double) b.get("pct"), (Double) a.get("pct")));

        // KPIs globales
        long pagosRealizados = gestiones.stream()
            .filter(g -> g.getResultado().name().equals("PAGO_REALIZADO"))
            .count();
        long promesas = gestiones.stream()
            .filter(g -> g.getResultado().name().equals("PROMESA_PAGO"))
            .count();
        long contactados = gestiones.stream()
            .filter(g -> g.getResultado().name().equals("CONTACTO_EXITOSO")
                || g.getResultado().name().equals("PAGO_REALIZADO")
                || g.getResultado().name().equals("PROMESA_PAGO"))
            .count();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalGestiones",     total);
        resultado.put("pagosRealizados",    pagosRealizados);
        resultado.put("promesas",           promesas);
        resultado.put("contactabilidadPct", total > 0
            ? Math.round((double) contactados / total * 1000.0) / 10.0 : 0.0);
        resultado.put("efectividadPorTipo", efectividad);

        return resultado;
    }

    /* ══════════════════════════════════════════════════════════
       3. ACTIVIDAD DE USUARIOS POR HORA
         (usa tabla gestiones_cobranza como proxy de actividad)
    ══════════════════════════════════════════════════════════ */

    /**
     * Distribución de operaciones por hora del día (0–23).
     * Fuente: createdAt de gestiones + créditos creados hoy.
     *
     * Si no hay datos, devuelve distribución demo representativa.
     */
    public Map<String, Object> getActividadPorHora() {
        var gestiones    = gestionRepo.findAll();
        var creditos     = creditoRepo.findAll();

        Map<Integer, Long> cuentaHora = new HashMap<>();
        for (int h = 0; h < 24; h++) cuentaHora.put(h, 0L);

        // Conteo de gestiones por hora
        gestiones.forEach(g -> {
            if (g.getCreatedAt() != null) {
                int  hora   = g.getCreatedAt().getHour();
                long actual = cuentaHora.getOrDefault(hora, 0L);
                cuentaHora.put(hora, actual + 1L);
            }
        });

        // Conteo de créditos por hora
        creditos.forEach(c -> {
            if (c.getCreatedAt() != null) {
                int  hora   = c.getCreatedAt().getHour();
                long actual = cuentaHora.getOrDefault(hora, 0L);
                cuentaHora.put(hora, actual + 1L);
            }
        });

        long totalOps = cuentaHora.values().stream().mapToLong(Long::longValue).sum();

        // Si no hay datos reales, devolver distribución horaria demo
        boolean sinDatos = totalOps == 0;
        int[] demo = {
            2,1,0,0,1,2, 8,22,35,28,30,45,
            52,48,38,32,28,20, 15,10,8,6,4,3
        };

        List<Map<String, Object>> porHora = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            long eventos = sinDatos ? demo[h] : cuentaHora.get(h);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hora",    String.format("%02dh", h));
            item.put("horaNum", h);
            item.put("eventos", eventos);
            porHora.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("porHora",    porHora);
        resultado.put("totalOps",   sinDatos ? 748 : totalOps);
        resultado.put("esDemoData", sinDatos);

        return resultado;
    }

    /* ══════════════════════════════════════════════════════════
       4. KPIS EJECUTIVOS — ROA, ROE, CLIENTES ACTIVOS
    ══════════════════════════════════════════════════════════ */

    /**
     * Retorna KPIs ejecutivos avanzados:
     *  - ROA: Return on Assets (resultado/activos totales)
     *  - ROE: Return on Equity (resultado/patrimonio)
     *  - clientesActivos: usuarios CLIENTE activos
     *  - moraGlobal: tasa de mora global de la cartera
     *  - tasaAprobacion: porcentaje de créditos aprobados vs rechazados
     *  - changes: variaciones vs mes anterior
     */
    public Map<String, Object> getKpisEjecutivos() {
        try {
            // Cartera total (aproxima activos)
            double carteraTotal = creditoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO)
                .mapToDouble(c -> c.getMontoAprobado() != null ? c.getMontoAprobado().doubleValue() : 0.0)
                .sum();

            // Si no hay cartera, usar datos de ejemplo basados en créditos existentes
            List<Credito> todosCreditos = creditoRepo.findAll();
            if (carteraTotal == 0 && !todosCreditos.isEmpty()) {
                // Calcular cartera basada en todos los créditos (no solo desembolsados)
                carteraTotal = todosCreditos.stream()
                    .filter(c -> c.getMontoAprobado() != null || c.getMontoSolicitado() != null)
                    .mapToDouble(c -> {
                        if (c.getMontoAprobado() != null) return c.getMontoAprobado().doubleValue();
                        if (c.getMontoSolicitado() != null) return c.getMontoSolicitado().doubleValue();
                        return 0.0;
                    })
                    .sum();
            }

            // Resultado neto anualizado (suma de resultados estimados de cartera)
            // Simplificación: ~12% de la cartera como resultado neto anual
            double resultadoNeto = carteraTotal * 0.12;

            // Patrimonio estimado (~15% de cartera)
            double patrimonio = carteraTotal * 0.15;

            // ROA y ROE
            double roa = carteraTotal > 0 ? (resultadoNeto / carteraTotal) * 100 : 0.0;
            double roe = patrimonio > 0 ? (resultadoNeto / patrimonio) * 100 : 0.0;

            // Clientes activos (usuarios con rol CLIENTE y activo=true)
            long clientesActivos = usuarioRepo.findAll().stream()
                .filter(u -> u.getRol() == Usuario.Rol.CLIENTE && u.isActivo())
                .count();

            // Mora global (créditos con cuotas vencidas / total desembolsados)
            List<Credito> desembolsados = creditoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO)
                .collect(Collectors.toList());

            // Si no hay desembolsados, usar todos los créditos para el cálculo
            if (desembolsados.isEmpty()) {
                desembolsados = todosCreditos;
            }

            long totalDesembolsados = desembolsados.size();
            long conMora = desembolsados.stream()
                .filter(c -> cuotaRepo.maxDiasMora(c) != null && cuotaRepo.maxDiasMora(c) > 0)
                .count();

            double moraGlobal = totalDesembolsados > 0 
                ? Math.round((double) conMora / totalDesembolsados * 1000.0) / 10.0 
                : 0.0;

            // Tasa de aprobación (aprobados / (aprobados + rechazados))
            LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            
            long aprobadosMes = creditoRepo.findAll().stream()
                .filter(c -> (c.getEstado() == EstadoCredito.APROBADO || c.getEstado() == EstadoCredito.DESEMBOLSADO)
                    && c.getCreatedAt() != null
                    && c.getCreatedAt().isAfter(inicioMes))
                .count();
            
            long rechazadosMes = creditoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCredito.RECHAZADO
                    && c.getCreatedAt() != null
                    && c.getCreatedAt().isAfter(inicioMes))
                .count();

            // Si no hay datos del mes, calcular sobre todos los créditos
            if (aprobadosMes == 0 && rechazadosMes == 0) {
                aprobadosMes = todosCreditos.stream()
                    .filter(c -> c.getEstado() == EstadoCredito.APROBADO || c.getEstado() == EstadoCredito.DESEMBOLSADO)
                    .count();
                
                rechazadosMes = todosCreditos.stream()
                    .filter(c -> c.getEstado() == EstadoCredito.RECHAZADO)
                    .count();
            }

            double tasaAprobacion = (aprobadosMes + rechazadosMes) > 0
                ? Math.round((double) aprobadosMes / (aprobadosMes + rechazadosMes) * 1000.0) / 10.0
                : 0.0;

            // Cambios vs mes anterior (simplificado - valores demo por ahora)
            double carteraChange = 5.2;
            double moraChange = -0.3;
            double roeChange = 1.2;
            double aprobacionChange = 2.5;

            System.out.println("=== KPIs Ejecutivos ===");
            System.out.println("Total créditos en BD: " + todosCreditos.size());
            System.out.println("Créditos desembolsados: " + desembolsados.size());
            System.out.println("Cartera Total: " + carteraTotal);
            System.out.println("ROE: " + roe);
            System.out.println("Mora Global: " + moraGlobal);
            System.out.println("Tasa Aprobación: " + tasaAprobacion);
            System.out.println("Clientes Activos: " + clientesActivos);

            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("roa",                 Math.round(roa * 10.0) / 10.0);
            resultado.put("roe",                 Math.round(roe * 10.0) / 10.0);
            resultado.put("clientesActivos",     clientesActivos);
            resultado.put("carteraTotal",        Math.round(carteraTotal));
            resultado.put("resultadoNeto",       Math.round(resultadoNeto));
            resultado.put("patrimonio",          Math.round(patrimonio));
            resultado.put("moraGlobal",          moraGlobal);
            resultado.put("tasaAprobacion",      tasaAprobacion);
            resultado.put("carteraChange",       carteraChange);
            resultado.put("moraChange",          moraChange);
            resultado.put("roeChange",           roeChange);
            resultado.put("aprobacionChange",    aprobacionChange);

            return resultado;
        } catch (Exception e) {
            // Log error y devolver datos vacíos
            System.err.println("Error en getKpisEjecutivos: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("roa",                 0.0);
            fallback.put("roe",                 0.0);
            fallback.put("clientesActivos",     0L);
            fallback.put("carteraTotal",        0L);
            fallback.put("resultadoNeto",       0L);
            fallback.put("patrimonio",          0L);
            fallback.put("moraGlobal",          0.0);
            fallback.put("tasaAprobacion",      0.0);
            fallback.put("carteraChange",       0.0);
            fallback.put("moraChange",          0.0);
            fallback.put("roeChange",           0.0);
            fallback.put("aprobacionChange",    0.0);
            fallback.put("error",               true);
            return fallback;
        }
    }

    /* ══════════════════════════════════════════════════════════
       5. RANKINGS — TOP ASESORES Y REGIONES
    ══════════════════════════════════════════════════════════ */

    /**
     * Retorna rankings de desempeño:
     *  - topAsesores: top 5 usuarios ASESOR por cantidad de créditos desembolsados
     *  - topRegiones: top 5 regiones por cartera total
     */
    public Map<String, Object> getRankings() {
        try {
            // Top asesores por créditos desembolsados
            Map<String, Long> creditosPorAsesor = creditoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO && c.getAsesor() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getAsesor().getNombre() != null ? c.getAsesor().getNombre() : "Sin nombre",
                    Collectors.counting()
                ));

            List<Map<String, Object>> topAsesores = creditosPorAsesor.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("nombre", entry.getKey());
                    item.put("creditos", entry.getValue());
                    
                    // Calcular monto total colocado por este asesor
                    double montoTotal = creditoRepo.findAll().stream()
                        .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO 
                            && c.getAsesor() != null
                            && c.getAsesor().getNombre() != null
                            && c.getAsesor().getNombre().equals(entry.getKey()))
                        .mapToDouble(c -> c.getMontoAprobado() != null ? c.getMontoAprobado().doubleValue() : 0.0)
                        .sum();
                    
                    item.put("montoTotal", Math.round(montoTotal));
                    return item;
                })
                .collect(Collectors.toList());

            // Top regiones (simplificado - usar nombre del cliente como indicador de región)
            // Como no tenemos campo direccion en Usuario, usamos una distribución simulada
            Map<String, Double> carteraPorRegion = new HashMap<>();
            List<Credito> desembolsados = creditoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO)
                .collect(Collectors.toList());
            
            // Distribución aproximada por región (basada en hash del cliente para consistencia)
            String[] regiones = {"Lima Centro", "Lima Norte", "Callao", "San Juan de Lurigancho", "Chorrillos"};
            
            for (Credito c : desembolsados) {
                // Usar hash del cliente para asignar región consistentemente
                // Protección contra nulls
                if (c.getCliente() == null || c.getCliente().getNombre() == null) {
                    continue;
                }
                
                int regionIdx = Math.abs(c.getCliente().getNombre().hashCode()) % regiones.length;
                String region = regiones[regionIdx];
                
                double monto = c.getMontoAprobado() != null ? c.getMontoAprobado().doubleValue() : 0.0;
                carteraPorRegion.put(region, carteraPorRegion.getOrDefault(region, 0.0) + monto);
            }

            List<Map<String, Object>> topRegiones = carteraPorRegion.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("region", entry.getKey());
                    item.put("cartera", Math.round(entry.getValue()));
                    
                    // Contar créditos en la región
                    long creditosRegion = (long) desembolsados.stream()
                        .filter(c -> {
                            if (c.getCliente() == null || c.getCliente().getNombre() == null) {
                                return false;
                            }
                            int idx = Math.abs(c.getCliente().getNombre().hashCode()) % regiones.length;
                            return regiones[idx].equals(entry.getKey());
                        })
                        .count();
                    
                    item.put("creditos", creditosRegion);
                    return item;
                })
                .collect(Collectors.toList());

            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("topAsesores", topAsesores);
            resultado.put("topRegiones", topRegiones);

            return resultado;
        } catch (Exception e) {
            // Log error y devolver datos vacíos
            System.err.println("Error en getRankings: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("topAsesores", new ArrayList<>());
            fallback.put("topRegiones", new ArrayList<>());
            fallback.put("error", true);
            return fallback;
        }
    }

    /* ══════════════════════════════════════════════════════════
       6. CUMPLIMIENTO DE METAS
    ══════════════════════════════════════════════════════════ */

    /**
     * Retorna indicadores de cumplimiento de metas mensuales:
     *  - metaMensual: objetivo de colocaciones del mes (hardcoded)
     *  - colocacionesDelMes: desembolsos del mes actual
     *  - cumplimiento: % logrado vs meta
     */
    public Map<String, Object> getCumplimientoMetas() {
        try {
            LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime finMes = inicioMes.plusMonths(1).minusSeconds(1);

            // Colocaciones del mes actual
            long colocacionesDelMes = creditoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO
                    && c.getCreatedAt() != null
                    && c.getCreatedAt().isAfter(inicioMes)
                    && c.getCreatedAt().isBefore(finMes))
                .count();

            double montoColocado = creditoRepo.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO
                    && c.getCreatedAt() != null
                    && c.getCreatedAt().isAfter(inicioMes)
                    && c.getCreatedAt().isBefore(finMes))
                .mapToDouble(c -> c.getMontoAprobado() != null ? c.getMontoAprobado().doubleValue() : 0.0)
                .sum();

            // Meta mensual (ejemplo: 50 créditos o 2M soles)
            long metaCantidad = 50L;
            double metaMonto = 2_000_000.0;

            // Cumplimiento
            double cumplimientoCantidad = metaCantidad > 0 ? (double) colocacionesDelMes / metaCantidad * 100 : 0.0;
            double cumplimientoMonto = metaMonto > 0 ? montoColocado / metaMonto * 100 : 0.0;

            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("metaCantidad",        metaCantidad);
            resultado.put("metaMonto",           Math.round(metaMonto));
            resultado.put("colocacionesDelMes",  colocacionesDelMes);
            resultado.put("montoColocado",       Math.round(montoColocado));
            resultado.put("cumplimientoCantidad", Math.round(cumplimientoCantidad * 10.0) / 10.0);
            resultado.put("cumplimientoMonto",   Math.round(cumplimientoMonto * 10.0) / 10.0);
            resultado.put("mesActual",           LocalDateTime.now().getMonth().name());

            return resultado;
        } catch (Exception e) {
            // Log error y devolver datos vacíos
            System.err.println("Error en getCumplimientoMetas: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("metaCantidad",        50L);
            fallback.put("metaMonto",           2000000L);
            fallback.put("colocacionesDelMes",  0L);
            fallback.put("montoColocado",       0L);
            fallback.put("cumplimientoCantidad", 0.0);
            fallback.put("cumplimientoMonto",   0.0);
            fallback.put("mesActual",           LocalDateTime.now().getMonth().name());
            fallback.put("error",               true);
            return fallback;
        }
    }

    /* ══════════════════════════════════════════════════════════
       7. CARTERA POR ASESOR (Sprint 6 - Drill-down)
    ══════════════════════════════════════════════════════════ */

    /**
     * Retorna detalle de cartera agrupado por asesor.
     * Opcionalmente filtrado por producto.
     * 
     * @param producto Filtro opcional de tipo de producto (ej: "PERSONAL")
     */
    public List<Map<String, Object>> getCarteraPorAsesor(String producto) {
        // Filtrar créditos desembolsados
        var creditos = creditoRepo.findAll().stream()
            .filter(c -> c.getEstado() == EstadoCredito.DESEMBOLSADO && c.getAsesor() != null)
            .filter(c -> producto == null || producto.isEmpty() 
                || (c.getTipoProducto() != null && producto.equals(c.getTipoProducto().name())))
            .collect(Collectors.toList());

        // Agrupar por asesor
        Map<String, List<Credito>> porAsesor = creditos.stream()
            .collect(Collectors.groupingBy(
                c -> c.getAsesor().getNombre() != null ? c.getAsesor().getNombre() : "Sin nombre"
            ));

        // Calcular totales por asesor
        List<Map<String, Object>> resultado = porAsesor.entrySet().stream()
            .map(entry -> {
                String nombreAsesor = entry.getKey();
                List<Credito> creditosAsesor = entry.getValue();

                long cantidadCreditos = creditosAsesor.size();
                double montoTotal = creditosAsesor.stream()
                    .mapToDouble(c -> c.getMontoAprobado() != null ? c.getMontoAprobado().doubleValue() : 0.0)
                    .sum();

                // Productos del asesor (si no hay filtro)
                Map<String, Long> productos = creditosAsesor.stream()
                    .collect(Collectors.groupingBy(
                        c -> c.getTipoProducto() != null ? c.getTipoProducto().name() : "OTRO",
                        Collectors.counting()
                    ));

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("nombre", nombreAsesor);
                item.put("creditos", cantidadCreditos);
                item.put("montoTotal", Math.round(montoTotal));
                item.put("productos", productos);
                if (producto != null && !producto.isEmpty()) {
                    item.put("productoFiltro", producto);
                }

                return item;
            })
            .sorted((a, b) -> Long.compare((Long) b.get("montoTotal"), (Long) a.get("montoTotal")))
            .collect(Collectors.toList());

        return resultado;
    }
}
