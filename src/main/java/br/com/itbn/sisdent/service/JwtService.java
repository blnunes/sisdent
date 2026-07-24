package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.TokenResponse;
import br.com.itbn.sisdent.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${sisdent.security.jwt.expiration-seconds}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    public TokenResponse issue(User user) {
        Instant issuedAt = Instant.now();
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + user.getRole().name());
        user.getPermissions().stream()
                .map(Enum::name)
                .forEach(authorities::add);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sisdent")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expirationSeconds))
                .subject(user.getIdentificationType() + ":" + user.getIdentificationNumber())
                .claim("userId", user.getId())
                .claim("authorities", authorities)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", expirationSeconds);
    }
}
