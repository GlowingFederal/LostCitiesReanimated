package mcjty.lostcities.dimensions.world;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;

/** Biome layers belong to each world, including successive integrated-server sessions. */
public class LostWorldTypeBOP extends LostWorldType {
    public LostWorldTypeBOP(){super("lostcities_bop");}
    @Override protected BiomeProvider getInternalBiomeProvider(World world){
        return mcjty.lostcities.compat.BopBiomes.create(world);
    }
}
