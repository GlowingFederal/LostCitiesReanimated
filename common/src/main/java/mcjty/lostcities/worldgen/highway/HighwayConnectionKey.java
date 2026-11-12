package mcjty.lostcities.worldgen.highway;

public final class HighwayConnectionKey  implements Comparable<HighwayConnectionKey> {
    private final HubKey first;
    public HubKey first() { return first; }
    private final HubKey second;
    public HubKey second() { return second; }
    public HighwayConnectionKey(HubKey first, HubKey second) {

        if (first.compareTo(second) >= 0) {
            throw new IllegalArgumentException("Highway connection endpoints must be distinct and canonical");
        }
    
        this.first = first;
        this.second = second;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof HighwayConnectionKey)) return false; HighwayConnectionKey other = (HighwayConnectionKey) value; return java.util.Objects.equals(first, other.first) && java.util.Objects.equals(second, other.second); }
    @Override public int hashCode() { return java.util.Objects.hash(first, second); }
    @Override public String toString() { return "HighwayConnectionKey[" + "first=" + first + ", " + "second=" + second + "]"; }

    

    public static HighwayConnectionKey of(HubKey a, HubKey b) {
        return a.compareTo(b) < 0 ? new HighwayConnectionKey(a, b) : new HighwayConnectionKey(b, a);
    }

    public HubKey owner() {
        return first;
    }

    @Override
    public int compareTo(HighwayConnectionKey other) {
        int comparison = first.compareTo(other.first);
        return comparison != 0 ? comparison : second.compareTo(other.second);
    }
}

