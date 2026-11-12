package mcjty.lostcities.worldgen.street;

/** A short access road. The origin lies on an existing primary/secondary road. */
public final class TertiaryRoadSegment  {
    private final long id;
    public long id() { return id; }
    private final int originX;
    public int originX() { return originX; }
    private final int originZ;
    public int originZ() { return originZ; }
    private final RoadDirection direction;
    public RoadDirection direction() { return direction; }
    private final int length;
    public int length() { return length; }
    public TertiaryRoadSegment(long id, int originX, int originZ, RoadDirection direction, int length) {

        this.id = id;
        this.originX = originX;
        this.originZ = originZ;
        this.direction = direction;
        this.length = length;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof TertiaryRoadSegment)) return false; TertiaryRoadSegment other = (TertiaryRoadSegment) value; return java.util.Objects.equals(id, other.id) && java.util.Objects.equals(originX, other.originX) && java.util.Objects.equals(originZ, other.originZ) && java.util.Objects.equals(direction, other.direction) && java.util.Objects.equals(length, other.length); }
    @Override public int hashCode() { return java.util.Objects.hash(id, originX, originZ, direction, length); }

    public boolean contains(int chunkX, int chunkZ) {
        int dx = chunkX - originX;
        int dz = chunkZ - originZ;
        int distance = dx * direction.stepX() + dz * direction.stepZ();
        return distance >= 1 && distance <= length
                && dx * direction.stepZ() == dz * direction.stepX();
    }
}

