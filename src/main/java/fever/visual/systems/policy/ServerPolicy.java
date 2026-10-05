package fever.visual.systems.policy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fever.visual.FeverVisual;
import fever.visual.systems.modules.Module;
import fever.visual.ui.menu.dropdown.DropDownScreen;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;

public final class ServerPolicy {
   private static final ServerPolicy INSTANCE = new ServerPolicy();
   private static final String CLIENT_ID = FeverVisual.MOD_ID;
   private static final long REQUEST_COOLDOWN_MS = 10_000L;
   private static final long REQUEST_TIMEOUT_MS = 10_000L;
   private static final long MAX_RETRY_MS = 60_000L;
   private static final Set<String> HOLYWORLD_HOSTS = Set.of(
         "mc.holyworld.ru",
         "mc.holyworld.me",
         "mc.holyworld.io",
         "play.holyworld.ru",
         "play.holyworld.me",
         "play.holyworld.io",
         "mc.hollyworld.ru",
         "play.hollyworld.ru",
         "fra.holyworld.ru",
         "fra.holyworld.me",
         "fconnect.holyworld.ru",
         "fconnect.holyworld.me",
         "hub.holyworld.ru",
         "hub.holyworld.me"
   );
   private static final Set<String> HOLYWORLD_ROOTS = Set.of(
         "holyworld.ru",
         "holyworld.me",
         "holyworld.io",
         "hollyworld.ru"
   );

   private final Map<String, Long> pending = new ConcurrentHashMap<>();
   private volatile Set<String> blockedFeatures = Collections.emptySet();
   private boolean started;
   private boolean holyWorld;
   private long lastRequestAt;
   private long nextRetryAt;
   private int retryAttempt;

   private ServerPolicy() {
   }

   public static ServerPolicy getInstance() {
      return INSTANCE;
   }

