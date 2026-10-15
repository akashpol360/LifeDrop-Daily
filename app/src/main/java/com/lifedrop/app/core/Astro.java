package com.lifedrop.app.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/** Low-precision sun and moon positions (Meeus, Astronomical Algorithms) and event finders. Pure Java, no Android classes. */
public final class Astro {
    private Astro() {}
    public static final ZoneOffset IST = ZoneOffset.ofHoursMinutes(5, 30);

    public static double toJd(Instant i) { return i.getEpochSecond() / 86400.0 + i.getNano() / 86400e9 + 2440587.5; }
    public static Instant fromJd(double jd) {
        double s = (jd - 2440587.5) * 86400.0; long sec = (long) Math.floor(s);
        return Instant.ofEpochSecond(sec, (long) ((s - sec) * 1e9));
    }
    public static LocalDate istDate(double jd) { return fromJd(jd).atOffset(IST).toLocalDate(); }
    /** Julian day (UTC) for a local IST date and decimal hour. */
    public static double jdIst(LocalDate d, double hour) {
        return d.atStartOfDay().toInstant(IST).getEpochSecond() / 86400.0 + 2440587.5 + hour / 24.0;
    }
    static double norm(double x) { x %= 360.0; return x < 0 ? x + 360.0 : x; }
    static double wrap180(double x) { x = norm(x); return x > 180 ? x - 360 : x; }
    static double r(double d) { return Math.toRadians(d); }

    /** Apparent ecliptic longitude of the Sun (tropical, degrees). */
    public static double sunLon(double jd) {
        double T = (jd - 2451545.0) / 36525.0;
        double L0 = 280.46646 + 36000.76983 * T + 0.0003032 * T * T;
        double M = 357.52911 + 35999.05029 * T - 0.0001537 * T * T;
        double C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * Math.sin(r(M)) + (0.019993 - 0.000101 * T) * Math.sin(r(2 * M)) + 0.000289 * Math.sin(r(3 * M));
        double om = 125.04 - 1934.136 * T;
        return norm(L0 + C - 0.00569 - 0.00478 * Math.sin(r(om)));
    }
    public static double sunDistAu(double jd) {
        double T = (jd - 2451545.0) / 36525.0;
        double M = 357.52911 + 35999.05029 * T;
        double C = (1.914602 - 0.004817 * T) * Math.sin(r(M)) + 0.019993 * Math.sin(r(2 * M));
        double e = 0.016708634 - 0.000042037 * T;
        return 1.000001018 * (1 - e * e) / (1 + e * Math.cos(r(M + C)));
    }
    /** Lahiri ayanamsa (degrees). */
    public static double ayanamsa(double jd) { return 23.853 + (jd - 2451545.0) / 365.25 * 0.013972; }

    // D, M, M', F, sum-l (1e-6 deg), sum-r (1e-3 km)
    private static final double[][] LR = {
        {0,0,1,0,6288774,-20905355},{2,0,-1,0,1274027,-3699111},{2,0,0,0,658314,-2955968},{0,0,2,0,213618,-569925},
        {0,1,0,0,-185116,48888},{0,0,0,2,-114332,-3149},{2,0,-2,0,58793,246158},{2,-1,-1,0,57066,-152138},
        {2,0,1,0,53322,-170733},{2,-1,0,0,45758,-204586},{0,1,-1,0,-40923,-129620},{1,0,0,0,-34720,108743},
        {0,1,1,0,-30383,104755},{2,0,0,-2,15327,10321},{0,0,1,2,-12528,0},{0,0,1,-2,10980,79661},
        {4,0,-1,0,10675,-34782},{0,0,3,0,10034,-23210},{4,0,-2,0,8548,-21636},{2,1,-1,0,-7888,24208},
        {2,1,0,0,-6766,30824},{1,0,-1,0,-5163,-8379},{1,1,0,0,4987,-16675},{2,-1,1,0,4036,-12831},
        {2,0,2,0,3994,-10445},{4,0,0,0,3861,-11650},{2,0,-3,0,3665,14403},{0,1,-2,0,-2689,-7003},
        {2,0,-1,2,-2602,0},{2,-1,-2,0,2390,10056},{1,0,1,0,-2348,6322},{2,-2,0,0,2236,-9884}};
    private static final double[][] B = {
        {0,0,0,1,5128122},{0,0,1,1,280602},{0,0,1,-1,277693},{2,0,0,-1,173237},{2,0,-1,1,55413},{2,0,-1,-1,46271},
        {2,0,0,1,32573},{0,0,2,1,17198},{2,0,1,-1,9266},{0,0,2,-1,8822},{2,-1,0,-1,8216},{2,0,-2,-1,4324},
        {2,0,1,1,4200},{2,1,0,-1,-3359},{2,-1,-1,1,2463},{2,-1,0,1,2211},{2,-1,-1,-1,2065},{0,1,-1,-1,-1870},
        {4,0,-1,-1,1828},{0,1,0,1,-1794},{0,0,0,3,-1749},{0,1,-1,1,-1565},{1,0,0,1,-1491},{0,1,1,1,-1475},
        {0,1,1,-1,-1410},{0,1,0,-1,-1344},{1,0,0,-1,-1335},{0,0,3,1,1107},{4,0,0,-1,1021},{4,0,-1,1,833}};
    private static double ePow(double m, double E) { double a = Math.abs(m); return a == 0 ? 1 : a == 1 ? E : E * E; }

