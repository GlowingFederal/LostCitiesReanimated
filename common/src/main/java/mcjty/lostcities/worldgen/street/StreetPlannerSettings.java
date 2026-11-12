package mcjty.lostcities.worldgen.street;



public final class StreetPlannerSettings  {
    private final int primarySpacingX;
    public int primarySpacingX() { return primarySpacingX; }
    private final int primarySpacingZ;
    public int primarySpacingZ() { return primarySpacingZ; }
    private final float primaryOptionalChance;
    public float primaryOptionalChance() { return primaryOptionalChance; }
    private final int primaryForceEvery;
    public int primaryForceEvery() { return primaryForceEvery; }
    private final int secondaryMinCountX;
    public int secondaryMinCountX() { return secondaryMinCountX; }
    private final int secondaryMaxCountX;
    public int secondaryMaxCountX() { return secondaryMaxCountX; }
    private final int secondaryMinCountZ;
    public int secondaryMinCountZ() { return secondaryMinCountZ; }
    private final int secondaryMaxCountZ;
    public int secondaryMaxCountZ() { return secondaryMaxCountZ; }
    private final int minimumRoadSeparation;
    public int minimumRoadSeparation() { return minimumRoadSeparation; }
    private final int minimumEdgeDistance;
    public int minimumEdgeDistance() { return minimumEdgeDistance; }
    private final float tertiaryChance;
    public float tertiaryChance() { return tertiaryChance; }
    private final int tertiaryMinLength;
    public int tertiaryMinLength() { return tertiaryMinLength; }
    private final int tertiaryMaxLength;
    public int tertiaryMaxLength() { return tertiaryMaxLength; }
    public StreetPlannerSettings(int primarySpacingX, int primarySpacingZ, float primaryOptionalChance, int primaryForceEvery, int secondaryMinCountX, int secondaryMaxCountX, int secondaryMinCountZ, int secondaryMaxCountZ, int minimumRoadSeparation, int minimumEdgeDistance, float tertiaryChance, int tertiaryMinLength, int tertiaryMaxLength) {

        if (primarySpacingX < 8 || primarySpacingZ < 8) {
            throw new IllegalArgumentException("Primary road candidate spacing must be at least 8 chunks");
        }
        if (primaryOptionalChance < 0 || primaryOptionalChance > 1
                || primaryForceEvery < 1 || primaryForceEvery > 16) {
            throw new IllegalArgumentException("Invalid primary road activation settings");
        }
        if (secondaryMinCountX < 0 || secondaryMinCountZ < 0
                || secondaryMinCountX > secondaryMaxCountX || secondaryMinCountZ > secondaryMaxCountZ) {
            throw new IllegalArgumentException("Invalid secondary road count range");
        }
        if (minimumRoadSeparation < 2 || minimumEdgeDistance < 2) {
            throw new IllegalArgumentException("Road separation and edge distance must be at least 2 chunks");
        }
        if (tertiaryChance < 0 || tertiaryChance > 1 || tertiaryMinLength < 1 || tertiaryMinLength > tertiaryMaxLength) {
            throw new IllegalArgumentException("Invalid tertiary road settings");
        }
    
        this.primarySpacingX = primarySpacingX;
        this.primarySpacingZ = primarySpacingZ;
        this.primaryOptionalChance = primaryOptionalChance;
        this.primaryForceEvery = primaryForceEvery;
        this.secondaryMinCountX = secondaryMinCountX;
        this.secondaryMaxCountX = secondaryMaxCountX;
        this.secondaryMinCountZ = secondaryMinCountZ;
        this.secondaryMaxCountZ = secondaryMaxCountZ;
        this.minimumRoadSeparation = minimumRoadSeparation;
        this.minimumEdgeDistance = minimumEdgeDistance;
        this.tertiaryChance = tertiaryChance;
        this.tertiaryMinLength = tertiaryMinLength;
        this.tertiaryMaxLength = tertiaryMaxLength;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof StreetPlannerSettings)) return false; StreetPlannerSettings other = (StreetPlannerSettings) value; return java.util.Objects.equals(primarySpacingX, other.primarySpacingX) && java.util.Objects.equals(primarySpacingZ, other.primarySpacingZ) && java.util.Objects.equals(primaryOptionalChance, other.primaryOptionalChance) && java.util.Objects.equals(primaryForceEvery, other.primaryForceEvery) && java.util.Objects.equals(secondaryMinCountX, other.secondaryMinCountX) && java.util.Objects.equals(secondaryMaxCountX, other.secondaryMaxCountX) && java.util.Objects.equals(secondaryMinCountZ, other.secondaryMinCountZ) && java.util.Objects.equals(secondaryMaxCountZ, other.secondaryMaxCountZ) && java.util.Objects.equals(minimumRoadSeparation, other.minimumRoadSeparation) && java.util.Objects.equals(minimumEdgeDistance, other.minimumEdgeDistance) && java.util.Objects.equals(tertiaryChance, other.tertiaryChance) && java.util.Objects.equals(tertiaryMinLength, other.tertiaryMinLength) && java.util.Objects.equals(tertiaryMaxLength, other.tertiaryMaxLength); }
    @Override public int hashCode() { return java.util.Objects.hash(primarySpacingX, primarySpacingZ, primaryOptionalChance, primaryForceEvery, secondaryMinCountX, secondaryMaxCountX, secondaryMinCountZ, secondaryMaxCountZ, minimumRoadSeparation, minimumEdgeDistance, tertiaryChance, tertiaryMinLength, tertiaryMaxLength); }

    


}

