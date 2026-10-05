package fever.visual.systems.modules.modules.visuals;

import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.framework.msdf.Fonts;
import fever.visual.framework.objects.BorderRadius;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.PreHudRenderEvent;
import fever.visual.systems.event.impl.window.KeyPressEvent;
import fever.visual.systems.localization.Localizator;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.MessageUtility;
import fever.visual.utility.render.Utils;
import fever.visual.utility.sounds.ClientSounds;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ModuleInfo(name = "Waypoints", category = ModuleCategory.VISUALS, enabledByDefault = false)
public class Waypoints extends BaseModule {
   private final Map<String, Waypoint> waypoints = new HashMap<>();
   private final EventListener<KeyPressEvent> onClientPlayerTickEvent = event -> {
      if (event.getKey() == 86 && event.getAction() == 1 && mc.currentScreen == null && mc.player != null && mc.world != null) {
         Vec3d start = mc.player.getEyePos();
         Vec3d direction = mc.player.getRotationVec(mc.getRenderTickCounter().getTickProgress(true));
         Vec3d end = start.add(direction.multiply(200.0));
         PlayerEntity targetPlayer = null;
         double closestDistance = Double.MAX_VALUE;

         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !this.isInvisible(player)) {
               Box hitbox = player.getBoundingBox().expand(0.3);
               Vec3d hit = (Vec3d)hitbox.raycast(start, end).orElse(null);
               if (hit != null) {
                  double distance = start.distanceTo(hit);
                  if (distance < closestDistance && distance <= 200.0) {
                     closestDistance = distance;
                     targetPlayer = player;
                  }
               }
            }
         }

         if (targetPlayer != null) {
            UUID playerUUID = targetPlayer.getUuid();
            if (this.waypoints.values().stream().anyMatch(w -> playerUUID.equals(w.playerUUID))) {
               return;
            }

            String name = targetPlayer.getName().getString();
            Vec3d pos = targetPlayer.getEntityPos();
            this.add(name, pos.x, pos.y, pos.z, true, playerUUID);
            return;
         }

         HitResult raycastResult = mc.player.raycast(200.0, mc.getRenderTickCounter().getTickProgress(true), false);
         if (raycastResult.getType() == Type.BLOCK && raycastResult instanceof BlockHitResult blockHit) {
            Vec3d pos = blockHit.getPos();
            String baseName = Localizator.translate("modules.waypoints.base_name");
            String name = baseName;
            int counter = 1;

            while (this.waypoints.containsKey(name)) {
               name = baseName + " " + counter++;
            }

            this.add(name, pos.x, pos.y, pos.z, true, null);
         }
      }
   };
   private final EventListener<PreHudRenderEvent> onHudRenderEvent = event -> {
      MatrixStack matrices = event.getContext().getMatrices();
      float tickDelta = event.getTickDelta();
      long currentTime = System.currentTimeMillis();
      this.waypoints.entrySet().removeIf(entry -> {
         Waypoint waypointx = entry.getValue();
         if (waypointx.temp && currentTime - waypointx.creationTime > 5000L) {
            return true;
         } else {
            if (waypointx.playerUUID != null) {
               PlayerEntity player = mc.world.getPlayerByUuid(waypointx.playerUUID);
               if (player == null || this.isInvisible(player)) {
                  MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.player_removed", waypointx.name)));
                  return true;
               }

               Vec3d targetPos = Utils.getInterpolatedPos(player, tickDelta);
               float alpha = 0.2F * tickDelta;
               waypointx.pos = waypointx.pos.lerp(targetPos, MathHelper.clamp(alpha, 0.0F, 1.0F));
            }

            return false;
         }
      });

      for (Waypoint waypoint : this.waypoints.values()) {
         Vec3d renderPos = waypoint.pos;
         Vec3d renderPosAdjusted = renderPos.add(0.0, 0.5, 0.0);
         Vec2f screenPos = Utils.worldToScreen(renderPosAdjusted);
         if (screenPos != null) {
            float distance = (float)mc.player.getEntityPos().distanceTo(renderPos);
            float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
            matrices.push();
            matrices.translate(screenPos.x, screenPos.y, 0.0F);
            matrices.scale(scale, scale, 1.0F);
            String text = waypoint.name + " (" + String.format("%.1f", distance) + "m)";
            int width = (int)Fonts.MEDIUM.getFont(11.0F).width(text);
            int x = -width / 2;
            int iconSize = 32;
            event.getContext()
                    .drawRoundedRect(
                            (float)(x - 3),
                            2.0F,
                            (float)(width + 8),
                            Fonts.MEDIUM.getFont(11.0F).height() + 6.0F,
                            BorderRadius.all(3.0F),
                            new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F)
                    );
            event.getContext().drawText(Fonts.MEDIUM.getFont(11.0F), text, x, 5.0F, ColorRGBA.WHITE);
            if (waypoint.playerUUID != null) {
               int iconY = (int)(-22.0F + (Fonts.MEDIUM.getFont(11.0F).height() - iconSize) / 2.0F);
               event.getContext().drawTexture(FeverVisual.id("icons/target2.png"), -15.0F, iconY, iconSize, iconSize, ColorRGBA.WHITE);
            }

            matrices.pop();
         }
      }
   };

   private void add(String name, double x, double y, double z, boolean isTemp, UUID playerUUID) {
      Vec3d pos = new Vec3d(x, y, z);
      if (this.waypoints.containsKey(name)) {
         MessageUtility.error(Text.of(Localizator.translate("modules.waypoints.exists", name)));
      } else {
         this.waypoints.put(name, new Waypoint(name, pos, isTemp, System.currentTimeMillis(), playerUUID));
         ClientSounds.MODULE.play(0.5F);
      }
   }

   private boolean isInvisible(PlayerEntity player) {
      if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) return true;
      if (player.isInvisible()) return true;
      if (mc.player != null && player.isInvisibleTo(mc.player)) return true;
      return false;
   }

   private static class Waypoint {
      public String name;
      public Vec3d pos;
      public boolean temp;
      public long creationTime;
      public UUID playerUUID;

      @Generated
      public Waypoint(String name, Vec3d pos, boolean temp, long creationTime, UUID playerUUID) {
         this.name = name;
         this.pos = pos;
         this.temp = temp;
         this.creationTime = creationTime;
         this.playerUUID = playerUUID;
      }
   }
}
