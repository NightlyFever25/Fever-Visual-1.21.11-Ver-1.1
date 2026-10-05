package fever.visual.systems.modules.modules.other;

import lombok.Generated;
import fever.visual.minecraft.MinecraftClientMixinProtection;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.StringSetting;

@ModuleInfo(name = "Client Name", category = ModuleCategory.MISC, desc = "modules.descriptions.client_name")
public class ClientName extends BaseModule {
    private final StringSetting clientName = new StringSetting(this, "modules.settings.name_protect.client_name").text("Fever Visual -> 1.21.11 Release");

    private String lastTitle = "";

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftClientMixinProtection.forceUpdateTitle();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClientMixinProtection.forceUpdateTitle();
    }

    @Override
    public void tick() {
        String currentTitle = getClientName();
        if (!currentTitle.equals(lastTitle)) {
            lastTitle = currentTitle;
            if (this.isEnabled()) {
                MinecraftClientMixinProtection.forceUpdateTitle();
            }
        }
    }

    public String getClientName() {
        if (this.isEnabled() && this.clientName != null && this.clientName.getText() != null) {
            String customName = this.clientName.getText().trim();
            if (!customName.isEmpty()) {
                return processDynamicTitle(customName);
            }
        }
        return "Fever Visual -> 1.21.11 Release";
    }

    private String processDynamicTitle(String title) {
        if (title.contains("%time%")) {
            title = title.replace("%time%", getCurrentTime());
        }

        if (title.contains("%fps%") && mc != null && mc.getCurrentFps() != 0) {
            title = title.replace("%fps%", String.valueOf(mc.getCurrentFps()));
        }

        if (title.contains("%version%")) {
            title = title.replace("%version%", "1.21.11 Release");
        }

        return title;
    }

    private String getCurrentTime() {
        return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public void updateTitle() {
        if (this.isEnabled()) {
            MinecraftClientMixinProtection.forceUpdateTitle();
        }
    }


    @Generated
    public StringSetting getClientNameSetting() {
        return this.clientName;
    }
}