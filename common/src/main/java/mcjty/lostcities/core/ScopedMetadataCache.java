package mcjty.lostcities.core;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;

/** The generator owns its metadata. Static accessors retain no world or generator. */
public final class ScopedMetadataCache<K,V> extends AbstractMap<K,V> {
    private final int capacity;
    private final Predicate<V> pinned;
    private final List<WeakReference<BoundedMetadataCache<K,V>>> live = new ArrayList<>();

    public ScopedMetadataCache(int capacity){this(capacity,value->false);}
    public ScopedMetadataCache(int capacity,Predicate<V> pinned){this.capacity=capacity;this.pinned=pinned;}

    @SuppressWarnings("unchecked")
    private BoundedMetadataCache<K,V> current(){
        Map<Object,Object> stores=GenerationScope.cacheOwner().metadataCaches();
        BoundedMetadataCache<K,V> cache=(BoundedMetadataCache<K,V>)stores.get(this);
        if(cache==null){
            cache=new BoundedMetadataCache<>(capacity,pinned);
            stores.put(this,cache);
            live.removeIf(reference->reference.get()==null);
            live.add(new WeakReference<>(cache));
        }
        return cache;
    }
    @Override public V get(Object key){return current().get(key);}
    @Override public boolean containsKey(Object key){return current().containsKey(key);}
    @Override public V put(K key,V value){return current().put(key,value);}
    @Override public V remove(Object key){return current().remove(key);}
    @Override public int size(){return current().size();}
    @Override public Set<Entry<K,V>> entrySet(){return current().entrySet();}
    public void trim(){current().trim();}
    /** Lifecycle cleanup is called on the server thread, outside a generation scope. */
    @Override public void clear(){
        Iterator<WeakReference<BoundedMetadataCache<K,V>>> entries=live.iterator();
        while(entries.hasNext()){BoundedMetadataCache<K,V> cache=entries.next().get();if(cache==null)entries.remove();else cache.clear();}
    }
}
