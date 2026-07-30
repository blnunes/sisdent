package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.TokenResponse;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.repository.MembershipRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final MembershipRepository membershipRepository;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            MembershipRepository membershipRepository,
            @Value("${sisdent.security.jwt.expiration-seconds}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.membershipRepository = membershipRepository;
        this.expirationSeconds = expirationSeconds;
    }

    public TokenResponse issue(Account account) {
        Instant issuedAt = Instant.now();
        List<String> authorities = new ArrayList<>();
        if (account.isPlatformAdministrator()) {
            authorities.add("ROLE_PLATFORM_ADMIN");
        }
        if (account.getLegacyUser() != null) {
            authorities.add("ROLE_" + account.getLegacyUser().getRole().name());
            account.getLegacyUser().getPermissions().stream()
                    .map(Enum::name)
                    .forEach(authorities::add);
        }
        List<Map<String, Object>> memberships = membershipRepository
                .findAllByAccount_IdAndActiveTrue(account.getId()).stream()
                .map(this::membershipClaim)
                .toList();

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("sisdent")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expirationSeconds))
                .subject(account.getGlobalId().toString())
                .claim("accountId", account.getGlobalId().toString())
                .claim("email", account.getEmail())
                .claim("emailMigrationRequired", account.isEmailMigrationRequired())
                .claim("platformAdministrator", account.isPlatformAdministrator())
                .claim("memberships", memberships)
                .claim("authorities", authorities);
        if (account.getLegacyUser() != null) {
            claimsBuilder.claim("userId", account.getLegacyUser().getId());
        }
        JwtClaimsSet claims = claimsBuilder.build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", expirationSeconds);
    }

    private Map<String, Object> membershipClaim(Membership membership) {
        Map<String, Object> claim = new LinkedHashMap<>();
        claim.put("id", membership.getGlobalId().toString());
        claim.put("organizationId", membership.getOrganization().getGlobalId().toString());
        if (membership.getClinicUnit() != null) {
            claim.put("clinicUnitId", membership.getClinicUnit().getGlobalId().toString());
        }
        claim.put("role", membership.getRole().name());
        return claim;
    }
}
