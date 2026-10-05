package fever.visual.systems.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fever.visual.systems.modules.modules.combat.*;
import fever.visual.systems.modules.modules.other.*;
import fever.visual.systems.modules.modules.player.*;
import fever.visual.systems.modules.modules.visuals.*;
import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.event.impl.window.KeyPressEvent;
import fever.visual.systems.event.impl.window.MouseEvent;
import fever.visual.systems.modules.exception.UnknownModuleException;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.policy.ServerPolicy;
import net.minecraft.client.MinecraftClient;

public class ModuleManager {
   private final List<Module> modules = new ArrayList<>();
   private final Map<Class<? extends Module>, Module> modulesByClass = new HashMap<>();
   private final Map<String, Module> modulesByName = new HashMap<>();
   private final EventListener<ClientPlayerTickEvent> tickListener;
   private final EventListener<HudRenderEvent> moduleWidgetRenderer;
   private final EventListener<KeyPressEvent> onKeyPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            if (module.getKey() == event.getKey() && module.getKey() != -1 && event.getAction() == 1) {
               module.toggle();
            }
         }
      }
   };
   private final EventListener<MouseEvent> onMouseButtonPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            if (module.getKey() == event.getButton() && module.getKey() != -1 && event.getAction() == 1) {
               module.toggle();
            }
         }
      }
   };

   public ModuleManager(EventListener<ClientPlayerTickEvent> tickListener, EventListener<HudRenderEvent> moduleWidgetRenderer) {
      this.tickListener = tickListener;
      this.moduleWidgetRenderer = moduleWidgetRenderer;
      FeverVisual.getInstance().getEventManager().subscribe(this);
   }

   public void registerModules() {
      this.register(new AutoSprint());
      this.register(new MenuModule());
      this.register(new NoRender());
      this.register(new Optimization());
      this.register(new Ambience());
      this.register(new FullBright());
      this.register(new Interface());
      this.register(new CustomButtons());
      this.register(new CustomSwords());
      this.register(new SelfTag());
      this.register(new Crosshair());
      this.register(new Emotions());
      this.register(new ExplosionWave());
      this.register(new ModelCollapse());
      this.register(new GlassVapor());
      this.register(new GlowEsp());
      this.register(new NameTags());
      this.register(new CustomChat());
      this.register(new CustomInv());
      this.register(new SwingAnimation());
      this.register(new BetterTab());
      this.register(new FriendMarkers());
      this.register(new TNTTimer());
      this.register(new ViewModel());
      this.register(new TargetESP());
      this.register(new CustomFog());
      this.register(new World());
      this.register(new KillEffects());
      this.register(new Prediction());
      this.register(new MineHelper());
      this.register(new MCF());
      this.register(new ItemScroller());
      this.register(new ItemPickup());
      this.register(new NameProtect());
      this.register(new SafeMode());
      this.register(new AutoAccept());
      this.register(new DeathCords());
      this.register(new AutoDuels());
      this.register(new AutoAuth());
      this.register(new AutoJoin());
      this.register(new HoldMyItems());
      this.register(new FemaleGender());
      this.register(new ServerHelper());
      this.register(new Sounds());
      this.register(new LittleSnickers());
      this.register(new HitBubbles());
      this.register(new ClientName());
      this.register(new AutoRespawn());
      this.register(new Freelook());
      this.register(new Spheres());
      this.register(new JumpEffect());
      this.register(new HitEffect());
      this.register(new HitColor());
      this.register(new CustomHitbox());
      this.register(new BlockOverlay());
      this.register(new Zoom());
      this.register(new AspectRatio());
      this.register(new ChinaHatModule());
      this.register(new HitParticles());
      this.register(new ModelChanger());
      this.register(new ItemHighlighter());
      this.register(new BadTrip());
      this.register(new MoveParticles());
      this.register(new Trails());
      this.register(new JumpCircle());
      this.register(new Cape());
      this.register(new TotemAngle());
      this.register(new Wings());
      this.register(new SkyShader());
      this.register(new GlassHands());
      this.register(new HandShader());
      this.register(new WorldParticles());
      this.register(new ArmorDurability());
      this.register(new Nimb());
      this.register(new CustomModels());
      this.register(new HitSound());
      this.register(new KillSound());
      this.register(new Saturation());
      this.register(new ItemPhysics());
      this.register(new Taksa());
   }
   public void enableModules() {
      for (Module module : this.modules) {
         if (module.getInfo().enabledByDefault()) {
            module.enable();
         }
      }

      FeverVisual.LOGGER.info("Enabled default modules");
   }

   public void register(BaseModule module) {
      this.modules.add(module);
      this.modulesByClass.put(module.getClass(), module);
      this.modulesByName.put(module.getName().toLowerCase(Locale.ROOT), module);
      this.modulesByName.put(normalizeModuleName(module.getName()), module);
   }

   public <T extends Module> T getModule(String name) {
      if (name == null || name.isBlank()) {
         throw new UnknownModuleException(String.valueOf(name));
      }

      Module module = this.modulesByName.get(name.toLowerCase(Locale.ROOT));
      if (module == null) {
         module = this.modulesByName.get(normalizeModuleName(name));
      }
      if (module != null && ServerPolicy.getInstance().isVisible(module)) {
         return (T)module;
      }

      for (Module candidate : this.modules) {
         if (ServerPolicy.getInstance().isVisible(candidate)
               && (candidate.getName().equalsIgnoreCase(name) || normalizeModuleName(candidate.getName()).equals(normalizeModuleName(name)))) {
            return (T)candidate;
         }
      }
      throw new UnknownModuleException(name);
   }

   private static String normalizeModuleName(String name) {
      return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-]", "");
   }

   public <T extends Module> T getModule(Class<T> clazz) {
      Module module = this.modulesByClass.get(clazz);
      if (module != null) {
         return (T)module;
      }
      throw new UnknownModuleException(clazz.getSimpleName());
   }

   public <T extends Module> T getModuleSafe(Class<T> clazz) {
      return (T)this.modulesByClass.get(clazz);
   }

   public void disableAllModules() {
      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.disable();
         }
      }
   }

   @Generated
   public List<Module> getModules() {
      return this.modules.stream().filter(ServerPolicy.getInstance()::isVisible).toList();
   }

   public List<Module> getAllModules() {
      return List.copyOf(this.modules);
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getTickListener() {
      return this.tickListener;
   }

   @Generated
   public EventListener<HudRenderEvent> getModuleWidgetRenderer() {
      return this.moduleWidgetRenderer;
   }

   @Generated
   public EventListener<KeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<MouseEvent> getOnMouseButtonPress() {
      return this.onMouseButtonPress;
   }
}
