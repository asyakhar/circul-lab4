package ru.ifmo.soa.booking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class BookingStore {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS bookings (
                ticket_id INTEGER PRIMARY KEY,
                person_id INTEGER NOT NULL,
                price REAL NOT NULL
            )
            """;

    private final String jdbcUrl;

    @Autowired
    public BookingStore(
            @Value("${booking.store.path:.run/booking.db}")
            String databasePath
    ) {
        this(Path.of(databasePath));
    }

    BookingStore(Path databasePath) {
        Path absolutePath = databasePath.toAbsolutePath().normalize();
        createParentDirectory(absolutePath);
        jdbcUrl = "jdbc:sqlite:" + absolutePath;
        initializeSchema();
    }

    public boolean sell(long ticketId, long personId, double price) {
        String sql = """
                INSERT OR IGNORE INTO bookings(ticket_id, person_id, price)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            statement.setLong(2, personId);
            statement.setDouble(3, price);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw storageFailure("Не удалось сохранить продажу билета.", exception);
        }
    }

    public int cancelByPerson(long personId) {
        String sql = "DELETE FROM bookings WHERE person_id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, personId);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw storageFailure("Не удалось отменить бронирования.", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
            statement.execute("PRAGMA journal_mode = WAL");
        } catch (SQLException exception) {
            throw storageFailure(
                    "Не удалось инициализировать хранилище бронирований.",
                    exception
            );
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA busy_timeout = 10000");
            }
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    private void createParentDirectory(Path databasePath) {
        Path parent = databasePath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw storageFailure(
                    "Не удалось создать каталог хранилища бронирований.",
                    exception
            );
        }
    }

    private IllegalStateException storageFailure(
            String message,
            Exception cause
    ) {
        return new IllegalStateException(message, cause);
    }
}
