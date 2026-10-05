package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ButtonSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.systems.setting.settings.StringSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(
   name = "Crosshair",
   category = ModuleCategory.VISUALS,
   desc = "Custom crosshair renderer"
)
public class Crosshair extends BaseModule {
   public static final int GRID_SIZE = 15;

   private final ModeSetting mode = new ModeSetting(this, "Mode");
   private final ModeSetting.Value crossMode = new ModeSetting.Value(this.mode, "Cross").select();
   private final ModeSetting.Value circleMode = new ModeSetting.Value(this.mode, "Circle");
   private final ModeSetting.Value customMode = new ModeSetting.Value(this.mode, "Custom");
   private final SliderSetting attackOffset = new SliderSetting(this, "Attack Offset").min(0.0F).max(12.0F).step(0.5F).currentValue(4.0F);
   private final SliderSetting indent = new SliderSetting(this, "Indent", () -> !this.crossMode.isSelected()).min(0.0F).max(10.0F).step(0.5F).currentValue(3.0F);
   private final SliderSetting length = new SliderSetting(this, "Length", () -> !this.crossMode.isSelected()).min(2.0F).max(20.0F).step(0.5F).currentValue(6.0F);
   private final SliderSetting thickness = new SliderSetting(this, "Thickness", () -> this.circleMode.isSelected()).min(1.0F).max(5.0F).step(0.5F).currentValue(1.0F);
   private final SliderSetting customScale = new SliderSetting(this, "Custom Scale", () -> !this.customMode.isSelected()).min(0.5F).max(3.0F).step(0.5F).currentValue(1.0F);
   private final BooleanSetting drawDot = new BooleanSetting(this, "Draw Dot", () -> this.circleMode.isSelected()).enable();
   private final BooleanSetting inertia = new BooleanSetting(this, "Inertia").enable();
   private final ButtonSetting editor = new ButtonSetting(this, "Editor", () -> !this.customMode.isSelected()).action(() -> mc.setScreen(new CrosshairEditorScreen(this, mc.currentScreen)));
   private final ModeSetting customColorMode = new ModeSetting(this, "Custom Color", () -> !this.customMode.isSelected());
   private final ModeSetting.Value customWhite = new ModeSetting.Value(this.customColorMode, "White").select();
   private final ModeSetting.Value customAccent = new ModeSetting.Value(this.customColorMode, "Accent");
   private final ModeSetting.Value customRainbow = new ModeSetting.Value(this.customColorMode, "Rainbow");
   private final BooleanSetting customOutline = new BooleanSetting(this, "Custom Outline", () -> !this.customMode.isSelected()).enable();
   private final BooleanSetting customTargetReact = new BooleanSetting(this, "Custom Target React", () -> !this.customMode.isSelected()).enable();
   private final StringSetting customPixels = new StringSetting(this, "Pixels", () -> true).text(defaultGrid());
   private float lastPitch;
   private float lastYaw;
   private float offsetX;
   private float offsetY;
   private float customRed = 1.0F;

   private final EventListener<HudRenderEvent> onHudRender = event -> {
      if (mc.player == null || mc.world == null || !mc.options.getPerspective().isFirstPerson() || mc.options.hudHidden) {
         return;
      }

      CustomDrawContext context = event.getContext();
      this.updateInertia(event.getTickDelta());
      float centerX = context.getScaledWindowWidth() / 2.0F + (this.circleMode.isSelected() ? this.offsetX : 0.0F);
      float centerY = context.getScaledWindowHeight() / 2.0F + (this.circleMode.isSelected() ? this.offsetY : 0.0F);

      ColorRGBA color = this.getColor();
      float attack = this.getAttackOffset();
      if (this.crossMode.isSelected()) {
         this.renderCross(context, centerX, centerY, attack, color);
      } else if (this.circleMode.isSelected()) {
         this.renderCircle(context, centerX, centerY, attack, color);
      } else {
         this.renderCustom(context, centerX, centerY, attack, color);
      }
   };

