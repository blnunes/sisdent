package br.com.itbn.sisdent.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

class V4PermissionMigrationTest {

    @Test
    void expandsLegacyAndAdministratorPermissionsWithoutChangingTheirScope() throws Exception {
        String url = "jdbc:h2:mem:v4_permissions_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        migrate(url, "3");

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            long readUser = insertUser(connection, "legacy-read", "USER");
            grant(connection, readUser, "READ");
            long maintainUser = insertUser(connection, "legacy-maintain", "USER");
            grant(connection, maintainUser, "CREATE");
            grant(connection, maintainUser, "MANAGE_USERS");
            long specificUser = insertUser(connection, "specific-read", "USER");
            grant(connection, specificUser, "READ_PATIENTS");
            long adminUser = insertUser(connection, "administrator", "ADMIN");

            migrate(url, "4");

            assertThat(permissions(connection, readUser)).containsExactly(
                    "READ_ADDRESSES",
                    "READ_COUNTRIES",
                    "READ_PATIENTS",
                    "READ_SPECIALITIES",
                    "READ_STATES",
                    "READ_USERS");
            assertThat(permissions(connection, maintainUser)).containsExactly("MAINTAIN_USERS");
            assertThat(permissions(connection, specificUser)).containsExactly("READ_PATIENTS");
            assertThat(permissions(connection, adminUser)).hasSize(12);
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

    private long insertUser(Connection connection, String identificationNumber, String role)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app_users (identification_type, identification_number, password, role) VALUES ('NATIONAL_ID', ?, 'password', ?)",
                new String[] {"id"})) {
            statement.setString(1, identificationNumber);
            statement.setString(2, role);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void grant(Connection connection, long userId, String permission) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO user_permissions (user_id, permission) VALUES (?, ?)")) {
            statement.setLong(1, userId);
            statement.setString(2, permission);
            statement.executeUpdate();
        }
    }

    private List<String> permissions(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT permission FROM user_permissions WHERE user_id = ? ORDER BY permission")) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                List<String> permissions = new java.util.ArrayList<>();
                while (rows.next()) {
                    permissions.add(rows.getString("permission"));
                }
                return permissions;
            }
        }
    }
}
