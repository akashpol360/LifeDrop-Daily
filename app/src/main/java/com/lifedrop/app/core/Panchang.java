package com.lifedrop.app.core;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Hindu lunisolar (amanta) calendar helper: lunar months and tithi-based festival dates, computed from sun/moon positions. */
public final class Panchang {
    private Panchang() {}
    public static final String[] MONTHS = {"", "Chaitra", "Vaishakha", "Jyeshtha", "Ashadha", "Shravana", "Bhadrapada", "Ashwin", "Kartika", "Margashirsha", "Pausha", "Magha", "Phalguna"};

    public static final class Month {
        public final double startJd, endJd; public final int no; public final boolean adhik;
        Month(double s, double e, int n, boolean a) { startJd = s; endJd = e; no = n; adhik = a; }
    }

    /** Lunar months (new moon to new moon) covering Oct of the previous year to Mar of the next year. */
    public static List<Month> months(int year) {
        List<Double> nm = Astro.crossings(Astro::elong, 0, Astro.jdOf(year - 1, 10, 1), Astro.jdOf(year + 1, 3, 31), 1.0);
        List<Month> out = new ArrayList<>();
        for (int i = 0; i + 1 < nm.size(); i++) {
            double a = nm.get(i), b = nm.get(i + 1);
            int s1 = (int) Math.floor(Astro.norm(Astro.sunLon(a) - Astro.ayanamsa(a)) / 30.0);
            int s2 = (int) Math.floor(Astro.norm(Astro.sunLon(b) - Astro.ayanamsa(b)) / 30.0);
            out.add(new Month(a, b, ((s1 + 1) % 12) + 1, s1 == s2));
        }
        return out;
    }

    /** Date (IST) in the given year when tithi [tithi] (1..30) prevails at [hour] IST, in non-adhik lunar month [monthNo]. */
    public static LocalDate festival(int year, int monthNo, int tithi, double hour) {
        for (Month m : months(year)) {
            if (m.no != monthNo || m.adhik) continue;
            LocalDate d0 = Astro.istDate(m.startJd), d1 = Astro.istDate(m.endJd);
            LocalDate hit = null;
            for (LocalDate d = d0; !d.isAfter(d1); d = d.plusDays(1)) {
                double jd = Astro.jdIst(d, hour);
                if (jd < m.startJd || jd >= m.endJd) continue; // the instant must lie inside this lunar month
                int t = Astro.tithi(jd);
                if (t == tithi) { hit = d; break; }
                int t2 = Astro.tithi(Astro.jdIst(d.plusDays(1), hour));
                if (t == tithi - 1 && t2 == tithi + 1) { hit = d; break; } // tithi skipped at this hour: use the day it occurs
            }
            if (hit != null && hit.getYear() == year) return hit;
        }
        return null;
    }

    /** A computed calendar event. type: fest | hol | obs | sky | moon. */
    public static final class Ev {
        public final String name, emoji, type, note; public final LocalDate date;
        Ev(String n, String e, String t, LocalDate d, String note) { name = n; emoji = e; type = t; date = d; this.note = note; }
    }

