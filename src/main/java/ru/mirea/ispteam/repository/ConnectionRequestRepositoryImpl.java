package ru.mirea.ispteam.repository;

import ru.mirea.ispteam.exception.DatabaseException;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Владелец: A.
 * Реализация доступа к таблице connection_requests через JDBC + PreparedStatement.
 * enum'ы хранятся в БД как строки (VARCHAR), маппинг — через valueOf/name.
 */
public class ConnectionRequestRepositoryImpl implements ConnectionRequestRepository {

    private final DatabaseManager db;

    public ConnectionRequestRepositoryImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public ConnectionRequest save(ConnectionRequest r) {
        String sql = "INSERT INTO connection_requests " +
                "(subscriber_id, type, status, tariff_plan, description, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindWritableFields(ps, r);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getLong(1));
                }
            }
            return r;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении заявки", e);
        }
    }

    @Override
    public Optional<ConnectionRequest> findById(Long id) {
        String sql = "SELECT * FROM connection_requests WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске заявки по id=" + id, e);
        }
    }

    @Override
    public List<ConnectionRequest> findAll() {
        String sql = "SELECT * FROM connection_requests ORDER BY id";
        return queryList(sql, null);
    }

    @Override
    public List<ConnectionRequest> findBySubscriberId(Long subscriberId) {
        String sql = "SELECT * FROM connection_requests WHERE subscriber_id = ? ORDER BY id";
        return queryList(sql, subscriberId);
    }

    @Override
    public ConnectionRequest update(ConnectionRequest r) {
        String sql = "UPDATE connection_requests SET subscriber_id = ?, type = ?, status = ?, " +
                "tariff_plan = ?, description = ?, created_at = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindWritableFields(ps, r);
            ps.setLong(8, r.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DatabaseException("Заявка с id=" + r.getId() + " не найдена для обновления");
            }
            return r;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении заявки id=" + r.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM connection_requests WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении заявки id=" + id, e);
        }
    }

    // Заполняет 7 записываемых полей (позиции 1..7) — общие для INSERT и UPDATE.
    private void bindWritableFields(PreparedStatement ps, ConnectionRequest r) throws SQLException {
        ps.setLong(1, r.getSubscriberId());
        ps.setString(2, r.getType() != null ? r.getType().name() : null);
        ps.setString(3, r.getStatus() != null ? r.getStatus().name() : null);
        ps.setString(4, r.getTariffPlan());
        ps.setString(5, r.getDescription());
        ps.setTimestamp(6, r.getCreatedAt() != null ? Timestamp.valueOf(r.getCreatedAt()) : null);
        ps.setTimestamp(7, r.getUpdatedAt() != null ? Timestamp.valueOf(r.getUpdatedAt()) : null);
    }

    private List<ConnectionRequest> queryList(String sql, Long param) {
        List<ConnectionRequest> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setLong(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при выборке заявок", e);
        }
    }

    private ConnectionRequest mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new ConnectionRequest(
                rs.getLong("id"),
                rs.getLong("subscriber_id"),
                RequestType.valueOf(rs.getString("type")),
                RequestStatus.valueOf(rs.getString("status")),
                rs.getString("tariff_plan"),
                rs.getString("description"),
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
    }
}
