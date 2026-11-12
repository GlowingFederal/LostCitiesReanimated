package mcjty.lostcities.legacy;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.BiomeGenBase;
import java.util.*;

public final class LegacyRegistries {
    public static final BlocksRegistry BLOCKS=new BlocksRegistry();
    public static final BiomesRegistry BIOMES=new BiomesRegistry();
    public static ResourceLocation blockName(Block block){return new ResourceLocation(String.valueOf(Block.blockRegistry.getNameForObject(block)));}
    public static final class BlocksRegistry {
        public Block getValue(ResourceLocation id){return Block.blockRegistry.containsKey(id.toString())?(Block)Block.blockRegistry.getObject(id.toString()):null;}
        public Set<ResourceLocation> getKeys(){Set<ResourceLocation> keys=new TreeSet<>(Comparator.comparing(ResourceLocation::toString));for(Object key:Block.blockRegistry.getKeys())keys.add(new ResourceLocation(key.toString()));return keys;}
    }
    public static final class BiomesRegistry {
        public BiomeGenBase getValue(ResourceLocation id){return LegacyBiomes.find(id);}
        public Set<ResourceLocation> getKeys(){Set<ResourceLocation> keys=new TreeSet<>(Comparator.comparing(ResourceLocation::toString));for(BiomeGenBase biome:BiomeGenBase.getBiomeGenArray())if(biome!=null)keys.add(LegacyBiomes.name(biome));return keys;}
    }
    private LegacyRegistries(){}
}
