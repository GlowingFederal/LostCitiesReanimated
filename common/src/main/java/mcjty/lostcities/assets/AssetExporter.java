package mcjty.lostcities.assets;

import com.google.gson.*;
import java.util.*;

/** Bounded native editor export; each part carries the palette needed to load it independently. */
public final class AssetExporter {
    public interface Blocks { JsonObject at(int x,int y,int z); }
    public static JsonArray export(String name,int slices,boolean building,Blocks blocks){
        if(slices<1||slices>256||building&&slices%6!=0)throw new IllegalArgumentException("Export must fit 1–256 native block layers");
        JsonArray assets=new JsonArray(),palette=new JsonArray();Map<String,Character> mapping=new LinkedHashMap<>();int next=33;
        List<JsonArray> layers=new ArrayList<>();
        for(int y=0;y<slices;y++){
            JsonArray rows=new JsonArray();for(int z=0;z<16;z++){StringBuilder row=new StringBuilder(16);for(int x=0;x<16;x++){
                JsonObject entry=blocks.at(x,y,z);String key=entry.toString();Character character=mapping.get(key);
                if(character==null){while(next==35||next>=127&&next<161||next>=0xD800&&next<=0xDFFF)next++;if(next>0xFFFD)throw new IllegalArgumentException("Export exceeds the palette character limit");character=(char)next++;mapping.put(key,character);entry.addProperty("char",String.valueOf(character));palette.add(entry);}
                row.append(character.charValue());
            }rows.add(new JsonPrimitive(row.toString()));}layers.add(rows);
        }
        int count=building?slices/6:1;JsonArray selection=new JsonArray();
        for(int index=0;index<count;index++){
            String partName=building?name+"_floor_"+index:name;JsonObject part=new JsonObject();part.addProperty("type","part");part.addProperty("name",partName);part.addProperty("xsize",16);part.addProperty("zsize",16);
            JsonArray partLayers=new JsonArray();for(int y=building?index*6:0;y<(building?(index+1)*6:slices);y++)partLayers.add(layers.get(y));part.add("slices",partLayers);part.add("palette",palette);assets.add(part);
            JsonObject choice=new JsonObject();choice.addProperty("part",partName);choice.addProperty("floor",index);selection.add(choice);
        }
        if(building){
            JsonObject filler=new JsonObject();filler.addProperty("char","#");filler.addProperty("block","minecraft:stone@0");palette.add(filler);
            JsonObject definition=new JsonObject();definition.addProperty("type","building");definition.addProperty("name",name);definition.addProperty("filler","#");definition.addProperty("minfloors",count-1);definition.addProperty("maxfloors",count-1);definition.addProperty("mincellars",0);definition.addProperty("maxcellars",0);definition.addProperty("overrideFloors",true);definition.add("palette",palette);definition.add("parts",selection);assets.add(definition);
        }
        return assets;
    }
    private AssetExporter(){}
}
