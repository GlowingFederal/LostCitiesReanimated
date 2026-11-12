package mcjty.lostcities.core;

import com.google.gson.*;
import mcjty.lostcities.assets.ModernAssetPack;
import mcjty.lostcities.varia.QualityRandom;
import java.util.*;

/** Upstream area selection, whole-footprint validation and roadside alignment, without loading world chunks. */
public final class ScatteredPlanner {
    public interface Environment {
        boolean biome(int x,int z,JsonElement matcher);
        boolean occupied(int x,int z);
        Terrain terrain(int x,int z);
        /** Absolute surface height, or null for absent/tunnel highways. Axis 0 is east-west. */
        Integer highway(int x,int z,int axis);
        int seaLevel();
        boolean voidTerrain();
    }
    public static final class Terrain {
        public final int minimum,maximum,average;
        public Terrain(int minimum,int maximum,int average){this.minimum=minimum;this.maximum=maximum;this.average=average;}
    }
    public static final class Plan {
        public final int x,z,width,depth,height,rotation;
        public final Integer highwayHeight;
        public final JsonObject definition;
        private final JsonObject multi;
        private final String single;
        private final long seed;
        private final int selectionBound,positionXBound,positionZBound,candidateCount,singleCount;
        private Plan(int x,int z,int width,int depth,int height,int rotation,Integer highwayHeight,JsonObject definition,JsonObject multi,String single,long seed,int selectionBound,int positionXBound,int positionZBound,int candidateCount,int singleCount){
            this.x=x;this.z=z;this.width=width;this.depth=depth;this.height=height;this.rotation=rotation;this.highwayHeight=highwayHeight;this.definition=definition;this.multi=multi;this.single=single;this.seed=seed;this.selectionBound=selectionBound;this.positionXBound=positionXBound;this.positionZBound=positionZBound;this.candidateCount=candidateCount;this.singleCount=singleCount;
        }
        public boolean contains(int x,int z){return x>=this.x&&z>=this.z&&x<this.x+width&&z<this.z+depth;}
        public String building(int x,int z){
            if(multi==null)return single;
            int rx=x-this.x,rz=z-this.z,ox,oz,w=integer(multi,"dimx",1),d=integer(multi,"dimz",1);
            switch(rotation){case 0:ox=rx;oz=rz;break;case 1:ox=rz;oz=d-1-rx;break;case 2:ox=w-1-rx;oz=d-1-rz;break;case 3:ox=w-1-rz;oz=rx;break;default:throw new IllegalStateException();}
            return multi.getAsJsonArray("buildings").get(ox).getAsJsonArray().get(oz).getAsString();
        }
        /** Replay selection draws so every section starts with the same floor-selection stream. */
        public Random random(){
            Random random=new QualityRandom(seed);random.nextFloat();random.nextInt(selectionBound);
            if(candidateCount>0)random.nextInt(candidateCount);else {random.nextInt(positionXBound);random.nextInt(positionZBound);}
            if(singleCount>1)random.nextInt(singleCount);return random;
        }
        public boolean opensRailing(int highwayX,int highwayZ,int height){
            if(!flag(definition,"clearhighwayrailing",false)||highwayHeight==null||highwayHeight!=height)return false;
            switch(rotation){case 0:return highwayZ==z-1&&highwayX>=x&&highwayX<x+width;case 1:return highwayX==x+width&&highwayZ>=z&&highwayZ<z+depth;case 2:return highwayZ==z+depth&&highwayX>=x&&highwayX<x+width;case 3:return highwayX==x-1&&highwayZ>=z&&highwayZ<z+depth;default:return false;}
        }
    }
    private static final class Candidate {
        final int x,z,w,d,rotation;final Integer highway;final Terrain terrain;
        Candidate(int x,int z,int w,int d,int rotation,Integer highway,Terrain terrain){this.x=x;this.z=z;this.w=w;this.d=d;this.rotation=rotation;this.highway=highway;this.terrain=terrain;}
    }
    private final long seed;
    private final JsonObject settings;
    private final Environment environment;
    private final int area;
    private final float multiplier;
    // Null is a cached invalid plan. Access is confined to the native world thread.
    private final Map<Long,Plan> plans=new LinkedHashMap<Long,Plan>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,Plan> entry){return size()>512;}};
    public ScatteredPlanner(long seed,JsonObject settings,float multiplier,Environment environment){
        this.seed=seed;this.settings=settings;this.environment=environment;this.multiplier=multiplier;area=integer(settings,"areasize",8);
        if(area<1||area>64)throw new IllegalArgumentException("Scattered areasize must be 1..64 chunks");
    }
    public Plan at(int x,int z){Plan plan=areaAt(x,z);return plan!=null&&plan.contains(x,z)?plan:null;}
    private Plan areaAt(int x,int z){
        int ax=Math.floorDiv(x+2000000,area),az=Math.floorDiv(z+2000000,area);long key=((long)ax<<32)^(az&0xffffffffL);
        if(plans.containsKey(key))return plans.get(key);
        Plan plan=calculate(ax,az);plans.put(key,plan);return plan;
    }
    private Plan calculate(int ax,int az){
        long randomSeed=seed+ax*5564338337L+az*25564337621L;Random random=new QualityRandom(randomSeed);
        if(random.nextFloat()>=decimal(settings,"chance",0)*multiplier||!settings.has("list"))return null;
        int originX=ax*area-2000000,originZ=az*area-2000000,total=integer(settings,"weightnone",0);
        List<JsonObject> eligible=new ArrayList<>();
        for(JsonElement element:settings.getAsJsonArray("list")){JsonObject ref=element.getAsJsonObject();int weight=integer(ref,"weight",1);if(weight<0)throw new IllegalArgumentException("Negative scattered weight");if(environment.biome(originX,originZ,ref.get("biomes"))){eligible.add(ref);total+=weight;}}
        if(eligible.isEmpty()||total<=0)return null;
        int selection=random.nextInt(total);JsonObject reference=null;
        for(JsonObject ref:eligible){int weight=integer(ref,"weight",1);if(selection<weight){reference=ref;break;}selection-=weight;}
        if(reference==null)return null;
        String name=reference.get("name").getAsString();JsonObject definition=required("scattered",name);
        JsonObject multi=definition.has("multibuilding")?required("multibuildings",definition.get("multibuilding").getAsString()):null;
        int w=multi==null?1:integer(multi,"dimx",1),d=multi==null?1:integer(multi,"dimz",1);
        if(w>area||d>area)return null;
        Candidate chosen;int boundX=0,boundZ=0,candidates=0;
        if(flag(reference,"nearhighway",false)){
            List<Candidate> choices=new ArrayList<>();int rotations=flag(definition,"rotatable",false)?4:1;
            for(int r=0;r<rotations;r++){int width=(r&1)==0?w:d,depth=(r&1)==0?d:w;
                for(int x=originX;x<=originX+area-width;x++)for(int z=originZ;z<=originZ+area-depth;z++){
                    Integer height=connection(x,z,width,depth,r);if(height==null)continue;
                    Terrain terrain=terrain(reference,x,z,width,depth);if(terrain!=null)choices.add(new Candidate(x,z,width,depth,r,height,terrain));
                }
            }
            if(choices.isEmpty())return null;candidates=choices.size();chosen=choices.get(random.nextInt(candidates));
        }else {
            boundX=area-w+1;boundZ=area-d+1;int x=originX+random.nextInt(boundX),z=originZ+random.nextInt(boundZ);
            Terrain terrain=terrain(reference,x,z,w,d);if(terrain==null)return null;chosen=new Candidate(x,z,w,d,0,null,terrain);
        }
        String single=null;int singleCount=0;
        if(multi==null){JsonArray buildings=definition.getAsJsonArray("buildings");if(buildings==null||buildings.size()==0)throw new IllegalArgumentException("Scattered "+name+" has no buildings");singleCount=buildings.size();single=buildings.get(singleCount==1?0:random.nextInt(singleCount)).getAsString();}
        String mode=string(definition,"terrainheight","highest");int height;
        if(chosen.highway!=null)height=chosen.highway;
        else if(mode.equals("lowest"))height=chosen.terrain.minimum;
        else if(mode.equals("average"))height=chosen.terrain.average;
        else if(mode.equals("ocean"))height=environment.seaLevel();
        else if(mode.equals("highest"))height=chosen.terrain.maximum;
        else throw new IllegalArgumentException("Unknown scattered terrainheight "+mode);
        height+=integer(definition,"heightoffset",0);if(height<1||height>254)return null;
        return new Plan(chosen.x,chosen.z,chosen.w,chosen.d,height,chosen.rotation,chosen.highway,definition,multi,single,randomSeed,total,boundX,boundZ,candidates,singleCount);
    }
    private Terrain terrain(JsonObject reference,int x,int z,int w,int d){
        int min=255,max=0,sum=0;
        for(int dx=0;dx<w;dx++)for(int dz=0;dz<d;dz++){
            if(!environment.biome(x+dx,z+dz,reference.get("biomes"))||environment.occupied(x+dx,z+dz))return null;
            Terrain terrain=environment.terrain(x+dx,z+dz);
            if(environment.voidTerrain()&&!flag(reference,"allowvoid",false)&&terrain.average<=3)return null;
            min=Math.min(min,terrain.minimum);max=Math.max(max,terrain.maximum);sum+=terrain.average;
        }
        if(max-min>integer(reference,"maxheightdiff",255))return null;return new Terrain(min,max,sum/(w*d));
    }
    private Integer connection(int x,int z,int w,int d,int rotation){
        Integer height=null;int edge=(rotation&1)==0?w:d;
        for(int i=0;i<edge;i++){int hx=x,hz=z;switch(rotation){case 0:hx+=i;hz--;break;case 1:hx+=w;hz+=i;break;case 2:hx+=i;hz+=d;break;case 3:hx--;hz+=i;break;default:throw new IllegalStateException();}
            Integer current=environment.highway(hx,hz,(rotation&1)==0?0:1);if(current==null||height!=null&&!height.equals(current))return null;height=current;
        }return height;
    }
    private static JsonObject required(String category,String name){JsonObject object=ModernAssetPack.definition(category,name);if(object==null)throw new IllegalArgumentException("Missing "+category+"/"+name);return object;}
    private static int integer(JsonObject object,String key,int fallback){return object.has(key)?object.get(key).getAsInt():fallback;}
    private static float decimal(JsonObject object,String key,float fallback){return object.has(key)?object.get(key).getAsFloat():fallback;}
    private static boolean flag(JsonObject object,String key,boolean fallback){return object.has(key)?object.get(key).getAsBoolean():fallback;}
    private static String string(JsonObject object,String key,String fallback){return object.has(key)?object.get(key).getAsString():fallback;}
}
