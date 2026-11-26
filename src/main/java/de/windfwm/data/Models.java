package de.windfwm.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gespeicherte Daten (öffentliche Felder für Gson). */
public final class Models {
    private Models() {}

    /** Order-Daten pro Server (Schlüssel = Serveradresse). */
    public static class OrderData {
        public Map<String, OrderProfile> servers = new HashMap<>();
    }

    public static class OrderProfile {
        public List<Payment> payments = new ArrayList<>();
        public List<Claim> claims = new ArrayList<>();
    }

    /** "Order payment: +$X" */
    public static class Payment {
        public long t;
        public double amount;

        public Payment() {}

        public Payment(long t, double amount) {
            this.t = t;
            this.amount = amount;
        }
    }

    /** "Claimed N ordered item(s)." */
    public static class Claim {
        public long t;
        public int count;

        public Claim() {}

        public Claim(long t, int count) {
            this.t = t;
            this.count = count;
        }
    }
}
