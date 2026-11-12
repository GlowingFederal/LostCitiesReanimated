package mcjty.lostcities.worldgen.highway;

public final class HighwayHub  {
    private final HubKey key;
    public HubKey key() { return key; }
    private final int chunkX;
    public int chunkX() { return chunkX; }
    private final int chunkZ;
    public int chunkZ() { return chunkZ; }
    private final int potentialScore;
    public int potentialScore() { return potentialScore; }
    private final int cityLevel;
    public int cityLevel() { return cityLevel; }
    public HighwayHub(HubKey key, int chunkX, int chunkZ, int potentialScore, int cityLevel) {

        this.key = key;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.potentialScore = potentialScore;
        this.cityLevel = cityLevel;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof HighwayHub)) return false; HighwayHub other = (HighwayHub) value; return java.util.Objects.equals(key, other.key) && java.util.Objects.equals(chunkX, other.chunkX) && java.util.Objects.equals(chunkZ, other.chunkZ) && java.util.Objects.equals(potentialScore, other.potentialScore) && java.util.Objects.equals(cityLevel, other.cityLevel); }
    @Override public int hashCode() { return java.util.Objects.hash(key, chunkX, chunkZ, potentialScore, cityLevel); }
    @Override public String toString() { return "HighwayHub[" + "key=" + key + ", " + "chunkX=" + chunkX + ", " + "chunkZ=" + chunkZ + ", " + "potentialScore=" + potentialScore + ", " + "cityLevel=" + cityLevel + "]"; }

    public float strength() {
        return potentialScore / 1_000_000.0f;
    }
}

