package mcjty.lostcities.worldgen.highway;

public final class HubKey  implements Comparable<HubKey> {
    private final int planningCellX;
    public int planningCellX() { return planningCellX; }
    private final int planningCellZ;
    public int planningCellZ() { return planningCellZ; }
    public HubKey(int planningCellX, int planningCellZ) {

        this.planningCellX = planningCellX;
        this.planningCellZ = planningCellZ;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof HubKey)) return false; HubKey other = (HubKey) value; return java.util.Objects.equals(planningCellX, other.planningCellX) && java.util.Objects.equals(planningCellZ, other.planningCellZ); }
    @Override public int hashCode() { return java.util.Objects.hash(planningCellX, planningCellZ); }
    @Override public String toString() { return "HubKey[" + "planningCellX=" + planningCellX + ", " + "planningCellZ=" + planningCellZ + "]"; }

    @Override
    public int compareTo(HubKey other) {
        int comparison = Integer.compare(planningCellX, other.planningCellX);
        return comparison != 0 ? comparison : Integer.compare(planningCellZ, other.planningCellZ);
    }
}

