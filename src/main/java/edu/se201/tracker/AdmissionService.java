package edu.se201.tracker;


import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AdmissionService {
    public enum State { ACTIVE, WAITING, BUSY, EXPIRED }
    public record Admission(State state, int position) {}
    public record Snapshot(List<Long> active, List<Long> waiting) {}
    private final SettingsService settings;
    private final Clock clock;
    private final Map<Long,Instant> active = new LinkedHashMap<>();
    private final Map<Long,Instant> waiting = new LinkedHashMap<>();
    private static final int WAITING_LIVENESS_SECONDS = 15;
    public AdmissionService(SettingsService settings, Clock clock) { this.settings=settings; this.clock=clock; }

    // Every operation uses the same monitor. Promotion happens before a newcomer can claim a place.
    public synchronized Admission enter(long id, boolean activity) {
        clean();
        if (active.containsKey(id)) {
            if (activity) active.put(id,clock.instant());
            return new Admission(State.ACTIVE,0);
        }
        if (waiting.containsKey(id)) {
            waiting.put(id,clock.instant()); return result(id);
        }
        AdmissionSettings s=settings.get();
        if (active.size()<s.maxActive() && waiting.isEmpty()) {
            active.put(id,clock.instant()); return new Admission(State.ACTIVE,0);
        }
        if (waiting.size()>=s.queueCapacity()) return new Admission(State.BUSY,0);
        waiting.put(id,clock.instant()); return result(id);
    }
    // Dashboard polling observes only. It neither enqueues nor renews an active teacher.
    public synchronized Admission inspect(long id) { clean(); return result(id); }
    public synchronized boolean touch(long id) {
        clean();
        if (!active.containsKey(id)) return false;
        active.put(id,clock.instant()); return true;
    }
    public synchronized void release(long id) { active.remove(id); waiting.remove(id); clean(); }
    public synchronized Snapshot snapshot() { clean(); return new Snapshot(List.copyOf(active.keySet()),List.copyOf(waiting.keySet())); }
    public synchronized void configure(AdmissionSettings s) { settings.save(s); clean(); }
    @Scheduled(fixedDelay=1000) public synchronized void sweep() { clean(); }
    private Admission result(long id) {
        if (active.containsKey(id)) return new Admission(State.ACTIVE,0);
        int pos=1;
        for (long queued : waiting.keySet()) { if (queued==id) return new Admission(State.WAITING,pos); pos++; }
        return new Admission(State.EXPIRED,0);
    }
    private void clean() {
        Instant now=clock.instant(); AdmissionSettings s=settings.get();
        active.values().removeIf(t -> !t.plusSeconds(s.inactivitySeconds()).isAfter(now));
        waiting.values().removeIf(t -> !t.plusSeconds(WAITING_LIVENESS_SECONDS).isAfter(now));
        // Lowering capacity keeps current occupants; new entries wait until the count falls below it.
        while (active.size()<s.maxActive() && !waiting.isEmpty()) {
            long id=waiting.keySet().iterator().next(); waiting.remove(id); active.put(id,now);
        }
    }
}
