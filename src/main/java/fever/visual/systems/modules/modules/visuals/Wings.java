package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.compat.BufferRenderer;
import fever.visual.utility.render.TestikRender3D;
import fever.visual.utility.render.Utils;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleInfo(name = "Wings", category = ModuleCategory.VISUALS, desc = "modules.descriptions.wings")
public class Wings extends BaseModule implements IMinecraft {
   private static final float DEFAULT_SPREAD = 8.0F;
   private static final int[] RIB_INDICES = new int[] {2, 4, 7, 9, 11};
   private static final WingPoint[] SHAPE = new WingPoint[] {
           new WingPoint(0.08F, 0.10F, 0.88F),
           new WingPoint(0.28F, 0.34F, 0.78F),
           new WingPoint(0.56F, 0.82F, 0.62F),
           new WingPoint(0.86F, 0.30F, 0.52F),
           new WingPoint(1.14F, 0.46F, 0.40F),
           new WingPoint(1.24F, 0.04F, 0.30F),
           new WingPoint(1.02F, -0.18F, 0.28F),
           new WingPoint(1.18F, -0.64F, 0.22F),
           new WingPoint(0.86F, -0.46F, 0.20F),
           new WingPoint(0.80F, -0.98F, 0.14F),
           new WingPoint(0.54F, -0.74F, 0.16F),
           new WingPoint(0.30F, -1.16F, 0.12F),
           new WingPoint(0.10F, -0.54F, 0.18F)
   };

   private final BooleanSetting self = new BooleanSetting(this, "modules.settings.wings.self", "modules.settings.wings.self.description").enabled(true);
   private final BooleanSetting friends = new BooleanSetting(this, "modules.settings.wings.friends", "modules.settings.wings.friends.description").enabled(false);
   private final SliderSetting size = new SliderSetting(this, "modules.settings.wings.size", "modules.settings.wings.size.description")
           .min(0.65F).max(1.35F).step(0.05F).currentValue(1.0F);
   private final SliderSetting transparency = new SliderSetting(this, "modules.settings.wings.alpha", "modules.settings.wings.alpha.description")
           .min(0.25F).max(1.0F).step(0.05F).currentValue(0.86F);
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.wings.color_mode");
   private final ModeSetting.Value colorTheme = new ModeSetting.Value(this.colorMode, "modules.settings.wings.color_mode.theme").select();
   private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.wings.color_mode.custom");
   private final ColorSetting colorFirst = new ColorSetting(this, "modules.settings.wings.color", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(160, 115, 255, 255));
   private final ColorSetting colorSecond = new ColorSetting(this, "modules.settings.wings.color_second", () -> !this.colorCustom.isSelected())
           .color(new ColorRGBA(100, 160, 255, 255));

   private float selfBodyYaw;
   private boolean selfBodyYawInitialized;

   private final EventListener<Render3DEvent> onRender3D = new EventListener<>() {
      @Override
      public void onEvent(Render3DEvent event) {
         if (mc.world == null || mc.player == null) {
            return;
         }

         MatrixStack matrices = event.getMatrices();
         Vec3d cameraPos = event.getCamera().getCameraPos();
         float tickDelta = event.getTickDelta();
         TestikRender3D.setup(false);
         try {
            if (self.isEnabled()
                    && !mc.options.getPerspective().isFirstPerson()
                    && mc.player.isAlive()
                    && !hasElytra(mc.player)) {
               renderWings(matrices, mc.player, tickDelta, cameraPos);
            }

            if (friends.isEnabled()) {
               for (PlayerEntity player : mc.world.getPlayers()) {
                  if (player != mc.player && player.isAlive() && isVisibleFriend(player) && !hasElytra(player)) {
                     Vec3d pos = player.getLerpedPos(tickDelta);
                     if (pos.squaredDistanceTo(cameraPos) <= 4096.0
                             && Utils.isInViewHemisphere(event.getCamera(), pos.x, pos.y + 1.0, pos.z, 3.0)) {
                        renderWings(matrices, player, tickDelta, cameraPos);
                     }
                  }
               }
            }
         } finally {
            TestikRender3D.reset();
         }
      }

      @Override
      public int getPriority() {
         return -100;
      }
   };

