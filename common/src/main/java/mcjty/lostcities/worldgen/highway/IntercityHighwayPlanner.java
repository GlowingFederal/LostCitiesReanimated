package mcjty.lostcities.worldgen.highway;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * Pure, order-independent INTERCITY_NETWORK_V1 planner.
 *
 * Planning cells own at most one approximate city hub. Each hub independently
 * selects bounded, sector-diverse neighbours; an undirected edge exists only
 * when both endpoints select each other. The lexicographically smaller hub key
 * owns the route. Bounded LRU caches affect performance only.
 */
public final class IntercityHighwayPlanner {

    private static final long VERSION_SALT = 0x494e544552563101L; // "INTERV1" + version byte
    private static final long HUB_SAMPLE_SALT = 0x5d39a74e10c26bf1L;
    private static final long HUB_POSITION_SALT = 0x27f4b8916d3a50ceL;
    private static final long HUB_STRENGTH_SALT = 0x618ad2e5073fb94cL;
    private static final long CONNECTION_RANK_SALT = 0x34ce791b582da6f0L;
    private static final long CONNECTION_ACCEPT_SALT = 0x72b514e90c63dfa8L;
    private static final long ROUTE_SHAPE_SALT = 0x19e8c347a65d2bf0L;
    private static final int POTENTIAL_SCALE = CityPotential.SCORE_SCALE;
    private static final int ROUTE_PENALTY_SAMPLE_SPACING = 8;
    private static final int ENDPOINT_PENALTY_DISCOUNT = 16;

    private final long seed;
    private final long dimensionSalt;
    private final HighwayPlannerSettings settings;
    private final CityPotential cityPotential;
    private final HighwayLevelSource highwayLevelSource;
    private final HighwayHubPersistence hubPersistence;
    private final BoundedCache<HubKey, Optional<HighwayHub>> hubCache = new BoundedCache<>(4096);
    private final BoundedCache<HubKey, List<ConnectionCandidate>> candidateCache = new BoundedCache<>(2048);
    private final BoundedCache<HubKey, List<HubKey>> selectionCache = new BoundedCache<>(2048);
    private final BoundedCache<HubKey, List<HighwayRoute>> routeCache = new BoundedCache<>(2048);
    private final BoundedCache<ChunkKey, HighwayInfo> chunkCache = new BoundedCache<>(8192);
    private final AtomicLong uncachedChunkQueries = new AtomicLong();

    public IntercityHighwayPlanner(long seed, String dimensionId, HighwayPlannerSettings settings,
                                   CityPotential cityPotential) {
        this(seed, dimensionId, settings, cityPotential,
                (chunkX, chunkZ) -> settings.networkLevel(), HighwayHubPersistence.NONE);
    }

    public IntercityHighwayPlanner(long seed, String dimensionId, HighwayPlannerSettings settings,
                                   CityPotential cityPotential, HighwayHubPersistence hubPersistence) {
        this(seed, dimensionId, settings, cityPotential,
                (chunkX, chunkZ) -> settings.networkLevel(), hubPersistence);
    }

    public IntercityHighwayPlanner(long seed, String dimensionId, HighwayPlannerSettings settings,
                                   CityPotential cityPotential, HighwayLevelSource highwayLevelSource,
                                   HighwayHubPersistence hubPersistence) {
        this.seed = seed;
        this.dimensionSalt = stableStringHash(dimensionId);
        this.settings = settings;
        this.cityPotential = cityPotential;
        this.highwayLevelSource = highwayLevelSource;
        this.hubPersistence = hubPersistence;
    }

    public HighwayPlannerSettings settings() {
        return settings;
    }

    public HubKey getPlanningCell(int chunkX, int chunkZ) {
        return new HubKey(Math.floorDiv(chunkX, settings.planningCellSize()),
                Math.floorDiv(chunkZ, settings.planningCellSize()));
    }

    public Optional<HighwayHub> getHub(int planningCellX, int planningCellZ) {
        HubKey key = new HubKey(planningCellX, planningCellZ);
        return getHub(key);
    }

    public Optional<HighwayHub> getHub(HubKey key) {
        return hubCache.computeIfAbsent(key, this::loadOrCalculateHub);
    }

