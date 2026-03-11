package net.fosterlink.fosterlinkbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    /** JWT claim name for auth token version (session invalidation). */
    public static final String CLAIM_TOKEN_VERSION = "tv";

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExp;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generates an access token for a username with the given token version.
     * Used by the refresh endpoint where we have already validated the refresh token
     * and identified the user without re-authenticating their password.
     */
    public String generateTokenForUsername(String username, int authTokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExp);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_TOKEN_VERSION, authTokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        int authTokenVersion = 0;
        if (authentication.getPrincipal() instanceof net.fosterlink.fosterlinkbackend.models.auth.LoggedInUser loggedIn) {
            authTokenVersion = loggedIn.getAuthTokenVersion();
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExp);

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_TOKEN_VERSION, authTokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Returns the token version from the JWT claim, or 0 if missing (e.g. legacy tokens).
     */
    public int getTokenVersionFromJWT(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer tv = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
            return tv != null ? tv : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

}
