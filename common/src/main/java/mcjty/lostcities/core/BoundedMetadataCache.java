package mcjty.lostcities.core;

import java.util.*;
import java.util.function.Predicate;

/** World-thread LRU. Deferred population work stays pinned until its native callback consumes it. */
public final class BoundedMetadataCache<K,V> extends LinkedHashMap<K,V> {
    private final int capacity;
    private final Predicate<V> pinned;
    public BoundedMetadataCache(int capacity){this(capacity,value->false);}
    public BoundedMetadataCache(int capacity,Predicate<V> pinned){super(256,.75F,true);if(capacity<1)throw new IllegalArgumentException("Cache capacity must be positive");this.capacity=capacity;this.pinned=pinned;}
    @Override public V put(K key,V value){V previous=super.put(key,value);trim();return previous;}
    @Override public void putAll(Map<? extends K,? extends V> values){for(Map.Entry<? extends K,? extends V> entry:values.entrySet())put(entry.getKey(),entry.getValue());}
    public void trim(){
        if(size()<=capacity)return;
        Iterator<Map.Entry<K,V>> entries=entrySet().iterator();
        while(size()>capacity&&entries.hasNext())if(!pinned.test(entries.next().getValue()))entries.remove();
    }
}
