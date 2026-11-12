package mcjty.lostcities.core;

import mcjty.lostcities.worldgen.street.HierarchicalStreetPlanner;
import mcjty.lostcities.worldgen.street.PlannedRoadType;
import java.util.*;

/** Upstream bounded water-gap spans along the primary-road field. Higher-level ramps remain unsupported upstream. */
public final class PrimaryBridgePlanner {
    public interface Environment {
        boolean city(int x,int z);
        boolean water(int x,int z);
        boolean endpoint(int x,int z);
    }
    public static final class Span {
        public final long id;
        public final int axis,minX,minZ,maxX,maxZ,length;
        private Span(long id,int axis,int minX,int minZ,int maxX,int maxZ,int length){this.id=id;this.axis=axis;this.minX=minX;this.minZ=minZ;this.maxX=maxX;this.maxZ=maxZ;this.length=length;}
    }
    private final long seed,dimension;
    private final int maximum,intersectionAxis;
    private final float chance;
    private final HierarchicalStreetPlanner streets;
    private final Environment environment;
    private final Map<Key,Optional<Span>> cache=new BoundedMetadataCache<>(8192);
    public PrimaryBridgePlanner(long seed,String dimension,HierarchicalStreetPlanner streets,int maximum,float chance,Environment environment){
        if(maximum<1||maximum>64||chance<0||chance>1)throw new IllegalArgumentException("Invalid primary bridge settings");
        this.seed=seed;this.dimension=hash(dimension);this.streets=streets;this.maximum=maximum;this.chance=chance;this.environment=environment;
        intersectionAxis=(int)(mix(seed^this.dimension^0x67a14ce3b9052df8L)&1L);
    }
    public Span at(int x,int z,int axis){Key key=new Key(x,z,axis);Optional<Span> found=cache.get(key);if(found==null){found=Optional.ofNullable(calculate(x,z,axis));cache.put(key,found);}return found.orElse(null);}
    private Span calculate(int x,int z,int axis){
        if(!gap(x,z,axis))return null;
        int negative=endpointDistance(x,z,axis,-1),positive=endpointDistance(x,z,axis,1);
        if(negative<0||positive<0)return null;int length=negative+positive-1;if(length<1||length>maximum)return null;
        int minX=x-(axis==0?negative:0),minZ=z-(axis==1?negative:0),maxX=x+(axis==0?positive:0),maxZ=z+(axis==1?positive:0);
        if(!environment.endpoint(minX,minZ)||!environment.endpoint(maxX,maxZ))return null;
        long value=seed^dimension^0x31d6a7f04c82be59L;
        value^=mix((long)minX+0x9e3779b97f4a7c15L);value^=mix((long)minZ+0xc2b2ae3d27d4eb4fL);
        value^=mix((long)maxX+0x165667b19e3779f9L);value^=mix((long)maxZ+0x85ebca77c2b2ae63L);
        value^=axis==0?0x243f6a8885a308d3L:0x13198a2e03707344L;long id=mix(value);
        return (id>>>11)*0x1.0p-53>=chance?null:new Span(id,axis,minX,minZ,maxX,maxZ,length);
    }
    private int endpointDistance(int x,int z,int axis,int direction){
        for(int distance=1;distance<=maximum+1;distance++){
            int cx=x+(axis==0?distance*direction:0),cz=z+(axis==1?distance*direction:0);
            if(environment.city(cx,cz)&&streets.getRoadType(cx,cz)==PlannedRoadType.PRIMARY)return distance;
            if(distance>maximum||!gap(cx,cz,axis))return -1;
        }return -1;
    }
    private boolean gap(int x,int z,int axis){
        boolean horizontal=streets.isHorizontalPrimary(z),vertical=streets.isVerticalPrimary(x);
        if(environment.city(x,z)||(axis==0?!horizontal:!vertical))return false;
        if(horizontal&&vertical&&axis!=intersectionAxis)return false;
        return environment.water(x,z);
    }
    private static long hash(String text){long result=0xcbf29ce484222325L;for(int i=0;i<text.length();i++){result^=text.charAt(i);result*=0x100000001b3L;}return mix(result);}
    private static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    private static final class Key {
        final int x,z,axis;Key(int x,int z,int axis){this.x=x;this.z=z;this.axis=axis;}
        @Override public boolean equals(Object other){return other instanceof Key&&((Key)other).x==x&&((Key)other).z==z&&((Key)other).axis==axis;}
        @Override public int hashCode(){return (31*x+z)*2+axis;}
    }
}