   private void updateInertia(float tickDelta) {
      if (!this.circleMode.isSelected() || !this.inertia.isEnabled() || mc.player == null) {
         this.offsetX = 0.0F;
         this.offsetY = 0.0F;
         if (mc.player != null) {
            this.lastYaw = MathHelper.lerp(tickDelta, mc.player.lastYaw, mc.player.getYaw());
            this.lastPitch = MathHelper.lerp(tickDelta, mc.player.lastPitch, mc.player.getPitch());
         }
         return;
      }
      float yaw = MathHelper.lerp(tickDelta, mc.player.lastYaw, mc.player.getYaw());
      float pitch = MathHelper.lerp(tickDelta, mc.player.lastPitch, mc.player.getPitch());
      this.offsetX = MathHelper.clamp((this.lastYaw - yaw) * 0.55F, -8.0F, 8.0F);
      this.offsetY = MathHelper.clamp((this.lastPitch - pitch) * 0.55F, -8.0F, 8.0F);
      this.lastYaw = yaw;
      this.lastPitch = pitch;
   }

   private void renderCross(CustomDrawContext context, float x, float y, float attack, ColorRGBA color) {
      float gap = this.indent.getCurrentValue() + attack;
      float len = this.length.getCurrentValue();
      float thick = this.thickness.getCurrentValue();
      this.rect(context, x - thick / 2.0F, y - gap - len, thick, len, color);
      this.rect(context, x - thick / 2.0F, y + gap, thick, len, color);
      this.rect(context, x - gap - len, y - thick / 2.0F, len, thick, color);
      this.rect(context, x + gap, y - thick / 2.0F, len, thick, color);
      if (this.drawDot.isEnabled()) {
         this.rect(context, x - thick / 2.0F, y - thick / 2.0F, thick, thick, color);
      }
   }

   private void renderCircle(CustomDrawContext context, float x, float y, float attack, ColorRGBA color) {
      float radius = 5.0F + attack;
      int segments = 192;
      float[] xs = new float[segments + 1];
      float[] ys = new float[segments + 1];
      int[] colors = new int[segments + 1];
      for (int i = 0; i <= segments; i++) {
         double angle = Math.PI * 2.0 * i / segments;
         xs[i] = x + (float)Math.cos(angle) * radius;
         ys[i] = y + (float)Math.sin(angle) * radius;
         colors[i] = Colors.getAccentColor(i * 360.0F / segments).withAlpha(color.getAlpha()).getRGB();
      }
      context.drawLineStrip(xs, ys, colors, segments + 1);
      if (this.drawDot.isEnabled()) {
         context.drawRect(x - 0.75F, y - 0.75F, 1.5F, 1.5F, color);
      }
   }

   private void renderCustom(CustomDrawContext context, float x, float y, float attack, ColorRGBA color) {
      float target = this.customTargetReact.isEnabled() && this.isTargetingEntity() ? 5.0F : 1.0F;
      this.customRed += (target - this.customRed) * 0.18F;
      this.renderGridStyled(context, this.customGrid(), x, y, this.customScale.getCurrentValue(), this.customRed);
   }

