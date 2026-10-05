package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.NoRender;
import fever.visual.ui.hud.impl.island.DynamicIsland;
import fever.visual.ui.hud.impl.island.impl.PVPStatus;
import fever.visual.utility.game.server.ServerUtility;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(BossBarHud.class)
public class BossBarHudMixin implements IMinecraft {
    @Shadow
    @Final
    private Map<UUID, ClientBossBar> bossBars;

    @Unique
    private static final Pattern PVP_TIME_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(?:\\u0441\\u0435\\u043a(?:\\u0443\\u043d\\u0434[\\u0430\\u044b]?)?|sec|s)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Unique
    private static long fevervisual$lastCombatTickMs;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, CallbackInfo ci) {
        double ctTimer = -1.0;

        for (ClientBossBar bossBar : this.bossBars.values()) {
            if (bossBar.getName() == null) {
                continue;
            }
            String name = bossBar.getName().getString().toLowerCase();
            if (!name.contains("\u0431\u043e\u0439") && !name.contains("pvp")) {
                continue;
            }

            Matcher matcher = PVP_TIME_PATTERN.matcher(bossBar.getName().getString());
            if (matcher.find()) {
                try {
                    ctTimer = Double.parseDouble(matcher.group(1).replace(',', '.'));
                } catch (Exception ignored) {
                    ctTimer = -1.0;
                }
            }
            break;
        }

        if (ctTimer > 0.0) {
            ServerUtility.setHasCT(true);
            ServerUtility.setCtTime((int) Math.ceil(ctTimer));
            fevervisual$lastCombatTickMs = System.currentTimeMillis();
        } else {
            long now = System.currentTimeMillis();
            if (fevervisual$lastCombatTickMs == 0L) {
                fevervisual$lastCombatTickMs = now;
            }
            if (ServerUtility.hasCT && now - fevervisual$lastCombatTickMs >= 1000L) {
                int ticks = (int) ((now - fevervisual$lastCombatTickMs) / 1000L);
                ServerUtility.setCtTime(Math.max(0, ServerUtility.ctTime - ticks));
                ServerUtility.setHasCT(ServerUtility.ctTime > 0);
                fevervisual$lastCombatTickMs += (long) ticks * 1000L;
            }
        }

        NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender.isEnabled() && noRender.getBossBar().isSelected()) {
            return;
        }

        if (FeverVisual.getInstance().getHud().getIsland().isShowing() && !this.bossBars.isEmpty() && !ServerUtility.isCM()) {
            DynamicIsland island = FeverVisual.getInstance().getHud().getIsland();
            boolean islandShowingPvp = island.isShowing() && island.statuses().stream().anyMatch(status -> status instanceof PVPStatus);
            if (ServerUtility.hasCT && islandShowingPvp) {
                return;
            }

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, island.getSize().height + 7.0f);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(CallbackInfo ci) {
        NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
        DynamicIsland island = FeverVisual.getInstance().getHud().getIsland();
        boolean islandShowingPvp = island.isShowing() && island.statuses().stream().anyMatch(status -> status instanceof PVPStatus);
        if ((noRender.isEnabled() && noRender.getBossBar().isSelected()) || (ServerUtility.hasCT && islandShowingPvp)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, CallbackInfo ci) {
        NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender.isEnabled() && noRender.getBossBar().isSelected()) {
            return;
        }

        if (FeverVisual.getInstance().getHud().getIsland().isShowing() && !this.bossBars.isEmpty() && !ServerUtility.isCM()) {
            context.getMatrices().popMatrix();
        }
    }
}
