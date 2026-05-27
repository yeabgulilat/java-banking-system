package com.habeshabank.repository;

import com.habeshabank.model.IddirEvent;
import com.habeshabank.model.IddirGroup;
import com.habeshabank.model.IddirMember;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for Iddir groups, members, and events.
 */
public interface IddirRepository {

    // ── Groups ────────────────────────────────────────────────────────────────

    IddirGroup      saveGroup(IddirGroup group);
    void            updateGroup(IddirGroup group);
    Optional<IddirGroup> findGroupByCode(String groupCode);
    Optional<IddirGroup> findGroupById(long groupId);
    List<IddirGroup>    findGroupsByUserId(long userId);
    boolean             groupCodeExists(String groupCode);

    // ── Members ───────────────────────────────────────────────────────────────

    IddirMember     saveMember(IddirMember member);
    List<IddirMember> findMembersByGroupId(long groupId);
    Optional<IddirMember> findMember(long groupId, long userId);
    boolean          isMember(long groupId, long userId);
    int              countMembers(long groupId);

    // ── Events ────────────────────────────────────────────────────────────────

    IddirEvent      saveEvent(IddirEvent event);
    void            updateEvent(IddirEvent event);
    Optional<IddirEvent> findEventById(long eventId);
    List<IddirEvent> findEventsByGroupId(long groupId);
    List<IddirEvent> findAllEventsForUser(long userId);
    double           sumContributionsByUser(long userId);
    int              countMembersContributed(long eventId);
}