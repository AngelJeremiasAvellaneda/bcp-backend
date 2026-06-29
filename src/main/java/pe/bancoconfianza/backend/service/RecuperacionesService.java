package pe.bancoconfianza.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.bancoconfianza.backend.dto.GestionCobranzaRequest;
import pe.bancoconfianza.backend.model.*;
import pe.bancoconfianza.backend.model.Credito.EstadoCredito;
import pe.bancoconfianza.backend.repository.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Módulo de Recuperaciones / Mora — R1, R2, R3.
 *
 * Bandas de mora:
 *   Preventiva : 1–30 días
 *   Temprana   : 31–60 días
 *   Tardía     : 61–120 días
 *   Judicial   : 121–180 días
 *   Castigo    : > 180 días
 */
@Service
public class RecuperacionesService {

    // Umbrales (días) para transiciones R3
    private static final int DIAS_JUDICIAL = 121;
    private static final int DIAS_CASTIGO  = 181;

    private final CreditoRepository        creditoRepo;
    private final CuotaCreditoRepository   cuotaRepo;
    private final GestionCobranzaRepository gestionRepo;
    private final UsuarioRepository         usuarioRepo;

    public RecuperacionesService(CreditoRepository creditoRepo,
                                 CuotaCreditoRepository cuotaRepo,
                                 GestionCobranzaRepository gestionRepo,
                                 UsuarioRepository usuarioRepo) {
        this.creditoRepo  = creditoRepo;
        this.cuotaRepo    = cuotaRepo;
        this.gestionRepo  = gestionRepo;
        this.usuarioRepo  = usuarioRepo;
    }

    // ════════════════════════════════════════
    // R1 — CONSULTA DE CARTERA MOROSA + KPIs
    // ════════════════════════════════════════

    /**
     * Devuelve la cartera morosa clasificada por bandas.
     * Cada registro incluye: crédito, cliente, días de mora, saldo, banda.
     */
    public Map<String, Object> getCarteraMorosa() {
        // Recalcula mora al momento de la consulta
        actualizarDiasMora();

        List<Credito> desembolsados = creditoRepo.findByEstadoIn(
            new String[]{"DESEMBOLSADO"}
        );

        List<Map<String, Object>> cartera = new ArrayList<>();
        Map<String, Integer> contadorBanda   = new LinkedHashMap<>();
        Map<String, Double>  saldoBanda      = new LinkedHashMap<>();

        for (String banda : List.of("AL_DIA","PREVENTIVA","TEMPRANA","TARDIA","JUDICIAL","CASTIGO")) {
            contadorBanda.put(banda, 0);
            saldoBanda.put(banda, 0.0);
        }

        for (Credito c : desembolsados) {
            Integer diasMora = cuotaRepo.maxDiasMora(c);
            int dias = diasMora != null ? diasMora : 0;
            String banda = banda(dias);

            if (dias > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("creditoId",        c.getId());
                item.put("numeroOperacion",   c.getNumeroOperacion());
                item.put("clienteNombre",     c.getCliente().getNombre());
                item.put("clienteEmail",      c.getCliente().getEmail());
                item.put("tipoProducto",      c.getTipoProducto().name());
                item.put("montoAprobado",     c.getMontoAprobado());
                item.put("diasMoraMax",       dias);
                item.put("banda",             banda);
                item.put("estado",            c.getEstado().name());
                cartera.add(item);

                contadorBanda.merge(banda, 1, Integer::sum);
                saldoBanda.merge(banda,
                    c.getMontoAprobado() != null ? c.getMontoAprobado().doubleValue() : 0.0,
                    Double::sum);
            }
        }

        // KPIs
        int totalCreditos    = desembolsados.size();
        int creditosEnMora   = cartera.size();
        double tasaMora      = totalCreditos > 0
            ? (double) creditosEnMora / totalCreditos * 100 : 0.0;

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalCreditos",    totalCreditos);
        kpis.put("creditosEnMora",   creditosEnMora);
        kpis.put("tasaMoraPct",      Math.round(tasaMora * 10.0) / 10.0);
        kpis.put("contadorPorBanda", contadorBanda);
        kpis.put("saldoPorBanda",    saldoBanda);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpis",   kpis);
        result.put("cartera", cartera);
        return result;
    }

