package com.aztu.support.security;

import com.aztu.support.config.AppProperties;
import com.aztu.support.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates stateless JWT access tokens (HS256).
 * Refresh tokens are opaque and handled separately in the auth service.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long accessTokenMinutes;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.getJwt().getIssuer();
        this.accessTokenMinutes = props.getJwt().getAccessTokenExpirationMinutes();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(issuer)
                .claim("email", user.getEmail())
                .claim("role", user.roleName() != null ? user.roleName().name() : null)
                .claim("name", user.fullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenMinutes * 60;
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getPayload().getSubject());
    }
}
