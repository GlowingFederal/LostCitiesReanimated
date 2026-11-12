package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.config.*;
import mcjty.lostcities.dimensions.world.lost.CitySphere;
import net.minecraft.world.World;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.biome.*;
import java.util.*;

/** Translates the owning generator's biome samples without replacing its biome layers. */
public final class LostWorldFilteredBiomeProvider extends WorldChunkManager {
    private final World world;
    private final WorldChunkManager original;
    private final BiomeTranslator inside,outside;
    private final LostCityProfile profile;
    private LostWorldFilteredBiomeProvider(World world,WorldChunkManager original,LostCityProfile profile,LostCityProfile outer){
        this.world=world;this.original=original;this.profile=profile;
        inside=new BiomeTranslator(profile.ALLOWED_BIOME_FACTORS,profile.MANUAL_BIOME_MAPPINGS,profile.BIOME_SELECTION_STRATEGY);
        outside=new BiomeTranslator(outer.ALLOWED_BIOME_FACTORS.length==0?profile.ALLOWED_BIOME_FACTORS:outer.ALLOWED_BIOME_FACTORS,outer.MANUAL_BIOME_MAPPINGS,outer.BIOME_SELECTION_STRATEGY);
    }
    public static WorldChunkManager wrap(World world,WorldChunkManager original){
        LostCityProfile profile=WorldTypeTools.getProfile(world),outer=profile;
        if(profile.isSphereLandscape()&&profile.CITYSPHERE_LANDSCAPE_OUTSIDE&&!profile.CITYSPHERE_OUTSIDE_PROFILE.isEmpty()){
            outer=LostCityConfiguration.profiles.get(profile.CITYSPHERE_OUTSIDE_PROFILE);
            if(outer==null)throw new IllegalArgumentException("Unknown outside profile "+profile.CITYSPHERE_OUTSIDE_PROFILE);
        }
        if(profile.ALLOWED_BIOME_FACTORS.length==0&&profile.MANUAL_BIOME_MAPPINGS.length==0&&outer.ALLOWED_BIOME_FACTORS.length==0&&outer.MANUAL_BIOME_MAPPINGS.length==0&&!profile.CITYSPHERE_SINGLE_BIOME)return original;
        return new LostWorldFilteredBiomeProvider(world,original,profile,outer);
    }
    private BiomeGenBase translate(BiomeGenBase biome,int x,int z){
        if(world.isRemote)return biome;
        LostCityChunkGenerator provider=WorldTypeTools.getChunkGenerator(world.provider.dimensionId);
        if(profile.isSphereLandscape()&&provider!=null){CitySphere sphere=CitySphere.getCitySphere(x>>4,z>>4,provider);
            if(sphere.isEnabled()&&CitySphere.squaredDistance(sphere.getCenterPos().getX(),sphere.getCenterPos().getZ(),x,z)<=Math.pow(sphere.getRadius()-2,2)){
                if(sphere.getBiome()!=null)return sphere.getBiome();
            }else if(profile.CITYSPHERE_LANDSCAPE_OUTSIDE)return outside.translate(biome);
        }
        return inside.translate(biome);
    }
    private BiomeGenBase[] translate(BiomeGenBase[] biomes,int x,int z,int width,int height,int step){for(int dz=0;dz<height;dz++)for(int dx=0;dx<width;dx++){int i=dx+dz*width;biomes[i]=translate(biomes[i],(x+dx)*step,(z+dz)*step);}return biomes;}
    @Override public BiomeGenBase getBiomeGenAt(int x,int z){return translate(original.getBiomeGenAt(x,z),x,z);}
    @Override public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] reuse,int x,int z,int width,int height){return translate(original.getBiomesForGeneration(reuse,x,z,width,height),x,z,width,height,4);}
    @Override public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] reuse,int x,int z,int width,int height){return getBiomeGenAt(reuse,x,z,width,height,true);}
    @Override public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] reuse,int x,int z,int width,int height,boolean cache){return translate(original.getBiomeGenAt(reuse,x,z,width,height,cache),x,z,width,height,1);}
    @Override public float[] getRainfall(float[] reuse,int x,int z,int width,int height){if(reuse==null||reuse.length<width*height)reuse=new float[width*height];BiomeGenBase[] biomes=loadBlockGeneratorData(null,x,z,width,height);for(int i=0;i<width*height;i++)reuse[i]=Math.min(1,biomes[i].getIntRainfall()/65536F);return reuse;}
    @Override public List getBiomesToSpawnIn(){List<BiomeGenBase> result=new ArrayList<>();for(Object value:original.getBiomesToSpawnIn()){BiomeGenBase biome=inside.translate((BiomeGenBase)value);if(!result.contains(biome))result.add(biome);}return result;}
    @Override public boolean areBiomesViable(int x,int z,int radius,List allowed){int left=(x-radius)>>2,top=(z-radius)>>2,width=((x+radius)>>2)-left+1,height=((z+radius)>>2)-top+1;BiomeGenBase[] biomes=getBiomesForGeneration(null,left,top,width,height);for(int i=0;i<width*height;i++)if(!allowed.contains(biomes[i]))return false;return true;}
    @Override public ChunkPosition findBiomePosition(int x,int z,int radius,List allowed,Random random){int left=(x-radius)>>2,top=(z-radius)>>2,width=((x+radius)>>2)-left+1,height=((z+radius)>>2)-top+1,count=0;BiomeGenBase[] biomes=getBiomesForGeneration(null,left,top,width,height);ChunkPosition result=null;for(int i=0;i<width*height;i++)if(allowed.contains(biomes[i])&&random.nextInt(++count)==0)result=new ChunkPosition((left+i%width)<<2,0,(top+i/width)<<2);return result;}
    @Override public void cleanupCache(){super.cleanupCache();original.cleanupCache();}
}
