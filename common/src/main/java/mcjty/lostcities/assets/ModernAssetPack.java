package mcjty.lostcities.assets;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Converts modern asset envelopes once at startup, preserving the original slices and selectors. */
public final class ModernAssetPack {
    private static final Map<String,Map<String,JsonObject>> DEFINITIONS=new TreeMap<>();
    private static final Map<String,String> TYPES=new HashMap<>();
    static {
        TYPES.put("buildings","building");TYPES.put("citystyles","citystyle");TYPES.put("conditions","condition");
        TYPES.put("multibuildings","multibuilding");TYPES.put("palettes","palette");TYPES.put("parts","part");
        TYPES.put("styles","style");TYPES.put("worldstyles","worldstyle");TYPES.put("cities","city");TYPES.put("spheres","sphere");
    }
    private static JsonObject copy(JsonObject value){return new JsonParser().parse(value.toString()).getAsJsonObject();}
    public static String name(String value){return value.startsWith("lostcities:")?value.substring(11):value;}
    public static JsonObject definition(String category,String name){Map<String,JsonObject> map=DEFINITIONS.get(category);return map==null?null:map.get(name(name));}
    public static Collection<JsonObject> definitions(String category){Map<String,JsonObject> map=DEFINITIONS.get(category);return map==null?Collections.emptyList():Collections.unmodifiableCollection(map.values());}

    /** Resolve every declared palette material before world creation, reporting all bad entries together. */
    public static void validateMaterials(java.util.function.Function<String,?> resolver){
        Set<String> materials=new TreeSet<>();
        for(String category:Arrays.asList("palettes","variants","parts","buildings"))for(JsonObject object:definitions(category))collectMaterials(object,materials);
        List<String> errors=new ArrayList<>();
        for(String material:materials)try{resolver.apply(material);}catch(RuntimeException error){errors.add(material+": "+error.getMessage());}
        if(!errors.isEmpty())throw new IllegalArgumentException("Unresolved modern palette materials:\n"+String.join("\n",errors));
    }
    private static void collectMaterials(JsonElement element,Set<String> materials){
        if(element.isJsonArray()){for(JsonElement child:element.getAsJsonArray())collectMaterials(child,materials);}
        else if(element.isJsonObject())for(Map.Entry<String,JsonElement> entry:element.getAsJsonObject().entrySet()){
            if((entry.getKey().equals("block")||entry.getKey().equals("damaged"))&&entry.getValue().isJsonPrimitive()&&entry.getValue().getAsJsonPrimitive().isString())materials.add(entry.getValue().getAsString());
            else if(!entry.getKey().equals("tag"))collectMaterials(entry.getValue(),materials);
        }
    }

