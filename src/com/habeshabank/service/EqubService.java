package com.habeshabank.service;

import com.habeshabank.exception.BankingException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.*;
import com.habeshabank.repository.EqubRepository;
import com.habeshabank.repository.SqliteEqubRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Manages Equb (rotating savings) group lifecycle:
 * create, join, contribute, draw winner, advance rounds.
 *
 * Architecture
 * ────────────
 * • EqubService coordinates EqubRepository (group/member/round persistence)
 *   and TransactionService (financial debits/credits).
 * • All financial side-effects go through TransactionService so they appear
 *   in the standard transaction history.
 * • Group codes are 6-char uppercase alphanumeric, generated randomly and
 *   checked for uniqueness before use.
 * • Singleton — same pattern as AuthService and TransactionService.
 */
public class EqubService {

    private final EqubRepository    equbRepo;
    private final TransactionService txService;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static EqubService instance;

    public static EqubService getInstance() {
        if (instance == null) instance = new EqubService();
        return instance;
    }

    private EqubService() {
        this.equbRepo  = new SqliteEqubRepository();
        this.txService = TransactionService.getInstance();
    }

    // ── Group creation ────────────────────────────────────────────────────────

    /**
     * Creates a new Equb group and adds the creator as the first member.
     *
     * @param name               display name (e.g. "Bole Savers")
     * @param contributionAmount amount each member pays per round (ETB)
     * @param totalRounds        planned number of rounds (== planned member count)
     * @return the generated group code the creator shares with invitees
     * @throws ValidationException if any field is invalid
     */
    public EqubGroup createGroup(String name, double contributionAmount, int totalRounds)
            throws ValidationException {

        validateGroupFields(name, contributionAmount, totalRounds);

        UserSession session = UserSession.getInstance();
        long userId         = resolveUserId(session);
        String accountNum   = session.getAccountNumber();
        String fullName     = session.getFullName();

        String code = generateUniqueCode(equbRepo);

        EqubGroup group = new EqubGroup(name, code, contributionAmount, totalRounds, userId);
        equbRepo.saveGroup(group);

        // Creator is automatically the first member
        EqubMember creator = new EqubMember(group.getId(), userId, accountNum, fullName);
        equbRepo.saveMember(creator);

        // Open round 1 immediately
        openRound(group, 1);

        System.out.println("[EqubService] Group created: " + name + " code=" + code);
        return group;
    }

    // ── Join ──────────────────────────────────────────────────────────────────

    /**
     * Adds the session user to an existing Equb group by invite code.
     *
     * @param groupCode 6-char invite code (case-insensitive)
     * @return the group that was joined
     * @throws ValidationException if the code is invalid, already a member,
     *                             or the group is not accepting new members
     */
    public EqubGroup joinGroup(String groupCode) throws ValidationException {
        if (groupCode == null || groupCode.isBlank())
            throw new ValidationException("groupCode", "Group code is required.");

        EqubGroup group = equbRepo.findGroupByCode(groupCode.trim().toUpperCase())
                .orElseThrow(() -> new ValidationException("groupCode",
                        "No Equb group found with code: " + groupCode.toUpperCase()));

        if (!group.isActive())
            throw new ValidationException("groupCode", "This Equb group is no longer active.");

        UserSession session = UserSession.getInstance();
        long userId = resolveUserId(session);

        if (equbRepo.isMember(group.getId(), userId))
            throw new ValidationException("groupCode", "You are already a member of this group.");

        int currentMembers = equbRepo.countMembers(group.getId());
        if (currentMembers >= group.getTotalRounds())
            throw new ValidationException("groupCode",
                    "This group is full (" + group.getTotalRounds() + " members maximum).");

        EqubMember member = new EqubMember(
                group.getId(), userId,
                session.getAccountNumber(), session.getFullName());
        equbRepo.saveMember(member);

        System.out.println("[EqubService] User " + userId + " joined group: " + group.getName());
        return group;
    }

    // ── Contribution ──────────────────────────────────────────────────────────

    /**
     * Records one member's contribution to the current open round of a group.
     * Debits the session account via TransactionService.
     * If all members have now contributed, automatically draws the winner.
     *
     * @param groupId the group to contribute to
     * @return the recorded Transaction
     * @throws BankingException if the user is not a member, already contributed
     *                          this round, or has insufficient funds
     */
    public Transaction contribute(long groupId) throws BankingException {
        UserSession session = UserSession.getInstance();
        long userId = resolveUserId(session);

        EqubGroup group = equbRepo.findGroupById(groupId)
                .orElseThrow(() -> new ValidationException("groupId", "Group not found."));

        if (!group.isActive())
            throw new ValidationException("groupId", "This group is no longer active.");

        if (!equbRepo.isMember(groupId, userId))
            throw new ValidationException("groupId", "You are not a member of this group.");

        EqubRound round = equbRepo.findOpenRound(groupId)
                .orElseThrow(() -> new ValidationException("groupId",
                        "No open round found for this group."));

        // Debit contribution via TransactionService
        Transaction tx = txService.equbContribution(
                group.getContributionAmount(), group.getName());

        // Increment contributions counter on the round
        round.setContributionsIn(round.getContributionsIn() + 1);
        equbRepo.updateRound(round);

        // Check if all members have now contributed → auto-draw
        int memberCount = equbRepo.countMembers(groupId);
        if (round.getContributionsIn() >= memberCount) {
            drawWinner(group, round, memberCount);
        }

        return tx;
    }

