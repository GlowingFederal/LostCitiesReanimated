package mcjty.lostcities.compat;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;

/** Constructs BOP's own layers without changing a shared or derived world header. */
public final class BopBiomes {
    public static BiomeProvider create(World world) {
        for (WorldType type : WorldType.WORLD_TYPES) {
            if (type == null || !"BIOMESOP".equalsIgnoreCase(type.getName())) continue;
            try {
                // WorldServerMulti uses DerivedWorldInfo: setting its terrain type does not
                // change what BOP's World constructor reads from the overworld header.
                Class<?> provider = Class.forName("biomesoplenty.common.world.BiomeProviderBOP",
                        true, BopBiomes.class.getClassLoader());
                return (BiomeProvider) provider.getConstructor(long.class, WorldType.class, String.class)
                        .newInstance(world.getSeed(), type, world.getWorldInfo().getGeneratorOptions());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Cannot construct the native BOP biome provider", error);
            }
        }
        throw new IllegalStateException("Lost Cities BOP biomes require Biomes O' Plenty");
    }

    private BopBiomes() {}
}
