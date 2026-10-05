package fever.visual.utility.game;

import lombok.Generated;
import fever.visual.utility.game.server.ServerUtility;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class EntityUtility implements IMinecraft {
   private static float timer = 1.0F;

   public static void resetTimer() {
      timer = 1.0F;
   }

   public static Block getBlock() {
      return getBlock(0.0, 0.0, 0.0);
   }

   public static Block getBlock(double x, double y, double z) {
      return !isInGame() ? Blocks.AIR : mc.world.getBlockState(BlockPos.ofFloored(mc.player.getEntityPos().add(x, y, z))).getBlock();
   }

   public static boolean collideWith(LivingEntity entity) {
      return collideWith(entity, 0.0F);
   }

   public static boolean collideWith(LivingEntity entity, float grow) {
      Box box = mc.player.getBoundingBox();
      Box targetbox = entity.getBoundingBox().expand(grow, 0.0, grow);
      return box.maxX > targetbox.minX
         && box.maxY > targetbox.minY
         && box.maxZ > targetbox.minZ
         && box.minX < targetbox.maxX
         && box.minY < targetbox.maxY
         && box.minZ < targetbox.maxZ;
   }

   public static void setSpeed(double speed) {
      double forward = mc.player.input.getMovementInput().y;
      double strafe = mc.player.input.getMovementInput().x;
      float yaw = mc.player.getYaw();
      if (forward == 0.0 && strafe == 0.0) {
         mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
      } else {
         if (forward != 0.0) {
            if (strafe > 0.0) {
               yaw += forward > 0.0 ? -45 : 45;
            } else if (strafe < 0.0) {
               yaw += forward > 0.0 ? 45 : -45;
            }

            strafe = 0.0;
            forward = forward > 0.0 ? 1.0 : -1.0;
         }

         double motionX = forward * speed * Math.cos(Math.toRadians(yaw + 90.0)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90.0));
         double motionZ = forward * speed * Math.sin(Math.toRadians(yaw + 90.0)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90.0));
         mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
      }
   }

   public static double getVelocity() {
      return Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
   }

   public static Block getBlockStandingOnPlayer() {
      if (mc.player != null && mc.world != null) {
         BlockPos pos = mc.player.getBlockPos();
         return getBlockAt(pos, mc.world);
      } else {
         return null;
      }
   }

   public static Block getBlockAt(BlockPos pos, World world) {
      return world.getBlockState(pos).getBlock();
   }

   public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
      if (moveForward < 0.0) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0) {
         forward = -0.5F;
      } else if (moveForward > 0.0) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians(rotationYaw);
   }

   public static boolean isInGame() {
      return mc.player != null && mc.world != null;
   }

   public static float getHealth(PlayerEntity ent) {
      if (ent == null) {
         return 0.0F;
      } else if (!ServerUtility.isServerForHPFix()) {
         return ent.getHealth() + ent.getAbsorptionAmount();
      } else {
         ScoreboardObjective scoreBoard = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
         if (scoreBoard != null) {
            ReadableScoreboardScore score = mc.world.getScoreboard().getScore(ent, scoreBoard);
            String text = ReadableScoreboardScore.getFormattedScore(score, scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY)).getString();
            String digits = text.replaceAll("[^0-9.]", "");

            try {
               return Float.parseFloat(digits);
            } catch (NumberFormatException var6) {
            }
         }

         return ent.getMaxHealth();
      }
   }

   @Generated
   private EntityUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static void setTimer(float timer) {
      EntityUtility.timer = timer;
   }

   @Generated
   public static float getTimer() {
      return timer;
   }
}