   public void renderGridStyled(CustomDrawContext context, boolean[] grid, float x, float y, float scale, float redFactor) {
      float px = Math.max(0.5F, Math.round(scale * 2.0F) / 2.0F);
      float startX = snap(x - GRID_SIZE * px * 0.5F);
      float startY = snap(y - GRID_SIZE * px * 0.5F);
      if (this.customOutline.isEnabled()) {
         float outline = Math.min(0.5F, px * 0.5F);
         for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
               int index = row * GRID_SIZE + col;
               if (index < grid.length && grid[index]) {
                  float cellX = snap(startX + col * px);
                  float cellY = snap(startY + row * px);
                  ColorRGBA outlineColor = new ColorRGBA(0, 0, 0, 200);
                  if (!isGridFilled(grid, row - 1, col)) {
                     context.drawRect(cellX, snap(cellY - outline), snapSize(px), snapSize(outline), outlineColor);
                  }
                  if (!isGridFilled(grid, row + 1, col)) {
                     context.drawRect(cellX, snap(cellY + px), snapSize(px), snapSize(outline), outlineColor);
                  }
                  if (!isGridFilled(grid, row, col - 1)) {
                     context.drawRect(snap(cellX - outline), cellY, snapSize(outline), snapSize(px), outlineColor);
                  }
                  if (!isGridFilled(grid, row, col + 1)) {
                     context.drawRect(snap(cellX + px), cellY, snapSize(outline), snapSize(px), outlineColor);
                  }
               }
            }
         }
      }
      for (int row = 0; row < GRID_SIZE; row++) {
         for (int col = 0; col < GRID_SIZE; col++) {
            int index = row * GRID_SIZE + col;
            if (index < grid.length && grid[index]) {
               context.drawRect(snap(startX + col * px), snap(startY + row * px), snapSize(px), snapSize(px), this.getCustomPixelColor(col, redFactor));
            }
         }
      }
   }

   private static float snap(float value) {
      return Math.round(value * 2.0F) / 2.0F;
   }

   private static float snapSize(float value) {
      return Math.max(0.5F, Math.round(value * 2.0F) / 2.0F);
   }

   private static boolean isGridFilled(boolean[] grid, int row, int col) {
      return row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE && grid[row * GRID_SIZE + col];
   }

   private void rect(CustomDrawContext context, float x, float y, float w, float h, ColorRGBA color) {
      context.drawRect(x, y, w, h, color);
   }

   private float getAttackOffset() {
      float progress = mc.player == null ? 1.0F : mc.player.getAttackCooldownProgress(0.0F);
      return (1.0F - progress) * this.attackOffset.getCurrentValue();
   }

   private ColorRGBA getColor() {
      boolean target = this.isTargetingEntity();
      return target ? new ColorRGBA(255, 80, 80, 255) : Colors.getAccentColor(0.0F);
   }

   private boolean isTargetingEntity() {
      HitResult hitResult = mc.crosshairTarget;
      if (hitResult instanceof EntityHitResult entityHitResult) {
         Entity entity = entityHitResult.getEntity();
         return entity != null && entity.isAlive();
      }
      return false;
   }

   private ColorRGBA getCustomPixelColor(int col, float redFactor) {
      ColorRGBA base;
      if (this.customAccent.isSelected()) {
         base = Colors.getAccentColor(col * 18.0F);
      } else if (this.customRainbow.isSelected()) {
         float hue = (System.currentTimeMillis() % 4000L) / 4000.0F + col * 0.03F;
         base = ColorRGBA.fromInt(0xFF000000 | (Color.HSBtoRGB(hue - (float)Math.floor(hue), 0.65F, 1.0F) & 0xFFFFFF));
      } else {
         base = ColorRGBA.WHITE;
      }
      float t = MathHelper.clamp((redFactor - 1.0F) / 4.0F, 0.0F, 1.0F);
      return t <= 0.001F ? base : base.mix(new ColorRGBA(245, 5, 34, 255), t);
   }

   public boolean[] customGrid() {
      String pixels = this.customPixels.getText();
      if (pixels == null || pixels.length() != GRID_SIZE * GRID_SIZE) {
         pixels = defaultGrid();
      }
      boolean[] grid = new boolean[GRID_SIZE * GRID_SIZE];
      for (int i = 0; i < grid.length; i++) {
         grid[i] = pixels.charAt(i) == '1';
      }
      return grid;
   }

   public void setCustomGrid(boolean[] grid) {
      StringBuilder builder = new StringBuilder(GRID_SIZE * GRID_SIZE);
      for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
         builder.append(i < grid.length && grid[i] ? '1' : '0');
      }
      this.customPixels.text(builder.toString());
   }

   public static String defaultGrid() {
      StringBuilder builder = new StringBuilder(GRID_SIZE * GRID_SIZE);
      int c = GRID_SIZE / 2;
      for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
         int row = i / GRID_SIZE;
         int col = i % GRID_SIZE;
         boolean vertical = col == c && ((row >= c - 5 && row <= c - 2) || (row >= c + 2 && row <= c + 5));
         boolean horizontal = row == c && ((col >= c - 5 && col <= c - 2) || (col >= c + 2 && col <= c + 5));
         boolean dot = row == c && col == c;
         builder.append(vertical || horizontal || dot ? '1' : '0');
      }
      return builder.toString();
   }

   public static boolean shouldReplaceVanilla() {
      if (FeverVisual.getInstance() == null || FeverVisual.getInstance().getModuleManager() == null) {
         return false;
      }
      Crosshair module = FeverVisual.getInstance().getModuleManager().getModuleSafe(Crosshair.class);
      return module != null && module.isEnabled();
   }
}
