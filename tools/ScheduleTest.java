package dev.local.reelblock;

// Checks State.inWindow, the Pro schedule rule. Run (CI does): see the "Schedule" step in .github/workflows/build.yml.
// Days: bit 0 = Sunday. dow: 0 = Sunday. Minutes since midnight.
public class ScheduleTest {
    static int fails;

    static void is(boolean want, int start, int end, int days, int dow, int minute) {
        if (State.inWindow(start, end, days, dow, minute) != want) {
            fails++;
            System.out.printf("FAIL %s-%s days=%s dow=%d at %d: want %b%n", start, end, Integer.toBinaryString(days), dow, minute, want);
        }
    }

    public static void main(String[] a) {
        int weekdays = 0b0111110, nine = 9 * 60, five = 17 * 60, ten = 22 * 60, seven = 7 * 60;
        is(true, nine, five, weekdays, 1, nine);        // Monday 09:00: start is inside
        is(false, nine, five, weekdays, 1, five);       // Monday 17:00: end is outside
        is(false, nine, five, weekdays, 0, 12 * 60);    // Sunday noon: not a chosen day
        is(true, ten, seven, weekdays, 5, 23 * 60);     // Friday 23:00, overnight window
        is(true, ten, seven, weekdays, 6, 2 * 60);      // Saturday 02:00 belongs to Friday night
        is(false, ten, seven, weekdays, 1, 2 * 60);     // Monday 02:00: Sunday night is not chosen
        is(false, ten, seven, weekdays, 1, 12 * 60);    // Monday noon: between the two parts
        is(true, nine, nine, 1, 0, 3 * 60);             // start == end: the whole chosen day
        is(false, nine, nine, 1, 1, 3 * 60);
        is(false, nine, five, 0, 3, 12 * 60);           // no days chosen: never blocks
        System.out.println(fails == 0 ? "schedule: 10 checks pass" : "schedule: " + fails + " failed");
        System.exit(fails == 0 ? 0 : 1);
    }
}
