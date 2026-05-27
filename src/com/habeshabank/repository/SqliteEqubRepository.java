package com.habeshabank.repository;

import com.habeshabank.database.DatabaseManager;
import com.habeshabank.model.EqubGroup;
import com.habeshabank.model.EqubMember;
import com.habeshabank.model.EqubRound;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite-backed implementation of {@link EqubRepository}.
 */
public class SqliteEqubRepository implements EqubRepository {

    // ── Groups ────────────────────────────────────────────────────────────────

    @Override
    public EqubGroup saveGroup(EqubGroup g) {
        String sql = """
            INSERT INTO equb_groups
                (name, group_code, contribution_amount, total_rounds,
                 current_round, status, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getName());
            ps.setString(2, g.getGroupCode());
            ps.setDouble(3, g.getContributionAmount());
            ps.setInt(4,    g.getTotalRounds());
            ps.setInt(5,    g.getCurrentRound());
            ps.setString(6, g.getStatus().name());
            ps.setLong(7,   g.getCreatedByUserId());
            ps.setString(8, g.getCreatedAt().toString());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) g.setId(k.getLong(1)); }
        } catch (SQLException e) { throw new RuntimeException("saveGroup failed", e); }
        return g;
    }

    @Override
    public void updateGroup(EqubGroup g) {
        String sql = """
            UPDATE equb_groups SET
                current_round = ?, status = ?, total_rounds = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1,    g.getCurrentRound());
            ps.setString(2, g.getStatus().name());
            ps.setInt(3,    g.getTotalRounds());
            ps.setLong(4,   g.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("updateGroup failed", e); }
    }

    @Override
    public Optional<EqubGroup> findGroupByCode(String code) {
        String sql = "SELECT * FROM equb_groups WHERE group_code = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapGroup(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findGroupByCode failed", e); }
        return Optional.empty();
    }

    @Override
    public Optional<EqubGroup> findGroupById(long id) {
        String sql = "SELECT * FROM equb_groups WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapGroup(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findGroupById failed", e); }
        return Optional.empty();
    }

    @Override
    public List<EqubGroup> findGroupsByUserId(long userId) {
        String sql = """
            SELECT g.* FROM equb_groups g
            INNER JOIN equb_members m ON m.group_id = g.id
            WHERE m.user_id = ?
            ORDER BY g.created_at DESC
        """;
        List<EqubGroup> list = new ArrayList<>();
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
        String sql = "SELECT 1 FROM equb_groups WHERE group_code = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException("groupCodeExists failed", e); }
    }

    // ── Members ───────────────────────────────────────────────────────────────

    @Override
    public EqubMember saveMember(EqubMember m) {
        String sql = """
            INSERT INTO equb_members
                (group_id, user_id, account_number, full_name,
                 has_received_pot, joined_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1,   m.getGroupId());
            ps.setLong(2,   m.getUserId());
            ps.setString(3, m.getAccountNumber());
            ps.setString(4, m.getFullName());
            ps.setInt(5,    m.hasReceivedPot() ? 1 : 0);
            ps.setString(6, m.getJoinedAt().toString());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) m.setId(k.getLong(1)); }
        } catch (SQLException e) { throw new RuntimeException("saveMember failed", e); }
        return m;
    }

    @Override
    public void updateMember(EqubMember m) {
        String sql = "UPDATE equb_members SET has_received_pot = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1,  m.hasReceivedPot() ? 1 : 0);
            ps.setLong(2, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("updateMember failed", e); }
    }

    @Override
    public List<EqubMember> findMembersByGroupId(long groupId) {
        String sql = "SELECT * FROM equb_members WHERE group_id = ? ORDER BY joined_at";
        List<EqubMember> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMember(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findMembersByGroupId failed", e); }
        return list;
    }

    @Override
    public Optional<EqubMember> findMember(long groupId, long userId) {
        String sql = "SELECT * FROM equb_members WHERE group_id = ? AND user_id = ? LIMIT 1";
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
        String sql = "SELECT 1 FROM equb_members WHERE group_id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException("isMember failed", e); }
    }

    @Override
    public int countMembers(long groupId) {
        String sql = "SELECT COUNT(*) AS cnt FROM equb_members WHERE group_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt("cnt") : 0; }
        } catch (SQLException e) { throw new RuntimeException("countMembers failed", e); }
    }

    // ── Rounds ────────────────────────────────────────────────────────────────

    @Override
    public EqubRound saveRound(EqubRound r) {
        String sql = """
            INSERT INTO equb_rounds
                (group_id, round_number, winner_user_id, winner_name,
                 pot_amount, contributions_in, status, due_date, paid_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1,   r.getGroupId());
            ps.setInt(2,    r.getRoundNumber());
            ps.setLong(3,   r.getWinnerUserId());
            ps.setString(4, r.getWinnerName());
            ps.setDouble(5, r.getPotAmount());
            ps.setInt(6,    r.getContributionsIn());
            ps.setString(7, r.getStatus().name());
            ps.setString(8, r.getDueDate() != null ? r.getDueDate().toString() : null);
            ps.setString(9, r.getPaidAt()  != null ? r.getPaidAt().toString()  : null);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) r.setId(k.getLong(1)); }
        } catch (SQLException e) { throw new RuntimeException("saveRound failed", e); }
        return r;
    }

    @Override
    public void updateRound(EqubRound r) {
        String sql = """
            UPDATE equb_rounds SET
                winner_user_id = ?, winner_name = ?,
                contributions_in = ?, status = ?, paid_at = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1,   r.getWinnerUserId());
            ps.setString(2, r.getWinnerName());
            ps.setInt(3,    r.getContributionsIn());
            ps.setString(4, r.getStatus().name());
            ps.setString(5, r.getPaidAt() != null ? r.getPaidAt().toString() : null);
            ps.setLong(6,   r.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("updateRound failed", e); }
    }

    @Override
    public Optional<EqubRound> findOpenRound(long groupId) {
        String sql = """
            SELECT r.*, g.name AS group_name FROM equb_rounds r
            JOIN equb_groups g ON g.id = r.group_id
            WHERE r.group_id = ? AND r.status = 'OPEN' LIMIT 1
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRound(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findOpenRound failed", e); }
        return Optional.empty();
    }

    @Override
    public List<EqubRound> findRoundsByGroupId(long groupId) {
        String sql = """
            SELECT r.*, g.name AS group_name FROM equb_rounds r
            JOIN equb_groups g ON g.id = r.group_id
            WHERE r.group_id = ?
            ORDER BY r.round_number DESC
        """;
        List<EqubRound> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRound(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findRoundsByGroupId failed", e); }
        return list;
    }

    @Override
    public List<EqubRound> findAllRoundsForUser(long userId) {
        String sql = """
            SELECT r.*, g.name AS group_name FROM equb_rounds r
            JOIN equb_groups  g ON g.id = r.group_id
            JOIN equb_members m ON m.group_id = g.id AND m.user_id = ?
            ORDER BY r.group_id, r.round_number DESC
        """;
        List<EqubRound> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRound(rs));
            }
        } catch (SQLException e) { throw new RuntimeException("findAllRoundsForUser failed", e); }
        return list;
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private EqubGroup mapGroup(ResultSet rs) throws SQLException {
        EqubGroup g = new EqubGroup();
        g.setId(rs.getLong("id"));
        g.setName(rs.getString("name"));
        g.setGroupCode(rs.getString("group_code"));
        g.setContributionAmount(rs.getDouble("contribution_amount"));
        g.setTotalRounds(rs.getInt("total_rounds"));
        g.setCurrentRound(rs.getInt("current_round"));
        g.setStatus(EqubGroup.Status.valueOf(rs.getString("status")));
        g.setCreatedByUserId(rs.getLong("created_by"));
        String ca = rs.getString("created_at");
        if (ca != null) g.setCreatedAt(LocalDateTime.parse(ca));
        return g;
    }

    private EqubMember mapMember(ResultSet rs) throws SQLException {
        EqubMember m = new EqubMember();
        m.setId(rs.getLong("id"));
        m.setGroupId(rs.getLong("group_id"));
        m.setUserId(rs.getLong("user_id"));
        m.setAccountNumber(rs.getString("account_number"));
        m.setFullName(rs.getString("full_name"));
        m.setHasReceivedPot(rs.getInt("has_received_pot") == 1);
        String ja = rs.getString("joined_at");
        if (ja != null) m.setJoinedAt(LocalDateTime.parse(ja));
        return m;
    }

    private EqubRound mapRound(ResultSet rs) throws SQLException {
        EqubRound r = new EqubRound();
        r.setId(rs.getLong("id"));
        r.setGroupId(rs.getLong("group_id"));
        try { r.setGroupName(rs.getString("group_name")); } catch (SQLException ignored) {}
        r.setRoundNumber(rs.getInt("round_number"));
        r.setWinnerUserId(rs.getLong("winner_user_id"));
        r.setWinnerName(rs.getString("winner_name"));
        r.setPotAmount(rs.getDouble("pot_amount"));
        r.setContributionsIn(rs.getInt("contributions_in"));
        r.setStatus(EqubRound.Status.valueOf(rs.getString("status")));
        String dd = rs.getString("due_date");
        if (dd != null) r.setDueDate(LocalDateTime.parse(dd));
        String pa = rs.getString("paid_at");
        if (pa != null) r.setPaidAt(LocalDateTime.parse(pa));
        return r;
    }

    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }
}