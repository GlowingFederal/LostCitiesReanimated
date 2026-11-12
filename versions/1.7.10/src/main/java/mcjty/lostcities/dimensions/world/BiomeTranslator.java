package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.config.BiomeSelectionStrategy;
import mcjty.lostcities.legacy.LegacyBiomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.BiomeGenBase;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import java.util.*;

/** Native legacy biome restriction, preserving climate similarity and stable randomized choices. */
public final class BiomeTranslator {
    private final Map<BiomeGenBase,BiomeGenBase> translations=new IdentityHashMap<>();
    private final List<Pair<Float,BiomeGenBase>> allowed;
    private final Map<BiomeGenBase,BiomeGenBase> manual=new IdentityHashMap<>();
    private final BiomeSelectionStrategy strategy;

    public BiomeTranslator(String[] allowedFactors,String[] mappings,BiomeSelectionStrategy strategy){
        allowed=parseBiomes(allowedFactors);
        this.strategy=strategy==null?BiomeSelectionStrategy.ORIGINAL:strategy;
        for(String mapping:mappings){String[] pair=split(mapping);BiomeGenBase source=find(pair[0]),destination=find(pair[1]);if(source!=null&&destination!=null)manual.put(source,destination);}
    }
    public static List<Pair<Float,BiomeGenBase>> parseBiomes(String[] specifications){
        List<Pair<Float,BiomeGenBase>> result=new ArrayList<>();
        for(String entry:specifications){String[] pair=split(entry);float factor=Float.parseFloat(pair[1]);if(!Float.isFinite(factor)||factor<0)throw new IllegalArgumentException("Invalid biome factor: "+entry);BiomeGenBase biome=find(pair[0]);if(biome!=null)result.add(Pair.of(factor,biome));}
        return result;
    }
    private static String[] split(String entry){String[] pair=entry.split("=",-1);if(pair.length!=2||pair[0].trim().isEmpty()||pair[1].trim().isEmpty())throw new IllegalArgumentException("Expected biome=value: "+entry);return new String[]{pair[0].trim(),pair[1].trim()};}
    private static BiomeGenBase find(String name){BiomeGenBase biome=LegacyBiomes.find(new ResourceLocation(name));if(biome==null)LogManager.getLogger("LostCities").warn("Unknown biome in profile mapping: "+name);return biome;}

    public BiomeGenBase translate(BiomeGenBase biome){
        if(biome==null)return null;
        BiomeGenBase mapped=manual.get(biome);if(mapped!=null)return mapped;
        if(allowed.isEmpty())return biome;
        mapped=translations.get(biome);if(mapped!=null)return mapped;
        List<BiomeGenBase> best=new ArrayList<>();double distance=Double.MAX_VALUE;
        float sameness=strategy==BiomeSelectionStrategy.RANDOMIZED?.2F:strategy==BiomeSelectionStrategy.VARIED?1F:0;
        for(Pair<Float,BiomeGenBase> candidate:allowed){BiomeGenBase other=candidate.getRight();double current=distance(biome,other)*candidate.getLeft();
            if(sameness>0&&Math.abs(current-distance)<sameness)best.add(other);
            else if(current<distance){distance=current;best.clear();best.add(other);}
        }
        Random random=new Random(biome.biomeName.hashCode());random.nextFloat();random.nextFloat();
        mapped=best.size()==1?best.get(0):best.get(random.nextInt(best.size()));translations.put(biome,mapped);return mapped;
    }
    private static double distance(BiomeGenBase a,BiomeGenBase b){if(a==b)return -1000;float rain=a.rainfall-b.rainfall,temp=a.temperature-b.temperature,variation=a.heightVariation-b.heightVariation,height=a.rootHeight-b.rootHeight;return Math.sqrt(rain*rain+temp*temp+variation*variation+height*height);}
}