   @Override
   public void onDisable() {
      this.selfBodyYawInitialized = false;
   }

   private void renderWings(MatrixStack matrices, PlayerEntity player, float tickDelta, Vec3d cameraPos) {
      WingPose pose = resolvePose(player, tickDelta);
      if (pose == null) {
         return;
      }

      Vec3d origin = player.getLerpedPos(tickDelta).subtract(cameraPos);
      float bodyYaw = resolveBodyYaw(player, tickDelta);
      float move = MathHelper.clamp(player.limbAnimator.getAmplitude(tickDelta), 0.0F, 1.0F);
      float flap = MathHelper.sin((player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
      float open = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
      float wingScale = this.size.getCurrentValue() * pose.scaleMultiplier * (player.isBaby() ? 0.55F : 1.0F);

      ColorRGBA base = this.getWingColor(0);
      ColorRGBA edge = this.getWingColor(20);
      ColorRGBA glow = edge.mix(ColorRGBA.WHITE, 0.28F);
      ColorRGBA core = base.mix(edge, 0.5F).mix(ColorRGBA.WHITE, 0.45F);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      BufferBuilder triangles = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      renderWingLayers(triangles, matrix, origin, bodyYaw, -1.0F, open, wingScale, pose, glow, core, base);
      renderWingLayers(triangles, matrix, origin, bodyYaw, 1.0F, open, wingScale, pose, glow, core, base);
      BufferRenderer.drawWithGlobalProgram(triangles.end());

      BufferBuilder lines = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);
      renderWingLines(lines, matrix, origin, bodyYaw, -1.0F, open, wingScale, pose, glow, base);
      renderWingLines(lines, matrix, origin, bodyYaw, 1.0F, open, wingScale, pose, glow, base);
      BufferRenderer.drawWithGlobalProgram(lines.end());
   }

   private ColorRGBA getWingColor(float index) {
      if (this.colorCustom.isSelected()) {
         return this.colorFirst.getColorSafe().mix(this.colorSecond.getColorSafe(), (index % 40.0F) / 39.0F);
      }

      return Colors.getAccentColor(index * 6.0F);
   }

   private void renderWingLayers(BufferBuilder buffer, Matrix4f matrix, Vec3d origin, float bodyYaw, float side, float open, float wingScale, WingPose pose, ColorRGBA glow, ColorRGBA core, ColorRGBA base) {
      drawWingLayer(buffer, matrix, origin, bodyYaw, side, open, wingScale, pose, 1.22F, withAlpha(glow, 0.24F), withAlpha(glow, 0.08F));
      drawWingLayer(buffer, matrix, origin, bodyYaw, side, open, wingScale, pose, 0.84F, withAlpha(core, 0.28F), withAlpha(core, 0.10F));
      drawWingLayer(buffer, matrix, origin, bodyYaw, side, open, wingScale, pose, 1.0F, withAlpha(base, 0.76F), withAlpha(base, 0.16F));
   }

   private void renderWingLines(BufferBuilder buffer, Matrix4f matrix, Vec3d origin, float bodyYaw, float side, float open, float wingScale, WingPose pose, ColorRGBA glow, ColorRGBA base) {
      drawWingOutline(buffer, matrix, origin, bodyYaw, side, open, wingScale, pose, withAlpha(base, 0.72F));
      drawWingRibs(buffer, matrix, origin, bodyYaw, side, open, wingScale, pose, withAlpha(glow, 0.42F));
   }

   private void drawWingLayer(BufferBuilder buffer, Matrix4f matrix, Vec3d origin, float bodyYaw, float side, float open, float wingScale, WingPose pose, float layerScale, ColorRGBA rootColor, ColorRGBA edgeColor) {
      Vec3d root = wingPoint(origin, bodyYaw, side, open, wingScale, pose, 0.0F, 0.0F, layerScale, 0.0F);

      for (int i = 0; i < SHAPE.length; i++) {
         WingPoint current = SHAPE[i];
         WingPoint next = SHAPE[(i + 1) % SHAPE.length];
         Vec3d currentPoint = wingPoint(origin, bodyYaw, side, open, wingScale, pose, current.x, current.y, layerScale, 0.012F * (1.22F - layerScale));
         Vec3d nextPoint = wingPoint(origin, bodyYaw, side, open, wingScale, pose, next.x, next.y, layerScale, 0.012F * (1.22F - layerScale));
         ColorRGBA currentColor = applyPointAlpha(edgeColor, current.alphaMul);
         ColorRGBA nextColor = applyPointAlpha(edgeColor, next.alphaMul);
         if (side > 0.0F) {
            TestikRender3D.drawTriangle(buffer, matrix, root, nextPoint, currentPoint, rootColor, nextColor, currentColor);
         } else {
            TestikRender3D.drawTriangle(buffer, matrix, root, currentPoint, nextPoint, rootColor, currentColor, nextColor);
         }
      }

   }

   private void drawWingOutline(BufferBuilder buffer, Matrix4f matrix, Vec3d origin, float bodyYaw, float side, float open, float wingScale, WingPose pose, ColorRGBA outlineColor) {
      Vec3d previous = wingPoint(origin, bodyYaw, side, open, wingScale, pose, SHAPE[SHAPE.length - 1].x, SHAPE[SHAPE.length - 1].y, 1.0F, 0.018F);
      for (WingPoint point : SHAPE) {
         Vec3d current = wingPoint(origin, bodyYaw, side, open, wingScale, pose, point.x, point.y, 1.0F, 0.018F);
         TestikRender3D.drawGradientLine(buffer, matrix, previous, current, outlineColor, applyPointAlpha(outlineColor, point.alphaMul));
         previous = current;
      }
   }

   private void drawWingRibs(BufferBuilder buffer, Matrix4f matrix, Vec3d origin, float bodyYaw, float side, float open, float wingScale, WingPose pose, ColorRGBA ribColor) {
      Vec3d root = wingPoint(origin, bodyYaw, side, open, wingScale, pose, 0.0F, 0.0F, 0.96F, 0.02F);
      for (int index : RIB_INDICES) {
         WingPoint point = SHAPE[index];
         Vec3d end = wingPoint(origin, bodyYaw, side, open, wingScale, pose, point.x, point.y, 0.96F, 0.02F);
         TestikRender3D.drawGradientLine(buffer, matrix, root, end, withAlpha(ribColor, 0.18F), applyPointAlpha(ribColor, point.alphaMul));
      }
   }

   private Vec3d wingPoint(Vec3d origin, float bodyYaw, float side, float open, float wingScale, WingPose pose, float x, float y, float layerScale, float layerBackOffset) {
      float roll = (float)Math.toRadians(side * pose.sideRoll);
      float pitch = (float)Math.toRadians(pose.sidePitch);
      float openRad = (float)Math.toRadians(open);
      float scaledX = x * layerScale;
      float scaledY = y * layerScale;
      float rolledX = scaledX * MathHelper.cos(roll) - scaledY * MathHelper.sin(roll) * 0.22F;
      float rolledY = scaledX * MathHelper.sin(roll) * 0.34F + scaledY * MathHelper.cos(roll);
      float localSide = side * (pose.sideOffset + rolledX * MathHelper.cos(openRad));
      float localUp = pose.anchorY + pose.sideYOffset + rolledY * MathHelper.cos(pitch);
      float localBack = pose.anchorBack + pose.sideBackOffset
              + Math.abs(rolledX) * MathHelper.sin(openRad) * 0.35F
              - rolledY * MathHelper.sin(pitch) * 0.12F
              + layerBackOffset;
      float posePitch = (float)Math.toRadians(pose.pitchRotation);
      float poseRoll = (float)Math.toRadians(pose.rollRotation);
      float pitchedUp = localUp * MathHelper.cos(posePitch) - localBack * MathHelper.sin(posePitch);
      float pitchedBack = localUp * MathHelper.sin(posePitch) + localBack * MathHelper.cos(posePitch);
      float rolledSide = localSide * MathHelper.cos(poseRoll) - pitchedUp * MathHelper.sin(poseRoll);
      float rolledUp = localSide * MathHelper.sin(poseRoll) + pitchedUp * MathHelper.cos(poseRoll);
      return transformLocal(origin, bodyYaw, rolledSide * wingScale, (pose.preTranslateY + rolledUp) * wingScale, (pose.preTranslateBack + pitchedBack) * wingScale);
   }

   private Vec3d transformLocal(Vec3d origin, float bodyYaw, float side, float up, float back) {
      float yawRad = (float)Math.toRadians(-bodyYaw + 90.0F);
      double rightX = Math.sin(yawRad);
      double rightZ = Math.cos(yawRad);
      double forwardX = Math.sin(yawRad + Math.PI / 2.0D);
      double forwardZ = Math.cos(yawRad + Math.PI / 2.0D);
      return origin.add(rightX * side + forwardX * back, up, rightZ * side + forwardZ * back);
   }

   private boolean hasElytra(PlayerEntity player) {
      return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   private boolean isVisibleFriend(PlayerEntity player) {
      return FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString())
              && !player.hasStatusEffect(StatusEffects.INVISIBILITY)
              && !player.isInvisible()
              && (mc.player == null || !player.isInvisibleTo(mc.player));
   }

   private WingPose resolvePose(PlayerEntity player, float tickDelta) {
      float pitch = player.getPitch(tickDelta);
      if (player.isGliding()) {
         float flightProgress = MathHelper.clamp((player.age + tickDelta) / 10.0F, 0.0F, 1.0F);
         float pitchRotation = flightProgress * (-90.0F - pitch);
         return new WingPose(0.34F, 0.46F, 0.0F, 0.0F, pitchRotation, 0.0F, 0.76F, 0.92F, 0.10F, 0.58F, 0.05F, 0.06F, -0.05F, -5.0F, -2.0F, 0.13F);
      }
      if (player.isSneaking()) {
         return new WingPose(0.0F, 0.0F, 0.96F, 0.10F, 18.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F, 0.06F, 0.0F, 0.02F, -11.0F, -4.0F, 0.12F);
      }
      return new WingPose(0.0F, 0.0F, 1.38F, 0.10F, 0.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F, 0.06F, 0.0F, 0.02F, -11.0F, -4.0F, 0.12F);
   }

   private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
      float target = MathHelper.lerpAngleDegrees(tickDelta, player.lastBodyYaw, player.bodyYaw);
      if (player != mc.player) {
         return target;
      }

      if (!this.selfBodyYawInitialized || player.age < 2) {
         this.selfBodyYaw = target;
         this.selfBodyYawInitialized = true;
         return this.selfBodyYaw;
      }

      this.selfBodyYaw = approachDegrees(this.selfBodyYaw, target, 14.0F);
      return this.selfBodyYaw;
   }

   private float approachDegrees(float current, float target, float maxDelta) {
      float delta = MathHelper.wrapDegrees(target - current);
      delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
      return current + delta;
   }

   private ColorRGBA applyPointAlpha(ColorRGBA color, float multiplier) {
      return color.withAlpha(color.getAlpha() * MathHelper.clamp(multiplier, 0.28F, 1.0F));
   }

   private ColorRGBA withAlpha(ColorRGBA color, float multiplier) {
      return color.withAlpha(MathHelper.clamp(multiplier * this.transparency.getCurrentValue(), 0.0F, 1.0F) * 255.0F);
   }

   private record WingPoint(float x, float y, float alphaMul) {
   }

   private record WingPose(
           float preTranslateY,
           float preTranslateBack,
           float anchorY,
           float anchorBack,
           float pitchRotation,
           float rollRotation,
           float openMultiplier,
           float scaleMultiplier,
           float motionSpreadBoost,
           float flapAmplitude,
           float sideOffset,
           float sideYOffset,
           float sideBackOffset,
           float sideRoll,
           float sidePitch,
           float flapSpeed
   ) {
   }
}
