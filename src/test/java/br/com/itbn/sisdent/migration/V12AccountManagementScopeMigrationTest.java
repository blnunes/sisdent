package br.com.itbn.sisdent.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class V12AccountManagementScopeMigrationTest {

    @Test
    void assignsTheEarliestEligibleOrganizationOnlyToActiveOrganizationAdministrators()
            throws Exception {
        String url = "jdbc:h2:mem:v12_scope_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        migrate(url, "11");

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            removeLegacyRoleConstraint(connection);
            long firstOrganization = insertOrganization(connection, "First");
            long secondOrganization = insertOrganization(connection, "Second");
            long clinicUnit = insertClinicUnit(connection, firstOrganization);
            long eligibleAccount = insertAccount(connection, "eligible");
            long inactiveAccount = insertAccount(connection, "inactive");
            long clinicAccount = insertAccount(connection, "clinic");
            long managerAccount = insertAccount(connection, "manager");
            grantMembership(connection, eligibleAccount, firstOrganization, null, "ORGANIZATION_ADMIN", true);
            grantMembership(connection, eligibleAccount, secondOrganization, null, "ORGANIZATION_ADMIN", true);
            grantMembership(connection, inactiveAccount, firstOrganization, null, "ORGANIZATION_ADMIN", false);
            grantMembership(connection, clinicAccount, firstOrganization, clinicUnit, "ORGANIZATION_ADMIN", true);
            grantMembership(connection, managerAccount, firstOrganization, null, "MANAGER", true);

            migrate(url, "12");

            assertThat(managementOrganization(connection, eligibleAccount)).isEqualTo(firstOrganization);
            assertThat(managementOrganization(connection, inactiveAccount)).isNull();
            assertThat(managementOrganization(connection, clinicAccount)).isNull();
            assertThat(managementOrganization(connection, managerAccount)).isNull();
        }
    }

    private void migrate(String url, String targetVersion) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion(targetVersion))
                .load()
                .migrate();
    }

    private long insertOrganization(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO organizations (name) VALUES (?)", new String[] {"id"})) {
            statement.setString(1, name);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private void removeLegacyRoleConstraint(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE memberships DROP CONSTRAINT ck_memberships_role")) {
            statement.executeUpdate();
        }
    }

    private long insertAccount(Connection connection, String emailPrefix) throws SQLException {
        long personId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO persons (display_name) VALUES (?)", new String[] {"id"})) {
            statement.setString(1, emailPrefix);
            statement.executeUpdate();
            personId = generatedId(statement);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO accounts (person_id, email, password) VALUES (?, ?, 'password')",
                new String[] {"id"})) {
            statement.setLong(1, personId);
            statement.setString(2, emailPrefix + "@example.test");
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private long insertClinicUnit(Connection connection, long organizationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO clinic_units (organization_id, name) VALUES (?, 'Clinic')",
                new String[] {"id"})) {
            statement.setLong(1, organizationId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private void grantMembership(
            Connection connection,
            long accountId,
            long organizationId,
            Long clinicUnitId,
            String role,
            boolean active)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO memberships (account_id, organization_id, clinic_unit_id, role, active) VALUES (?, ?, ?, ?, ?)")) {
            statement.setLong(1, accountId);
            statement.setLong(2, organizationId);
            if (clinicUnitId == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, clinicUnitId);
            }
            statement.setString(4, role);
            statement.setBoolean(5, active);
            statement.executeUpdate();
        }
    }

    private Long managementOrganization(Connection connection, long accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT account_management_organization_id FROM accounts WHERE id = ?")) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                long organizationId = result.getLong(1);
                return result.wasNull() ? null : organizationId;
            }
        }
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            keys.next();
            return keys.getLong(1);
        }
    }
}
