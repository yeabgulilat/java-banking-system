package com.habeshabank.repository;

import com.habeshabank.model.EqubGroup;
import com.habeshabank.model.EqubMember;
import com.habeshabank.model.EqubRound;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for Equb groups, members, and rounds.
 */
public interface EqubRepository {

    // ── Groups ────────────────────────────────────────────────────────────────

    EqubGroup saveGroup(EqubGroup group);
    void      updateGroup(EqubGroup group);
    Optional<EqubGroup> findGroupByCode(String groupCode);
    Optional<EqubGroup> findGroupById(long groupId);
    List<EqubGroup>     findGroupsByUserId(long userId);
    boolean             groupCodeExists(String groupCode);

    // ── Members ───────────────────────────────────────────────────────────────

    EqubMember      saveMember(EqubMember member);
    void            updateMember(EqubMember member);
    List<EqubMember> findMembersByGroupId(long groupId);
    Optional<EqubMember> findMember(long groupId, long userId);
    boolean          isMember(long groupId, long userId);
    int              countMembers(long groupId);

    // ── Rounds ────────────────────────────────────────────────────────────────

    EqubRound       saveRound(EqubRound round);
    void            updateRound(EqubRound round);
    Optional<EqubRound> findOpenRound(long groupId);
    List<EqubRound> findRoundsByGroupId(long groupId);
    List<EqubRound> findAllRoundsForUser(long userId);
}