package com.apiatlas.security;

import com.apiatlas.config.AtlasProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final Duration expiration;

    public JwtService(AtlasProperties props) {
        var jwt = props.security().jwt();
        byte[] secret = jwt.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("apiatlas.security.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.issuer = jwt.issuer();
        this.expiration = jwt.expiration();
    }

    public Duration expiration() {
        return expiration;
    }

    public String generate(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** @return the subject if the token is valid, otherwise empty. */
    public Optional<String> validate(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).requireIssuer(issuer).build().parseSignedClaims(token).getPayload();
            return Optional.ofNullable(c.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
