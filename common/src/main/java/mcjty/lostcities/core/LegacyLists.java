package mcjty.lostcities.core;
import java.util.*;
public final class LegacyLists {
 @SafeVarargs public static <T> List<T> of(T... values){return Collections.unmodifiableList(Arrays.asList(values.clone()));}
 public static <T> List<T> copyOf(Collection<? extends T> values){return Collections.unmodifiableList(new ArrayList<T>(values));}
 private LegacyLists(){}
}
