package pe.bancoconfianza.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * Endpoint de diagnóstico — Verifica estado de la BD
 * GET /api/public/diagnostics
 */
@RestController
@RequestMapping("/api/public/diagnostics")
public class DiagnosticsController {

    @Autowired
    private DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> diagnose() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Connection conn = dataSource.getConnection();
            DatabaseMetaData metadata = conn.getMetaData();

            result.put("status", "✅ Conectado a BD");
            result.put("database", metadata.getDatabaseProductName());
            result.put("version", metadata.getDatabaseProductVersion());
            result.put("url", metadata.getURL());
            result.put("user", metadata.getUserName());

            // Listar tablas
            ResultSet tables = metadata.getTables(null, "public", "%", new String[]{"TABLE"});
            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }
            result.put("tables_found", tableNames.size());
            result.put("tables", tableNames);

            conn.close();

        } catch (Exception e) {
            result.put("status", "❌ Error conectando a BD");
            result.put("error", e.getMessage());
            result.put("exception", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(result);
    }
}
