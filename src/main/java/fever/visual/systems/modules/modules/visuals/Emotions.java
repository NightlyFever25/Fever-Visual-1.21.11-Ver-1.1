package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.visuals.emotions.Emotion;
import fever.visual.systems.modules.modules.visuals.emotions.EmotionWheelScreen;
import fever.visual.systems.setting.settings.BindSetting;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ButtonSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
   name = "Emotions",
   category = ModuleCategory.VISUALS,
   desc = "Local player emote animations"
)
public class Emotions extends BaseModule {
   private final ModeSetting selectedEmotion = new ModeSetting(this, "Emotion");
   private final SliderSetting speed = new SliderSetting(this, "Speed").min(0.25F).max(3.0F).step(0.05F).currentValue(1.0F);
   private final BooleanSetting loop = new BooleanSetting(this, "Loop").enabled(false);
   private final BooleanSetting thirdPerson = new BooleanSetting(this, "Third Person").enable();
   private final BooleanSetting stopOnMove = new BooleanSetting(this, "Stop On Move").enable();
   private final BindSetting wheelBind = new BindSetting(this, "Wheel Bind").key(GLFW.GLFW_KEY_B);
   private final ButtonSetting play = new ButtonSetting(this, "Play").action(() -> this.playEmotion(this.getSelectedEmotion()));
   private final ButtonSetting stop = new ButtonSetting(this, "Stop").action(this::stopEmotion);
   private Emotion currentEmotion;
   private long startedAt;
   private boolean restoredPerspective;
   private Perspective previousPerspective;
   private boolean wheelWasDown;

   public Emotions() {
      for (Emotion emotion : Emotion.values()) {
         ModeSetting.Value value = new ModeSetting.Value(this.selectedEmotion, emotion.displayName());
         if (emotion == Emotion.WAVE) {
            value.select();
         }
      }
   }

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null) {
         this.stopEmotion();
         return;
      }

      this.handleWheelBind();
      if (this.currentEmotion == null) {
         this.restorePerspective();
         return;
      }

      if (this.stopOnMove.isEnabled() && (Math.abs(mc.player.forwardSpeed) > 0.01F || Math.abs(mc.player.sidewaysSpeed) > 0.01F)) {
         this.stopEmotion();
         return;
      }

      if (!this.loop.isEnabled() && this.getPlaybackTime() >= this.currentEmotion.duration()) {
         this.stopEmotion();
         return;
      }

      if (this.thirdPerson.isEnabled() && mc.options.getPerspective().isFirstPerson()) {
         if (!this.restoredPerspective) {
            this.previousPerspective = mc.options.getPerspective();
         }
         mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         this.restoredPerspective = true;
      }
   }

   @Override
   public void onDisable() {
      this.stopEmotion();
      this.restorePerspective();
   }

   public void playEmotion(Emotion emotion) {
      if (emotion == null) {
         return;
      }
      this.currentEmotion = emotion;
      this.startedAt = System.nanoTime();
   }

   public void stopEmotion() {
      this.currentEmotion = null;
      this.startedAt = 0L;
   }

   public boolean isPlaying() {
      return this.currentEmotion != null;
   }

   public Emotion getCurrentEmotion() {
      return this.currentEmotion;
   }

   public float getPlaybackTime() {
      if (this.currentEmotion == null || this.startedAt == 0L) {
         return 0.0F;
      }
      float time = (System.nanoTime() - this.startedAt) / 1_000_000_000.0F * this.speed.getCurrentValue();
      return this.loop.isEnabled() ? time % Math.max(0.1F, this.currentEmotion.duration()) : time;
   }

   public float getPlaybackWeight() {
      if (this.currentEmotion == null) {
         return 0.0F;
      }
      float time = this.getPlaybackTime();
      float fade = 0.18F;
      float in = Math.min(1.0F, time / fade);
      float out = this.loop.isEnabled() ? 1.0F : Math.min(1.0F, Math.max(0.0F, (this.currentEmotion.duration() - time) / fade));
      return Math.min(in, out);
   }

   public Emotion getSelectedEmotion() {
      return this.selectedEmotion.getValue() == null ? Emotion.WAVE : Emotion.byDisplayName(this.selectedEmotion.getValue().getName());
   }

   private void handleWheelBind() {
      boolean down = mc.currentScreen == null && this.isWheelKeyDown();
      if (down && !this.wheelWasDown) {
         mc.setScreen(new EmotionWheelScreen(this));
      }
      this.wheelWasDown = down;
   }

   public boolean isWheelKeyDown() {
      return this.wheelBind.getKey() != -1 && org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), this.wheelBind.getKey()) == GLFW.GLFW_PRESS;
   }

   private void restorePerspective() {
      if (this.restoredPerspective) {
         mc.options.setPerspective(this.previousPerspective == null ? Perspective.FIRST_PERSON : this.previousPerspective);
         this.restoredPerspective = false;
         this.previousPerspective = null;
      }
   }

   public static Emotions getModule() {
      if (FeverVisual.getInstance() == null || FeverVisual.getInstance().getModuleManager() == null) {
         return null;
      }
      return FeverVisual.getInstance().getModuleManager().getModuleSafe(Emotions.class);
   }
}
