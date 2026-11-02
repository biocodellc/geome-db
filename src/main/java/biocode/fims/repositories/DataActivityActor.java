package biocode.fims.repositories;

import java.util.function.Supplier;

/** Explicit attribution for user-initiated work scheduled on pooled threads. */
public final class DataActivityActor {
    private static final ThreadLocal<String> ACTOR = new ThreadLocal<>();
    private DataActivityActor() {}
    public static String current() { return ACTOR.get(); }
    public static <T> T as(String username, Supplier<T> action) {
        String previous = ACTOR.get();
        try {
            if (username == null) ACTOR.remove(); else ACTOR.set(username);
            return action.get();
        } finally {
            if (previous == null) ACTOR.remove(); else ACTOR.set(previous);
        }
    }
}
