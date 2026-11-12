package mcjty.lostcities.worldgen.highway;



@FunctionalInterface
public interface CityPotential {
    int SCORE_SCALE = 1_000_000;

    float getPotential(int chunkX, int chunkZ);

    default int getScoreWithUpperBound(int chunkX, int chunkZ, int requiredScore) {
        return score(getPotential(chunkX, chunkZ));
    }

    static int score(float potential) {
        return Math.round(Math.max(0.0f, Math.min(potential, 1.0f)) * SCORE_SCALE);
    }
}
