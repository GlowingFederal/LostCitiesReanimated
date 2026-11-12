package mcjty.lostcities.worldgen.highway;



public final class HighwayPlannerSettings  {
    private final int planningCellSize;
    public int planningCellSize() { return planningCellSize; }
    private final int hubSampleSpacing;
    public int hubSampleSpacing() { return hubSampleSpacing; }
    private final float hubMinimumPotential;
    public float hubMinimumPotential() { return hubMinimumPotential; }
    private final int hubSearchRadiusCells;
    public int hubSearchRadiusCells() { return hubSearchRadiusCells; }
    private final int minimumHubDistance;
    public int minimumHubDistance() { return minimumHubDistance; }
    private final int maximumHubDistance;
    public int maximumHubDistance() { return maximumHubDistance; }
    private final int maximumConnectionsPerHub;
    public int maximumConnectionsPerHub() { return maximumConnectionsPerHub; }
    private final int minimumRouteLength;
    public int minimumRouteLength() { return minimumRouteLength; }
    private final float routeCityPenalty;
    public float routeCityPenalty() { return routeCityPenalty; }
    private final int levelFromCitiesMode;
    public int levelFromCitiesMode() { return levelFromCitiesMode; }
    private final int networkLevel;
    public int networkLevel() { return networkLevel; }
    private final int railwaySpacingNorthSouth;
    public int railwaySpacingNorthSouth() { return railwaySpacingNorthSouth; }
    private final int railwaySpacingEastWest;
    public int railwaySpacingEastWest() { return railwaySpacingEastWest; }
    public HighwayPlannerSettings(int planningCellSize, int hubSampleSpacing, float hubMinimumPotential, int hubSearchRadiusCells, int minimumHubDistance, int maximumHubDistance, int maximumConnectionsPerHub, int minimumRouteLength, float routeCityPenalty, int levelFromCitiesMode, int networkLevel, int railwaySpacingNorthSouth, int railwaySpacingEastWest) {

        if (planningCellSize < 32 || planningCellSize > 512) {
            throw new IllegalArgumentException("Highway planning-cell size must be between 32 and 512 chunks");
        }
        if (hubSampleSpacing < 1 || hubSampleSpacing > planningCellSize) {
            throw new IllegalArgumentException("Highway hub sample spacing must be positive and no larger than the planning cell");
        }
        if (hubMinimumPotential < 0.0f || hubMinimumPotential > 1.0f) {
            throw new IllegalArgumentException("Highway hub minimum potential must be between 0 and 1");
        }
        if (hubSearchRadiusCells < 0 || hubSearchRadiusCells > 8) {
            throw new IllegalArgumentException("Highway hub search radius must be between 0 and 8 cells");
        }
        if (minimumHubDistance < 0 || minimumHubDistance > maximumHubDistance) {
            throw new IllegalArgumentException("Highway minimum hub distance must not exceed the maximum");
        }
        if (maximumHubDistance > 4096) {
            throw new IllegalArgumentException("Highway maximum hub distance must not exceed 4096 chunks");
        }
        if (maximumConnectionsPerHub < 1 || maximumConnectionsPerHub > 8) {
            throw new IllegalArgumentException("Highway maximum connection degree must be between 1 and 8");
        }
        if (minimumRouteLength < 0 || routeCityPenalty < 0.0f || routeCityPenalty > 1000.0f) {
            throw new IllegalArgumentException("Invalid highway route length or city penalty");
        }
        if (levelFromCitiesMode < 0 || levelFromCitiesMode > 4 || networkLevel < 0 || networkLevel > 32) {
            throw new IllegalArgumentException("Invalid highway level mode or fixed network level");
        }
        if (railwaySpacingNorthSouth < 4 || railwaySpacingNorthSouth > 128
                || railwaySpacingEastWest < 4 || railwaySpacingEastWest > 128
                || (railwaySpacingNorthSouth & 1) != 0 || (railwaySpacingEastWest & 1) != 0) {
            throw new IllegalArgumentException("Railway spacing must be an even number from " + 4
                    + " through " + 128 + " chunks");
        }
    
        this.planningCellSize = planningCellSize;
        this.hubSampleSpacing = hubSampleSpacing;
        this.hubMinimumPotential = hubMinimumPotential;
        this.hubSearchRadiusCells = hubSearchRadiusCells;
        this.minimumHubDistance = minimumHubDistance;
        this.maximumHubDistance = maximumHubDistance;
        this.maximumConnectionsPerHub = maximumConnectionsPerHub;
        this.minimumRouteLength = minimumRouteLength;
        this.routeCityPenalty = routeCityPenalty;
        this.levelFromCitiesMode = levelFromCitiesMode;
        this.networkLevel = networkLevel;
        this.railwaySpacingNorthSouth = railwaySpacingNorthSouth;
        this.railwaySpacingEastWest = railwaySpacingEastWest;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof HighwayPlannerSettings)) return false; HighwayPlannerSettings other = (HighwayPlannerSettings) value; return java.util.Objects.equals(planningCellSize, other.planningCellSize) && java.util.Objects.equals(hubSampleSpacing, other.hubSampleSpacing) && java.util.Objects.equals(hubMinimumPotential, other.hubMinimumPotential) && java.util.Objects.equals(hubSearchRadiusCells, other.hubSearchRadiusCells) && java.util.Objects.equals(minimumHubDistance, other.minimumHubDistance) && java.util.Objects.equals(maximumHubDistance, other.maximumHubDistance) && java.util.Objects.equals(maximumConnectionsPerHub, other.maximumConnectionsPerHub) && java.util.Objects.equals(minimumRouteLength, other.minimumRouteLength) && java.util.Objects.equals(routeCityPenalty, other.routeCityPenalty) && java.util.Objects.equals(levelFromCitiesMode, other.levelFromCitiesMode) && java.util.Objects.equals(networkLevel, other.networkLevel) && java.util.Objects.equals(railwaySpacingNorthSouth, other.railwaySpacingNorthSouth) && java.util.Objects.equals(railwaySpacingEastWest, other.railwaySpacingEastWest); }
    @Override public int hashCode() { return java.util.Objects.hash(planningCellSize, hubSampleSpacing, hubMinimumPotential, hubSearchRadiusCells, minimumHubDistance, maximumHubDistance, maximumConnectionsPerHub, minimumRouteLength, routeCityPenalty, levelFromCitiesMode, networkLevel, railwaySpacingNorthSouth, railwaySpacingEastWest); }
    @Override public String toString() { return "HighwayPlannerSettings[" + "planningCellSize=" + planningCellSize + ", " + "hubSampleSpacing=" + hubSampleSpacing + ", " + "hubMinimumPotential=" + hubMinimumPotential + ", " + "hubSearchRadiusCells=" + hubSearchRadiusCells + ", " + "minimumHubDistance=" + minimumHubDistance + ", " + "maximumHubDistance=" + maximumHubDistance + ", " + "maximumConnectionsPerHub=" + maximumConnectionsPerHub + ", " + "minimumRouteLength=" + minimumRouteLength + ", " + "routeCityPenalty=" + routeCityPenalty + ", " + "levelFromCitiesMode=" + levelFromCitiesMode + ", " + "networkLevel=" + networkLevel + ", " + "railwaySpacingNorthSouth=" + railwaySpacingNorthSouth + ", " + "railwaySpacingEastWest=" + railwaySpacingEastWest + "]"; }

    


}

