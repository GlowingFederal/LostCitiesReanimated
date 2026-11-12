package mcjty.lostcities.dimensions.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.ModDimensions;
import mcjty.lostcities.network.PacketHandler;
import mcjty.lostcities.network.PacketRequestProfile;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class WorldTypeTools {

    // A map which maps dimension id to the profile
    private static final Map<World, LostCityProfile> serverProfiles = new java.util.WeakHashMap<>();
    private static final Map<Integer, LostCityProfile> profileMap = new java.util.LinkedHashMap<Integer,LostCityProfile>(16,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Integer,LostCityProfile> entry){return size()>64;}};
    private static final java.util.Set<Integer> nonCityDimensions=new java.util.LinkedHashSet<>();
    private static final java.util.concurrent.atomic.AtomicInteger clientConnection = new java.util.concurrent.atomic.AtomicInteger();
    private static int appliedConnection;

    // This map is constructed dynamically to allow having to avoid having an instanceof
    // on LostCityChunkGenerator which breaks on sponge
    private static Map<Integer, WeakReference<LostCityChunkGenerator>> chunkGeneratorMap = new HashMap<>();

    // To prevent the client from asking the profile to the server too much
    private static long clientTimeout = -1;

    public static void cleanCache() {
        serverProfiles.clear();
    }

    // Network events invalidate immediately; only the client thread touches its map.
    public static void cleanClientCache() { clientConnection.incrementAndGet(); }
    public static int clientConnection() { return clientConnection.get(); }
    private static void applyClientConnection() {
        int connection = clientConnection.get();
        if (connection != appliedConnection) {
            profileMap.clear();
            nonCityDimensions.clear();
            clientTimeout = -1;
            appliedConnection = connection;
        }
    }

    public static void cleanChunkGeneratorMap() {
        chunkGeneratorMap.clear();
    }

    public static void registerChunkGenerator(Integer dimension, LostCityChunkGenerator chunkGenerator) {
        chunkGeneratorMap.put(dimension, new WeakReference<LostCityChunkGenerator>(chunkGenerator));
    }

    @Nullable
    public static LostCityChunkGenerator getChunkGenerator(int dimension) {
        if (chunkGeneratorMap.containsKey(dimension)) {
            WeakReference<LostCityChunkGenerator> reference = chunkGeneratorMap.get(dimension);
            return reference.get();
        }
        return null;
    }

    public static LostCityProfile getProfile(World world) {
        if (world instanceof WorldServer) {
            LostCityProfile cached = serverProfiles.get(world);
            if (cached != null) return cached;
            LostCityProfile profile = getProfileOnServer(world);
            serverProfiles.put(world, profile);
            return profile;
        } else {
            applyClientConnection();
            LostCityProfile cached = profileMap.get(world.provider.getDimension());
            if (cached != null) return cached;
            // We don't know the information yet so we ask the server. We set a timeout to make sure this
            // message is not sent all the time
            long time = System.currentTimeMillis();
            if (!nonCityDimensions.contains(world.provider.getDimension()) && (clientTimeout == -1 || clientTimeout + 2000 < time)) {
                PacketHandler.INSTANCE.sendToServer(new PacketRequestProfile(world.provider.getDimension()));
                clientTimeout = time;
            }
            if (ModDimensions.dimensionProfileMap.keySet().contains(world.provider.getDimension())) {
                // Don't put in cache because we might want to ask again
                return LostCityConfiguration.profiles.get(ModDimensions.dimensionProfileMap.get(world.provider.getDimension()));
            } else {
                // Don't put in cache because we might want to ask again
                return LostCityConfiguration.profiles.get(LostCityConfiguration.DEFAULT_PROFILE);
            }
        }
    }

    // Called client side when we get an answer from the server about our profile
    public static void setProfileFromServer(int dimension, String profileName) {
        applyClientConnection();
        if(profileName.isEmpty()){
            profileMap.remove(dimension);nonCityDimensions.add(dimension);
            if(nonCityDimensions.size()>64)nonCityDimensions.remove(nonCityDimensions.iterator().next());
            return;
        }
        nonCityDimensions.remove(dimension);
        LostCityProfile profile = LostCityConfiguration.profiles.get(profileName);
        if (profile == null) {
            throw new RuntimeException("Cannot find profile '" + profileName + "' that the server is using! Please make client configs for Lost Cities compatible");
        }
        profileMap.put(dimension, profile);
    }

    public static void setProfileFromServer(int dimension, String profileName, int connection) {
        if (connection == clientConnection.get()) setProfileFromServer(dimension, profileName);
    }

    /**
     * If possible return the LostCityChunkGenerator that belongs to this world. Return
     * null if it is not a Lost City world
     */
    @Nullable
    public static LostCityChunkGenerator getLostCityChunkGenerator(World world) {
        LostCityChunkGenerator generator=getChunkGenerator(world.provider.getDimension());
        return generator!=null&&generator.worldObj==world?generator:null;
//        WorldServer worldServer = (WorldServer) world;
//        IChunkGenerator chunkGenerator = worldServer.getChunkProvider().chunkGenerator;
//        // @todo not compatible with Sponge! No clue how to solve atm
//        // XXX
//        if (!(chunkGenerator instanceof LostCityChunkGenerator)) {
//            return null;
//        }
//        return (LostCityChunkGenerator) chunkGenerator;
    }

    public static boolean isLostCities(World world) {
        if(world.isRemote){applyClientConnection();if(!profileMap.containsKey(world.provider.getDimension())&&!nonCityDimensions.contains(world.provider.getDimension()))getProfile(world);if(profileMap.containsKey(world.provider.getDimension()))return true;}
        else if(getLostCityChunkGenerator(world)!=null)return true;
        if (ModDimensions.dimensionProfileMap.containsKey(world.provider.getDimension())) {
            return true;
        }
        if (world.provider.getDimension() != 0) {
            return false;
        }
        return world.getWorldType() instanceof LostWorldType || world.getWorldType() instanceof LostWorldTypeBOP;
    }

    private static LostCityProfile getProfileOnServer(World world) {
        String overlay=LostCityConfiguration.OVERLAY_DIMENSIONS.get(world.provider.getDimension());
        if(overlay!=null&&!(world.getWorldType() instanceof LostWorldType)&&!ModDimensions.dimensionProfileMap.containsKey(world.provider.getDimension())){LostCityProfile profile=LostCityConfiguration.profiles.get(overlay);if(profile==null)throw new IllegalArgumentException("Unknown overlay profile "+overlay);return profile;}
        if (ModDimensions.dimensionProfileMap.containsKey(world.provider.getDimension())) {
            LostCityProfile profile = LostCityConfiguration.profiles.get(ModDimensions.dimensionProfileMap.get(world.provider.getDimension()));
            if (profile != null) {
                return profile;
            }
            throw new IllegalArgumentException("Unknown Lost Cities dimension profile "+ModDimensions.dimensionProfileMap.get(world.provider.getDimension()));
        }
        String generatorOptions = world.getWorldInfo().getGeneratorOptions();
        LostCityProfile p;
        if (generatorOptions == null || generatorOptions.trim().isEmpty()) {
            p = LostCityConfiguration.profiles.get(LostCityConfiguration.DEFAULT_PROFILE);
            if (p == null) {
                throw new RuntimeException("Something went wrong! Profile '" + LostCityConfiguration.DEFAULT_PROFILE + "' is missing!");
            }
        } else {
            JsonParser parser = new JsonParser();
            JsonElement parsed = parser.parse(generatorOptions);
            String profileName;
            if (parsed.getAsJsonObject().has("profile")) {
                profileName = parsed.getAsJsonObject().get("profile").getAsString();
            } else {
                profileName = LostCityConfiguration.DEFAULT_PROFILE;
            }
            p = LostCityConfiguration.profiles.get(profileName);
            if (p == null) {
                throw new RuntimeException("Something went wrong! Profile '" + profileName + "' is missing!");
            }
        }
        return p;
    }
}
