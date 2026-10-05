package fever.visual.utility.render;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OverlayTextureReloadBridge {
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private OverlayTextureReloadBridge() {
    }

    public static void register(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void unregister(Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static void reloadAll() {
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }
}
