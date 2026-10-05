package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ModeSetting;

@ModuleInfo(
   name = "Custom Swords",
   category = ModuleCategory.VISUALS,
   desc = "Custom sword item models"
)
public class CustomSwords extends BaseModule {
   private static final String[] WEAPONS = new String[]{
      "Abominable Blade", "Abominable Great Saber", "Abominable Scythe", "Acidic Cleaver", "Amethyst Shuriken",
      "Ancient Royal Great Sword", "Aquatic Sacred Blade", "Arcanethyst", "Ashura's Blade", "Awakened Lichblade",
      "Blood Edge", "Bloody Death", "Bramblethorn", "Brimstone Claymore", "Carian Knight's Sword", "Chrono Blade",
      "Corrupted Mythic Blade", "Creation Splitter", "Crescent Rose", "Cyber Katana", "Cyber Mantis Blade",
      "Cyber Sword", "Cybernetic Chainsaw Blade", "Cybernetic Katana", "Cybernetic Knife", "Dainsleif", "Dark Blade",
      "Dark Cleaver", "Death Knight's Dagger", "Death Knight's Sword", "Demigod's Unholy Blade",
      "Demigod's Unholy Halberd", "Demon Lord's Great Axe", "Demon Lord's Sword", "Demonic Blade", "Demonic Cleaver",
      "Divine Axe Rhitta", "Divine Justice", "Divine Punisher", "Divine Reaper", "Dragon Slaying blade",
      "Edge Of The Astral Plane", "Emberblade", "Enigma", "Epic Sword", "Estoc", "Fallen God's Spear",
      "Fallen God's Sword", "Floral Longsword", "Floral Sabre", "Forest Guardian's Glaive", "Frost Axe",
      "Frost Blade", "Frost Scythe", "Hearthflame", "Hero Sword", "Holy Moonlight Sword", "Hornet's Needle",
      "Icewhisper", "Jade Halberd", "Katana", "Legendary Sword", "Longsword", "Magi Scythe", "Masamune",
      "Mjolnir", "Molten Blade", "Molten Sword", "Muramasa", "Mystical Spellblade", "Mythic Blade", "Ocean's Rage",
      "Partisan", "Pharaoh's Treasure", "Pheonix Grace", "Plague Longsword", "Power Fuse Hammer", "Power Fuse Sword",
      "Requiem of the Ninth Abyss", "Ribbon Cleaver", "Righteous Relic", "Rivers Of Blood", "Royal Chakram",
      "Royal Rapier", "Sabre", "Scissor Blade", "Sculk Cleaver", "Sculk Scythe", "Sculk Sword", "Sentinel's Will",
      "Silverine Blade", "Soul Claws", "Soul Collector", "Soul Devourer", "Soul Edge", "Soul Harvester",
      "Soul Stealer", "Soulrender", "Star's Edge", "Steel Sword", "Stop Sign", "Storm Bringer", "Storm's Edge",
      "Sunbreak", "Tengen's Blade", "Terra Blade", "Thousand Demon Daggers", "Thunder Bringer", "Thunderbrand",
      "True Excalibur", "Vampiric Needle", "Wakizashi", "Watcher Claymore", "Watching Warglaive", "Waxweaver",
      "Whisperwind", "Wickpiercer", "Wraith Scythe", "Yoru"
   };

   private final BooleanSetting selfOnly = new BooleanSetting(this, "Self Only").enable();
   private final ModeSetting weapon = new ModeSetting(this, "Weapon");

   public CustomSwords() {
      for (int i = 0; i < WEAPONS.length; i++) {
         ModeSetting.Value value = new ModeSetting.Value(this.weapon, WEAPONS[i].replace("'", ""));
         if (i == 0) {
            value.select();
         }
      }
   }

   public boolean isSelfOnly() {
      return this.selfOnly.isEnabled();
   }

   public String getSelectedWeapon() {
      String selected = this.weapon.getValue() == null ? WEAPONS[0].replace("'", "") : this.weapon.getValue().getName();
      for (String weapon : WEAPONS) {
         if (weapon.replace("'", "").equals(selected)) {
            return weapon;
         }
      }
      return selected;
   }

   public static CustomSwords getModule() {
      if (FeverVisual.getInstance() == null || FeverVisual.getInstance().getModuleManager() == null) {
         return null;
      }
      return FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomSwords.class);
   }
}
