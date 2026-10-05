package com.wildfire.api;

import org.jetbrains.annotations.NotNull;

public interface IGenderArmor {
   default boolean coversBreasts() {
      return true;
   }

   default boolean alwaysHidesBreasts() {
      return false;
   }

   default float physicsResistance() {
      return 0.5F;
   }

   default float tightness() {
      return 0.0F;
   }

   default boolean armorStandsCopySettings() {
      return !this.alwaysHidesBreasts() && this.coversBreasts() && this.physicsResistance() == 1.0F;
   }

   default @NotNull IBreastArmorTexture texture() {
      return new IBreastArmorTexture() {
      };
   }
}