    private Optional<HighwayHub> loadOrCalculateHub(HubKey key) {
        HighwayHubPersistence.Lookup persisted = hubPersistence.get(key);
        if (persisted.known()) {
            return persisted.hub();
        }
        Optional<HighwayHub> calculated = calculateHub(key);
        hubPersistence.put(key, calculated);
        return calculated;
    }

    public List<HighwayHub> getNearbyHubs(int chunkX, int chunkZ) {
        HubKey center = getPlanningCell(chunkX, chunkZ);
        int radius = settings.hubSearchRadiusCells();
        List<HighwayHub> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                getHub(offset(center, dx, dz)).ifPresent(result::add);
            }
        }
        result.sort(Comparator.comparing(HighwayHub::key));
        return mcjty.lostcities.core.LegacyLists.copyOf(result);
    }

    public List<ConnectionCandidate> getConnectionCandidates(HubKey source) {
        return candidateCache.computeIfAbsent(source, this::calculateCandidates);
    }

    public List<HubKey> getSelectedNeighbours(HubKey source) {
        return selectionCache.computeIfAbsent(source, this::calculateSelectedNeighbours);
    }

    public List<HighwayConnectionKey> getAcceptedConnections(HubKey hubKey) {
        Optional<HighwayHub> hub = getHub(hubKey);
        if (!hub.isPresent()) {
            return mcjty.lostcities.core.LegacyLists.of();
        }
        List<HighwayConnectionKey> connections = new ArrayList<>();
        for (HubKey neighbour : getSelectedNeighbours(hubKey)) {
            if (getSelectedNeighbours(neighbour).contains(hubKey)) {
                connections.add(HighwayConnectionKey.of(hubKey, neighbour));
            }
        }
        connections.sort(Comparator.naturalOrder());
        return mcjty.lostcities.core.LegacyLists.copyOf(connections);
    }

    public List<HighwayRoute> getOwnedRoutes(HubKey owner) {
        return routeCache.computeIfAbsent(owner, this::calculateOwnedRoutes);
    }

    public Optional<HighwayRoute> getRoute(HighwayConnectionKey key) {
        return getOwnedRoutes(key.owner()).stream().filter(route -> route.key().equals(key)).findFirst();
    }

    public HighwayInfo getHighwayInfo(int chunkX, int chunkZ) {
        ChunkKey key = new ChunkKey(chunkX, chunkZ);
        return chunkCache.computeIfAbsent(key, ignored -> {
            uncachedChunkQueries.incrementAndGet();
            return calculateHighwayInfo(chunkX, chunkZ);
        });
    }

    public CacheStats getCacheStats() {
        return new CacheStats(hubCache.hits(), hubCache.misses(), chunkCache.hits(), chunkCache.misses(),
                uncachedChunkQueries.get());
    }

    public void clearCaches() {
        hubCache.clear();
        candidateCache.clear();
        selectionCache.clear();
        routeCache.clear();
        chunkCache.clear();
    }

    private Optional<HighwayHub> calculateHub(HubKey cell) {
        int size = settings.planningCellSize();
        int spacing = settings.hubSampleSpacing();
        int minimum = Math.round(settings.hubMinimumPotential() * POTENTIAL_SCALE);
        int startX = Math.toIntExact((long) cell.planningCellX() * size);
        int startZ = Math.toIntExact((long) cell.planningCellZ() * size);
        int offsetX = floorModHash(hash(HUB_POSITION_SALT, cell.planningCellX(), cell.planningCellZ(), 0), spacing);
        int offsetZ = floorModHash(hash(HUB_POSITION_SALT, cell.planningCellX(), cell.planningCellZ(), 1), spacing);
        HighwayHub best = null;
        long bestTie = 0L;
        for (int localX = offsetX; localX < size; localX += spacing) {
            for (int localZ = offsetZ; localZ < size; localZ += spacing) {
                int chunkX = moveHubXOffRailwayCorridor(Math.addExact(startX, localX), localX, size);
                int chunkZ = moveHubZOffRailwayCorridor(Math.addExact(startZ, localZ), localZ, size);
                long tie = hash(HUB_SAMPLE_SALT, chunkX, chunkZ,
                        floorModHash(hash(HUB_STRENGTH_SALT, cell.planningCellX(), cell.planningCellZ(), 0), Integer.MAX_VALUE));
                boolean hasQualifyingBest = best != null && best.potentialScore() >= minimum;
                int requiredScore = hasQualifyingBest
                        ? best.potentialScore() + (Long.compareUnsigned(tie, bestTie) < 0 ? 0 : 1)
                        : minimum;
                int potentialScore = cityPotential.getScoreWithUpperBound(chunkX, chunkZ, requiredScore);
                if (potentialScore < 0) {
                    continue;
                }
                if (best == null || potentialScore > best.potentialScore()
                        || potentialScore == best.potentialScore() && Long.compareUnsigned(tie, bestTie) < 0) {
                    best = new HighwayHub(cell, chunkX, chunkZ, potentialScore, 0);
                    bestTie = tie;
                }
            }
        }
        if (best == null || best.potentialScore() < minimum) {
            return Optional.empty();
        }
        return Optional.of(new HighwayHub(best.key(), best.chunkX(), best.chunkZ(), best.potentialScore(),
                highwayLevelSource.getCityLevel(best.chunkX(), best.chunkZ())));
    }

    private List<ConnectionCandidate> calculateCandidates(HubKey sourceKey) {
        Optional<HighwayHub> sourceOptional = getHub(sourceKey);
        if (!sourceOptional.isPresent()) {
            return mcjty.lostcities.core.LegacyLists.of();
        }
        HighwayHub source = sourceOptional.get();
        int radius = settings.hubSearchRadiusCells();
        long minimumSquared = square(settings.minimumHubDistance());
        long maximumSquared = square(settings.maximumHubDistance());
        List<ConnectionCandidate> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                HubKey targetKey = offset(sourceKey, dx, dz);
                if (targetKey.equals(sourceKey)) {
                    continue;
                }
                Optional<HighwayHub> targetOptional = getHub(targetKey);
                if (!targetOptional.isPresent()) {
                    continue;
                }
                HighwayHub target = targetOptional.get();
                long deltaX = (long) target.chunkX() - source.chunkX();
                long deltaZ = (long) target.chunkZ() - source.chunkZ();
                long distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                int routeLength = Math.toIntExact(Math.abs(deltaX) + Math.abs(deltaZ));
                if (distanceSquared < minimumSquared || distanceSquared > maximumSquared
                        || routeLength < settings.minimumRouteLength()
                        || !hasRailwayClearRoute(source, target)) {
                    continue;
                }
                HighwayConnectionKey key = HighwayConnectionKey.of(sourceKey, targetKey);
                long rank = hash(CONNECTION_RANK_SALT, key.first().planningCellX(), key.first().planningCellZ(),
                        pairExtra(key.second()));
                long acceptanceTie = hash(CONNECTION_ACCEPT_SALT, key.first().planningCellX(), key.first().planningCellZ(),
                        pairExtra(key.second()));
                candidates.add(new ConnectionCandidate(source, target, key, sector(source, target),
                        distanceSquared, routeLength, rank, acceptanceTie));
            }
        }
        candidates.sort(candidateComparator());
        return mcjty.lostcities.core.LegacyLists.copyOf(candidates);
    }

    private List<HubKey> calculateSelectedNeighbours(HubKey source) {
        List<ConnectionCandidate> candidates = getConnectionCandidates(source);
        if (candidates.isEmpty()) {
            return mcjty.lostcities.core.LegacyLists.of();
        }
        int maximum = settings.maximumConnectionsPerHub();
        List<HubKey> selected = new ArrayList<>(maximum);
        Set<HighwaySector> usedSectors = EnumSet.noneOf(HighwaySector.class);
        for (ConnectionCandidate candidate : candidates) {
            if (usedSectors.add(candidate.sector())) {
                selected.add(candidate.target().key());
                if (selected.size() == maximum) {
                    return mcjty.lostcities.core.LegacyLists.copyOf(selected);
                }
            }
        }
        for (ConnectionCandidate candidate : candidates) {
            HubKey target = candidate.target().key();
            if (!selected.contains(target)) {
                selected.add(target);
                if (selected.size() == maximum) {
                    break;
                }
            }
        }
        return mcjty.lostcities.core.LegacyLists.copyOf(selected);
    }

    private List<HighwayRoute> calculateOwnedRoutes(HubKey owner) {
        Optional<HighwayHub> ownerHub = getHub(owner);
        if (!ownerHub.isPresent()) {
            return mcjty.lostcities.core.LegacyLists.of();
        }
        List<HighwayRoute> routes = new ArrayList<>();
        for (HubKey neighbour : getSelectedNeighbours(owner)) {
            HighwayConnectionKey key = HighwayConnectionKey.of(owner, neighbour);
            if (!key.owner().equals(owner) || !getSelectedNeighbours(neighbour).contains(owner)) {
                continue;
            }
            HighwayHub target = getHub(neighbour).orElseThrow(() -> new IllegalStateException("Selected hub is absent"));
            routes.add(createRoute(key, ownerHub.get(), target));
        }
        routes.sort(Comparator.comparing(HighwayRoute::key));
        return mcjty.lostcities.core.LegacyLists.copyOf(routes);
    }

    private HighwayRoute createRoute(HighwayConnectionKey key, HighwayHub a, HighwayHub b) {
        HighwayHub first = a.key().equals(key.first()) ? a : b;
        HighwayHub second = a.key().equals(key.second()) ? a : b;
        int length = Math.abs(second.chunkX() - first.chunkX()) + Math.abs(second.chunkZ() - first.chunkZ());
        int level = selectHighwayLevel(first, second);
        if (first.chunkZ() == second.chunkZ()) {
            HighwaySegment segment = new HighwaySegment(first.chunkX(), first.chunkZ(), second.chunkX(), second.chunkZ(), HighwayAxis.X);
            return new HighwayRoute(key, first, second, mcjty.lostcities.core.LegacyLists.of(segment), level, HighwayRoute.RouteShape.STRAIGHT, length, 0);
        }
        if (first.chunkX() == second.chunkX()) {
            HighwaySegment segment = new HighwaySegment(first.chunkX(), first.chunkZ(), second.chunkX(), second.chunkZ(), HighwayAxis.Z);
            return new HighwayRoute(key, first, second, mcjty.lostcities.core.LegacyLists.of(segment), level, HighwayRoute.RouteShape.STRAIGHT, length, 0);
        }

        List<HighwaySegment> horizontalFirst = mcjty.lostcities.core.LegacyLists.of(
                new HighwaySegment(first.chunkX(), first.chunkZ(), second.chunkX(), first.chunkZ(), HighwayAxis.X),
                new HighwaySegment(second.chunkX(), first.chunkZ(), second.chunkX(), second.chunkZ(), HighwayAxis.Z));
        List<HighwaySegment> verticalFirst = mcjty.lostcities.core.LegacyLists.of(
                new HighwaySegment(first.chunkX(), first.chunkZ(), first.chunkX(), second.chunkZ(), HighwayAxis.Z),
                new HighwaySegment(first.chunkX(), second.chunkZ(), second.chunkX(), second.chunkZ(), HighwayAxis.X));
        boolean horizontalClear = isRailwayClear(horizontalFirst);
        boolean verticalClear = isRailwayClear(verticalFirst);
        long horizontalPenalty = routePenalty(horizontalFirst, first, second);
        long verticalPenalty = routePenalty(verticalFirst, first, second);
        boolean chooseHorizontal;
        if (horizontalClear != verticalClear) {
            chooseHorizontal = horizontalClear;
        } else if (horizontalPenalty != verticalPenalty) {
            chooseHorizontal = horizontalPenalty < verticalPenalty;
        } else {
            chooseHorizontal = (hash(ROUTE_SHAPE_SALT, key.first().planningCellX(), key.first().planningCellZ(),
                    pairExtra(key.second())) & 1L) == 0L;
        }
        return new HighwayRoute(key, first, second,
                chooseHorizontal ? horizontalFirst : verticalFirst,
                level,
                chooseHorizontal ? HighwayRoute.RouteShape.HORIZONTAL_THEN_VERTICAL : HighwayRoute.RouteShape.VERTICAL_THEN_HORIZONTAL,
                length,
                chooseHorizontal ? horizontalPenalty : verticalPenalty);
    }

    private int selectHighwayLevel(HighwayHub first, HighwayHub second) {
        switch (settings.levelFromCitiesMode()) {
            case 0: return first.cityLevel();
            case 1: return Math.min(first.cityLevel(), second.cityLevel());
            case 2: return Math.max(first.cityLevel(), second.cityLevel());
            case 3: return (first.cityLevel() + second.cityLevel()) / 2;
            case 4: return settings.networkLevel();
            default: throw new IllegalStateException("Unsupported highway level mode "
                    + settings.levelFromCitiesMode());
        }
    }

    private boolean hasRailwayClearRoute(HighwayHub first, HighwayHub second) {
        if (first.chunkZ() == second.chunkZ()) {
            return isRailwayClear(mcjty.lostcities.core.LegacyLists.of(new HighwaySegment(first.chunkX(), first.chunkZ(),
                    second.chunkX(), second.chunkZ(), HighwayAxis.X)));
        }
        if (first.chunkX() == second.chunkX()) {
            return isRailwayClear(mcjty.lostcities.core.LegacyLists.of(new HighwaySegment(first.chunkX(), first.chunkZ(),
                    second.chunkX(), second.chunkZ(), HighwayAxis.Z)));
        }
        List<HighwaySegment> horizontalFirst = mcjty.lostcities.core.LegacyLists.of(
                new HighwaySegment(first.chunkX(), first.chunkZ(), second.chunkX(), first.chunkZ(), HighwayAxis.X),
                new HighwaySegment(second.chunkX(), first.chunkZ(), second.chunkX(), second.chunkZ(), HighwayAxis.Z));
        List<HighwaySegment> verticalFirst = mcjty.lostcities.core.LegacyLists.of(
                new HighwaySegment(first.chunkX(), first.chunkZ(), first.chunkX(), second.chunkZ(), HighwayAxis.Z),
                new HighwaySegment(first.chunkX(), second.chunkZ(), second.chunkX(), second.chunkZ(), HighwayAxis.X));
        return isRailwayClear(horizontalFirst) || isRailwayClear(verticalFirst);
    }

    /**
     * Crossing an underground railway is safe, but sharing its chunk line or crossing a
     * surface-access station can make railway parts replace the highway. Keep parallel highway
     * segments off the configured railway corridors and vertical highways off the columns
     * containing stations.
     */
    private boolean isRailwayClear(List<HighwaySegment> segments) {
        for (HighwaySegment segment : segments) {
            if (segment.axis() == HighwayAxis.X && isHorizontalRailwayCorridor(segment.startZ())) {
                return false;
            }
            if (segment.axis() == HighwayAxis.Z && isVerticalRailwayOrStationCorridor(segment.startX())) {
                return false;
            }
        }
        return true;
    }

    private int moveHubXOffRailwayCorridor(int chunkX, int localX, int cellSize) {
        if (!isVerticalRailwayOrStationCorridor(chunkX)) {
            return chunkX;
        }
        return localX + 1 < cellSize ? Math.addExact(chunkX, 1) : Math.subtractExact(chunkX, 1);
    }

    private int moveHubZOffRailwayCorridor(int chunkZ, int localZ, int cellSize) {
        if (!isHorizontalRailwayCorridor(chunkZ)) {
            return chunkZ;
        }
        return localZ + 1 < cellSize ? Math.addExact(chunkZ, 1) : Math.subtractExact(chunkZ, 1);
    }

    private boolean isHorizontalRailwayCorridor(int chunkZ) {
        return Math.floorMod(chunkZ + 1, settings.railwaySpacingNorthSouth()) == 0;
    }

    private boolean isVerticalRailwayOrStationCorridor(int chunkX) {
        int spacing = settings.railwaySpacingEastWest();
        int gridX = Math.floorMod(chunkX + 1, spacing);
        return gridX == spacing / 2 || gridX == 0;
    }

    /** Builds canonical V1 geometry for an already accepted hub pair. */
    public HighwayRoute createRoute(HighwayHub first, HighwayHub second) {
        return createRoute(HighwayConnectionKey.of(first.key(), second.key()), first, second);
    }

    private long routePenalty(List<HighwaySegment> segments, HighwayHub first, HighwayHub second) {
        if (settings.routeCityPenalty() == 0.0f) {
            return 0L;
        }
        long sum = 0L;
        for (HighwaySegment segment : segments) {
            int length = segment.length();
            for (int distance = 0; distance <= length; distance += ROUTE_PENALTY_SAMPLE_SPACING) {
                int chunkX = interpolate(segment.startX(), segment.endX(), distance, length);
                int chunkZ = interpolate(segment.startZ(), segment.endZ(), distance, length);
                if (manhattan(chunkX, chunkZ, first.chunkX(), first.chunkZ()) <= ENDPOINT_PENALTY_DISCOUNT
                        || manhattan(chunkX, chunkZ, second.chunkX(), second.chunkZ()) <= ENDPOINT_PENALTY_DISCOUNT) {
                    continue;
                }
                sum += potentialScore(chunkX, chunkZ);
            }
        }
        return Math.round(sum * (double) settings.routeCityPenalty());
    }

    private HighwayInfo calculateHighwayInfo(int chunkX, int chunkZ) {
        HubKey queryCell = getPlanningCell(chunkX, chunkZ);
        int radius = settings.hubSearchRadiusCells();
        Map<HighwayConnectionKey, HighwayRoute> routes = new TreeMap<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (HighwayRoute route : getOwnedRoutes(offset(queryCell, dx, dz))) {
                    if (route.contains(chunkX, chunkZ)) {
                        routes.put(route.key(), route);
                    }
                }
            }
        }
        if (routes.isEmpty()) {
            return HighwayInfo.NONE;
        }
        int xLevel = -1;
        int zLevel = -1;
        List<HighwayInfo.RouteHit> hits = new ArrayList<>();
        for (HighwayRoute route : routes.values()) {
            boolean xAxis = false;
            boolean zAxis = false;
            for (HighwaySegment segment : route.segments()) {
                if (segment.contains(chunkX, chunkZ)) {
                    xAxis |= segment.axis() == HighwayAxis.X;
                    zAxis |= segment.axis() == HighwayAxis.Z;
                }
            }
            if (xAxis) {
                xLevel = xLevel < 0 ? route.highwayLevel() : Math.min(xLevel, route.highwayLevel());
            }
            if (zAxis) {
                zLevel = zLevel < 0 ? route.highwayLevel() : Math.min(zLevel, route.highwayLevel());
            }
            hits.add(new HighwayInfo.RouteHit(route, xAxis, zAxis, xAxis && zAxis));
        }
        HighwayInfo.Classification classification;
        if (xLevel >= 0 && zLevel >= 0) {
            classification = xLevel == zLevel ? HighwayInfo.Classification.SAME_LEVEL_INTERSECTION
                    : HighwayInfo.Classification.MULTI_LEVEL_INTERSECTION;
        } else {
            classification = xLevel >= 0 ? HighwayInfo.Classification.X_HIGHWAY : HighwayInfo.Classification.Z_HIGHWAY;
        }
        return new HighwayInfo(xLevel, zLevel, classification, hits);
    }

    private int potentialScore(int chunkX, int chunkZ) {
        return CityPotential.score(cityPotential.getPotential(chunkX, chunkZ));
    }

    private Comparator<ConnectionCandidate> candidateComparator() {
        return (first, second) -> {
            int comparison = Long.compare(first.distanceSquared(), second.distanceSquared());
            if (comparison != 0) {
                return comparison;
            }
            comparison = Integer.compare(second.target().potentialScore(), first.target().potentialScore());
            if (comparison != 0) {
                return comparison;
            }
            comparison = Long.compareUnsigned(first.rankTieBreaker(), second.rankTieBreaker());
            if (comparison != 0) {
                return comparison;
            }
            comparison = Long.compareUnsigned(first.acceptanceTieBreaker(), second.acceptanceTieBreaker());
            return comparison != 0 ? comparison : first.target().key().compareTo(second.target().key());
        };
    }

    private static HighwaySector sector(HighwayHub source, HighwayHub target) {
        long dx = (long) target.chunkX() - source.chunkX();
        long dz = (long) target.chunkZ() - source.chunkZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? HighwaySector.EAST : HighwaySector.WEST;
        }
        return dz >= 0 ? HighwaySector.SOUTH : HighwaySector.NORTH;
    }

    private long hash(long streamSalt, int x, int z, int extra) {
        long value = seed ^ dimensionSalt ^ VERSION_SALT ^ streamSalt;
        value ^= mix64(Integer.toUnsignedLong(x) + 0x9e3779b97f4a7c15L);
        value ^= mix64(Integer.toUnsignedLong(z) + 0xc2b2ae3d27d4eb4fL);
        value ^= mix64(Integer.toUnsignedLong(extra) + 0x165667b19e3779f9L);
        return mix64(value);
    }

    private static int pairExtra(HubKey key) {
        long mixed = mix64(Integer.toUnsignedLong(key.planningCellX())
                ^ Long.rotateLeft(Integer.toUnsignedLong(key.planningCellZ()), 32));
        return (int) (mixed ^ mixed >>> 32);
    }

    private static HubKey offset(HubKey key, int dx, int dz) {
        return new HubKey(Math.addExact(key.planningCellX(), dx), Math.addExact(key.planningCellZ(), dz));
    }

    private static long square(int value) {
        return (long) value * value;
    }

    private static int interpolate(int start, int end, int distance, int length) {
        if (length == 0) {
            return start;
        }
        return start + Integer.signum(end - start) * distance;
    }

    private static int manhattan(int x1, int z1, int x2, int z2) {
        return Math.toIntExact(Math.abs((long) x2 - x1) + Math.abs((long) z2 - z1));
    }

    private static int floorModHash(long hash, int modulus) {
        return (int) Math.floorMod(hash, (long) modulus);
    }

    private static long stableStringHash(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        return mix64(hash);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public static final class ConnectionCandidate  {
    private final HighwayHub source;
    public HighwayHub source() { return source; }
    private final HighwayHub target;
    public HighwayHub target() { return target; }
    private final HighwayConnectionKey key;
    public HighwayConnectionKey key() { return key; }
    private final HighwaySector sector;
    public HighwaySector sector() { return sector; }
    private final long distanceSquared;
    public long distanceSquared() { return distanceSquared; }
    private final int routeLength;
    public int routeLength() { return routeLength; }
    private final long rankTieBreaker;
    public long rankTieBreaker() { return rankTieBreaker; }
    private final long acceptanceTieBreaker;
    public long acceptanceTieBreaker() { return acceptanceTieBreaker; }
    public ConnectionCandidate(HighwayHub source, HighwayHub target, HighwayConnectionKey key, HighwaySector sector, long distanceSquared, int routeLength, long rankTieBreaker, long acceptanceTieBreaker) {

        this.source = source;
        this.target = target;
        this.key = key;
        this.sector = sector;
        this.distanceSquared = distanceSquared;
        this.routeLength = routeLength;
        this.rankTieBreaker = rankTieBreaker;
        this.acceptanceTieBreaker = acceptanceTieBreaker;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof ConnectionCandidate)) return false; ConnectionCandidate other = (ConnectionCandidate) value; return java.util.Objects.equals(source, other.source) && java.util.Objects.equals(target, other.target) && java.util.Objects.equals(key, other.key) && java.util.Objects.equals(sector, other.sector) && java.util.Objects.equals(distanceSquared, other.distanceSquared) && java.util.Objects.equals(routeLength, other.routeLength) && java.util.Objects.equals(rankTieBreaker, other.rankTieBreaker) && java.util.Objects.equals(acceptanceTieBreaker, other.acceptanceTieBreaker); }
    @Override public int hashCode() { return java.util.Objects.hash(source, target, key, sector, distanceSquared, routeLength, rankTieBreaker, acceptanceTieBreaker); }
    @Override public String toString() { return "ConnectionCandidate[" + "source=" + source + ", " + "target=" + target + ", " + "key=" + key + ", " + "sector=" + sector + ", " + "distanceSquared=" + distanceSquared + ", " + "routeLength=" + routeLength + ", " + "rankTieBreaker=" + rankTieBreaker + ", " + "acceptanceTieBreaker=" + acceptanceTieBreaker + "]"; }
 }


    public static final class CacheStats  {
    private final long hubHits;
    public long hubHits() { return hubHits; }
    private final long hubMisses;
    public long hubMisses() { return hubMisses; }
    private final long chunkHits;
    public long chunkHits() { return chunkHits; }
    private final long chunkMisses;
    public long chunkMisses() { return chunkMisses; }
    private final long uncachedChunkQueries;
    public long uncachedChunkQueries() { return uncachedChunkQueries; }
    public CacheStats(long hubHits, long hubMisses, long chunkHits, long chunkMisses, long uncachedChunkQueries) {

        this.hubHits = hubHits;
        this.hubMisses = hubMisses;
        this.chunkHits = chunkHits;
        this.chunkMisses = chunkMisses;
        this.uncachedChunkQueries = uncachedChunkQueries;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof CacheStats)) return false; CacheStats other = (CacheStats) value; return java.util.Objects.equals(hubHits, other.hubHits) && java.util.Objects.equals(hubMisses, other.hubMisses) && java.util.Objects.equals(chunkHits, other.chunkHits) && java.util.Objects.equals(chunkMisses, other.chunkMisses) && java.util.Objects.equals(uncachedChunkQueries, other.uncachedChunkQueries); }
    @Override public int hashCode() { return java.util.Objects.hash(hubHits, hubMisses, chunkHits, chunkMisses, uncachedChunkQueries); }
    @Override public String toString() { return "CacheStats[" + "hubHits=" + hubHits + ", " + "hubMisses=" + hubMisses + ", " + "chunkHits=" + chunkHits + ", " + "chunkMisses=" + chunkMisses + ", " + "uncachedChunkQueries=" + uncachedChunkQueries + "]"; }
 }


    private static final class ChunkKey  {
    private final int chunkX;
    public int chunkX() { return chunkX; }
    private final int chunkZ;
    public int chunkZ() { return chunkZ; }
    public ChunkKey(int chunkX, int chunkZ) {

        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
    @Override public boolean equals(Object value) { if (this == value) return true; if (!(value instanceof ChunkKey)) return false; ChunkKey other = (ChunkKey) value; return java.util.Objects.equals(chunkX, other.chunkX) && java.util.Objects.equals(chunkZ, other.chunkZ); }
    @Override public int hashCode() { return java.util.Objects.hash(chunkX, chunkZ); }
    @Override public String toString() { return "ChunkKey[" + "chunkX=" + chunkX + ", " + "chunkZ=" + chunkZ + "]"; }
 }


    private static final class BoundedCache<K, V> {
        private final int maximumSize;
        private final ConcurrentHashMap<K, CompletableFuture<V>> values = new ConcurrentHashMap<>();
        private final AtomicBoolean trimming = new AtomicBoolean();
        private final LongAdder hits = new LongAdder();
        private final LongAdder misses = new LongAdder();

        private BoundedCache(int maximumSize) {
            this.maximumSize = maximumSize;
        }

        V computeIfAbsent(K key, Function<K, V> factory) {
            CompletableFuture<V> existing = values.get(key);
            if (existing == null) {
                CompletableFuture<V> created = new CompletableFuture<>();
                existing = values.putIfAbsent(key, created);
                if (existing == null) {
                    misses.increment();
                    V value;
                    try {
                        value = factory.apply(key);
                    } catch (Throwable e) {
                        values.remove(key, created);
                        created.completeExceptionally(e);
                        throw e;
                    }
                    created.complete(value);
                    trim();
                    return value;
                }
            }
            hits.increment();
            return existing.join();
        }

        void clear() {
            values.clear();
            hits.reset();
            misses.reset();
        }

        long hits() {
            return hits.sum();
        }

        long misses() {
            return misses.sum();
        }

        private void trim() {
            if (values.size() <= maximumSize || !trimming.compareAndSet(false, true)) {
                return;
            }
            boolean retry;
            try {
                Iterator<Map.Entry<K, CompletableFuture<V>>> iterator = values.entrySet().iterator();
                while (values.size() > maximumSize && iterator.hasNext()) {
                    Map.Entry<K, CompletableFuture<V>> entry = iterator.next();
                    CompletableFuture<V> future = entry.getValue();
                    if (future.isDone()) {
                        values.remove(entry.getKey(), future);
                    }
                }
            } finally {
                trimming.set(false);
                retry = values.size() > maximumSize && values.values().stream().anyMatch(CompletableFuture::isDone);
            }
            if (retry) {
                trim();
            }
        }
    }
}
