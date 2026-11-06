package ru.mirea.ispteam.repository;

import ru.mirea.ispteam.exception.DatabaseException;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.util.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Владелец: A.
 * Реализация доступа к таблице subscribers через JDBC + PreparedStatement.
 */
public class SubscriberRepositoryImpl implements SubscriberRepository {

    private final DatabaseManager db;

    public SubscriberRepositoryImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Subscriber save(Subscriber s) {
        String sql = "INSERT INTO subscribers (full_name, phone, email, address, registration_date) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getFullName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getAddress());
            ps.setDate(5, s.getRegistrationDate() != null ? Date.valueOf(s.getRegistrationDate()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setId(keys.getLong(1));
                }
            }
            return s;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении абонента", e);
        }
    }

    @Override
    public Optional<Subscriber> findById(Long id) {
        String sql = "SELECT * FROM subscribers WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске абонента по id=" + id, e);
        }
    }

    @Override
    public List<Subscriber> findAll() {
        String sql = "SELECT * FROM subscribers ORDER BY id";
        List<Subscriber> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка абонентов", e);
        }
    }

    @Override
    public Subscriber update(Subscriber s) {
        String sql = "UPDATE subscribers SET full_name = ?, phone = ?, email = ?, address = ?, " +
                "registration_date = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getFullName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getAddress());
            ps.setDate(5, s.getRegistrationDate() != null ? Date.valueOf(s.getRegistrationDate()) : null);
            ps.setLong(6, s.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Абонент с id=" + s.getId() + " не найден для обновления");
            }
            return s;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении абонента id=" + s.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM subscribers WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении абонента id=" + id, e);
        }
    }

    private Subscriber mapRow(ResultSet rs) throws SQLException {
        Date regDate = rs.getDate("registration_date");
        return new Subscriber(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                regDate != null ? regDate.toLocalDate() : null
        );
    }
}
