package fever.visual.mixin.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.Cape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    @Final
    private GameProfile profile;

    @Unique
    private static Cape getCapeModule() {
        try {
            if (FeverVisual.getInstance() == null || FeverVisual.getInstance().getModuleManager() == null) {
                return null;
            }
            return FeverVisual.getInstance().getModuleManager().getModule(Cape.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private PlayerEntity getPlayerByName(String playerName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getName().getString().equals(playerName)) {
                return player;
            }
        }
        return null;
    }

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            Cape capeModule = getCapeModule();
            if (capeModule == null || !capeModule.isEnabled()) {
                return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            String playerName = profile.name();

            PlayerEntity player = getPlayerByName(playerName);
            if (player == null) return;
            if (!capeModule.shouldRenderForPlayer(player)) {
                return;
            }

            SkinTextures original = cir.getReturnValue();
            if (original != null) {
                AssetInfo.TextureAsset cape = new AssetInfo.TextureAssetInfo(
                        capeModule.getCapeTexture(),
                        capeModule.getCapeTexture()
                );
                SkinTextures modified = new SkinTextures(
                        original.body(),
                        cape,
                        cape,
                        original.model(),
                        original.secure()
                );
                cir.setReturnValue(modified);
            }
        } catch (Exception e) {

        }
    }
}
