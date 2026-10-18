package event;
/** One defeated enemy; source event IDs must remain stable across redelivery. */
public final class EnemyDefeatedEvent implements GameplayEvent {
    private final String runId;
    private final String eventId;
    public EnemyDefeatedEvent(final String runId, final String eventId) {
        if (runId == null || eventId == null || runId.trim().isEmpty() || eventId.trim().isEmpty())
            throw new IllegalArgumentException("runId and eventId are required.");
        this.runId=runId; this.eventId=eventId;
    }
    public String getRunId() { return this.runId; }
    public String getEventId() { return this.eventId; }
}
