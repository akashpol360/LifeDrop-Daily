# How the festival / sky calendar stays current

**You do not need to update it every year.** Any year works:

- `core/Astro.java` computes sun and moon positions (Meeus formulas) and finds new/full moons, equinoxes, solstices and eclipses (type included).
- `core/Panchang.java` builds the Hindu lunar calendar from those positions (months, adhik maas, tithis) and places the major festivals (Holi, Ram Navami, Janmashtami, Ganesh Chaturthi, Navratri, Dussehra, Diwali, Raksha Bandhan, ...). It also computes Easter, Good Friday, Eid/Muharram/Milad-un-Nabi (Umm al-Qura calendar + 1 day for India) and approximate Jupiter/Saturn oppositions.
- Hours used to read each tithi were calibrated on ~120 official dates from 2023-2027 (118 matched; see `PanchangTest.java`).

`calendar.json` is a list of **fixed-date** days (Republic Day, Teachers' Day, meteor showers, ...) plus **verified 2026-2027 dates** from the DoPT list. Verified rows win over calculated ones, so you can also fix any date by adding a row.

Row format: `[name, emoji, type, when, note?]`. `type`: `fest`, `hol`, `obs`, `sky`. `when`: `"MM-DD"` (every year), `"YYYY-MM-DD"` (that day only) or `"R:month-weekday-nth"` (`R:5-7-2` = 2nd Sunday of May).

Known limits: calculated festival dates can differ by a day in tricky years (e.g. Diwali 2024: 31 Oct vs 1 Nov); solar/regional festivals such as Onam, Losar, Pongal timing and Planet events other than Jupiter/Saturn are not calculated; eclipse visibility depends on location. After each July (when DoPT publishes next year's list), add a row for any date you want to pin.
