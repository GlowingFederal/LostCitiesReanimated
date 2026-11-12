package mcjty.lostcities.core;

/** Context ownership is explicit even when two dimensions use the same asset mode. */
public final class GenerationScope {
    public interface CacheOwner { java.util.Map<Object,Object> metadataCaches(); }
    private static final ThreadLocal<Object> CURRENT=new ThreadLocal<>();
    public static CacheOwner cacheOwner(){Object owner=CURRENT.get();if(!(owner instanceof CacheOwner))throw new IllegalStateException("World metadata requires a generation owner");return (CacheOwner)owner;}
    public static boolean owns(Object owner){return CURRENT.get()==owner;}
    public static Object bind(Object owner){Object previous=CURRENT.get();CURRENT.set(owner);return previous;}
    public static void restore(Object previous){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    private GenerationScope(){}
}