    public static void load(File custom,Consumer<JsonObject> importer,Consumer<String> diagnostics){
        DEFINITIONS.clear();
        LootAssetPack.load(custom);
        try(InputStream stream=ModernAssetPack.class.getResourceAsStream("/assets/lostcities/modern/index.txt")){
            if(stream==null)throw new IllegalStateException("Missing built-in modern asset index");
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
                String line;while((line=reader.readLine())!=null){
                    if(line.trim().isEmpty())continue;
                    try(InputStream in=ModernAssetPack.class.getResourceAsStream("/assets/lostcities/modern/"+line)){
                        if(in==null)throw new IllegalArgumentException("Missing indexed asset "+line);
                        read(line,new InputStreamReader(in,StandardCharsets.UTF_8));
                    }
                }
            }
        }catch(IOException error){throw new UncheckedIOException(error);}
        if(custom.isDirectory())try(Stream<Path> paths=Files.walk(custom.toPath())){
            List<Path> files=new ArrayList<>();paths.filter(path->Files.isRegularFile(path)&&path.toString().endsWith(".json")).forEach(files::add);
            files.sort(Comparator.comparing(path->custom.toPath().relativize(path).toString()));
            for(Path path:files){String relative=custom.toPath().relativize(path).toString().replace('\\','/');if(LootAssetPack.isPath(relative))continue;try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){read(relative,reader);}}
        }catch(IOException error){throw new UncheckedIOException(error);}
        boolean previous=AssetContext.bind(true);
        try {
            for(Map.Entry<String,Map<String,JsonObject>> category:DEFINITIONS.entrySet()){
                String type=TYPES.get(category.getKey());if(type==null)continue;
                for(Map.Entry<String,JsonObject> entry:category.getValue().entrySet()){
                    try{importer.accept(compile(type,entry.getKey(),entry.getValue()));}
                    catch(RuntimeException error){throw new IllegalArgumentException("Invalid modern "+category.getKey()+"/"+entry.getKey()+": "+error.getMessage(),error);}
                }
            }
        } finally {AssetContext.restore(previous);}
        diagnostics.accept("Loaded modern asset categories "+DEFINITIONS.keySet());
    }
    private static void read(String path,Reader reader){
        String namespace="lostcities";
        if(path.startsWith("data/")){
            String[] elements=path.split("/",5);
            if(elements.length!=5||!elements[2].equals("lostcities"))throw new IllegalArgumentException("Expected data/<namespace>/lostcities/<category>/<asset>.json: "+path);
            namespace=elements[1];path=elements[3]+"/"+elements[4];
        }
        int slash=path.indexOf('/');if(slash<0||!path.endsWith(".json"))throw new IllegalArgumentException("Expected <category>/<asset>.json: "+path);
        if(!namespace.matches("[a-z0-9_.-]+"))throw new IllegalArgumentException("Invalid asset namespace "+namespace);
        String category=path.substring(0,slash),name=name(namespace+":"+path.substring(slash+1,path.length()-5));
        if(!TYPES.containsKey(category)&&!category.equals("variants")&&!category.equals("scattered")&&!category.equals("stuff"))throw new IllegalArgumentException("Unsupported asset category "+category);
        JsonObject object=new JsonParser().parse(reader).getAsJsonObject();
        DEFINITIONS.computeIfAbsent(category,key->new TreeMap<>()).put(name,object);
    }
    public static String selectorValueKey(String selector){
        switch(selector){case "buildings":return "building";case "multibuildings":return "multibuilding";case "parks":return "park";case "fountains":return "fountain";case "stairs":return "stair";case "fronts":return "front";case "bridges":return "bridge";case "raildungeons":return "dungeon";default:return null;}
    }
    /** Check references against the complete modern and native legacy catalogs, without initializing assets. */
    public static void validateReferences(java.util.function.BiPredicate<String,String> nativeExists){
        List<String> errors=new ArrayList<>();
        for(Map.Entry<String,Map<String,JsonObject>> category:DEFINITIONS.entrySet())for(Map.Entry<String,JsonObject> asset:category.getValue().entrySet()){
            String location=category.getKey()+"/"+asset.getKey();JsonObject object=asset.getValue();
            java.util.function.BiConsumer<String,String> check=(target,id)->{if(id!=null&&!id.isEmpty()&&definition(target,id)==null&&!nativeExists.test(target,name(id)))errors.add(location+" references missing "+target+"/"+id);};
            try{
                switch(category.getKey()){
                    case "citystyles":
                        validateCitySettings(object);
                        reference(object,"inherit","citystyles",check);reference(object,"style","styles",check);
                        reference(object,"bridgesupportpart","parts",check);
                        if(object.has("selectors"))for(Map.Entry<String,JsonElement> selector:object.getAsJsonObject("selectors").entrySet())for(JsonElement entry:selector.getValue().getAsJsonArray()){
                            JsonObject choice=entry.getAsJsonObject();check.accept(selector.getKey().equals("buildings")?"buildings":selector.getKey().equals("multibuildings")?"multibuildings":"parts",choice.get("value").getAsString());weight(choice,"factor");
                        }
                        if(object.has("streetblocks"))for(String family:Arrays.asList("parts","largeparts","tertiaryparts"))if(object.getAsJsonObject("streetblocks").has(family))partSelections(object.getAsJsonObject("streetblocks").get(family),check);
                        break;
                    case "worldstyles":
                        reference(object,"outsidestyle","styles",check);reference(object,"bridgesupportpart","parts",check);reference(object,"highwaysupportpart","parts",check);
                        if(object.has("citystyles"))for(JsonElement entry:object.getAsJsonArray("citystyles")){reference(entry.getAsJsonObject(),"citystyle","citystyles",check);weight(entry.getAsJsonObject(),"factor");}
                        if(object.has("scattered")){JsonObject settings=object.getAsJsonObject("scattered");if(settings.has("list"))for(JsonElement entry:settings.getAsJsonArray("list")){reference(entry.getAsJsonObject(),"name","scattered",check);weight(entry.getAsJsonObject(),"weight");}}
                        if(object.has("parts"))partSelections(object.get("parts"),check);
                        break;
                    case "styles":
                        for(JsonElement group:object.getAsJsonArray("randompalettes"))for(JsonElement entry:group.getAsJsonArray()){reference(entry.getAsJsonObject(),"palette","palettes",check);weight(entry.getAsJsonObject(),"factor");}break;
                    case "buildings":
                        for(String section:Arrays.asList("parts","parts2"))if(object.has(section))for(JsonElement entry:object.getAsJsonArray(section))reference(entry.getAsJsonObject(),"part","parts",check);
                        reference(object,"refpalette","palettes",check);localPaletteReferences(object,check);break;
                    case "parts":reference(object,"refpalette","palettes",check);localPaletteReferences(object,check);break;
                    case "palettes":paletteReferences(object.getAsJsonArray("palette"),check);break;
                    case "multibuildings":for(JsonElement column:object.getAsJsonArray("buildings"))for(JsonElement building:column.getAsJsonArray())check.accept("buildings",building.getAsString());break;
                    case "scattered":
                        reference(object,"multibuilding","multibuildings",check);reference(object,"supportpart","parts",check);
                        if(object.has("buildings"))for(JsonElement building:object.getAsJsonArray("buildings"))check.accept("buildings",building.getAsString());break;
                    case "cities":
                        reference(object,"citystyle","citystyles",check);
                        if(object.has("buildings"))for(JsonElement building:object.getAsJsonArray("buildings")){JsonObject entry=building.getAsJsonObject();reference(entry,"building",entry.has("multi")&&entry.get("multi").getAsBoolean()?"multibuildings":"buildings",check);}break;
                    default:break;
                }
            }catch(RuntimeException error){errors.add(location+": "+error.getMessage());}
        }
        if(!errors.isEmpty())throw new IllegalArgumentException("Invalid modern asset references:\n"+String.join("\n",errors));
    }
    private static void reference(JsonObject object,String key,String category,java.util.function.BiConsumer<String,String> check){if(object.has(key)&&!object.get(key).isJsonNull())check.accept(category,object.get(key).getAsString());}
    private static void localPaletteReferences(JsonObject object,java.util.function.BiConsumer<String,String> check){
        if(!object.has("palette"))return;JsonElement palette=object.get("palette");
        if(palette.isJsonPrimitive())check.accept("palettes",palette.getAsString());else paletteReferences(palette.isJsonArray()?palette.getAsJsonArray():palette.getAsJsonObject().getAsJsonArray("palette"),check);
    }
    private static void paletteReferences(JsonArray palette,java.util.function.BiConsumer<String,String> check){for(JsonElement value:palette){JsonObject entry=value.getAsJsonObject();reference(entry,"mob","conditions",check);reference(entry,"loot","conditions",check);reference(entry,"variant","variants",check);}}
    private static void partSelections(JsonElement value,java.util.function.BiConsumer<String,String> check){if(value.isJsonNull())return;if(value.isJsonPrimitive())check.accept("parts",value.getAsString());else if(value.isJsonArray())for(JsonElement entry:value.getAsJsonArray())partSelections(entry,check);else for(Map.Entry<String,JsonElement> entry:value.getAsJsonObject().entrySet())partSelections(entry.getValue(),check);}
    private static void weight(JsonObject object,String key){if(object.has(key)){float value=object.get(key).getAsFloat();if(!Float.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid selector "+key+": "+value);}}
    private static void validateCitySettings(JsonObject object){
        String[][] probabilities={{"buildingsettings","buildingchance"},{"parkblocks","parkchance"},{"streetblocks","fountainchance","frontchance"},{"corridorblocks","corridorchance"},{"profileOverrides","openLotParkChance"}};
        for(String[] group:probabilities)if(object.has(group[0])){JsonObject values=object.getAsJsonObject(group[0]);for(int i=1;i<group.length;i++)if(values.has(group[i])){float value=values.get(group[i]).getAsFloat();if(!Float.isFinite(value)||value<0||value>1)throw new IllegalArgumentException(group[0]+"."+group[i]+" must be between 0 and 1");}}
        String[][] characters={{"parkblocks","grass","elevation"},{"streetblocks","street","streetbase","streetvariant","border","wall"},{"corridorblocks","roof","glass"},{"railblocks","railmain"}};
        for(String[] group:characters)if(object.has(group[0])){JsonObject values=object.getAsJsonObject(group[0]);for(int i=1;i<group.length;i++)if(values.has(group[i])&&values.get(group[i]).getAsString().length()!=1)throw new IllegalArgumentException(group[0]+"."+group[i]+" must be one palette character");}
        if(object.has("parkblocks")&&object.getAsJsonObject("parkblocks").has("parkstreetthreshold")){double value=object.getAsJsonObject("parkblocks").get("parkstreetthreshold").getAsDouble();if(!Double.isFinite(value)||value<0||value>8||value!=Math.floor(value))throw new IllegalArgumentException("parkstreetthreshold must be an integer between 0 and 8");}
    }
    private static JsonObject compile(String type,String name,JsonObject source){
        JsonObject object=copy(source);object.addProperty("name",name);object.addProperty("type",type);
        if(type.equals("citystyle")&&object.has("selectors")){
            JsonObject selectors=object.getAsJsonObject("selectors");
            Map<String,String> keys=new HashMap<>();keys.put("buildings","building");keys.put("multibuildings","multibuilding");keys.put("parks","park");keys.put("fountains","fountain");keys.put("stairs","stair");keys.put("fronts","front");keys.put("bridges","bridge");keys.put("raildungeons","dungeon");
            for(Map.Entry<String,String> key:keys.entrySet())if(selectors.has(key.getKey())){
                JsonArray array=new JsonArray();for(JsonElement element:selectors.getAsJsonArray(key.getKey())){JsonObject selection=copy(element.getAsJsonObject());if(selection.has("value")){selection.add(key.getValue(),selection.get("value"));selection.remove("value");}array.add(selection);}object.add(key.getKey(),array);
            }
        }
        if(type.equals("palette"))compilePalette(object.getAsJsonArray("palette"));
        if(type.equals("building")&&object.has("palette")){JsonElement local=object.get("palette");compilePalette(local.isJsonObject()?local.getAsJsonObject().getAsJsonArray("palette"):local.getAsJsonArray());}
        if(type.equals("multibuilding")){
            int width=object.get("dimx").getAsInt(),depth=object.get("dimz").getAsInt();
            JsonArray columns=object.getAsJsonArray("buildings"),rows=new JsonArray();
            if(width<1||depth<1||width>64||depth>64||columns.size()!=width)throw new IllegalArgumentException("Invalid multibuilding columns");
            for(JsonElement column:columns)if(column.getAsJsonArray().size()!=depth)throw new IllegalArgumentException("Invalid multibuilding depth");
            for(int z=0;z<depth;z++){JsonArray row=new JsonArray();for(int x=0;x<width;x++)row.add(columns.get(x).getAsJsonArray().get(z));rows.add(row);}
            object.add("buildings",rows);
        }
        if(type.equals("part")){
            int x=object.get("xsize").getAsInt(),z=object.get("zsize").getAsInt();
            if(x<1||x>16||z<1||z>16)throw new IllegalArgumentException("Part dimensions must be within a single legacy chunk");
            JsonArray slices=object.getAsJsonArray("slices");if(slices.size()>256)throw new IllegalArgumentException("Part exceeds legacy world height");
            for(JsonElement slice:slices){if(slice.getAsJsonArray().size()!=z)throw new IllegalArgumentException("Wrong row count in part");for(JsonElement row:slice.getAsJsonArray())if(row.getAsString().length()!=x)throw new IllegalArgumentException("Wrong row width in part");}
            if(object.has("refpalette"))object.add("palette",object.get("refpalette"));
            else if(object.has("palette")){
                JsonElement palette=object.get("palette");if(palette.isJsonObject())palette=palette.getAsJsonObject().get("palette");
                if(palette.isJsonArray())compilePalette(palette.getAsJsonArray());object.add("palette",palette);
            }
        }
        return object;
    }
    private static void compilePalette(JsonArray palette){
        Set<Character> seen=new HashSet<>();
        for(JsonElement element:palette){JsonObject entry=element.getAsJsonObject();String c=entry.get("char").getAsString();if(c.length()!=1||!seen.add(c.charAt(0)))throw new IllegalArgumentException("Invalid/duplicate palette character "+c);
            if(entry.has("variant")){String variant=entry.get("variant").getAsString();JsonObject definition=definition("variants",variant);if(definition==null)throw new IllegalArgumentException("Missing block variant "+variant);entry.add("blocks",copy(definition).get("blocks"));entry.remove("variant");}
            if(entry.has("torch")&&entry.get("torch").getAsBoolean()){
                JsonObject facing=new JsonObject();facing.addProperty("up",5);facing.addProperty("east",1);facing.addProperty("west",2);facing.addProperty("south",3);facing.addProperty("north",4);entry.add("facing",facing);
            }
            if(!entry.has("block")&&!entry.has("blocks")&&!entry.has("frompalette"))throw new IllegalArgumentException("Missing material for "+c);
            if(entry.has("frompalette")){String reference=entry.get("frompalette").getAsString();if(reference.length()!=1)throw new IllegalArgumentException("Palette reference must be one character: "+reference);}
            if(entry.has("blocks")){double total=0;for(JsonElement choice:entry.getAsJsonArray("blocks")){double weight=choice.getAsJsonObject().get("random").getAsDouble();if(!Double.isFinite(weight)||weight<0||weight>Integer.MAX_VALUE||weight!=Math.floor(weight))throw new IllegalArgumentException("Invalid palette weight for "+c);total+=weight;}if(total<=0)throw new IllegalArgumentException("Empty weighted palette for "+c);}
        }
    }
    private ModernAssetPack(){}
}
