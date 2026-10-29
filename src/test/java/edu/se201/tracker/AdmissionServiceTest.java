package edu.se201.tracker;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static edu.se201.tracker.AdmissionService.State.*;

class AdmissionServiceTest {
    static class MutableClock extends Clock {
        private Instant now=Instant.parse("2026-10-29T10:00:00Z");
        void advance(int seconds) { now=now.plusSeconds(seconds); }
        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
    }
    static class MemorySettings extends SettingsService {
        private AdmissionSettings value=new AdmissionSettings(1,10,3);
        MemorySettings() { super(null); }
        @Override public AdmissionSettings get() { return value; }
        @Override public void save(AdmissionSettings s) { value=s; }
    }
    MutableClock clock;
    AdmissionService queue;
    @BeforeEach void setup() { clock=new MutableClock(); queue=new AdmissionService(new MemorySettings(),clock); }
    @Test void firstAccountEntersAndNextAccountsWaitInArrivalOrder() {
        assertThat(queue.enter(1,true).state()).isEqualTo(ACTIVE);
        assertThat(queue.enter(2,true).position()).isEqualTo(1);
        assertThat(queue.enter(3,true).position()).isEqualTo(2);
        queue.release(1);
        assertThat(queue.inspect(2).state()).isEqualTo(ACTIVE);
        assertThat(queue.inspect(3).position()).isEqualTo(1);
    }
    @Test void tabsAndSessionsShareOnePlaceAndOneQueueEntry() {
        queue.enter(1,true); queue.enter(1,true); queue.enter(2,false); queue.enter(2,false);
        assertThat(queue.snapshot().active()).containsExactly(1L);
        assertThat(queue.snapshot().waiting()).containsExactly(2L);
    }
    @Test void meaningfulActivityExtendsAdmissionBeyondOriginalTimeout() {
        queue.enter(1,true); clock.advance(8); assertThat(queue.touch(1)).isTrue();
        clock.advance(8); assertThat(queue.inspect(1).state()).isEqualTo(ACTIVE);
        clock.advance(2); assertThat(queue.inspect(1).state()).isEqualTo(EXPIRED);
    }
    @Test void dashboardPollingDoesNotRenewActivityAndNextTeacherEnters() {
        queue.enter(1,true); queue.enter(2,false); clock.advance(8);
        assertThat(queue.inspect(1).state()).isEqualTo(ACTIVE); queue.enter(2,false);
        clock.advance(2); assertThat(queue.inspect(1).state()).isEqualTo(EXPIRED);
        assertThat(queue.inspect(2).state()).isEqualTo(ACTIVE);
    }
    @Test void waitingPagePollingDoesNotKeepPromotedTeacherActiveForever() {
        queue.enter(1,true); queue.enter(2,false); queue.release(1);
        clock.advance(8); queue.enter(2,false); clock.advance(2);
        assertThat(queue.inspect(2).state()).isEqualTo(EXPIRED);
    }
    @Test void expiredTeacherReturnsBehindWaitingAccounts() {
        queue.enter(1,true); queue.enter(2,false); queue.enter(3,false); clock.advance(10);
        assertThat(queue.enter(1,true).position()).isEqualTo(2);
        assertThat(queue.inspect(2).state()).isEqualTo(ACTIVE);
        assertThat(queue.inspect(3).position()).isEqualTo(1);
        assertThat(queue.touch(1)).isFalse();
    }
    @Test void queueCapacityIsFiniteAndReleaseRemovesQueuedAccounts() {
        queue.configure(new AdmissionSettings(1,10,1)); queue.enter(1,true); queue.enter(2,false);
        assertThat(queue.enter(3,false).state()).isEqualTo(BUSY);
        queue.release(2); assertThat(queue.enter(3,false).position()).isEqualTo(1);
    }
    @Test void staleWaitingAccountsAreRemovedBeforePromotion() {
        queue.configure(new AdmissionSettings(1,60,3)); queue.enter(1,true); queue.enter(2,false);
        clock.advance(15); queue.release(1);
        assertThat(queue.snapshot().active()).isEmpty(); assertThat(queue.snapshot().waiting()).isEmpty();
    }
    @Test void loweringCapacityDoesNotEjectCurrentTeachersAndRaisingPromotes() {
        queue.configure(new AdmissionSettings(2,60,3)); queue.enter(1,true); queue.enter(2,true);
        queue.configure(new AdmissionSettings(1,60,3)); queue.enter(3,false); queue.release(1);
        assertThat(queue.inspect(3).state()).isEqualTo(WAITING);
        queue.configure(new AdmissionSettings(2,60,3)); assertThat(queue.inspect(3).state()).isEqualTo(ACTIVE);
    }
    @Test void loweringQueueCapacityKeepsExistingWaitersAndBlocksNewOnes() {
        queue.enter(1,true); queue.enter(2,false); queue.enter(3,false);
        queue.configure(new AdmissionSettings(1,10,1));
        assertThat(queue.snapshot().waiting()).containsExactly(2L,3L);
        assertThat(queue.enter(4,false).state()).isEqualTo(BUSY);
    }
    @Test void invalidSettingsFailBeforeStateChanges() {
        assertThatThrownBy(() -> new AdmissionSettings(0,10,1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdmissionSettings(1,9,1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdmissionSettings(1,3601,1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdmissionSettings(1,10,0)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void simultaneousRequestsNeverExceedCapacityOrDuplicateAccounts() throws Exception {
        queue.configure(new AdmissionSettings(3,60,100));
        ExecutorService pool=Executors.newFixedThreadPool(12);
        try {
            CountDownLatch start=new CountDownLatch(1); List<Future<?>> futures=new ArrayList<>();
            for (int i=0;i<100;i++) { final long id=i%50; futures.add(pool.submit(() -> { start.await(); queue.enter(id,true); return null; })); }
            start.countDown(); for (Future<?> task:futures) task.get(5,TimeUnit.SECONDS);
            var snapshot=queue.snapshot(); assertThat(snapshot.active()).hasSize(3).doesNotHaveDuplicates();
            assertThat(snapshot.waiting()).hasSize(47).doesNotHaveDuplicates();
            assertThat(snapshot.active()).doesNotContainAnyElementsOf(snapshot.waiting());
        } finally { pool.shutdownNow(); }
    }
}
