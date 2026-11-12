package mcjty.lostcities.materials;

import java.util.*;

/** Builds native recovery states while the optional provider still exists, before Forge removes its IDs. */
public final class MaterialRecovery<S> {
    private final Map<String,List<Map.Entry<BlockSpec,BlockSpec>>> sources=new HashMap<>();
    private final MaterialResolver<S> nativeResolver;
    private final MaterialResolver.Registry<S> registry;
    public MaterialRecovery(Map<String,String> decisions,MaterialResolver<S> nativeResolver,MaterialResolver.Registry<S> registry){
        this.nativeResolver=nativeResolver;this.registry=registry;
        for(Map.Entry<String,String> entry:decisions.entrySet()){
            BlockSpec source=BlockSpec.parse(entry.getKey()),selected=BlockSpec.parse(entry.getValue());
            if(!selected.id.startsWith("minecraft:")&&source.id.startsWith("minecraft:"))sources.computeIfAbsent(selected.id,k->new ArrayList<>()).add(new AbstractMap.SimpleImmutableEntry<>(source,selected));
        }
    }
    public boolean contains(String id){return sources.containsKey(id);}
    /** Orientation can vary in a template; consolidated material variants must retain their metadata identity. */
    public String fallback(String state,Map<String,String> properties,int materialMask){
        BlockSpec actual=BlockSpec.parse(state);List<Map.Entry<BlockSpec,BlockSpec>> candidates=sources.get(actual.id);if(candidates==null)return null;
        Map.Entry<BlockSpec,BlockSpec> choice=null;
        for(Map.Entry<BlockSpec,BlockSpec> candidate:candidates)if(candidate.getValue().metadata==actual.metadata){choice=candidate;break;}
        if(choice==null)for(Map.Entry<BlockSpec,BlockSpec> candidate:candidates)if((candidate.getValue().metadata&materialMask)==(actual.metadata&materialMask)){choice=candidate;break;}
        if(choice==null)return null;
        Map<String,String> translated=new TreeMap<>(choice.getKey().properties);translated.putAll(properties);
        try{return registry.serialize(nativeResolver.resolve(new BlockSpec(choice.getKey().id,choice.getKey().metadata,translated,choice.getKey().explicitMetadata).toString()));}
        catch(IllegalArgumentException error){throw new IllegalArgumentException("Cannot preserve optional material "+state+" after provider removal: "+error.getMessage(),error);}
    }
}
