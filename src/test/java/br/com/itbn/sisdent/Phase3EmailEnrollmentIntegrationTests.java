package br.com.itbn.sisdent;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.AccountEmailClaim;
import br.com.itbn.sisdent.model.EmailClaimType;
import br.com.itbn.sisdent.model.IdentificationType;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.model.Role;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.AccountEmailClaimRepository;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import br.com.itbn.sisdent.repository.UserRepository;
import br.com.itbn.sisdent.service.InMemoryEmailVerificationDelivery;
import br.com.itbn.sisdent.service.IdentificationNumbers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class Phase3EmailEnrollmentIntegrationTests {
    private static final String PASSWORD = "phase3-password";

    @Autowired MockMvc mockMvc;
    @Autowired PersonRepository personRepository;
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountEmailClaimRepository emailClaimRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired InMemoryEmailVerificationDelivery delivery;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired JsonMapper jsonMapper;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void legacyUserEnrollsWithoutSecretDisclosureAndCutoverIsPermanent() throws Exception {
        Account account = saveMigratingAccount("cutover");
        String legacyToken = legacyLogin(account, PASSWORD);

        mockMvc.perform(get("/api/session").header("Authorization", bearer(legacyToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailMigrationRequired").value(true));

        String enrollmentResponse = startEnrollment(legacyToken, "  New.User@Example.COM  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHALLENGE_SENT"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String secret = delivery.latestFor(account.getId()).orElseThrow().secret();
        assertThat(enrollmentResponse).doesNotContain(secret);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT secret_hash FROM email_verification_challenges WHERE account_id = ?",
                String.class, account.getId())).doesNotContain(secret);

        verify(secret).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.accountId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        mockMvc.perform(get("/api/session").header("Authorization", bearer(legacyToken)))
                .andExpect(status().isUnauthorized());
        emailLogin("new.user@example.com", PASSWORD)
                .andExpect(status().isOk());
        legacyLoginRequest(account, PASSWORD)
                .andExpect(status().isUnauthorized());

        Account verified = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(verified.isEmailVerified()).isTrue();
        assertThat(verified.isEmailMigrationRequired()).isFalse();
        assertThat(verified.getPendingEmail()).isNull();
        assertThat(verified.getEmail()).isEqualTo("new.user@example.com");

        verify(secret).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_OR_EXPIRED"));
    }

    @Test
    void supersededExpiredMalformedAndUnknownChallengesShareGenericFailure() throws Exception {
        Account account = saveMigratingAccount("challenge-states");
        String token = legacyLogin(account, PASSWORD);
        startEnrollment(token, "first-" + UUID.randomUUID() + "@example.com")
                .andExpect(status().isOk());
        String superseded = delivery.latestFor(account.getId()).orElseThrow().secret();

        allowImmediateResend(account);
        startEnrollment(token, "second-" + UUID.randomUUID() + "@example.com")
                .andExpect(status().isOk());
        String active = delivery.latestFor(account.getId()).orElseThrow().secret();

        assertGenericFailure(superseded);
        assertGenericFailure("malformed");
        assertGenericFailure(UUID.randomUUID() + UUID.randomUUID().toString());

        jdbcTemplate.update("""
                UPDATE email_verification_challenges
                SET expires_at = ?
                WHERE secret_hash = ?
                """, Instant.now().minusSeconds(1), sha256(active));
        assertGenericFailure(active);
        assertThat(accountRepository.findById(account.getId()).orElseThrow()
                .isEmailMigrationRequired()).isTrue();
    }

    @Test
    void databaseClaimsPreventDuplicatePendingAndVerifiedEmailUse() throws Exception {
        Account first = saveMigratingAccount("claim-first");
        Account second = saveMigratingAccount("claim-second");
        String firstToken = legacyLogin(first, PASSWORD);
        String secondToken = legacyLogin(second, PASSWORD);
        String candidate = " Shared." + UUID.randomUUID() + "@Example.COM ";

        startEnrollment(firstToken, candidate).andExpect(status().isOk());
        startEnrollment(secondToken, candidate.toUpperCase(Locale.ROOT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email enrollment unavailable"))
                .andExpect(jsonPath("$.detail").value("Email enrollment could not be started."));

        Account verified = saveVerifiedAccount("claimed-" + UUID.randomUUID() + "@example.com");
        startEnrollment(secondToken, verified.getEmail().toUpperCase(Locale.ROOT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email enrollment could not be started."));
    }

    @Test
    void resendCooldownIsServerEnforcedAndAccountScoped() throws Exception {
        Account first = saveMigratingAccount("throttle-first");
        Account second = saveMigratingAccount("throttle-second");
        String firstToken = legacyLogin(first, PASSWORD);
        String secondToken = legacyLogin(second, PASSWORD);
        startEnrollment(firstToken, "first-" + UUID.randomUUID() + "@example.com")
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/account/email-enrollment/resend")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        mockMvc.perform(post("/api/account/email-enrollment/resend")
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isConflict());
        startEnrollment(secondToken, "second-" + UUID.randomUUID() + "@example.com")
                .andExpect(status().isOk());
    }

    @Test
    void verifiedAccountsIncludingPlatformAdministratorsCannotReplaceTheirEmail() throws Exception {
        String adminToken = emailLogin("admin@sisdent.local", "admin")
                .andReturn().getResponse().getContentAsString();
        String accessToken = jsonMapper.readTree(adminToken).get("accessToken").asText();

        startEnrollment(accessToken, "replacement-" + UUID.randomUUID() + "@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accountId").doesNotExist())
                .andExpect(jsonPath("$.memberships").doesNotExist());
    }

    @Test
    void concurrentAccountsCannotReserveTheSameNormalizedEmail() throws Exception {
        Account first = saveMigratingAccount("concurrent-first");
        Account second = saveMigratingAccount("concurrent-second");
        String candidate = "concurrent-" + UUID.randomUUID() + "@example.com";
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var firstAttempt = executor.submit(
                    () -> reserveClaim(transaction, first.getId(), candidate));
            var secondAttempt = executor.submit(
                    () -> reserveClaim(transaction, second.getId(), candidate.toUpperCase(Locale.ROOT)));

            assertThat(java.util.List.of(firstAttempt.get(), secondAttempt.get()))
                    .containsExactlyInAnyOrder(true, false);
        }
    }

    private Account saveMigratingAccount(String label) {
        String normalizedLabel = IdentificationNumbers.normalize(label + "-" + UUID.randomUUID());
        User user = userRepository.save(new User(
                IdentificationType.PASSPORT,
                normalizedLabel,
                passwordEncoder.encode(PASSWORD),
                Role.USER,
                Role.USER.defaultPermissions()));
        Person person = personRepository.save(new Person("Phase 3 " + label));
        String syntheticEmail = "passport." + normalizedLabel + "@legacy.sisdent.invalid";
        Account account = accountRepository.save(new Account(
                person, user, syntheticEmail, user.getPassword(), false, true));
        emailClaimRepository.save(new AccountEmailClaim(
                account, syntheticEmail, EmailClaimType.VERIFIED));
        return account;
    }

    private Account saveVerifiedAccount(String email) {
        Person person = personRepository.save(new Person(email));
        Account account = accountRepository.save(new Account(
                person, null, email, passwordEncoder.encode(PASSWORD), false, false));
        emailClaimRepository.save(new AccountEmailClaim(account, email, EmailClaimType.VERIFIED));
        return account;
    }

    private String legacyLogin(Account account, String password) throws Exception {
        String response = legacyLoginRequest(account, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions legacyLoginRequest(
            Account account, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identificationType":"%s","identificationNumber":"%s","password":"%s"}
                        """.formatted(
                        account.getLegacyUser().getIdentificationType(),
                        account.getLegacyUser().getIdentificationNumber(),
                        password)));
    }

    private org.springframework.test.web.servlet.ResultActions emailLogin(
            String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private org.springframework.test.web.servlet.ResultActions startEnrollment(
            String token, String email) throws Exception {
        return mockMvc.perform(post("/api/account/email-enrollment")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s"}
                        """.formatted(email)));
    }

    private org.springframework.test.web.servlet.ResultActions verify(String secret) throws Exception {
        return mockMvc.perform(post("/api/auth/email-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s"}
                        """.formatted(secret)));
    }

    private void assertGenericFailure(String secret) throws Exception {
        verify(secret).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID_OR_EXPIRED"))
                .andExpect(jsonPath("$.accountId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    private void allowImmediateResend(Account account) {
        jdbcTemplate.update("""
                UPDATE email_verification_challenges
                SET created_at = ?
                WHERE account_id = ?
                """, Instant.now().minusSeconds(120), account.getId());
    }

    private String sha256(String secret) throws Exception {
        return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private boolean reserveClaim(
            TransactionTemplate transaction, Long accountId, String candidate) {
        try {
            transaction.executeWithoutResult(status -> {
                Account account = accountRepository.findById(accountId).orElseThrow();
                emailClaimRepository.saveAndFlush(new AccountEmailClaim(
                        account, candidate, EmailClaimType.PENDING));
            });
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            return false;
        }
    }
}
