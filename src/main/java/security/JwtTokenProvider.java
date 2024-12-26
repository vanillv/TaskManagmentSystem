package security;

import exceptions.InvalidJwtTokenException;
import io.jsonwebtoken.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import java.util.Date;
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${jwt.secret.expiration}")
    private long jwtExpirationInMs;

    private final UserDetailsService userDetailsService;
    @Autowired
    public JwtTokenProvider(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(getUsernameFromToken(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
    public String getUsernameFromToken(String token) {
        Jws<Claims> claimsJws = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
        return claimsJws.getBody().getSubject();
    }
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token);
            return true;  // If no errors, the token is valid
        } catch (MalformedJwtException ex) {
            throw new InvalidJwtTokenException("Invalid JWT token.");
        } catch (ExpiredJwtException ex) {
            throw new InvalidJwtTokenException("Expired JWT token.");
        } catch (UnsupportedJwtException ex) {
            throw new InvalidJwtTokenException("Unsupported JWT token.");
        } catch (IllegalArgumentException ex) {
            throw new InvalidJwtTokenException("JWT claims string is empty.");
        }
    }
}

