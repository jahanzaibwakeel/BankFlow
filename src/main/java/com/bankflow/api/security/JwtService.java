package com.bankflow.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final String issuer;
    private final long accessTokenMinutes;
    private final SecretKey key;

    public JwtService(
        @Value("${bankflow.jwt.issuer}") String issuer,
        @Value("${bankflow.jwt.secret}") String secret,
        @Value("${bankflow.jwt.access-token-minutes}") long accessTokenMinutes
    ) {
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(BankFlowPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(issuer)
            .subject(principal.getUsername())
            .claim("uid", principal.id().toString())
            .claim("roles", principal.roles().stream().map(Enum::name).toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact();
    }

    public String subject(String token) {
        return claims(token).getSubject();
    }

    public Claims claims(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer).build().parseSignedClaims(token).getPayload();
    }
}
