package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.framework.base.CustomDrawContext;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.event.impl.render.PostHudRenderEvent;
import fever.visual.systems.event.impl.render.PreHudRenderEvent;
import fever.visual.systems.modules.modules.visuals.NoRender;
import fever.visual.systems.modules.modules.visuals.CustomFog;
import fever.visual.systems.modules.modules.visuals.Crosshair;
import fever.visual.systems.modules.modules.visuals.Interface;
import fever.visual.systems.modules.modules.visuals.Saturation;
import fever.visual.ui.hud.impl.CustomScoreboard;
import fever.visual.ui.hud.impl.CustomTab;
import fever.visual.ui.hud.impl.Effects;
import fever.visual.ui.hud.impl.Hotbar;

import fever.visual.utility.game.PlatformUtility;
import fever.visual.utility.game.server.ServerUtility;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.render.DrawUtility;
import fever.visual.utility.render.ScissorUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.bar.Bar;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InGameHud.class)
public class InGameHudMixin implements IMinecraft {

    @WrapWithCondition(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
    )
    private boolean suppressScoreboardSidebarCall(InGameHud instance, DrawContext context, RenderTickCounter tickCounter) {
        return !shouldSuppressVanillaScoreboard();
    }

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void suppressScoreboardSidebarRoot(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (shouldSuppressVanillaScoreboard()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (objective.getDisplayName().getString().contains("\u0410\u043D\u0430\u0440\u0445\u0438\u044F") && (ServerUtility.isFT() || ServerUtility.isST())) {
            try {
                ServerUtility.ftAn = Integer.parseInt(objective.getDisplayName().getString().split("-")[1].trim());
            } catch (Exception ignored) {
            }
        }

        if (shouldSuppressVanillaScoreboard()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void renderPortalOverlayHook(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender.isEnabled() && noRender.getPortal().isSelected()) {
            ci.cancel();
        }
    }

    @ModifyArgs(
            method = "renderMiscOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V",
                    ordinal = 0
            )
    )
    private void onRenderPumpkinOverlay(Args args) {
        NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender.isEnabled() && noRender.getPumpkin().isSelected()) {
            args.set(2, 0.0f);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void triggerPreHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context, CustomDrawContext.Pass.PRE);
        FeverVisual.getInstance().getEventManager().triggerEvent(new PreHudRenderEvent(customDrawContext, tickCounter.getTickProgress(false)));
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void triggerPostHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context, CustomDrawContext.Pass.POST);
        FeverVisual.getInstance().getEventManager().triggerEvent(new PostHudRenderEvent(customDrawContext, tickCounter.getTickProgress(false)));
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Hotbar.shouldSuppressVanilla()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void suppressVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Crosshair.shouldReplaceVanilla()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
            method = "renderMainHud",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V")
    )
    private boolean suppressVanillaExperienceLevel(DrawContext context, TextRenderer textRenderer, int level) {
        return !Hotbar.shouldSuppressVanilla();
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaStatusBars(DrawContext context, CallbackInfo ci) {
        if (Hotbar.shouldSuppressVanilla()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void hideVanillaStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Effects.shouldReplaceVanilla()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void triggerHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomDrawContext customDrawContext;
        if (shouldRenderLateLunarHudUnderScreen()) {
            customDrawContext = CustomDrawContext.of(context, CustomDrawContext.Pass.POST);
        } else {
            context.createNewRootLayer();
            customDrawContext = CustomDrawContext.isolated(context, CustomDrawContext.Pass.POST);
        }

        if (Interface.shouldUpdateHudBlur()) {
            customDrawContext.updateBlur();
        }
        FeverVisual.getInstance().getEventManager().triggerEvent(new HudRenderEvent(customDrawContext, tickCounter.getTickProgress(false)));
        Saturation.render(customDrawContext);
        ScissorUtility.clear();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        CustomTab.renderDeferred(context);
    }

    private boolean shouldSuppressVanillaScoreboard() {
        NoRender noRender = FeverVisual.getInstance().getModuleManager().getModule(NoRender.class);
        return noRender.isEnabled() && noRender.getScoreboard().isSelected() || CustomScoreboard.shouldReplaceVanilla();
    }

    private boolean shouldRenderLateLunarHudUnderScreen() {
        return PlatformUtility.isLunarClient() && mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen);
    }
}