    // ════════════════════════════════════════
    // R2 — REGISTRO DE GESTIONES DE COBRANZA
    // ════════════════════════════════════════

    @Transactional
    public Map<String, Object> registrarGestion(GestionCobranzaRequest req, String emailGestor) {
        Usuario gestor  = findUsuario(emailGestor);
        Credito credito = creditoRepo.findById(req.creditoId())
            .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado: " + req.creditoId()));

        CuotaCredito cuota = null;
        if (req.cuotaId() != null) {
            cuota = cuotaRepo.findById(req.cuotaId())
                .orElseThrow(() -> new IllegalArgumentException("Cuota no encontrada: " + req.cuotaId()));
        }

        Integer diasMora = cuotaRepo.maxDiasMora(credito);

        GestionCobranza gestion = new GestionCobranza();
        gestion.setCredito(credito);
        gestion.setCuota(cuota);
        gestion.setGestor(gestor);
        gestion.setTipoGestion(GestionCobranza.TipoGestion.valueOf(req.tipoGestion()));
        gestion.setResultado(GestionCobranza.ResultadoGestion.valueOf(req.resultado()));
        gestion.setDescripcion(req.descripcion());
        gestion.setFechaCompromisoPago(req.fechaCompromisoPago());
        gestion.setDiasMoraAlGestionar(diasMora != null ? diasMora : 0);

        GestionCobranza saved = gestionRepo.save(gestion);

        return Map.of(
            "id",             saved.getId(),
            "creditoId",      credito.getId(),
            "numeroOperacion", credito.getNumeroOperacion(),
            "tipoGestion",    saved.getTipoGestion().name(),
            "resultado",      saved.getResultado().name(),
            "diasMoraAlGestionar", saved.getDiasMoraAlGestionar(),
            "createdAt",      saved.getCreatedAt()
        );
    }

