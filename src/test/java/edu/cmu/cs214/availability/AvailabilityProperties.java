package edu.cmu.cs214.availability;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for {@link AvailabilityCalculator}.
 *
 * <p>One example property is provided below: it checks that no returned free slot
 * overlaps a booking, and it passes. In Milestone 1 you add a stronger property
 * that pins down what "correct availability" actually means. See the lab handout.
 */
class AvailabilityProperties {

    private final AvailabilityCalculator calc = new AvailabilityCalculator();

    /** Provided example: every returned free slot is genuinely free (overlaps no booking). */
    @Property
    void freeSlotsNeverOverlapABooking(@ForAll("scenarios") Scenario s) {
        List<TimeInterval> free = calc.freeSlots(s.dayStart(), s.dayEnd(), s.bookings());
        for (TimeInterval slot : free) {
            for (TimeInterval booking : s.bookings()) {
                assertFalse(slot.overlaps(booking),
                    () -> "free slot " + slot + " overlaps booking " + booking);
            }
        }
    }

    // --- Milestone 1: add your stronger property here ---

    /**
     * Full correctness: the calculator must account for every minute of the business day.
     * Each minute in {@code [dayStart, dayEnd)} is either covered by a booking or reported
     * as free, never both and never neither.
     *
     * <p>Unlike the provided property, this one quantifies over the minutes of the day
     * rather than over the slots that came back, so it can also see free time the
     * calculator failed to return.
     */
    @Property
    void everyMinuteOfTheDayIsExactlyOneOfBookedOrFree(@ForAll("scenarios") Scenario s) {
        List<TimeInterval> free = calc.freeSlots(s.dayStart(), s.dayEnd(), s.bookings());
        for (int minute = s.dayStart(); minute < s.dayEnd(); minute++) {
            final int m = minute;
            boolean booked = s.bookings().stream().anyMatch(b -> covers(b, m));
            boolean reportedFree = free.stream().anyMatch(slot -> covers(slot, m));

            assertFalse(booked && reportedFree,
                () -> "minute " + m + " is booked but was also reported free; free=" + free);
            assertFalse(!booked && !reportedFree,
                () -> "minute " + m + " is unbooked but was not reported free; free=" + free);
        }
    }

    /** Is {@code minute} inside the half-open interval {@code [start, end)}? */
    private static boolean covers(TimeInterval interval, int minute) {
        return interval.start() <= minute && minute < interval.end();
    }

    /** Generates a business day plus a list of bookings (possibly unsorted, overlapping, or outside hours). */
    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Integer> minutes = Arbitraries.integers().between(0, 1440);
        Arbitrary<TimeInterval> intervals = Combinators.combine(minutes, minutes)
            .as((a, b) -> new TimeInterval(Math.min(a, b), Math.max(a, b) + 1));
        Arbitrary<List<TimeInterval>> bookings = intervals.list().ofMaxSize(6);
        return Combinators.combine(minutes, minutes, bookings)
            .as((a, b, bk) -> new Scenario(Math.min(a, b), Math.max(a, b) + 1, bk));
    }

    record Scenario(int dayStart, int dayEnd, List<TimeInterval> bookings) {
    }
}