    // ── Winner draw ───────────────────────────────────────────────────────────

    /**
     * Selects a random winner from members who haven't yet received the pot,
     * credits their account, marks the round as PAID, and opens the next round.
     * If all rounds are complete, the group is marked COMPLETED.
     *
     * Only callable when the session user is the group creator.
     *
     * @param groupId the group to draw for
     */
    public void drawWinner(long groupId) throws BankingException {
        UserSession session  = UserSession.getInstance();
        long userId          = resolveUserId(session);

        EqubGroup group = equbRepo.findGroupById(groupId)
                .orElseThrow(() -> new ValidationException("groupId", "Group not found."));

        if (group.getCreatedByUserId() != userId)
            throw new ValidationException("groupId",
                    "Only the group creator can manually draw a winner.");

        EqubRound round = equbRepo.findOpenRound(groupId)
                .orElseThrow(() -> new ValidationException("groupId",
                        "No open round to draw for."));

        int memberCount = equbRepo.countMembers(groupId);
        drawWinner(group, round, memberCount);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns all groups the session user belongs to. */
    public List<EqubGroup> getMyGroups() {
        long userId = resolveUserId(UserSession.getInstance());
        return equbRepo.findGroupsByUserId(userId);
    }

    /** Returns all rounds across all groups the session user belongs to, most-recent first. */
    public List<EqubRound> getMyRoundHistory() {
        long userId = resolveUserId(UserSession.getInstance());
        return equbRepo.findAllRoundsForUser(userId);
    }

    /** Returns rounds for a specific group. */
    public List<EqubRound> getRoundsForGroup(long groupId) {
        return equbRepo.findRoundsByGroupId(groupId);
    }

    /** Returns members of a specific group. */
    public List<EqubMember> getMembersForGroup(long groupId) {
        return equbRepo.findMembersByGroupId(groupId);
    }

    /** Returns the open round for a group, or empty if none. */
    public Optional<EqubRound> getOpenRound(long groupId) {
        return equbRepo.findOpenRound(groupId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Core winner-draw logic. Selects randomly from members who haven't
     * received the pot. Credits their account if they are the current session
     * user (otherwise records a descriptive transaction without a real credit
     * — multi-user cross-account credit comes in a future phase).
     */
    private void drawWinner(EqubGroup group, EqubRound round, int memberCount) {
        List<EqubMember> eligible = equbRepo.findMembersByGroupId(group.getId())
                .stream()
                .filter(m -> !m.hasReceivedPot())
                .toList();

        if (eligible.isEmpty()) {
            // All members received pot — complete the group
            group.setStatus(EqubGroup.Status.COMPLETED);
            equbRepo.updateGroup(group);
            return;
        }

        // Random selection
        EqubMember winner = eligible.get(new Random().nextInt(eligible.size()));
        double pot        = group.getContributionAmount() * memberCount;

        // Mark winner as having received pot
        winner.setHasReceivedPot(true);
        equbRepo.updateMember(winner);

        // Update round record
        round.setWinnerUserId(winner.getUserId());
        round.setWinnerName(winner.getFullName());
        round.setPotAmount(pot);
        round.setStatus(EqubRound.Status.PAID);
        round.setPaidAt(LocalDateTime.now());
        equbRepo.updateRound(round);

        // Credit session user if they won
        UserSession session = UserSession.getInstance();
        long sessionUserId  = resolveUserId(session);
        if (winner.getUserId() == sessionUserId) {
            try {
                txService.equbPotReceived(pot, group.getName());
            } catch (BankingException e) {
                System.err.println("[EqubService] Failed to credit pot: " + e.getMessage());
            }
        }

        System.out.println("[EqubService] Round " + round.getRoundNumber()
                + " winner: " + winner.getFullName()
                + " pot: " + pot + " ETB");

        // Advance to next round or complete group
        int nextRound = round.getRoundNumber() + 1;
        List<EqubMember> stillEligible = equbRepo.findMembersByGroupId(group.getId())
                .stream().filter(m -> !m.hasReceivedPot()).toList();

        if (stillEligible.isEmpty()) {
            group.setStatus(EqubGroup.Status.COMPLETED);
            equbRepo.updateGroup(group);
            System.out.println("[EqubService] Group completed: " + group.getName());
        } else {
            group.setCurrentRound(nextRound);
            equbRepo.updateGroup(group);
            openRound(group, nextRound);
        }
    }

    private void openRound(EqubGroup group, int roundNumber) {
        EqubRound round = new EqubRound();
        round.setGroupId(group.getId());
        round.setRoundNumber(roundNumber);
        round.setPotAmount(0);
        round.setDueDate(LocalDateTime.now().plusDays(30));
        equbRepo.saveRound(round);
    }

    private void validateGroupFields(String name, double amount, int rounds)
            throws ValidationException {
        if (name == null || name.isBlank())
            throw new ValidationException("name", "Group name is required.");
        if (amount < 10)
            throw new ValidationException("contributionAmount",
                    "Contribution must be at least 10 ETB.");
        if (rounds < 2 || rounds > 50)
            throw new ValidationException("totalRounds",
                    "Group must have between 2 and 50 rounds.");
    }

    static String generateUniqueCode(EqubRepository repo) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random rand  = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
            code = sb.toString();
        } while (repo.groupCodeExists(code));
        return code;
    }

    private long resolveUserId(UserSession session) {
        if (session.getLiveUser() != null) return session.getLiveUser().getId();
        // Fallback for demo mode — use id=1
        return 1L;
    }
}