    /** {tropical longitude deg, latitude deg, distance km} of the Moon. */
    public static double[] moon(double jd) {
        double T = (jd - 2451545.0) / 36525.0, T2 = T * T, T3 = T2 * T, T4 = T3 * T;
        double Lp = 218.3164477 + 481267.88123421 * T - 0.0015786 * T2 + T3 / 538841.0 - T4 / 65194000.0;
        double D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T2 + T3 / 545868.0 - T4 / 113065000.0;
        double M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T2 + T3 / 24490000.0;
        double Mp = 134.9633964 + 477198.8675055 * T + 0.0087414 * T2 + T3 / 69699.0 - T4 / 14712000.0;
        double F = 93.2720950 + 483202.0175233 * T - 0.0036539 * T2 - T3 / 3526000.0 + T4 / 863310000.0;
        double A1 = 119.75 + 131.849 * T, A2 = 53.09 + 479264.290 * T, A3 = 313.45 + 481266.484 * T;
        double E = 1 - 0.002516 * T - 0.0000074 * T2;
        double sl = 0, sr = 0, sb = 0;
        for (double[] t : LR) {
            double a = r(t[0] * D + t[1] * M + t[2] * Mp + t[3] * F), e = ePow(t[1], E);
            sl += t[4] * e * Math.sin(a); sr += t[5] * e * Math.cos(a);
        }
        for (double[] t : B) sb += t[4] * ePow(t[1], E) * Math.sin(r(t[0] * D + t[1] * M + t[2] * Mp + t[3] * F));
        sl += 3958 * Math.sin(r(A1)) + 1962 * Math.sin(r(Lp - F)) + 318 * Math.sin(r(A2));
        sb += -2235 * Math.sin(r(Lp)) + 382 * Math.sin(r(A3)) + 175 * Math.sin(r(A1 - F)) + 175 * Math.sin(r(A1 + F)) + 127 * Math.sin(r(Lp - Mp)) - 115 * Math.sin(r(Lp + Mp));
        return new double[]{norm(Lp + sl / 1e6), sb / 1e6, 385000.56 + sr / 1000.0};
    }
    public static double elong(double jd) { return norm(moon(jd)[0] - sunLon(jd)); }
    /** Tithi number 1..30 (1-15 Shukla, 16-30 Krishna) at the instant. */
    public static int tithi(double jd) { return (int) Math.floor(elong(jd) / 12.0) + 1; }

    /** First time in [jd0, jd1] where f(jd) crosses target (degrees, increasing quantity). Returns NaN if none. */
    public static List<Double> crossings(DoubleUnaryOperator f, double target, double jd0, double jd1, double step) {
        List<Double> out = new ArrayList<>();
        double a = jd0, ga = wrap180(f.applyAsDouble(a) - target);
        for (double b = jd0 + step; b <= jd1 + step; b += step) {
            double gb = wrap180(f.applyAsDouble(b) - target);
            if (ga < 0 && gb >= 0 && gb - ga < 90) {
                double lo = a, hi = b;
                for (int i = 0; i < 45; i++) {
                    double mid = (lo + hi) / 2;
                    if (wrap180(f.applyAsDouble(mid) - target) < 0) lo = mid; else hi = mid;
                }
                out.add((lo + hi) / 2);
            }
            a = b; ga = gb;
        }
        return out;
    }
    public static double jdOf(int y, int m, int d) { return jdIst(LocalDate.of(y, m, d), 0); }
}
