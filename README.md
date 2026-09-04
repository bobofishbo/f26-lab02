# Lab 2 Starter: Availability Calculator

A small reservation component. Given a room's bookings and the day's business hours,
`AvailabilityCalculator.freeSlots` computes when the room is free. It is the code you
work in for Lab 2.

It ships with a generated test suite that passes, and a property-based test harness
(jqwik) with one example property. Everything is green. Your job in Lab 2 is to decide
whether green actually means correct.

**Read `ARCHITECTURE.md` before the code.**

## Build and test

```
mvn test
```

`mvn test` runs both files, the ordinary example-based tests (`AvailabilityCalculatorTest`)
and the property-based tests (`AvailabilityProperties`). A code-coverage report is written
to `target/site/jacoco/index.html`.

## Continuous integration

This repository has CI configured in `.github/workflows/ci.yml`. GitHub disables workflows on a
fresh fork, so enable them once on your fork (the handout shows where). After that, every
push runs `mvn test`. You will watch the gate go red when your new property finds the bug, then
green once you fix it.

## Where things are

- Component: `src/main/java/edu/cmu/cs214/availability/`
- Example-based tests: `src/test/java/edu/cmu/cs214/availability/AvailabilityCalculatorTest.java`
- Property-based tests: `src/test/java/edu/cmu/cs214/availability/AvailabilityProperties.java`
- Setup: `SETUP.md`

See the Lab 2 handout on the course page for the three milestones you show a TA.

---

## Milestone 3: auditing the generated suite

`AvailabilityCalculatorTest` held 100% instruction, branch, and line coverage on
`AvailabilityCalculator` and still passed over a real bug: the sweep loop never emitted the
free gap between the last booking and `dayEnd`, so any day not booked through to closing
lost its tail, and a day with no bookings returned no free time at all.

### Three weaknesses

**1. No test leaves free time at the end of the day — *controllability*.**
In five of the six tests the last booking ends exactly at `DAY_END` (1020):
`[540,1020)`, `[720,1020)`, `[900,1020)`, `[900,1020)`, and `[660,1020)` after merging. So
when the loop exits, `cursor == dayEnd` and the missing tail gap is empty anyway. Those
tests produce correct answers from broken code — the input never creates a tail to lose.

**2. `returnedSlotsNeverOverlapABooking` runs the bug but cannot see it — *observability*.**
This one books only `[600,660)` on a day ending at 1020, so it *does* trigger the bug: the
calculator drops `[660,1020)`, six free hours. But its assertion loops over the slots that
came back and only checks each one does not overlap a booking. That claim is one-directional
— it can catch a slot that is present and wrong, never one that is missing. The fewer slots
returned, the fewer assertions run. `return List.of();` would satisfy it on every input.

**3. The input space is never varied — *controllability*.**
`freeSlots` is never called with an empty booking list, the case where the bug is total
(the whole day should come back free; the buggy code returns `[]`). Every test also runs on
the same hardcoded 9:00–17:00 day, because `DAY_START`/`DAY_END` are constants and the
private `free(bookings)` helper does not expose the day parameters at all — no test *can*
vary the business hours.

### Why high coverage did not save it

Coverage measures which lines *ran*, not whether they produced the right answer — and it can
only measure code that exists. The fix was three lines that nobody had written, and an absent
line can never be marked red. Weakness 2 above is exactly the case: that test executed every
line and both sides of every branch, which is what pushed the class to 100%, while its
assertion was too weak to notice the wrong result. Coverage is also blind to assertions
entirely — delete every `assertEquals` in the file, keep the calls, and the report is
identical at 100%. Confirming this, fixing the bug made coverage go *up* (80 → 91
instructions, 8 → 10 branches): the missing code was never counted against the class.

The property in `AvailabilityProperties` catches all three because it quantifies over the
*input* (every minute of the business day) instead of the *output*, so an omission has
nowhere to hide.

## Tools used

Claude Code (model: Claude Opus 5) — used to explore the starter, draft the
`everyMinuteOfTheDayIsExactlyOneOfBookedOrFree` property, diagnose and fix the missing
trailing free slot, and draft this audit.
