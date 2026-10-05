package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.systems.modules.modules.visuals.BetterTab;
import fever.visual.ui.hud.impl.CustomTab;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Shadow
    @Nullable
    private Text header;
    @Shadow
    @Nullable
    private Text footer;

    @ModifyConstant(method = "collectPlayerEntries", constant = @Constant(longValue = 80L))
    private long fevervisual$morePlayerEntries(long original) {
        return BetterTab.getPlayerLimit(original);
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 20))
    private int fevervisual$moreRows(int original) {
        return BetterTab.getMaxRows(original);
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 50))
    private int fevervisual$smallerSideMargin(int original) {
        return BetterTab.getSideMargin(original);
    }

    @Inject(method = "setVisible", at = @At("HEAD"))
    private void fevervisual$trackCustomTabVisible(boolean visible, CallbackInfo ci) {
        CustomTab.onVanillaVisibleChanged(visible);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void fevervisual$renderCustomTab(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        if (CustomTab.renderReplacement(context, scaledWindowWidth, scoreboard, objective, (PlayerListHud) (Object) this, this.header, this.footer)) {
            ci.cancel();
        }
    }
}
