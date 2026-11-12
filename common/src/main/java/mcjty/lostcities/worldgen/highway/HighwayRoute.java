package mcjty.lostcities.worldgen.highway;

import java.util.List;

public final class HighwayRoute  {
    private final HighwayConnectionKey key;
    public HighwayConnectionKey key() { return key; }
    private final HighwayHub firstHub;
    public HighwayHub firstHub() { return firstHub; }
    private final HighwayHub secondHub;
    public HighwayHub secondHub() { return secondHub; }
    private final List<HighwaySegment> segments;
    public List<HighwaySegment> segments() { return segments; }
    private final int highwayLevel;
    public int highwayLevel() { return highwayLevel; }
    private final RouteShape shape;
    public RouteShape shape() { return shape; }
    private final int routeLength;
    public int routeLength() { return routeLength; }
    private final long cityPenalty;
    public long cityPenalty() { return cityPenalty; }
    public HighwayRoute(HighwayConnectionKey key, HighwayHub firstHub, HighwayHub secondHub, List<HighwaySegment> segments, int highwayLevel, RouteShape shape, int routeLength, long cityPenalty) {

        segments = mcjty.lostcities.core.LegacyLists.copyOf(segments);
    
        this.key = key;
        this.firstHub = firstHub;
        this.secondHub = secondHub;
        this.segments = segments;
        this.highwayLevel = highwayLevel;
        this.shape = shape;
        this.routeLength = routeLength;
        this.cityPenalty = cityPenalty;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof HighwayRoute)) return false; HighwayRoute other = (HighwayRoute) value; return java.util.Objects.equals(key, other.key) && java.util.Objects.equals(firstHub, other.firstHub) && java.util.Objects.equals(secondHub, other.secondHub) && java.util.Objects.equals(segments, other.segments) && java.util.Objects.equals(highwayLevel, other.highwayLevel) && java.util.Objects.equals(shape, other.shape) && java.util.Objects.equals(routeLength, other.routeLength) && java.util.Objects.equals(cityPenalty, other.cityPenalty); }
    @Override public int hashCode() { return java.util.Objects.hash(key, firstHub, secondHub, segments, highwayLevel, shape, routeLength, cityPenalty); }
    @Override public String toString() { return "HighwayRoute[" + "key=" + key + ", " + "firstHub=" + firstHub + ", " + "secondHub=" + secondHub + ", " + "segments=" + segments + ", " + "highwayLevel=" + highwayLevel + ", " + "shape=" + shape + ", " + "routeLength=" + routeLength + ", " + "cityPenalty=" + cityPenalty + "]"; }

    

    public boolean contains(int chunkX, int chunkZ) {
        return segments.stream().anyMatch(segment -> segment.contains(chunkX, chunkZ));
    }

    public enum RouteShape {
        STRAIGHT,
        HORIZONTAL_THEN_VERTICAL,
        VERTICAL_THEN_HORIZONTAL
    }
}

