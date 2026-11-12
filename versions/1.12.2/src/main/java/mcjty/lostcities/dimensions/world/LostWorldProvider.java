package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.config.BiomeSelectionStrategy;
import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.dimensions.ModDimensions;
import mcjty.lostcities.setup.ModSetup;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nonnull;

public class LostWorldProvider extends WorldProvider {

    @Override
    @Nonnull
    public DimensionType getDimensionType() {
        return ModDimensions.lostDimensionType;
    }

    @Override
    @Nonnull
    public String getSaveFolder() {
        if (getDimension() == LostCityConfiguration.DIMENSION_ID) {
            return "LOST";
        } else {
            return "LOST" + getDimension();
        }
    }

    @Override
    @Nonnull
    public IChunkGenerator createChunkGenerator() {
        return new LostCityChunkGenerator(world, (world.getSeed() >> 3) ^ 34328884229L);
    }

    private BiomeProvider getInternalBiomeProvider(World world) {
        return mcjty.lostcities.compat.BopBiomes.create(world);
    }

    @Override
    protected void init() {
        super.init();

        String profileName = ModDimensions.dimensionProfileMap.get(world.provider.getDimension());
        LostCityProfile profile = LostCityConfiguration.profiles.get(profileName);
        if (profile == null) {
            profile = WorldTypeTools.getProfile(world);
        }
        BiomeProvider biomeProvider;
        if (ModSetup.biomesoplenty && LostCityConfiguration.DIMENSION_BOP) {
            biomeProvider = getInternalBiomeProvider(world);
        } else {
            biomeProvider = new BiomeProvider(world.getWorldInfo());
        }
        this.biomeProvider=LostWorldFilteredBiomeProvider.wrap(world,biomeProvider);
    }

}
