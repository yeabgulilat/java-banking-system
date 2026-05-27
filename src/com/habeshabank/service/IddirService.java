package com.habeshabank.service;

import com.habeshabank.exception.BankingException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.*;
import com.habeshabank.repository.IddirRepository;
import com.habeshabank.repository.SqliteIddirRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages Iddir (mutual-aid association) group lifecycle:
 * create, join, report events, contribute, and distribute funds.
 *
 * Architecture mirrors EqubService:
 * • IddirRepository handles all persistence.
 * • TransactionService handles all financial side-effects.
 * • Singleton.
 */
public class IddirService {

    private final IddirRepository    iddirRepo;
    private final TransactionService txService;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static IddirService instance;

    public static IddirService getInstance() {
        if (instance == null) instance = new IddirService();
        return instance;
    }

    private IddirService() {
        this.iddirRepo = new SqliteIddirRepository();
        this.txService = TransactionService.getInstance();
    }

    // ── Group creation ────────────────────────────────────────────────────────

    /**
     * Creates a new Iddir group and adds the creator as the first member.
     *
     * @param name           display name (e.g. "Kebele 05 Iddir")
     * @param monthlyAmount  expected contribution per event per member (ETB)
     * @return the created group (with generated code)
     * @throws ValidationException if fields are invalid
     */
    public IddirGroup createGroup(String name, double monthlyAmount)
            throws ValidationException {

        if (name == null || name.isBlank())
            throw new ValidationException("name", "Group name is required.");
        if (monthlyAmount < 10)
            throw new ValidationException("monthlyAmount",
                    "Contribution amount must be at least 10 ETB.");

        UserSession session = UserSession.getInstance();
        long userId         = resolveUserId(session);
        String code         = generateUniqueCode();

        IddirGroup group = new IddirGroup(name, code, monthlyAmount, userId);
        iddirRepo.saveGroup(group);

        // Creator joins automatically
        IddirMember creator = new IddirMember(
                group.getId(), userId,
                session.getAccountNumber(), session.getFullName());
        iddirRepo.saveMember(creator);

        System.out.println("[IddirService] Group created: " + name + " code=" + code);
        return group;
    }

    // ── Join ──────────────────────────────────────────────────────────────────

    /**
     * Adds the session user to an existing Iddir group by invite code.
     *
     * @param groupCode 6-char invite code (case-insensitive)
     * @return the group that was joined
     * @throws ValidationException if code is invalid or user already a member
     */
    public IddirGroup joinGroup(String groupCode) throws ValidationException {
        if (groupCode == null || groupCode.isBlank())
            throw new ValidationException("groupCode", "Group code is required.");

        IddirGroup group = iddirRepo.findGroupByCode(groupCode.trim().toUpperCase())
                .orElseThrow(() -> new ValidationException("groupCode",
                        "No Iddir group found with code: " + groupCode.toUpperCase()));

        UserSession session = UserSession.getInstance();
        long userId = resolveUserId(session);

        if (iddirRepo.isMember(group.getId(), userId))
            throw new ValidationException("groupCode",
                    "You are already a member of this group.");

        IddirMember member = new IddirMember(
                group.getId(), userId,
                session.getAccountNumber(), session.getFullName());
        iddirRepo.saveMember(member);

        System.out.println("[IddirService] User " + userId + " joined group: " + group.getName());
        return group;
    }

    // ── Event reporting ───────────────────────────────────────────────────────

    /**
     * Reports a mutual-aid event — any member in need can create one.
     * The event starts in PENDING status; members then contribute individually.
     *
     * @param groupId          the Iddir group
     * @param beneficiaryName  name of the person in need
     * @param occasion         type of event
     * @param requestedAmount  total amount requested from the group
     * @return the created IddirEvent
     * @throws ValidationException if fields are invalid or user not a member
     */
    public IddirEvent reportEvent(long groupId, String beneficiaryName,
                                  String occasion, double requestedAmount)
            throws ValidationException {

        if (beneficiaryName == null || beneficiaryName.isBlank())
            throw new ValidationException("beneficiaryName", "Beneficiary name is required.");
        if (occasion == null || occasion.isBlank())
            throw new ValidationException("occasion", "Occasion is required.");
        if (requestedAmount < 1)
            throw new ValidationException("requestedAmount", "Requested amount must be positive.");

        UserSession session = UserSession.getInstance();
        long userId = resolveUserId(session);

        if (!iddirRepo.isMember(groupId, userId))
            throw new ValidationException("groupId",
                    "You must be a member of this group to report an event.");

        IddirEvent event = new IddirEvent();
        event.setGroupId(groupId);
        event.setBeneficiaryName(beneficiaryName);
        event.setOccasion(occasion);
        event.setRequestedAmount(requestedAmount);
        event.setCreatedByUserId(userId);
        iddirRepo.saveEvent(event);

        System.out.println("[IddirService] Event reported: " + occasion
                + " for " + beneficiaryName + " in group " + groupId);
        return event;
    }

    // ── Contribution to an event ──────────────────────────────────────────────

