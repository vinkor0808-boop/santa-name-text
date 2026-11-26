package de.windfwm.money;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.data.Models;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Berechnet Statistiken aus der Geldhistorie. Ergebnisse werden zwischengespeichert. */
public final class MoneyStats {
    public double ratePerHour;
    public double sessionIncome, sessionExpense, sessionRate;
    public double todayIncome, todayExpense;
    public double totalIncome, totalExpense;
    public int totalCount, todayCount;
    public double bestHour;
    /** Netto-Einnahmen der letzten 24 Stunden, ältester Wert zuerst. */
    public final double[] hourly = new double[24];
    public final Map<String, Double> incomeByCategory = new LinkedHashMap<>();
    public final List<Map.Entry<String, Double>> topSources = new ArrayList<>();

    private int lastVersion = -1;
    private long lastCompute;
    private String lastServer = "";

    public void update(String server, long sessionStart) {
        long now = System.currentTimeMillis();
        int v = WindFwm.money().version();
        if (v == lastVersion && server.equals(lastServer) && now - lastCompute < 1000) return;
        lastVersion = v;
        lastServer = server;
        lastCompute = now;
        compute(WindFwm.data().profile(server), sessionStart, now);
    }

    private void compute(Models.ServerProfile p, long sessionStart, long now) {
        WindConfig.Stats cfg = WindFwm.cfg().stats;
        long windowMs = Math.max(5, cfg.rateWindowMinutes) * 60_000L;
        long todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long hourMs = 3_600_000L;
        long hourAnchor = (now / hourMs) * hourMs; // Beginn der aktuellen Stunde

        sessionIncome = sessionExpense = todayIncome = todayExpense = totalIncome = totalExpense = 0;
        totalCount = todayCount = 0;
        double windowSum = 0;
        java.util.Arrays.fill(hourly, 0);
        incomeByCategory.clear();
        Map<String, Double> sources = new HashMap<>();

        for (Models.MoneyEntry e : p.entries) {
            double a = e.amount;
            boolean income = a > 0;
            totalCount++;
            if (income) totalIncome += a; else totalExpense -= a;
            if (e.t >= todayStart) {
                todayCount++;
                if (income) todayIncome += a; else todayExpense -= a;
            }
            if (sessionStart > 0 && e.t >= sessionStart) {
                if (income) sessionIncome += a; else sessionExpense -= a;
            }
            if (e.t >= now - windowMs) {
                if (income) windowSum += a;
                else if (cfg.netRate) windowSum += a;
            }
            long idx = (hourAnchor - (e.t / hourMs) * hourMs) / hourMs; // 0 = aktuelle Stunde
            if (idx >= 0 && idx < 24) hourly[23 - (int) idx] += a;
            if (income) {
                String cat = e.category == null || e.category.isBlank() ? "Sonstiges" : e.category;
                incomeByCategory.merge(cat, a, Double::sum);
                if (e.source != null && !e.source.isBlank()) sources.merge(e.source, a, Double::sum);
            }
        }

        // Ist die Sitzung kürzer als das Zeitfenster, auf die tatsächliche Dauer hochrechnen (mind. 5 Minuten).
        long effective = windowMs;
        if (sessionStart > 0) effective = Math.min(windowMs, Math.max(5 * 60_000L, now - sessionStart));
        ratePerHour = windowSum / (effective / (double) hourMs);

        double sessionNet = cfg.netRate ? sessionIncome - sessionExpense : sessionIncome;
        long sessionLen = sessionStart > 0 ? Math.max(5 * 60_000L, now - sessionStart) : 0;
        sessionRate = sessionLen > 0 ? sessionNet / (sessionLen / (double) hourMs) : 0;

        bestHour = 0;
        for (double h : hourly) bestHour = Math.max(bestHour, h);

        topSources.clear();
        sources.entrySet().stream()
                .sorted((x, y) -> Double.compare(y.getValue(), x.getValue()))
                .limit(5)
                .forEach(topSources::add);

        // Kategorien absteigend sortieren
        List<Map.Entry<String, Double>> cats = new ArrayList<>(incomeByCategory.entrySet());
        cats.sort((x, y) -> Double.compare(y.getValue(), x.getValue()));
        incomeByCategory.clear();
        for (Map.Entry<String, Double> c : cats) incomeByCategory.put(c.getKey(), c.getValue());
    }

    public void invalidate() {
        lastVersion = -1;
    }
}
