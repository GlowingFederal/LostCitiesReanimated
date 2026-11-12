package mcjty.lostcities.worldgen.street;

import java.util.List;

public final class PlannedStreetInfo  {
    private final PlannedRoadType roadType;
    public PlannedRoadType roadType() { return roadType; }
    private final boolean north;
    public boolean north() { return north; }
    private final boolean south;
    public boolean south() { return south; }
    private final boolean west;
    public boolean west() { return west; }
    private final boolean east;
    public boolean east() { return east; }
    private final int primaryBlockX;
    public int primaryBlockX() { return primaryBlockX; }
    private final int primaryBlockZ;
    public int primaryBlockZ() { return primaryBlockZ; }
    private final int primaryWestX;
    public int primaryWestX() { return primaryWestX; }
    private final int primaryNorthZ;
    public int primaryNorthZ() { return primaryNorthZ; }
    private final int primaryEastX;
    public int primaryEastX() { return primaryEastX; }
    private final int primarySouthZ;
    public int primarySouthZ() { return primarySouthZ; }
    private final double density;
    public double density() { return density; }
    private final List<Integer> secondaryRoadsX;
    public List<Integer> secondaryRoadsX() { return secondaryRoadsX; }
    private final List<Integer> secondaryRoadsZ;
    public List<Integer> secondaryRoadsZ() { return secondaryRoadsZ; }
    private final TertiaryRoadSegment tertiarySegment;
    public TertiaryRoadSegment tertiarySegment() { return tertiarySegment; }
    public PlannedStreetInfo(PlannedRoadType roadType, boolean north, boolean south, boolean west, boolean east, int primaryBlockX, int primaryBlockZ, int primaryWestX, int primaryNorthZ, int primaryEastX, int primarySouthZ, double density, List<Integer> secondaryRoadsX, List<Integer> secondaryRoadsZ, TertiaryRoadSegment tertiarySegment) {

        this.roadType = roadType;
        this.north = north;
        this.south = south;
        this.west = west;
        this.east = east;
        this.primaryBlockX = primaryBlockX;
        this.primaryBlockZ = primaryBlockZ;
        this.primaryWestX = primaryWestX;
        this.primaryNorthZ = primaryNorthZ;
        this.primaryEastX = primaryEastX;
        this.primarySouthZ = primarySouthZ;
        this.density = density;
        this.secondaryRoadsX = secondaryRoadsX;
        this.secondaryRoadsZ = secondaryRoadsZ;
        this.tertiarySegment = tertiarySegment;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof PlannedStreetInfo)) return false; PlannedStreetInfo other = (PlannedStreetInfo) value; return java.util.Objects.equals(roadType, other.roadType) && java.util.Objects.equals(north, other.north) && java.util.Objects.equals(south, other.south) && java.util.Objects.equals(west, other.west) && java.util.Objects.equals(east, other.east) && java.util.Objects.equals(primaryBlockX, other.primaryBlockX) && java.util.Objects.equals(primaryBlockZ, other.primaryBlockZ) && java.util.Objects.equals(primaryWestX, other.primaryWestX) && java.util.Objects.equals(primaryNorthZ, other.primaryNorthZ) && java.util.Objects.equals(primaryEastX, other.primaryEastX) && java.util.Objects.equals(primarySouthZ, other.primarySouthZ) && java.util.Objects.equals(density, other.density) && java.util.Objects.equals(secondaryRoadsX, other.secondaryRoadsX) && java.util.Objects.equals(secondaryRoadsZ, other.secondaryRoadsZ) && java.util.Objects.equals(tertiarySegment, other.tertiarySegment); }
    @Override public int hashCode() { return java.util.Objects.hash(roadType, north, south, west, east, primaryBlockX, primaryBlockZ, primaryWestX, primaryNorthZ, primaryEastX, primarySouthZ, density, secondaryRoadsX, secondaryRoadsZ, tertiarySegment); }

    public boolean isRoad() {
        return roadType != PlannedRoadType.NONE;
    }

    public boolean connects(RoadDirection direction) {
        switch (direction) {
            case NORTH: return north;
            case SOUTH: return south;
            case WEST: return west;
            case EAST: return east;
            default: throw new IllegalArgumentException("Invalid road direction");
        }
    }
}