    // name, emoji, type, lunar month, tithi (1-15 Shukla, 16-30 Krishna), IST hour at which the tithi is checked (calibrated on 2023-2027 dates)
    private static final Object[][] RULES = {
        {"Maha Shivaratri", "🔱", "fest", 11, 29, 22.0},
        {"Holika Dahan", "🔥", "fest", 12, 15, 14.0},
        {"Ugadi / Gudi Padwa / Cheti Chand", "🌸", "fest", 1, 1, 10.0},
        {"Chaitra Navratri begins", "🪔", "fest", 1, 1, 10.0},
        {"Ram Navami", "🏹", "hol", 1, 9, 12.0},
        {"Mahavir Jayanti", "🙏", "hol", 1, 13, 3.0},
        {"Hanuman Jayanti", "🐒", "fest", 1, 15, 6.5},
        {"Akshaya Tritiya", "✨", "fest", 2, 3, 10.0},
        {"Buddha Purnima", "☸️", "hol", 2, 15, 10.0},
        {"Rath Yatra", "🛕", "fest", 4, 2, 6.5},
        {"Guru Purnima", "🙏", "fest", 4, 15, 8.0},
        {"Raksha Bandhan", "🧵", "fest", 5, 15, 6.5},
        {"Janmashtami", "🦚", "hol", 5, 23, 18.0},
        {"Ganesh Chaturthi", "🐘", "fest", 6, 4, 10.0},
        {"Sharad Navratri begins", "🪔", "fest", 7, 1, 8.0},
        {"Maha Saptami", "🪔", "fest", 7, 7, 10.0},
        {"Maha Ashtami", "🪔", "fest", 7, 8, 10.0},
        {"Dussehra (Vijayadashami)", "🏹", "hol", 7, 10, 14.0},
        {"Karva Chauth", "🌙", "fest", 7, 19, 14.0},
        {"Dhanteras", "🪙", "fest", 7, 28, 17.0},
        {"Diwali", "🪔", "hol", 7, 30, 17.0},
        {"Govardhan Puja", "⛰️", "fest", 8, 1, 8.0},
        {"Bhai Dooj", "👫", "fest", 8, 2, 8.0},
        {"Guru Nanak Jayanti", "🙏", "hol", 8, 15, 8.0},
        {"Dev Diwali (Kartik Purnima)", "🪔", "fest", 8, 15, 8.0},
        {"Basant Panchami", "🌼", "fest", 11, 5, 10.0}};

    /** Easter Sunday (Gregorian). */
    public static LocalDate easter(int y) {
        int a = y % 19, b = y / 100, c = y % 100, d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31, day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(y, month, day);
    }

    /** Every date in Gregorian year [gy] on which Hijri day [hd] of month [hm] falls (a Hijri date can occur twice in one year). */
    private static List<LocalDate> hijri(int gy, int hm, int hd) {
        List<LocalDate> out = new ArrayList<>();
        int est = (int) Math.round((gy - 622) * 33.0 / 32.0);
        for (int hy = est - 1; hy <= est + 1; hy++) {
            try {
                LocalDate g = LocalDate.from(HijrahDate.of(hy, hm, hd)).plusDays(1); // +1: India usually sights the moon a day after Saudi Arabia
                if (g.getYear() == gy) out.add(g);
            } catch (RuntimeException ignored) { }
        }
        return out;
    }

    private static String monthName(List<Month> ms, double jd) {
        for (Month m : ms) if (jd >= m.startJd && jd < m.endJd) return (m.adhik ? "Adhik " : "") + MONTHS[m.no];
        return "";
    }

