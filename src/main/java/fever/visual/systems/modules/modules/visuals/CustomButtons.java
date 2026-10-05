package fever.visual.systems.modules.modules.visuals;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.pack.ExperimentalWarningScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.BackupPromptScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditGameRulesScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.OptimizeWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

@ModuleInfo(name = "Custom Buttons", category = ModuleCategory.DISPLAY, desc = "modules.descriptions.custom_buttons")
public class CustomButtons extends BaseModule {
    public static boolean isEnabledSafe() {
        if (FeverVisual.getInstance().getModuleManager() == null) {
            return false;
        }

        CustomButtons module = FeverVisual.getInstance().getModuleManager().getModuleSafe(CustomButtons.class);
        return module != null && module.isEnabled();
    }

    public static boolean shouldStyleCurrentButtonScreen() {
        return shouldStyleButtonScreen(MinecraftClient.getInstance().currentScreen);
    }

    public static boolean shouldStyleCurrentSettingsScreen() {
        return shouldStyleSettingsScreen(MinecraftClient.getInstance().currentScreen);
    }

    public static boolean shouldStyleButtonScreen(Screen screen) {
        if (screen == null) {
            return false;
        }

        if (shouldStyleSettingsScreen(screen)) {
            return true;
        }

        String screenName = screen.getClass().getSimpleName();
        return screen instanceof TitleScreen
                || screen instanceof GameMenuScreen
                || screen instanceof StatsScreen
                || screen instanceof AdvancementsScreen
                || screen instanceof OpenToLanScreen
                || screen instanceof CreditsScreen
                || screen instanceof CreditsAndAttributionScreen
                || screen instanceof MultiplayerScreen
                || screen instanceof AddServerScreen
                || screen instanceof DirectConnectScreen
                || screen instanceof SelectWorldScreen
                || screen instanceof EditWorldScreen
                || screen instanceof CreateWorldScreen
                || screen instanceof BackupPromptScreen
                || screen instanceof OptimizeWorldScreen
                || screen instanceof PackScreen
                || screenName.equals("TitleScreen")
                || screenName.equals("MainMenuScreen")
                || screenName.equals("GameMenuScreen")
                || screenName.equals("StatsScreen")
                || screenName.equals("AdvancementsScreen")
                || screenName.equals("FeedbackScreen")
                || screenName.equals("BugReportScreen")
                || screenName.equals("ShareToLanScreen")
                || screenName.equals("OpenToLanScreen")
                || screenName.equals("CreditsAndAttributionScreen")
                || screenName.equals("CreditsScreen")
                || screenName.equals("AttributionScreen")
                || screenName.equals("LicenseScreen")
                || screenName.equals("MultiplayerScreen")
                || screenName.equals("AddServerScreen")
                || screenName.equals("DirectConnectScreen")
                || screenName.equals("EditServerScreen")
                || screenName.equals("SelectWorldScreen")
                || screenName.equals("EditWorldScreen")
                || screenName.equals("CreateWorldScreen")
                || screenName.equals("BackupPromptScreen")
                || screenName.equals("OptimizeWorldScreen")
                || screenName.equals("PackScreen");
    }

    public static boolean shouldStyleSettingsScreen(Screen screen) {
        if (screen == null) {
            return false;
        }

        String className = screen.getClass().getName();
        String screenName = screen.getClass().getSimpleName();

        return screen instanceof GameOptionsScreen
                || screen instanceof PackScreen
                || screen instanceof ExperimentalWarningScreen
                || screen instanceof EditGameRulesScreen
                || className.contains(".client.gui.screen.option.")
                || screenName.endsWith("OptionsScreen")
                || screenName.equals("GameOptionsScreen")
                || screenName.equals("KeybindsScreen")
                || screenName.equals("PackScreen")
                || screenName.equals("ExperimentalWarningScreen")
                || screenName.equals("EditGameRulesScreen");
    }
}
