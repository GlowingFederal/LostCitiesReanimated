package mcjty.lostcities.materials;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Portable, canonical block-state description. Never contains a numerical registry ID. */
public final class BlockSpec {
    public final String id;
    public final int metadata;
    public final boolean explicitMetadata;
    public final Map<String, String> properties;

    public BlockSpec(String id, int metadata, Map<String, String> properties) {
        this(id,metadata,properties,metadata!=0);
    }
    public BlockSpec(String id,int metadata,Map<String,String> properties,boolean explicitMetadata) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid block identifier: " + id);
        }
        if (metadata < 0 || metadata > 15) throw new IllegalArgumentException("Metadata outside 0–15: " + metadata);
        this.id = id;
        this.metadata = metadata;
        this.explicitMetadata = explicitMetadata;
        this.properties = Collections.unmodifiableMap(new TreeMap<>(properties));
    }

    public static BlockSpec parse(String text) {
        text = text.trim();
        Map<String, String> properties = new TreeMap<>();
        int bracket = text.indexOf('[');
        if (bracket >= 0) {
            if (!text.endsWith("]")) throw new IllegalArgumentException("Unclosed block state: " + text);
            String body = text.substring(bracket + 1, text.length() - 1);
            if (!body.isEmpty()) for (String entry : body.split(",")) {
                String[] pair = entry.trim().split("=", -1);
                if (pair.length != 2 || pair[0].isEmpty() || pair[1].isEmpty()
                        || properties.put(pair[0], pair[1]) != null) {
                    throw new IllegalArgumentException("Invalid or duplicate block property: " + entry);
                }
            }
            text = text.substring(0, bracket);
        }
        int meta = 0;
        int at = text.indexOf('@');
        if (at >= 0) {
            meta = Integer.parseInt(text.substring(at + 1));
            text = text.substring(0, at);
        }
        if (text.indexOf(':') < 0) text = "minecraft:" + text;
        return new BlockSpec(text, meta, properties,at>=0);
    }

    public BlockSpec withId(String id) { return new BlockSpec(id, metadata, properties,explicitMetadata); }
    public BlockSpec withMeta(int meta) { return new BlockSpec(id, meta, properties,true); }
    public BlockSpec with(String name, String value) {
        Map<String, String> copy = new TreeMap<>(properties);
        copy.put(name, value);
        return new BlockSpec(id, metadata, copy,explicitMetadata);
    }
    public String property(String name, String fallback) { return properties.containsKey(name) ? properties.get(name) : fallback; }

    @Override public String toString() {
        StringBuilder result = new StringBuilder(id);
        if (explicitMetadata || metadata != 0) result.append('@').append(metadata);
        if (!properties.isEmpty()) {
            result.append('[');
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (result.charAt(result.length() - 1) != '[') result.append(',');
                result.append(entry.getKey()).append('=').append(entry.getValue());
            }
            result.append(']');
        }
        return result.toString();
    }
}
