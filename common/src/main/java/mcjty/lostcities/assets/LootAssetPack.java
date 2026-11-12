package mcjty.lostcities.assets;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/** Compiles modern loot resources to native legacy JSON and preserves them with the world. */
public final class LootAssetPack {
    private static final Map<String,JsonObject> TABLES=new TreeMap<>();
    public static boolean isPath(String path){return path.startsWith("loot_tables/")||path.startsWith("loot_table/")||path.matches("data/[^/]+/loot_tables?/.*");}
    public static boolean contains(String id){return TABLES.containsKey(id);}
    public static JsonObject definition(String id){return TABLES.get(id);}
    public static void validate(BiConsumer<String,JsonObject> validator){for(Map.Entry<String,JsonObject> entry:TABLES.entrySet())validator.accept(entry.getKey(),entry.getValue());}
    public static void load(File directory){
        TABLES.clear();if(!directory.isDirectory())return;
        try(Stream<Path> paths=Files.walk(directory.toPath())){
            List<Path> files=new ArrayList<>();paths.filter(path->Files.isRegularFile(path)&&path.toString().endsWith(".json")&&isPath(directory.toPath().relativize(path).toString().replace('\\','/'))).forEach(files::add);
            files.sort(Comparator.comparing(Path::toString));
            for(Path file:files){String relative=directory.toPath().relativize(file).toString().replace('\\','/');String[] split=relative.split("/",4);String namespace,path;
                if(split[0].equals("data")){namespace=split[1];path=split[3];}else{if(split.length<3)throw new IllegalArgumentException("Expected loot_tables/<namespace>/<table>.json: "+relative);namespace=split[1];path=relative.substring(relative.indexOf('/',relative.indexOf('/')+1)+1);}
                String id=namespace+":"+path.substring(0,path.length()-5);validateId(id);
                if(TABLES.containsKey(id))throw new IllegalArgumentException("Duplicate custom loot table "+id);
                try(Reader reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){JsonObject table=new JsonParser().parse(reader).getAsJsonObject();normalize(table);TABLES.put(id,table);}
                catch(RuntimeException error){throw new IllegalArgumentException("Invalid loot table "+id+": "+error.getMessage(),error);}
            }
        }catch(IOException error){throw new UncheckedIOException(error);}
        validateCycles();
    }
    private static String vanilla(String id){return id.startsWith("minecraft:")?id.substring(10):id;}
    private static void normalize(JsonObject table){
        if(!table.has("pools"))throw new IllegalArgumentException("Missing pools");
        if(table.has("conditions"))throw new IllegalArgumentException("Conditions belong on loot pools or entries, not the table root");
        normalizeOperations(table,"functions","function");
        int poolIndex=0;
        for(JsonElement element:table.getAsJsonArray("pools")){
            JsonObject pool=element.getAsJsonObject();if(!pool.has("name"))pool.addProperty("name","lostcities_pool_"+poolIndex++);
            pool.add("rolls",number(pool.has("rolls")?pool.get("rolls"):new JsonPrimitive(1)));
            if(pool.has("bonus_rolls"))pool.add("bonus_rolls",number(pool.get("bonus_rolls")));
            if(!pool.has("entries"))throw new IllegalArgumentException("Pool has no entries");
            normalizeOperations(pool,"conditions","condition");normalizeOperations(pool,"functions","function");
            int index=0;for(JsonElement value:pool.getAsJsonArray("entries")){
                JsonObject entry=value.getAsJsonObject();normalizeEntry(entry,index++);
                appendFunctions(entry,pool);appendFunctions(entry,table);
            }
            pool.remove("functions");
        }
        table.remove("functions");
    }
    private static void appendFunctions(JsonObject entry,JsonObject source){
        if(!source.has("functions")||source.getAsJsonArray("functions").size()==0||entry.get("type").getAsString().equals("empty"))return;
        if(!entry.get("type").getAsString().equals("item"))throw new IllegalArgumentException("Pool/table functions on non-item entries require a newer loot engine");
        JsonArray functions=entry.has("functions")?entry.getAsJsonArray("functions"):new JsonArray();for(JsonElement function:source.getAsJsonArray("functions"))functions.add(new JsonParser().parse(function.toString()));entry.add("functions",functions);
    }
    private static void normalizeEntry(JsonObject entry,int index){
        String type=vanilla(entry.get("type").getAsString());entry.addProperty("type",type);
        if(!entry.has("entryName"))entry.addProperty("entryName","lostcities_entry_"+index);
        if(entry.has("weight")){double weight=entry.get("weight").getAsDouble();if(!Double.isFinite(weight)||weight<0||weight>Integer.MAX_VALUE||weight!=Math.floor(weight))throw new IllegalArgumentException("Invalid entry weight");}
        if(type.equals("item")||type.equals("loot_table"))validateId(entry.get("name").getAsString());
        if(entry.has("children")){int child=0;for(JsonElement value:entry.getAsJsonArray("children"))normalizeEntry(value.getAsJsonObject(),child++);}
        normalizeOperations(entry,"conditions","condition");normalizeOperations(entry,"functions","function");
        if(type.equals("loot_table")&&entry.has("functions")&&entry.getAsJsonArray("functions").size()>0)throw new IllegalArgumentException("Functions on referenced loot-table entries require a newer loot engine");
    }
    private static void normalizeOperations(JsonObject object,String array,String key){
        if(!object.has(array))return;
        for(JsonElement element:object.getAsJsonArray(array)){
            JsonObject operation=element.getAsJsonObject();operation.addProperty(key,vanilla(operation.get(key).getAsString()));
            if(operation.has("add")&&operation.get("add").getAsBoolean())throw new IllegalArgumentException("Additive loot functions require a newer loot engine");
            for(String number:Arrays.asList("count","levels","damage","limit"))if(operation.has(number)&&!number.equals("limit"))operation.add(number,number(operation.get(number)));
            normalizeOperations(operation,"conditions","condition");
            if(operation.has("term")){JsonArray terms=new JsonArray();terms.add(operation.get("term"));JsonObject temporary=new JsonObject();temporary.add("conditions",terms);normalizeOperations(temporary,"conditions","condition");}
            normalizeOperations(operation,"terms","condition");
        }
    }
    private static JsonElement number(JsonElement value){
        if(value.isJsonPrimitive())return value;
        JsonObject object=value.getAsJsonObject();String type=object.has("type")?vanilla(object.get("type").getAsString()):"uniform";
        if(type.equals("constant"))return number(object.get("value"));
        if(!type.equals("uniform"))throw new IllegalArgumentException("Unsupported legacy loot number provider "+type);
        JsonObject result=new JsonObject();result.add("min",number(object.get("min")));result.add("max",number(object.get("max")));return result;
    }
    public static void validateId(String id){
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid loot identifier "+id);
        String namespace=id.substring(0,id.indexOf(':')),path=id.substring(id.indexOf(':')+1);if(namespace.equals(".")||namespace.equals("..")||path.endsWith("/"))throw new IllegalArgumentException("Invalid loot identifier "+id);
        for(String piece:id.substring(id.indexOf(':')+1).split("/"))if(piece.isEmpty()||piece.equals(".")||piece.equals(".."))throw new IllegalArgumentException("Invalid loot path "+id);
    }
    public static Set<String> references(JsonObject table){Set<String> references=new TreeSet<>();collectReferences(table,references);return references;}
    private static void collectReferences(JsonElement value,Set<String> references){
        if(value.isJsonArray())for(JsonElement child:value.getAsJsonArray())collectReferences(child,references);
        else if(value.isJsonObject()){JsonObject object=value.getAsJsonObject();if(object.has("type")&&object.get("type").getAsString().equals("loot_table"))references.add(object.get("name").getAsString());for(Map.Entry<String,JsonElement> entry:object.entrySet())collectReferences(entry.getValue(),references);}
    }
    private static void validateCycles(){
        Map<String,Integer> degree=new HashMap<>();Map<String,Set<String>> dependents=new HashMap<>();
        for(Map.Entry<String,JsonObject> entry:TABLES.entrySet()){int count=0;for(String reference:references(entry.getValue()))if(TABLES.containsKey(reference)){count++;dependents.computeIfAbsent(reference,key->new TreeSet<>()).add(entry.getKey());}degree.put(entry.getKey(),count);}
        ArrayDeque<String> ready=new ArrayDeque<>();for(Map.Entry<String,Integer> entry:degree.entrySet())if(entry.getValue()==0)ready.add(entry.getKey());int resolved=0;
        while(!ready.isEmpty()){String id=ready.remove();resolved++;for(String dependent:dependents.getOrDefault(id,Collections.emptySet())){int remaining=degree.get(dependent)-1;degree.put(dependent,remaining);if(remaining==0)ready.add(dependent);}}
        if(resolved!=TABLES.size())throw new IllegalArgumentException("Cyclic custom loot tables: "+degree);
    }
    public static File file(File world,String id){validateId(id);String[] pieces=id.split(":",2);return new File(new File(world,"data/loot_tables"),pieces[0]+"/"+pieces[1]+".json");}
    public static Map<String,File> files(File world){
        Path root=new File(world,"data/loot_tables").toPath();Map<String,File> files=new TreeMap<>();if(!Files.isDirectory(root))return files;
        try(Stream<Path> paths=Files.walk(root)){paths.filter(path->Files.isRegularFile(path)&&path.toString().endsWith(".json")).forEach(path->{String relative=root.relativize(path).toString().replace('\\','/');int separator=relative.indexOf('/');if(separator<1)return;String id=relative.substring(0,separator)+":"+relative.substring(separator+1,relative.length()-5);try{validateId(id);files.put(id,path.toFile());}catch(IllegalArgumentException unrelated){/* Not a native table identifier. */}});}catch(IOException error){throw new UncheckedIOException(error);}return files;
    }
    /** Existing unowned or locally edited world tables retain precedence. Removed pack resources remain saved. */
    public static boolean materialize(File world){
        if(TABLES.isEmpty())return false;
        Path root=new File(world,"data/loot_tables").toPath().toAbsolutePath().normalize();Path manifest=new File(world,"data/lostcities_loot_tables.json").toPath();JsonObject owned=new JsonObject();boolean changed=false;
        try{
            if(Files.isRegularFile(manifest))try(Reader reader=Files.newBufferedReader(manifest,StandardCharsets.UTF_8)){owned=new JsonParser().parse(reader).getAsJsonObject();}
            for(Map.Entry<String,JsonObject> entry:TABLES.entrySet()){
                Path target=file(world,entry.getKey()).toPath().toAbsolutePath().normalize();if(!target.startsWith(root))throw new IllegalArgumentException("Loot path leaves world directory");
                String relative=root.relativize(target).toString().replace('\\','/');byte[] data=entry.getValue().toString().getBytes(StandardCharsets.UTF_8);String hash=hash(data);
                if(Files.exists(target)){
                    if(Files.isSymbolicLink(target)||!owned.has(relative))continue;
                    String existing=hash(Files.readAllBytes(target));if(!existing.equals(owned.get(relative).getAsString()))continue;if(existing.equals(hash))continue;
                }
                write(target,data);owned.addProperty(relative,hash);changed=true;
            }
            if(changed)write(manifest,owned.toString().getBytes(StandardCharsets.UTF_8));
            return changed;
        }catch(IOException error){throw new UncheckedIOException(error);}
    }
    private static String hash(byte[] bytes){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder text=new StringBuilder();for(byte value:digest)text.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return text.toString();}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static void write(Path file,byte[] data)throws IOException{
        Files.createDirectories(file.toAbsolutePath().getParent());Path temporary=Files.createTempFile(file.toAbsolutePath().getParent(),"lostcities-loot-",".tmp");
        try{Files.write(temporary,data);try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temporary);}
    }
    private LootAssetPack(){}
}
