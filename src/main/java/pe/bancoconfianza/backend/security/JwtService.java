package pe.bancoconfianza.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Valida JWTs emitidos por Supabase Auth (RS256).
 * Descarga las claves públicas desde el endpoint JWKS de Supabase y las cachea.
 *
 * También puede validar JWTs HS256 propios (para compatibilidad).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${app.jwt.secret:MiClaveSecretaSuperSeguraParaBCP2026XYZ}")
    private String jwtSecret;

    // Cache de claves públicas de Supabase: kid → PublicKey
    private final Map<String, PublicKey> jwksCache = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    /* ── Extraer email/subject del token ── */
    public String extractUsername(String token) {
        try {
            // Intentar primero como JWT de Supabase (RS256)
            if (isSupabaseToken(token)) {
                return extractClaimSupabase(token, "email");
            }
            // Fallback: JWT propio HS256
            return extractClaimHs256(token, Claims::getSubject);
        } catch (Exception e) {
            log.warn("No se pudo extraer username del token: {}", e.getMessage());
            return null;
        }
    }

    /* ── Validar token ── */
    public boolean isTokenValid(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            if (username == null) return false;

            if (isSupabaseToken(token)) {
                return username.equals(userDetails.getUsername()) && validateSupabaseToken(token);
            }
            return username.equals(userDetails.getUsername()) && !isExpiredHs256(token);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    /* ── Detectar si es token de Supabase (RS256) ── */
    private boolean isSupabaseToken(String token) {
        try {
            String headerJson = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                StandardCharsets.UTF_8
            );
            JsonNode header = mapper.readTree(headerJson);
            return "RS256".equals(header.path("alg").asText());
        } catch (Exception e) {
            return false;
        }
    }

    /* ── Validar JWT de Supabase con JWKS ── */
    private boolean validateSupabaseToken(String token) {
        try {
            String kid = extractKid(token);
            PublicKey publicKey = getPublicKey(kid);
            if (publicKey == null) return false;

            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Supabase JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    /* ── Extraer claim de JWT de Supabase ── */
    private String extractClaimSupabase(String token, String claimName) {
        try {
            String kid = extractKid(token);
            PublicKey publicKey = getPublicKey(kid);
            if (publicKey == null) {
                return extractClaimUnsafe(token, claimName);
            }
            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            String email = claims.get("email", String.class);
            return email != null ? email : claims.getSubject();
        } catch (Exception e) {
            log.warn("Error extrayendo claim de Supabase JWT: {}", e.getMessage());
            return extractClaimUnsafe(token, claimName);
        }
    }

    /* ── Extraer claim sin verificar firma (solo para dev sin JWKS) ── */
    private String extractClaimUnsafe(String token, String claimName) {
        try {
            String payloadJson = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8
            );
            JsonNode payload = mapper.readTree(payloadJson);
            String value = payload.path(claimName).asText(null);
            if (value == null || value.isEmpty()) {
                value = payload.path("sub").asText(null);
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    /* ── Obtener clave pública de Supabase JWKS ── */
    private PublicKey getPublicKey(String kid) {
        if (jwksCache.containsKey(kid)) return jwksCache.get(kid);
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            log.warn("supabase.url no configurado — no se puede verificar firma RS256");
            return null;
        }
        try {
            String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUrl))
                .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode jwks = mapper.readTree(response.body());
            for (JsonNode key : jwks.path("keys")) {
                String keyId = key.path("kid").asText();
                PublicKey pk = buildRsaPublicKey(
                    key.path("n").asText(),
                    key.path("e").asText()
                );
                jwksCache.put(keyId, pk);
            }
            return jwksCache.get(kid);
        } catch (Exception e) {
            log.error("Error descargando JWKS de Supabase: {}", e.getMessage());
            return null;
        }
    }

    private PublicKey buildRsaPublicKey(String n, String e) throws Exception {
        BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private String extractKid(String token) {
        try {
            String headerJson = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                StandardCharsets.UTF_8
            );
            return mapper.readTree(headerJson).path("kid").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    /* ── Generar JWT HS256 propio ── */
    public String generateToken(String email, long expirationMs) {
        return Jwts.builder()
            .subject(email)
            .claim("email", email)
            .issuedAt(new java.util.Date())
            .expiration(new java.util.Date(System.currentTimeMillis() + expirationMs))
            .signWith(hs256Key())
            .compact();
    }

    /* ── HS256 (JWT propio — compatibilidad) ── */
    private SecretKey hs256Key() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        byte[] key32 = new byte[32];
        System.arraycopy(keyBytes, 0, key32, 0, Math.min(keyBytes.length, 32));
        return Keys.hmacShaKeyFor(key32);
    }

    private <T> T extractClaimHs256(String token, Function<Claims, T> resolver) {
        return resolver.apply(
            Jwts.parser().verifyWith(hs256Key()).build()
                .parseSignedClaims(token).getPayload()
        );
    }

    private boolean isExpiredHs256(String token) {
        return extractClaimHs256(token, Claims::getExpiration).before(new java.util.Date());
    }
}
