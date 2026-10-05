package fever.visual.ui.menu.api;

import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.utility.render.obj.CustomSprite;
import lombok.Generated;

public enum MenuCategory {
   COMBAT("Combat", ModuleCategory.COMBAT, CustomSprite.COMBAT, CustomSprite.BIG_COMBAT),
   VISUALS("Visuals", ModuleCategory.VISUALS, CustomSprite.VISUALS, CustomSprite.BIG_VISUALS),
   MISC("Misc", ModuleCategory.MISC, CustomSprite.PLAYER, CustomSprite.BIG_PLAYER),
   DISPLAY("Display", ModuleCategory.DISPLAY, CustomSprite.OTHER, CustomSprite.BIG_OTHER);

   private final String name;
   private final ModuleCategory category;
   private final CustomSprite menuSprite;
   private final CustomSprite bigMenuSprite;

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Generated
   public CustomSprite getMenuSprite() {
      return this.menuSprite;
   }

   @Generated
   public CustomSprite getBigMenuSprite() {
      return this.bigMenuSprite;
   }

   @Generated
   private MenuCategory(final String name, final ModuleCategory category, final CustomSprite menuSprite, final CustomSprite bigMenuSprite) {
      this.name = name;
      this.category = category;
      this.menuSprite = menuSprite;
      this.bigMenuSprite = bigMenuSprite;
   }
   private static MenuCategory[] $values() {
      return new MenuCategory[]{COMBAT, VISUALS, MISC, DISPLAY};
   }
}
