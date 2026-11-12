package mcjty.lostcities.core;

import com.google.gson.*;
import mcjty.lostcities.assets.*;
import java.util.*;

/** Column decorations use the original bounded attempts and per-chunk random stream. */
public final class StuffGenerator {
    public interface Environment {
        boolean building();String buildingName();
        int minimumHeight();int maximumHeight();
        boolean biome(String selector);
        boolean block(int x,int y,int z,String selector);
        boolean air(int x,int y,int z);
        boolean seesSky(int x,int y,int z);
        void place(int x,int y,int z,char paletteCharacter);
    }
    public static void generate(int chunkX,int chunkZ,Set<String> tags,Environment environment){
        Random random=new Random(chunkX*2570174657L+chunkZ*101754695981L);
        for(JsonObject definition:ModernAssetPack.definitions("stuff")){
            boolean enabled=false;
            if(!definition.has("tags")||definition.getAsJsonArray("tags").size()==0)enabled=tags.contains("all");
            else for(JsonElement tag:definition.getAsJsonArray("tags"))if(tags.contains(tag.getAsString()))enabled=true;
            if(!enabled||definition.has("inbuilding")&&definition.get("inbuilding").getAsBoolean()!=environment.building())continue;
            if(!BiomeMatcher.matches(definition.get("biomes"),environment::biome)||!BiomeMatcher.matches(definition.get("buildings"),name->ModernAssetPack.name(name).equals(ModernAssetPack.name(environment.buildingName()))))continue;
            String column=definition.get("column").getAsString();if(column.isEmpty())continue;
            int min=Math.max(1,integer(definition,"minheight",environment.minimumHeight())),max=Math.min(256-column.length(),integer(definition,"maxheight",environment.maximumHeight()));
            int minCount=integer(definition,"mincount",0),maxCount=integer(definition,"maxcount",minCount),attempts=integer(definition,"attempts",1);
            if(maxCount<minCount||minCount<0||maxCount>65536||attempts<0||attempts>1024)throw new IllegalArgumentException("Invalid decoration count/attempts: "+definition);
            if(max<=min)continue;int count=minCount+(maxCount==minCount?0:random.nextInt(maxCount-minCount));
            for(int item=0;item<count;item++)for(int attempt=0;attempt<attempts;attempt++){
                int x=random.nextInt(16),y=min+random.nextInt(max-min),z=random.nextInt(16);
                if(!BiomeMatcher.matches(definition.get("blocks"),selector->environment.block(x,y-1,z,selector))||!BiomeMatcher.matches(definition.get("upperblocks"),selector->environment.block(x,y+column.length(),z,selector)))continue;
                if(definition.has("seesky")&&definition.get("seesky").getAsBoolean()!=environment.seesSky(x,y,z))continue;
                boolean clear=true;for(int k=0;k<column.length();k++)if(!environment.air(x,y+k,z)){clear=false;break;}
                if(clear){for(int k=0;k<column.length();k++)environment.place(x,y+k,z,column.charAt(k));break;}
            }
        }
    }
    private static int integer(JsonObject object,String key,int fallback){return object.has(key)?object.get(key).getAsInt():fallback;}
    private StuffGenerator(){}
}