    /** Historial de gestiones para un crédito */
    public List<Map<String, Object>> getHistorialGestiones(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
            .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado: " + creditoId));

        return gestionRepo.findByCreditoOrderByCreatedAtDesc(credito)
            .stream()
            .map(g -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",          g.getId());
                m.put("tipoGestion", g.getTipoGestion().name());
                m.put("resultado",   g.getResultado().name());
                m.put("descripcion", g.getDescripcion());
                m.put("gestorNombre", g.getGestor().getNombre());
                m.put("diasMora",    g.getDiasMoraAlGestionar());
                m.put("fechaCompromisoPago", g.getFechaCompromisoPago());
                m.put("createdAt",   g.getCreatedAt());
                return m;
            })
            .collect(Collectors.toList());
    }

    // ════════════════════════════════════════
    // R3 — TRANSICIONES JUDICIAL / CASTIGO
    // ════════════════════════════════════════

    /**
     * Deriva un crédito a vía judicial (≥ 121 días mora).
     * Requiere rol JEFE_REGIONAL, RIESGOS o GERENCIA.
     */
    @Transactional
    public Map<String, Object> derivarJudicial(Long creditoId, String emailActor) {
        Usuario actor   = findUsuario(emailActor);
        assertRolJudicial(actor);

        Credito credito = creditoRepo.findById(creditoId)
            .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado: " + creditoId));

        if (credito.getEstado() != EstadoCredito.DESEMBOLSADO) {
            throw new IllegalStateException("Solo créditos DESEMBOLSADOS pueden derivarse a judicial.");
        }

        Integer diasMora = cuotaRepo.maxDiasMora(credito);
        if (diasMora == null || diasMora < DIAS_JUDICIAL) {
            throw new IllegalStateException(
                "Se requieren al menos " + DIAS_JUDICIAL + " días de mora para derivar a judicial. "
                + "Mora actual: " + (diasMora != null ? diasMora : 0) + " días.");
        }

        credito.setEstado(EstadoCredito.DESEMBOLSADO); // se mantiene DESEMBOLSADO pero con flag judicial
        credito.setComentarioEvaluacion(
            (credito.getComentarioEvaluacion() != null ? credito.getComentarioEvaluacion() + " | " : "")
            + "DERIVADO A JUDICIAL por " + actor.getNombre() + " — " + diasMora + " días mora."
        );
        credito.setUpdatedAt(java.time.LocalDateTime.now());
        creditoRepo.save(credito);

        return Map.of(
            "mensaje",     "Crédito derivado a vía judicial.",
            "creditoId",   creditoId,
            "diasMora",    diasMora,
            "derivadoPor", actor.getNombre()
        );
    }

    /**
     * Castiga un crédito (> 180 días mora).
     * Requiere rol GERENCIA.
     */
    @Transactional
    public Map<String, Object> castigarCredito(Long creditoId, String emailActor) {
        Usuario actor = findUsuario(emailActor);
        if (actor.getRol() != Usuario.Rol.GERENCIA && actor.getRol() != Usuario.Rol.ADMIN) {
            throw new SecurityException("Solo GERENCIA puede castigar créditos.");
        }

        Credito credito = creditoRepo.findById(creditoId)
            .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado: " + creditoId));

        Integer diasMora = cuotaRepo.maxDiasMora(credito);
        if (diasMora == null || diasMora <= DIAS_CASTIGO) {
            throw new IllegalStateException(
                "Se requieren más de " + DIAS_CASTIGO + " días de mora para castigar. "
                + "Mora actual: " + (diasMora != null ? diasMora : 0) + " días.");
        }

        credito.setEstado(EstadoCredito.RECHAZADO); // Estado CASTIGADO
        credito.setComentarioEvaluacion(
            (credito.getComentarioEvaluacion() != null ? credito.getComentarioEvaluacion() + " | " : "")
            + "CASTIGADO por " + actor.getNombre() + " — " + diasMora + " días mora."
        );
        credito.setUpdatedAt(java.time.LocalDateTime.now());
        creditoRepo.save(credito);

        return Map.of(
            "mensaje",     "Crédito castigado contablemente.",
            "creditoId",   creditoId,
            "diasMora",    diasMora,
            "castigadoPor", actor.getNombre()
        );
    }

    // ════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════

    private void actualizarDiasMora() {
        LocalDate hoy = LocalDate.now();
        List<CuotaCredito> pendientes = cuotaRepo.findVencidasSinPagar(hoy);
        for (CuotaCredito cuota : pendientes) {
            long dias = ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
            cuota.setDiasMora((int) dias);
            cuota.setEstadoCuota(CuotaCredito.EstadoCuota.VENCIDA);
            cuotaRepo.save(cuota);
        }
    }

    private String banda(int dias) {
        if (dias == 0)                           return "AL_DIA";
        if (dias <= 30)                          return "PREVENTIVA";
        if (dias <= 60)                          return "TEMPRANA";
        if (dias <= 120)                         return "TARDIA";
        if (dias <= 180)                         return "JUDICIAL";
        return "CASTIGO";
    }

    private void assertRolJudicial(Usuario actor) {
        if (actor.getRol() != Usuario.Rol.JEFE_REGIONAL
            && actor.getRol() != Usuario.Rol.RIESGOS
            && actor.getRol() != Usuario.Rol.GERENCIA
            && actor.getRol() != Usuario.Rol.ADMIN) {
            throw new SecurityException("Rol insuficiente para acción judicial.");
        }
    }

    private Usuario findUsuario(String email) {
        return usuarioRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
    }
}
