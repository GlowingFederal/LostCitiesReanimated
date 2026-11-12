package mcjty.lostcities.worldgen.highway;

import java.util.Optional;

/**
 * Persistent backing for the expensive zero-or-one hub decision of a planning cell.
 */
public interface HighwayHubPersistence {

    HighwayHubPersistence NONE = new HighwayHubPersistence() {
        @Override
        public Lookup get(HubKey key) {
            return Lookup.missing();
        }

        @Override
        public void put(HubKey key, Optional<HighwayHub> hub) {
        }
    };

    Lookup get(HubKey key);

    void put(HubKey key, Optional<HighwayHub> hub);

    static final class Lookup  {
    private final boolean known;
    public boolean known() { return known; }
    private final Optional<HighwayHub> hub;
    public Optional<HighwayHub> hub() { return hub; }
    public Lookup(boolean known, Optional<HighwayHub> hub) {

        this.known = known;
        this.hub = hub;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof Lookup)) return false; Lookup other = (Lookup) value; return java.util.Objects.equals(known, other.known) && java.util.Objects.equals(hub, other.hub); }
    @Override public int hashCode() { return java.util.Objects.hash(known, hub); }
    @Override public String toString() { return "Lookup[" + "known=" + known + ", " + "hub=" + hub + "]"; }

        public static Lookup missing() {
            return new Lookup(false, Optional.empty());
        }

        public static Lookup known(Optional<HighwayHub> hub) {
            return new Lookup(true, hub);
        }
    }

}
