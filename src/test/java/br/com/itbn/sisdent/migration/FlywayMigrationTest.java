package br.com.itbn.sisdent.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
                    .isEqualTo("NATIONAL_ID");
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
}
