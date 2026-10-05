package fever.visual.systems.policy;

import fever.visual.systems.modules.Module;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Stable feature identifiers sent to server policy providers. */
public final class ServerFeatureIds {
   private ServerFeatureIds() {
   }

   public static String id(Module module) {
      return module.getClass().getSimpleName().toLowerCase(Locale.ROOT);
   }

   public static Set<String> all(Collection<? extends Module> modules) {
      LinkedHashSet<String> ids = new LinkedHashSet<>();
      for (Module module : modules) {
         ids.add(id(module));
      }
      return Set.copyOf(ids);
   }
}
