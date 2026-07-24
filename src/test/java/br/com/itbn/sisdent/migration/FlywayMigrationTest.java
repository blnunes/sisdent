package br.com.itbn.sisdent.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class FlywayMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:legacy-migration;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @Test
    void baselinesAndMigratesAnExistingDatabaseWithoutLosingPatientData()
            throws Exception {
        Flyway.configure()
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(URL, "sa", "");
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
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(URL, "sa", "");
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
}
