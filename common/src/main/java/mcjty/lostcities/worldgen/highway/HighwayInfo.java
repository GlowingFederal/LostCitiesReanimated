package mcjty.lostcities.worldgen.highway;

import java.util.List;

public final class HighwayInfo  {
    private final int xLevel;
    public int xLevel() { return xLevel; }
    private final int zLevel;
    public int zLevel() { return zLevel; }
    private final Classification classification;
    public Classification classification() { return classification; }
    private final List<RouteHit> routeHits;
    public List<RouteHit> routeHits() { return routeHits; }
    public HighwayInfo(int xLevel, int zLevel, Classification classification, List<RouteHit> routeHits) {

        routeHits = mcjty.lostcities.core.LegacyLists.copyOf(routeHits);
    
        this.xLevel = xLevel;
        this.zLevel = zLevel;
        this.classification = classification;
        this.routeHits = routeHits;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof HighwayInfo)) return false; HighwayInfo other = (HighwayInfo) value; return java.util.Objects.equals(xLevel, other.xLevel) && java.util.Objects.equals(zLevel, other.zLevel) && java.util.Objects.equals(classification, other.classification) && java.util.Objects.equals(routeHits, other.routeHits); }
    @Override public int hashCode() { return java.util.Objects.hash(xLevel, zLevel, classification, routeHits); }
    @Override public String toString() { return "HighwayInfo[" + "xLevel=" + xLevel + ", " + "zLevel=" + zLevel + ", " + "classification=" + classification + ", " + "routeHits=" + routeHits + "]"; }

    public static final HighwayInfo NONE = new HighwayInfo(-1, -1, Classification.NONE, mcjty.lostcities.core.LegacyLists.of());

    

    public boolean hasHighway() {
        return xLevel >= 0 || zLevel >= 0;
    }

    public enum Classification {
        NONE,
        X_HIGHWAY,
        Z_HIGHWAY,
        SAME_LEVEL_INTERSECTION,
        MULTI_LEVEL_INTERSECTION
    }

    public static final class RouteHit  {
    private final HighwayRoute route;
    public HighwayRoute route() { return route; }
    private final boolean xAxis;
    public boolean xAxis() { return xAxis; }
    private final boolean zAxis;
    public boolean zAxis() { return zAxis; }
    private final boolean bend;
    public boolean bend() { return bend; }
    public RouteHit(HighwayRoute route, boolean xAxis, boolean zAxis, boolean bend) {

        this.route = route;
        this.xAxis = xAxis;
        this.zAxis = zAxis;
        this.bend = bend;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof RouteHit)) return false; RouteHit other = (RouteHit) value; return java.util.Objects.equals(route, other.route) && java.util.Objects.equals(xAxis, other.xAxis) && java.util.Objects.equals(zAxis, other.zAxis) && java.util.Objects.equals(bend, other.bend); }
    @Override public int hashCode() { return java.util.Objects.hash(route, xAxis, zAxis, bend); }
    @Override public String toString() { return "RouteHit[" + "route=" + route + ", " + "xAxis=" + xAxis + ", " + "zAxis=" + zAxis + ", " + "bend=" + bend + "]"; }
 }

}

