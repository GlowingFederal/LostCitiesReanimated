package mcjty.lostcities.worldgen.highway;

public final class HighwaySegment  {
    private final int startX;
    public int startX() { return startX; }
    private final int startZ;
    public int startZ() { return startZ; }
    private final int endX;
    public int endX() { return endX; }
    private final int endZ;
    public int endZ() { return endZ; }
    private final HighwayAxis axis;
    public HighwayAxis axis() { return axis; }
    public HighwaySegment(int startX, int startZ, int endX, int endZ, HighwayAxis axis) {

        if (axis == HighwayAxis.X && startZ != endZ) {
            throw new IllegalArgumentException("X highway segment must have a constant Z coordinate");
        }
        if (axis == HighwayAxis.Z && startX != endX) {
            throw new IllegalArgumentException("Z highway segment must have a constant X coordinate");
        }
    
        this.startX = startX;
        this.startZ = startZ;
        this.endX = endX;
        this.endZ = endZ;
        this.axis = axis;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof HighwaySegment)) return false; HighwaySegment other = (HighwaySegment) value; return java.util.Objects.equals(startX, other.startX) && java.util.Objects.equals(startZ, other.startZ) && java.util.Objects.equals(endX, other.endX) && java.util.Objects.equals(endZ, other.endZ) && java.util.Objects.equals(axis, other.axis); }
    @Override public int hashCode() { return java.util.Objects.hash(startX, startZ, endX, endZ, axis); }
    @Override public String toString() { return "HighwaySegment[" + "startX=" + startX + ", " + "startZ=" + startZ + ", " + "endX=" + endX + ", " + "endZ=" + endZ + ", " + "axis=" + axis + "]"; }

    

    public boolean contains(int chunkX, int chunkZ) {
        return chunkX >= Math.min(startX, endX) && chunkX <= Math.max(startX, endX)
                && chunkZ >= Math.min(startZ, endZ) && chunkZ <= Math.max(startZ, endZ);
    }

    public int length() {
        return Math.abs(endX - startX) + Math.abs(endZ - startZ);
    }
}

