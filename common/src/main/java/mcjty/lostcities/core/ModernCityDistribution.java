package mcjty.lostcities.core;

import mcjty.lostcities.core.noise.CityRarityMap;
import mcjty.lostcities.worldgen.highway.CityPotential;
import java.util.*;

/** Modern radial/noise city potential and spawn-distance scaling, independent of final building decisions. */
public final class ModernCityDistribution implements CityPotential {
    public interface Modifier {float apply(int x,int z,float factor);}
    private final float chance;
    private final int minRadius,maxRadius;
    private final ModernOptions options;
    private final Modifier modifier;
    private final float maximumMultiplier;
    private final Map<Long,Float> centers=new LinkedHashMap<Long,Float>(4096,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,Float> entry){return size()>65536;}};
    private final CityRarityMap noise;
    private final Map<Long,Float> cache=new LinkedHashMap<Long,Float>(4096,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,Float> entry){return size()>8192;}};
    public ModernCityDistribution(long seed,float chance,int minRadius,int maxRadius,ModernOptions options,Modifier modifier){this(seed,chance,minRadius,maxRadius,options,modifier,Float.NaN);}
    public ModernCityDistribution(long seed,float chance,int minRadius,int maxRadius,ModernOptions options,Modifier modifier,float maximumMultiplier){
        this.maximumMultiplier=maximumMultiplier;
        this.chance=chance;this.minRadius=minRadius;this.maxRadius=maxRadius;this.options=options;this.modifier=modifier;
        if(minRadius<1||maxRadius<minRadius)throw new IllegalArgumentException("Invalid city radius range");
        noise=chance<0?new CityRarityMap(seed,options.cityScale,options.cityOffset,options.cityInnerScale):null;
    }
    @Override public float getPotential(int x,int z){
        long key=((long)x<<32)^(z&0xffffffffL);Float result=cache.get(key);if(result!=null)return result;
        return finish(x,z,raw(x,z));
    }
    /** Raw noise influence for upstream city-style thresholds, before biome/spawn modifiers. */
    public float noiseFactor(int x,int z){return noise==null?0:noise.getCityFactor(x,z);}
    private float raw(int x,int z){
        float factor=0;
        if(noise!=null)factor=noise.getCityFactor(x,z);
        else {
            int offset=(maxRadius+15)/16;
            for(int cx=x-offset;cx<=x+offset;cx++)for(int cz=z-offset;cz<=z+offset;cz++){
                float radius;
                if(options.fastTerrainPlanning){long key=((long)cx<<32)^(cz&0xffffffffL);Float cached=centers.get(key);if(cached==null){cached=center(cx,cz,chance)?radius(cx,cz,minRadius,maxRadius):-1F;centers.put(key,cached);}radius=cached;if(radius<0)continue;}
                else {if(!center(cx,cz,chance))continue;radius=radius(cx,cz,minRadius,maxRadius);}double dx=(cx-(double)x)*16,dz=(cz-(double)z)*16,distance=dx*dx+dz*dz;
                if(distance<radius*radius)factor+=(float)((radius-Math.sqrt(distance))/radius);
            }
        }
        if(options.spawnDistance2>0){double bx=x*16D,bz=z*16D,distance=Math.sqrt(bx*bx+bz*bz);
            double position=Math.max(0,Math.min(1,(distance-options.spawnDistance1)/(options.spawnDistance2-options.spawnDistance1)));
            factor*=options.spawnMultiplier1+position*(options.spawnMultiplier2-options.spawnMultiplier1);
        }
        return factor;
    }
    private float finish(int x,int z,float factor){
        long key=((long)x<<32)^(z&0xffffffffL);
        if(factor>.0001F)factor=modifier.apply(x,z,factor);
        factor=Math.max(0,Math.min(1,factor));cache.put(key,factor);return factor;
    }
    @Override public int getScoreWithUpperBound(int x,int z,int requiredScore){
        if(!options.fastTerrainPlanning)return CityPotential.score(getPotential(x,z));
        long key=((long)x<<32)^(z&0xffffffffL);Float cached=cache.get(key);if(cached!=null)return CityPotential.score(cached);
        float raw=raw(x,z);
        if(Float.isFinite(maximumMultiplier)&&maximumMultiplier>=1&&CityPotential.score(Math.nextUp(raw*maximumMultiplier))<requiredScore)return -1;
        return CityPotential.score(finish(x,z,raw));
    }
    public static boolean center(int x,int z,float chance){return new Random(z*797003437L+x*295075153L).nextDouble()<chance;}
    public static float radius(int x,int z,int min,int max){return min+new Random(z*100001653L+x*295075153L).nextInt(Math.max(1,max-min));}
}
