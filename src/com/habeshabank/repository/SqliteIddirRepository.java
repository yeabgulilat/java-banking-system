package com.habeshabank.repository;

import com.habeshabank.database.DatabaseManager;
import com.habeshabank.model.IddirEvent;
import com.habeshabank.model.IddirGroup;
import com.habeshabank.model.IddirMember;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite-backed implementation of {@link IddirRepository}.
 */
public class SqliteIddirRepository implements IddirRepository {

    // ── Groups ────────────────────────────────────────────────────────────────

    @Override
    public IddirGroup saveGroup(IddirGroup g) {
        String sql = """
            INSERT INTO iddir_groups
                (name, group_code, monthly_amount, created_by, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getName());
            ps.setString(2, g.getGroupCode());
            ps.setDouble(3, g.getMonthlyAmount());
            ps.setLong(4,   g.getCreatedByUserId());
            ps.setString(5, g.getCreatedAt().toString());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) g.setId(k.getLong(1)); }
        } catch (SQLException e) { throw new RuntimeException("saveGroup failed", e); }
        return g;
    }

    @Override
    public void updateGroup(IddirGroup g) {
        String sql = "UPDATE iddir_groups SET name = ?, monthly_amount = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, g.getName());
            ps.setDouble(2, g.getMonthlyAmount());
            ps.setLong(3,   g.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("updateGroup failed", e); }
    }

    @Override
    public Optional<IddirGroup> findGroupByCode(String code) {
        String sql = "SELECT * FROM iddir_groups WHERE group_code = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapGroup(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findGroupByCode failed", e); }
        return Optional.empty();
    }

    @Override
    public Optional<IddirGroup> findGroupById(long id) {
        String sql = "SELECT * FROM iddir_groups WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapGroup(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findGroupById failed", e); }
        return Optional.empty();
    }

    @Override
    public List<IddirGroup> findGroupsByUserId(long userId) {
        String sql = """
            SELECT g.* FROM iddir_groups g
            INNER JOIN iddir_members m ON m.group_id = g.id
            WHERE m.user_id = ?
            ORDER BY g.created_at DESC
        """;
        List<IddirGroup> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGroup(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findGroupsByUserId failed", e); }
        return list;
    }

    @Override
    public boolean groupCodeExists(String code) {
        String sql = "SELECT 1 FROM iddir_groups WHERE group_code = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException("groupCodeExists failed", e); }
    }

    // ── Members ───────────────────────────────────────────────────────────────

    @Override
    public IddirMember saveMember(IddirMember m) {
        String sql = """
            INSERT INTO iddir_members
                (group_id, user_id, account_number, full_name, joined_at)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1,   m.getGroupId());
            ps.setLong(2,   m.getUserId());
            ps.setString(3, m.getAccountNumber());
            ps.setString(4, m.getFullName());
            ps.setString(5, m.getJoinedAt().toString());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) m.setId(k.getLong(1)); }
        } catch (SQLException e) { throw new RuntimeException("saveMember failed", e); }
        return m;
    }

    @Override
    public List<IddirMember> findMembersByGroupId(long groupId) {
        String sql = "SELECT * FROM iddir_members WHERE group_id = ? ORDER BY joined_at";
        List<IddirMember> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMember(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findMembersByGroupId failed", e); }
        return list;
    }

    @Override
    public Optional<IddirMember> findMember(long groupId, long userId) {
        String sql = "SELECT * FROM iddir_members WHERE group_id=? AND user_id=? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapMember(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findMember failed", e); }
        return Optional.empty();
    }

    @Override
    public boolean isMember(long groupId, long userId) {
        String sql = "SELECT 1 FROM iddir_members WHERE group_id=? AND user_id=? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException("isMember failed", e); }
    }

    @Override
    public int countMembers(long groupId) {
        String sql = "SELECT COUNT(*) AS cnt FROM iddir_members WHERE group_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt("cnt") : 0; }
        } catch (SQLException e) { throw new RuntimeException("countMembers failed", e); }
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @Override
    public IddirEvent saveEvent(IddirEvent e) {
        String sql = """
            INSERT INTO iddir_events
                (group_id, beneficiary_name, occasion, requested_amount,
                 total_collected, contribution_count, status, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1,   e.getGroupId());
            ps.setString(2, e.getBeneficiaryName());
            ps.setString(3, e.getOccasion());
            ps.setDouble(4, e.getRequestedAmount());
            ps.setDouble(5, e.getTotalCollected());
            ps.setInt(6,    e.getContributionCount());
            ps.setString(7, e.getStatus().name());
            ps.setLong(8,   e.getCreatedByUserId());
            ps.setString(9, e.getCreatedAt().toString());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) e.setId(k.getLong(1)); }
        } catch (SQLException ex) { throw new RuntimeException("saveEvent failed", ex); }
        return e;
    }

    @Override
    public void updateEvent(IddirEvent e) {
        String sql = """
            UPDATE iddir_events SET
                total_collected = ?, contribution_count = ?,
                status = ?, distributed_at = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, e.getTotalCollected());
            ps.setInt(2,    e.getContributionCount());
            ps.setString(3, e.getStatus().name());
            ps.setString(4, e.getDistributedAt() != null ? e.getDistributedAt().toString() : null);
            ps.setLong(5,   e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) { throw new RuntimeException("updateEvent failed", ex); }
    }

    @Override
    public Optional<IddirEvent> findEventById(long eventId) {
        String sql = """
            SELECT e.*, g.name AS group_name FROM iddir_events e
            JOIN iddir_groups g ON g.id = e.group_id
            WHERE e.id = ? LIMIT 1
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapEvent(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findEventById failed", e); }
        return Optional.empty();
    }

    @Override
    public List<IddirEvent> findEventsByGroupId(long groupId) {
        String sql = """
            SELECT e.*, g.name AS group_name FROM iddir_events e
            JOIN iddir_groups g ON g.id = e.group_id
            WHERE e.group_id = ?
            ORDER BY e.created_at DESC
        """;
        List<IddirEvent> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEvent(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findEventsByGroupId failed", e); }
        return list;
    }

    @Override
    public List<IddirEvent> findAllEventsForUser(long userId) {
        String sql = """
            SELECT e.*, g.name AS group_name FROM iddir_events e
            JOIN iddir_groups  g ON g.id = e.group_id
            JOIN iddir_members m ON m.group_id = g.id AND m.user_id = ?
            ORDER BY e.created_at DESC
        """;
        List<IddirEvent> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEvent(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findAllEventsForUser failed", e); }
        return list;
    }

    @Override
    public double sumContributionsByUser(long userId) {
        // Sum IDDIR transaction amounts for this user's account
        String acctSql = "SELECT account_number FROM accounts WHERE user_id = ? LIMIT 1";
        String acct = null;
        try (PreparedStatement ps = conn().prepareStatement(acctSql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) acct = rs.getString("account_number");
            }
        } catch (SQLException e) { throw new RuntimeException("sumContributionsByUser failed", e); }
        if (acct == null) return 0.0;

        String sumSql = "SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE account_number=? AND type='IDDIR'";
        try (PreparedStatement ps = conn().prepareStatement(sumSql)) {
            ps.setString(1, acct);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0.0;
            }
        } catch (SQLException e) { throw new RuntimeException("sumContributionsByUser sum failed", e); }
    }

    @Override
    public int countMembersContributed(long eventId) {
        // Count members whose individual contribution row exists for this event
        String sql = "SELECT contribution_count FROM iddir_events WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("contribution_count") : 0;
            }
        } catch (SQLException e) { throw new RuntimeException("countMembersContributed failed", e); }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private IddirGroup mapGroup(ResultSet rs) throws SQLException {
        IddirGroup g = new IddirGroup();
        g.setId(rs.getLong("id"));
        g.setName(rs.getString("name"));
        g.setGroupCode(rs.getString("group_code"));
        g.setMonthlyAmount(rs.getDouble("monthly_amount"));
        g.setCreatedByUserId(rs.getLong("created_by"));
        String ca = rs.getString("created_at");
        if (ca != null) g.setCreatedAt(LocalDateTime.parse(ca));
        return g;
    }

    private IddirMember mapMember(ResultSet rs) throws SQLException {
        IddirMember m = new IddirMember();
        m.setId(rs.getLong("id"));
        m.setGroupId(rs.getLong("group_id"));
        m.setUserId(rs.getLong("user_id"));
        m.setAccountNumber(rs.getString("account_number"));
        m.setFullName(rs.getString("full_name"));
        String ja = rs.getString("joined_at");
        if (ja != null) m.setJoinedAt(LocalDateTime.parse(ja));
        return m;
    }

    private IddirEvent mapEvent(ResultSet rs) throws SQLException {
        IddirEvent e = new IddirEvent();
        e.setId(rs.getLong("id"));
        e.setGroupId(rs.getLong("group_id"));
        try { e.setGroupName(rs.getString("group_name")); } catch (SQLException ignored) {}
        e.setBeneficiaryName(rs.getString("beneficiary_name"));
        e.setOccasion(rs.getString("occasion"));
        e.setRequestedAmount(rs.getDouble("requested_amount"));
        e.setTotalCollected(rs.getDouble("total_collected"));
        e.setContributionCount(rs.getInt("contribution_count"));
        e.setStatus(IddirEvent.Status.valueOf(rs.getString("status")));
        e.setCreatedByUserId(rs.getLong("created_by"));
        String ca = rs.getString("created_at");
        if (ca != null) e.setCreatedAt(LocalDateTime.parse(ca));
        String da = rs.getString("distributed_at");
        if (da != null) e.setDistributedAt(LocalDateTime.parse(da));
        return e;
    }

    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }
}