package com.wildfire.api;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.FemaleGender;
import net.minecraft.item.Item;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

public final class WildfireAPI {
   private static final Map<Item, IGenderArmor> GENDER_ARMORS = new HashMap<>();

   private WildfireAPI() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static void addGenderArmor(Item item, IGenderArmor genderArmor) {
      if (item != null && genderArmor != null) {
         GENDER_ARMORS.put(item, genderArmor);
      }
   }

   public static Map<Item, IGenderArmor> getGenderArmors() {
      return GENDER_ARMORS;
   }

   public static float getPlayerSize(PlayerEntity player) {
      if (player == null || FeverVisual.getInstance().getModuleManager() == null) {
         return 0.0F;
      }

      FemaleGender module = FeverVisual.getInstance().getModuleManager().getModuleSafe(FemaleGender.class);
      if (module == null || !module.isEnabled()) {
         return 0.0F;
      }

      return module.getSizeFor(player);
   }
}
