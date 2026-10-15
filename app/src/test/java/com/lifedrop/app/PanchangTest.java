package com.lifedrop.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.lifedrop.app.core.Panchang;
import java.time.LocalDate;
import org.junit.Test;

/** Checks the calculated calendar against dates published by the Government of India and standard almanacs. */
public class PanchangTest {
    private static LocalDate find(int year, String name) {
        for (Panchang.Ev e : Panchang.generate(year)) if (e.name.equals(name)) return e.date;
        return null;
    }
    private static void check(String name, String... isoDates) {
        for (String iso : isoDates) {
            LocalDate expected = LocalDate.parse(iso);
            assertEquals(name + " " + expected.getYear(), expected, find(expected.getYear(), name));
        }
    }
    @Test public void holi() { check("Holi", "2023-03-08", "2024-03-25", "2025-03-14", "2026-03-04", "2027-03-23"); }
    @Test public void ramNavami() { check("Ram Navami", "2023-03-30", "2024-04-17", "2025-04-06", "2026-03-26", "2027-04-15"); }
    @Test public void buddhaPurnima() { check("Buddha Purnima", "2023-05-05", "2024-05-23", "2025-05-12", "2026-05-01", "2027-05-20"); }
    @Test public void janmashtami() { check("Janmashtami", "2024-08-26", "2025-08-16", "2026-09-04", "2027-08-25"); }
    @Test public void ganeshChaturthi() { check("Ganesh Chaturthi", "2023-09-19", "2024-09-07", "2025-08-27", "2026-09-14"); }
    @Test public void dussehra() { check("Dussehra (Vijayadashami)", "2023-10-24", "2024-10-12", "2025-10-02", "2026-10-20", "2027-10-09"); }
    @Test public void diwali() { check("Diwali", "2023-11-12", "2025-10-20", "2026-11-08", "2027-10-29"); }
    @Test public void guruNanak() { check("Guru Nanak Jayanti", "2023-11-27", "2024-11-15", "2025-11-05", "2026-11-24", "2027-11-14"); }
    @Test public void easterAndGoodFriday() { check("Easter", "2024-03-31", "2025-04-20", "2026-04-05", "2027-03-28"); check("Good Friday", "2026-04-03", "2027-03-26"); }
    @Test public void eid() { check("Eid-ul-Fitr", "2024-04-11", "2025-03-31", "2026-03-21", "2027-03-10"); }
    @Test public void equinoxAndSolstice() { check("Spring Equinox", "2026-03-20"); check("Summer Solstice", "2026-06-21"); check("Winter Solstice", "2026-12-22"); }
    @Test public void eclipses2026() {
        boolean marLunar = false, augSolar = false;
        for (Panchang.Ev e : Panchang.generate(2026)) {
            if (e.name.startsWith("Lunar Eclipse") && e.date.equals(LocalDate.of(2026, 3, 3)) && e.note.startsWith("Total")) marLunar = true;
            if (e.name.startsWith("Solar Eclipse") && e.date.equals(LocalDate.of(2026, 8, 12)) && e.note.startsWith("Total")) augSolar = true;
        }
        assertEquals(true, marLunar);
        assertEquals(true, augSolar);
    }
    @Test public void worksForDistantYears() { assertNotNull(find(2050, "Diwali")); assertNotNull(find(2060, "Holi")); }
}
