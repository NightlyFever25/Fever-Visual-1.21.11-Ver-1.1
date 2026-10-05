package fever.visual.mixin.minecraft.client.render.entity.feature;

import fever.visual.systems.modules.modules.visuals.HitColor;
import fever.visual.utility.render.HitColorRenderContext;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererHitColorMixin<
        S extends BipedEntityRenderState,
        M extends BipedEntityModel<S>,
        A extends BipedEntityModel<S>> {

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD"),
            require = 1
    )
    private void fevervisual$startArmorHitRender(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            S state,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci
    ) {
        HitColor module = HitColor.getModule();
        boolean useHurtOverlay = module != null && module.isEnabled() && state != null && state.hurt;
        HitColorRenderContext.setHurt(useHurtOverlay);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("RETURN"),
            require = 1
    )
    private void fevervisual$endArmorHitRender(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            S state,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci
    ) {
        HitColorRenderContext.clear();
    }
}