    /** All computed events of one calendar year, sorted by date. */
    public static List<Ev> generate(int year) {
        List<Ev> out = new ArrayList<>();
        for (Object[] r : RULES) {
            LocalDate d = festival(year, (int) r[3], (int) r[4], (double) r[5]);
            if (d != null) out.add(new Ev((String) r[0], (String) r[1], (String) r[2], d, ""));
        }
        LocalDate hd = festival(year, 12, 15, 14.0);
        if (hd != null) out.add(new Ev("Holi", "🎨", "hol", hd.plusDays(1), ""));
        LocalDate easter = easter(year);
        out.add(new Ev("Easter", "🐣", "fest", easter, ""));
        out.add(new Ev("Good Friday", "✝️", "hol", easter.minusDays(2), ""));
        String sight = "Moon-sighting dependent";
        for (LocalDate x : hijri(year, 10, 1)) out.add(new Ev("Eid-ul-Fitr", "🌙", "hol", x, sight));
        for (LocalDate x : hijri(year, 12, 10)) out.add(new Ev("Eid al-Adha (Bakrid)", "🐑", "hol", x, sight));
        for (LocalDate x : hijri(year, 1, 10)) out.add(new Ev("Muharram", "🌙", "hol", x, sight));
        for (LocalDate x : hijri(year, 3, 12)) out.add(new Ev("Milad-un-Nabi", "🌙", "hol", x, sight));

        // Outer-planet oppositions from their synodic periods (approximate, a few days either way)
        LocalDate jup = LocalDate.of(2026, 1, 10), sat = LocalDate.of(2026, 10, 4);
        for (int n = -40; n <= 40; n++) {
            LocalDate dj = jup.plusDays(Math.round(n * 398.884)), ds = sat.plusDays(Math.round(n * 378.092));
            if (dj.getYear() == year) out.add(new Ev("Jupiter at Opposition", "🪐", "sky", dj, "Brightest of the year. Approximate date, a few days either way"));
            if (ds.getYear() == year) out.add(new Ev("Saturn at Opposition", "🪐", "sky", ds, "Brightest of the year; rings show in a telescope. Approximate date, a few days either way"));
        }

        // Equinoxes and solstices
        double[] targets = {0, 90, 180, 270};
        String[] names = {"Spring Equinox", "Summer Solstice", "Autumn Equinox", "Winter Solstice"};
        String[] emo = {"🌸", "☀️", "🌗", "🌅"};
        String[] notes = {"Day and night nearly equal", "Longest day in the northern hemisphere", "Day and night nearly equal", "Shortest day in the northern hemisphere"};
        for (int i = 0; i < 4; i++)
            for (double t : Astro.crossings(Astro::sunLon, targets[i], Astro.jdOf(year, 1, 1), Astro.jdOf(year, 12, 31), 1.0))
                out.add(new Ev(names[i], emo[i], "sky", Astro.istDate(t), notes[i]));

        // Moon phases and eclipses
        List<Month> ms = months(year);
        double j0 = Astro.jdOf(year, 1, 1), j1 = Astro.jdOf(year, 12, 31) + 1;
        for (int k = 0; k < 2; k++) {
            double target = k == 0 ? 0 : 180;
            for (double t : Astro.crossings(Astro::elong, target, j0, j1, 1.0)) {
                LocalDate d = Astro.istDate(t);
                if (d.getYear() != year) continue;
                String mn = k == 0 ? monthName(ms, t - 0.02) : monthName(ms, t);
                out.add(k == 0 ? new Ev("New Moon (Amavasya)", "🌑", "moon", d, mn.isEmpty() ? "" : mn + " Amavasya")
                               : new Ev("Full Moon (Purnima)", "🌕", "moon", d, mn.isEmpty() ? "" : mn + " Purnima"));
                double[] m = Astro.moon(t);
                double pi = Math.toDegrees(Math.asin(6378.14 / m[2])), sm = 0.2725 * pi, R = Astro.sunDistAu(t), ss = 0.26656 / R, ps = 0.002443 / R, beta = Math.abs(m[1]);
                if (k == 0) {
                    double g = beta / pi;
                    if (g < 1.54) {
                        String type = g < 0.9972 ? (sm > ss ? "Total" : "Annular") : "Partial";
                        out.add(new Ev("Solar Eclipse (Surya Grahan)", "🌑", "sky", d, type + ". Visibility varies by place; check a local source"));
                    }
                } else {
                    double ru = 1.02 * (pi + ps - ss), rp = 1.02 * (pi + ps + ss);
                    if (beta < rp + sm) {
                        String type = beta < ru - sm ? "Total" : (beta < ru + sm ? "Partial" : "Penumbral");
                        out.add(new Ev("Lunar Eclipse (Chandra Grahan)", "🌕", "sky", d, type + ". Visibility varies by place; check a local source"));
                    }
                }
            }
        }
        Collections.sort(out, Comparator.comparing((Ev e) -> e.date));
        return out;
    }
}
