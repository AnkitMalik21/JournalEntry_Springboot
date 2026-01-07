package com.pro.Journal_Entry.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Utility - Handles JWT token creation and validation

 * SIMPLE EXPLANATION:
 * JWT is like a secure ID card that contains:
 * - Who you are (username)
 * - What you can do (roles)
 * - When it expires

 * It's signed with a secret key so nobody can fake it
 */
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    //Generation of JWT token for user

    public String generateToken(String username,Long userId,String role){
        Map<String,Object> claims = new HashMap<>();
        claims.put("userId",userId);
        claims.put("role",role);
        return createToken(claims,username);
    }

    //create token with claims

    public String createToken(Map<String,Object> claims,String subject){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    //Get signing key from secret
    private Key getSignKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /*
     extract username from token
    */

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    /*
     extract userId from token
    */

    public Long extractUserId(String token){
        return extractClaim(token,claims->claims.get("userId",Long.class));
    }

    /**
     * Extract expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim
     */
     public <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
         final Claims claims = extractAllClaims(token);
         return claimsResolver.apply(claims);
     }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

}

