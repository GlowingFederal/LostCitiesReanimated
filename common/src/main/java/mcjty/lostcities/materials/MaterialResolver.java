package mcjty.lostcities.materials;

import java.util.*;
import java.util.function.Consumer;

/** Registry-backed resolution, shared by palette compilation and all generation adapters. */
public final class MaterialResolver<S> {
    public interface Registry<S> {
        /** Null means unavailable or an unrepresentable required state. Never return the registry's default air. */
        S find(BlockSpec spec);
        String serialize(S state);
    }

    private final Registry<S> registry;
    private final Consumer<String> diagnostic;
    private final Map<String, S> cache = new HashMap<>();
    private final Map<String, String> decisions = new TreeMap<>();
    private final Map<String, List<String>> providers;
    private final boolean uab;
    private final boolean substitutes;

    public MaterialResolver(Registry<S> registry, boolean uab, boolean substitutes,
                            Map<String, List<String>> providers, Consumer<String> diagnostic) {
        this.registry = registry;
        this.uab = uab;
        this.substitutes = substitutes;
        this.providers = providers;
        this.diagnostic = diagnostic;
    }

    public S resolve(String text) {
        BlockSpec input = BlockSpec.parse(text);
        String key = input.toString();
        S cached = cache.get(key);
        if (cached != null) return cached;
        List<BlockSpec> candidates = new ArrayList<>();
        candidates.add(input);
        if (input.id.startsWith("minecraft:")) {
            BlockSpec nativeAlias = SemanticMaterials.nativeAlias(input);
            candidates.add(nativeAlias);
            if (uab) {
                candidates.add(input.withId("etfuturum:" + input.id.substring(10)));
                candidates.addAll(SemanticMaterials.uabAliases(input));
            }
        }
        List<String> configured = providers.get(input.id);
        if (configured != null) for (String provider : configured) {
            BlockSpec target = BlockSpec.parse(provider);
            Map<String, String> properties = new TreeMap<>(input.properties);
            properties.putAll(target.properties);
            candidates.add(new BlockSpec(target.id, target.metadata, properties,target.explicitMetadata));
        }
        if (substitutes) candidates.addAll(SemanticMaterials.substitutes(input));
        S result = null;
        // Saved choices are authoritative, provided their actual state still exists.
        String previous = decisions.get(key);
        if (previous != null) result = registry.find(BlockSpec.parse(previous));
        for (BlockSpec candidate : candidates) {
            if (result != null) break;
            result = registry.find(candidate);
        }
        if (result == null) throw new IllegalArgumentException("No safe material for " + text + "; enable substitutions or configure a provider");
        String selected = registry.serialize(result);
        if (previous != null && !previous.equals(selected)) diagnostic.accept("Unavailable saved material " + previous + " for " + key + "; using " + selected);
        else if (!selected.equals(key)) diagnostic.accept("Material " + key + " -> " + selected);
        decisions.put(key, selected);
        cache.put(key, result);
        return result;
    }

    public Map<String, String> decisions() { return Collections.unmodifiableMap(decisions); }
    public String sourceFor(S state) {
        String saved=registry.serialize(state);
        for(Map.Entry<String,String> entry:decisions.entrySet())if(entry.getValue().equals(saved))return entry.getKey();
        return saved;
    }
    public void restore(Map<String, String> saved) { cache.clear(); decisions.clear(); decisions.putAll(saved); }
}