   public void start() {
      if (this.started) {
         return;
      }
      this.started = true;
      PayloadTypeRegistry.playC2S().register(FeatureControlPayload.ID, FeatureControlPayload.CODEC);
      PayloadTypeRegistry.playS2C().register(FeatureControlPayload.ID, FeatureControlPayload.CODEC);
      ClientPlayNetworking.registerGlobalReceiver(FeatureControlPayload.ID, (payload, context) ->
            context.client().execute(() -> this.handle(payload.json())));
      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> this.onJoin(handler));
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> this.onDisconnect());
      ClientTickEvents.END_CLIENT_TICK.register(this::tick);
   }

   public boolean canEnable(Module module) {
      return !this.isBlocked(module);
   }

   public boolean isVisible(Module module) {
      return module != null && !module.isHidden() && !this.isBlocked(module);
   }

   public boolean isBlocked(Module module) {
      return module != null && this.blockedFeatures.contains(ServerFeatureIds.id(module));
   }

   public Set<String> getBlockedFeatures() {
      return this.blockedFeatures;
   }

   public void enforceBlocked() {
      FeverVisual client = FeverVisual.getInstance();
      if (client.getModuleManager() == null || this.blockedFeatures.isEmpty()) {
         return;
      }
      for (Module module : client.getModuleManager().getAllModules()) {
         if (module.isEnabled() && this.isBlocked(module)) {
            module.setEnabled(false, true);
         }
      }
   }

   private void onJoin(ClientPlayNetworkHandler handler) {
      ServerInfo info = handler.getServerInfo();
      if (info == null) {
         info = MinecraftClient.getInstance().getCurrentServerEntry();
      }
      this.holyWorld = info != null && isHolyWorldAddress(info.address);
      this.pending.clear();
      this.retryAttempt = 0;
      this.nextRetryAt = 0L;
      this.applyBlocklist(Collections.emptySet());
      if (this.holyWorld) {
         this.sendCheck(true);
      }
   }

   private void onDisconnect() {
      this.holyWorld = false;
      this.pending.clear();
      this.retryAttempt = 0;
      this.lastRequestAt = 0L;
      this.nextRetryAt = 0L;
      this.applyBlocklist(Collections.emptySet());
   }

   private void tick(MinecraftClient client) {
      this.enforceBlocked();
      if (!this.holyWorld) {
         return;
      }

      long now = System.currentTimeMillis();
      if (this.lastRequestAt == 0L && this.pending.isEmpty()
            && FeverVisual.getInstance().getModuleManager() != null) {
         this.sendCheck(true);
         return;
      }
      boolean timedOut = this.pending.entrySet().removeIf(entry -> now - entry.getValue() >= REQUEST_TIMEOUT_MS);
      if (timedOut) {
         this.scheduleRetry(now);
      }
      if (this.pending.isEmpty() && this.nextRetryAt > 0L && now >= this.nextRetryAt) {
         this.sendCheck(false);
      }
   }

   private void sendCheck(boolean immediate) {
      if (!this.holyWorld || FeverVisual.getInstance().getModuleManager() == null || !this.pending.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      if (!immediate && now - this.lastRequestAt < REQUEST_COOLDOWN_MS) {
         this.nextRetryAt = this.lastRequestAt + REQUEST_COOLDOWN_MS;
         return;
      }

      Set<String> features = ServerFeatureIds.all(FeverVisual.getInstance().getModuleManager().getAllModules());
      if (features.isEmpty()) {
         return;
      }
      String id = UUID.randomUUID().toString();
      JsonArray featureArray = new JsonArray();
      features.forEach(featureArray::add);
      JsonObject payload = new JsonObject();
      payload.addProperty("client", CLIENT_ID);
      payload.add("features", featureArray);
      JsonObject request = new JsonObject();
      request.addProperty("id", id);
      request.addProperty("method", "checkFeatures");
      request.add("payload", payload);

      this.lastRequestAt = now;
      this.nextRetryAt = 0L;
      this.pending.put(id, now);
      try {
         ClientPlayNetworking.send(new FeatureControlPayload(request.toString()));
      } catch (RuntimeException exception) {
         this.pending.remove(id);
         FeverVisual.LOGGER.warn("Failed to send HolyWorld feature policy request", exception);
         this.scheduleRetry(now);
      }
   }

   private void handle(String raw) {
      if (!this.holyWorld) {
         return;
      }
      JsonObject response;
      try {
         JsonElement parsed = JsonParser.parseString(raw);
         if (!parsed.isJsonObject()) {
            return;
         }
         response = parsed.getAsJsonObject();
      } catch (RuntimeException exception) {
         FeverVisual.LOGGER.warn("Ignoring malformed HolyWorld feature policy response");
         this.scheduleRetry(System.currentTimeMillis());
         return;
      }

      if (!response.has("id") && response.has("event")) {
         JsonObject payload = object(response, "payload");
         if (payload != null && payload.has("blocklist")) {
            this.applyBlocklist(readBlocklist(payload));
         }
         return;
      }

      String id = string(response, "id");
      if (id == null || this.pending.remove(id) == null) {
         return;
      }
      boolean ok = response.has("ok") && response.get("ok").isJsonPrimitive() && response.get("ok").getAsBoolean();
      if (!ok) {
         FeverVisual.LOGGER.warn("HolyWorld feature policy rejected request: {}", string(response, "error"));
         this.scheduleRetry(System.currentTimeMillis());
         return;
      }

      this.retryAttempt = 0;
      this.nextRetryAt = 0L;
      this.applyBlocklist(readBlocklist(object(response, "payload")));
   }

   private void applyBlocklist(Set<String> features) {
      Set<String> previous = this.blockedFeatures;
      this.blockedFeatures = features.isEmpty() ? Collections.emptySet() : Set.copyOf(features);
      this.enforceBlocked();
      if (!this.blockedFeatures.equals(previous)) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.currentScreen instanceof DropDownScreen) {
            client.setScreen(null);
         }
      }
   }

   private void scheduleRetry(long now) {
      if (!this.holyWorld) {
         return;
      }
      this.retryAttempt = Math.min(this.retryAttempt + 1, 4);
      long backoff = Math.min(MAX_RETRY_MS, REQUEST_COOLDOWN_MS << (this.retryAttempt - 1));
      this.nextRetryAt = Math.max(this.lastRequestAt + REQUEST_COOLDOWN_MS, now + backoff);
   }

   private static Set<String> readBlocklist(JsonObject payload) {
      if (payload == null || !payload.has("blocklist") || !payload.get("blocklist").isJsonArray()) {
         return Collections.emptySet();
      }
      HashSet<String> result = new HashSet<>();
      for (JsonElement element : payload.getAsJsonArray("blocklist")) {
         if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String id = element.getAsString().trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
               result.add(id);
            }
         }
      }
      return result;
   }

   private static JsonObject object(JsonObject object, String key) {
      return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : null;
   }

   private static String string(JsonObject object, String key) {
      return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
   }

   private static boolean isHolyWorldAddress(String address) {
      if (address == null) {
         return false;
      }
      String host = address.trim().toLowerCase(Locale.ROOT);
      int colon = host.lastIndexOf(':');
      if (colon > 0 && host.indexOf(':') == colon) {
         host = host.substring(0, colon);
      }
      while (host.endsWith(".")) {
         host = host.substring(0, host.length() - 1);
      }
      if (HOLYWORLD_HOSTS.contains(host)) {
         return true;
      }
      for (String root : HOLYWORLD_ROOTS) {
         if (host.equals(root) || host.endsWith("." + root)) {
            return true;
         }
      }
      return false;
   }
}
