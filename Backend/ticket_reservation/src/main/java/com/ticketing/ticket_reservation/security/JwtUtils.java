package com.ticketing.ticket_reservation.security;

import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.ticketing.ticket_reservation.constants.ApplicationConstants;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component 
public class JwtUtils {
    @Value ("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String email, String role){
        return Jwts.builder()
                    .setSubject(email)
                    .claim(ApplicationConstants.CLAIM_ROLE,role)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(getSigningKey(),SignatureAlgorithm.HS256)
                    .compact();
    }

    public String extractEmail(String token){
        return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
    }

    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch(JwtException | IllegalArgumentException e){
            return false;
        }
    }
}