    /**
     * Records the session user's contribution to a specific Iddir event.
     * Debits the group's standard monthly amount from their account.
     *
     * @param eventId the event to contribute to
     * @return the recorded Transaction
     * @throws BankingException if not a member, or insufficient funds
     */
    public Transaction contributeToEvent(long eventId) throws BankingException {
        IddirEvent event = iddirRepo.findEventById(eventId)
                .orElseThrow(() -> new ValidationException("eventId", "Event not found."));

        if (!event.isPending())
            throw new ValidationException("eventId",
                    "This event has already been distributed.");

        UserSession session = UserSession.getInstance();
        long userId = resolveUserId(session);

        if (!iddirRepo.isMember(event.getGroupId(), userId))
            throw new ValidationException("eventId",
                    "You are not a member of this group.");

        // Get monthly amount from group
        IddirGroup group = iddirRepo.findGroupById(event.getGroupId())
                .orElseThrow(() -> new ValidationException("eventId", "Group not found."));

        // Debit via TransactionService
        Transaction tx = txService.iddirContribution(
                group.getMonthlyAmount(),
                event.getBeneficiaryName(),
                event.getOccasion());

        // Update event totals
        event.setTotalCollected(event.getTotalCollected() + group.getMonthlyAmount());
        event.setContributionCount(event.getContributionCount() + 1);
        iddirRepo.updateEvent(event);

        // Auto-distribute if enough collected
        int memberCount = iddirRepo.countMembers(event.getGroupId());
        if (event.getContributionCount() >= memberCount) {
            distributeEvent(event, group);
        }

        return tx;
    }

    // ── Distribution ──────────────────────────────────────────────────────────

    /**
     * Manually distributes collected funds to the beneficiary.
     * Only the group creator can trigger manual distribution.
     * Credits the session account if the beneficiary matches the session user.
     *
     * @param eventId the event to distribute
     */
    public void distributeEvent(long eventId) throws BankingException {
        IddirEvent event = iddirRepo.findEventById(eventId)
                .orElseThrow(() -> new ValidationException("eventId", "Event not found."));

        if (!event.isPending())
            throw new ValidationException("eventId", "Event already distributed.");

        IddirGroup group = iddirRepo.findGroupById(event.getGroupId())
                .orElseThrow(() -> new ValidationException("eventId", "Group not found."));

        UserSession session = UserSession.getInstance();
        long userId = resolveUserId(session);

        if (group.getCreatedByUserId() != userId)
            throw new ValidationException("eventId",
                    "Only the group creator can manually distribute funds.");

        distributeEvent(event, group);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns all Iddir groups the session user belongs to. */
    public List<IddirGroup> getMyGroups() {
        long userId = resolveUserId(UserSession.getInstance());
        return iddirRepo.findGroupsByUserId(userId);
    }

    /** Returns all events across all groups the session user belongs to. */
    public List<IddirEvent> getMyEvents() {
        long userId = resolveUserId(UserSession.getInstance());
        return iddirRepo.findAllEventsForUser(userId);
    }

    /** Returns events for a specific group. */
    public List<IddirEvent> getEventsForGroup(long groupId) {
        return iddirRepo.findEventsByGroupId(groupId);
    }

    /** Returns members of a specific group. */
    public List<IddirMember> getMembersForGroup(long groupId) {
        return iddirRepo.findMembersByGroupId(groupId);
    }

    /** Returns total Iddir contributions made by session user (all time). */
    public double getMyTotalContributions() {
        long userId = resolveUserId(UserSession.getInstance());
        return iddirRepo.sumContributionsByUser(userId);
    }

    /** Returns total active members across all session user's groups. */
    public int getTotalMemberCount() {
        List<IddirGroup> groups = getMyGroups();
        return groups.stream()
                .mapToInt(g -> iddirRepo.countMembers(g.getId()))
                .sum();
    }

    /** Returns total pool balance = sum of all contributions across all user's groups. */
    public double getTotalPoolBalance() {
        long userId = resolveUserId(UserSession.getInstance());
        List<IddirEvent> events = iddirRepo.findAllEventsForUser(userId);
        return events.stream().mapToDouble(IddirEvent::getTotalCollected).sum();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void distributeEvent(IddirEvent event, IddirGroup group) {
        event.setStatus(IddirEvent.Status.DISTRIBUTED);
        event.setDistributedAt(LocalDateTime.now());
        iddirRepo.updateEvent(event);

        // Credit session user if they are the beneficiary's account holder
        // (In a multi-user system, you'd look up the beneficiary by name —
        // for now, only the session user can receive the credit directly.)
        UserSession session = UserSession.getInstance();
        if (event.getBeneficiaryName().equalsIgnoreCase(session.getFullName())) {
            try {
                txService.iddirDistributionReceived(
                        event.getTotalCollected(), event.getOccasion());
            } catch (BankingException e) {
                System.err.println("[IddirService] Failed to credit distribution: "
                        + e.getMessage());
            }
        }

        System.out.println("[IddirService] Event distributed: "
                + event.getBeneficiaryName() + " – " + event.getOccasion()
                + " amount=" + event.getTotalCollected() + " ETB");
    }

    private String generateUniqueCode() {
        // Reuse EqubService's generator logic against iddir codes
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        java.util.Random rand = new java.util.Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
            code = sb.toString();
        } while (iddirRepo.groupCodeExists(code));
        return code;
    }

    private long resolveUserId(UserSession session) {
        if (session.getLiveUser() != null) return session.getLiveUser().getId();
        return 1L; // demo mode fallback
    }
}