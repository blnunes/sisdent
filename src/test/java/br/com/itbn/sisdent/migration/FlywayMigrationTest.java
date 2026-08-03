package br.com.itbn.sisdent.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class FlywayMigrationTest {

    private static String databaseUrl(String suffix) {
        return "jdbc:h2:mem:legacy-migration-" + UUID.randomUUID() + "-" + suffix
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=false";
    }

    @Test
    void baselinesAndMigratesAnExistingDatabaseWithoutLosingPatientData()
            throws Exception {
        String url = databaseUrl("v1");
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO states (id, name, abbreviation)
                    VALUES (1, 'Legacy State', 'LS')
                    """);
            statement.execute("""
                    INSERT INTO addresses (
                        id, street, district, postal_code, state_id
                    ) VALUES (
                        1, 'Legacy Street', 'Legacy District', '12345678', 1
                    )
                    """);
            statement.execute("""
                    INSERT INTO patients (
                        id, name, birth_date, active, gender, tax_id, address_id
                    ) VALUES (
                        1, 'Legacy Patient', DATE '1980-01-01', TRUE,
                        'OTHER', '12345678901', 1
                    )
                    """);
            statement.execute("DROP TABLE \"flyway_schema_history\"");
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT p.name, p.identification_type,
                            p.identification_number, c.code
                     FROM patients p
                     JOIN countries c ON c.id = p.nationality_country_id
                     WHERE p.id = 1
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("name")).isEqualTo("Legacy Patient");
            assertThat(result.getString("identification_type"))
                    .isEqualTo("NATIONAL_ID_CARD");
            assertThat(result.getString("identification_number"))
                    .isEqualTo("US12345678901");
            assertThat(result.getString("code")).isEqualTo("US");
        }
    }

    @Test
    void migratesLegacyUserPermissionsToFeatureBasedValues() throws Exception {
        String url = databaseUrl("v2");
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target("3")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_users (
                        id, identification_type, identification_number,
                        password, role, active
                    ) VALUES (
                        1, 'NATIONAL_ID', 'LEGACY_ADMIN',
                        'password', 'ADMIN', TRUE
                    )
                    """);
            statement.execute("""
                    INSERT INTO user_permissions (user_id, permission) VALUES
                        (1, 'READ'),
                        (1, 'CREATE'),
                        (1, 'UPDATE'),
                        (1, 'DELETE'),
                        (1, 'MANAGE_USERS'),
                        (1, 'CREATE_PATIENTS'),
                        (1, 'READ_SPECIALITIES'),
                        (1, 'DELETE_SPECIALITIES')
                    """);
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target("4")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT DISTINCT permission FROM user_permissions WHERE user_id = 1 ORDER BY permission")) {
            ArrayList<String> permissions = new ArrayList<>();
            while (result.next()) {
                permissions.add(result.getString("permission"));
            }
            assertThat(permissions).containsExactlyInAnyOrder(
                    "MAINTAIN_ADDRESSES",
                    "MAINTAIN_COUNTRIES",
                    "MAINTAIN_PATIENTS",
                    "MAINTAIN_SPECIALITIES",
                    "MAINTAIN_STATES",
                    "MAINTAIN_USERS",
                    "READ_ADDRESSES",
                    "READ_COUNTRIES",
                    "READ_PATIENTS",
                    "READ_SPECIALITIES",
                    "READ_STATES",
                    "READ_USERS"
            );
        }
    }

    @Test
    void renamesStatePermissionsWithoutChangingAssignedAccess() throws Exception {
        String url = databaseUrl("v7");
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target("6")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_users (
                        id, identification_type, identification_number,
                        password, role, active, created_at, updated_at,
                        created_by, updated_by, version
                    ) VALUES (
                        100, 'NATIONAL_ID', 'DIVISION_MANAGER',
                        'password', 'MANAGER', TRUE, CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, 'migration-test', 'migration-test', 0
                    )
                    """);
            statement.execute("""
                    INSERT INTO user_permissions (user_id, permission) VALUES
                        (100, 'READ_STATES'),
                        (100, 'MAINTAIN_STATES')
                    """);
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT permission FROM user_permissions WHERE user_id = 100 ORDER BY permission")) {
            ArrayList<String> permissions = new ArrayList<>();
            while (result.next()) {
                permissions.add(result.getString("permission"));
            }
            assertThat(permissions).containsExactlyInAnyOrder(
                    "MAINTAIN_ADMINISTRATIVE_DIVISIONS",
                    "READ_ADMINISTRATIVE_DIVISIONS");
        }
    }

    @Test
    void migratesLegacyUsersToUniqueGlobalAccountsWithoutRemovingLegacyLoginData() throws Exception {
        String url = databaseUrl("v8-accounts");
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target("7")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_users (
                        id, identification_type, identification_number, password, role, active,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES
                        (201, 'NATIONAL_ID', 'LEGACY-ONE', 'encoded-one', 'ADMIN', TRUE,
                         CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0),
                        (202, 'PASSPORT', 'LEGACY-TWO', 'encoded-two', 'USER', TRUE,
                         CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)
                    """);
            statement.execute("""
                    INSERT INTO addresses (
                        id, street, district, postal_code, administrative_division_id,
                        country_id, city
                    ) VALUES (
                        301, 'Phase 2 Street', 'Phase 2 District', '1000-001', NULL,
                        (SELECT id FROM countries WHERE code = 'US'), 'Lisbon'
                    )
                    """);
            statement.execute("""
                    INSERT INTO patients (
                        id, name, birth_date, active, gender, tax_id, address_id,
                        identification_type, identification_number,
                        nationality_country_id, document_issuer_country_id
                    ) VALUES (
                        301, 'Preserved Phase 2 Patient', DATE '1990-01-01', TRUE,
                        'OTHER', NULL, 301, 'NATIONAL_ID_CARD', 'PHASE2-PATIENT-301',
                        (SELECT id FROM countries WHERE code = 'US'),
                        (SELECT id FROM countries WHERE code = 'US')
                    )
                    """);
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT a.email, a.password, a.email_migration_required,
                            a.email_verified,
                            u.identification_number
                     FROM accounts a
                     JOIN app_users u ON u.id = a.legacy_user_id
                     ORDER BY u.id
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("email")).isEqualTo("national_id.legacy-one@legacy.sisdent.invalid");
            assertThat(result.getString("password")).isEqualTo("encoded-one");
            assertThat(result.getBoolean("email_migration_required")).isTrue();
            assertThat(result.getBoolean("email_verified")).isFalse();
            assertThat(result.getString("identification_number")).isEqualTo("LEGACY-ONE");
            assertThat(result.next()).isTrue();
            assertThat(result.getString("email")).isEqualTo("passport.legacy-two@legacy.sisdent.invalid");
        }

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT
                         (SELECT COUNT(*) FROM app_users) AS users_count,
                         (SELECT COUNT(*) FROM accounts) AS accounts_count,
                         (SELECT COUNT(*) FROM memberships) AS memberships_count,
                         (SELECT COUNT(*) FROM account_email_claims) AS claims_count,
                         (SELECT COUNT(*) FROM patient_organization_links
                          WHERE patient_id = 301) AS patient_links_count
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt("users_count")).isEqualTo(2);
            assertThat(result.getInt("accounts_count")).isEqualTo(2);
            assertThat(result.getInt("memberships_count")).isEqualTo(2);
            assertThat(result.getInt("claims_count")).isEqualTo(2);
            assertThat(result.getInt("patient_links_count")).isEqualTo(1);
        }
    }
}
