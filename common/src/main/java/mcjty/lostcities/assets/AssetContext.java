package mcjty.lostcities.assets;

/** Scoped catalog choice: an existing legacy world never silently changes to modern assets. */
public final class AssetContext {
    private static final ThreadLocal<Boolean> MODERN = new ThreadLocal<>();
    public static boolean modern(){return Boolean.TRUE.equals(MODERN.get());}
    public static boolean bind(boolean modern){boolean previous=modern();MODERN.set(modern);return previous;}
    public static void restore(boolean previous){if(previous)MODERN.set(true);else MODERN.remove();}
    private AssetContext(){}
}
