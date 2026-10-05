package fever.visual.mixin.minecraft.client.render.entity.equipment;

import fever.visual.systems.modules.modules.visuals.HitColor;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.render.HitColorRenderContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(EquipmentRenderer.class)
public abstract class EquipmentRendererHitColorMixin {
    @ModifyArg(
            method = "render("
                    + "Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;"
                    + "Lnet/minecraft/registry/RegistryKey;"
                    + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                    + "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I"
                    + "Lnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel("
                            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                            + "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;"
                            + "IIILnet/minecraft/client/texture/Sprite;I"
                            + "Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
            ),
            index = 6
    )
    private int fevervisual$tintArmorOnHit(int color) {
        return this.fevervisual$applyHitColor(color);
    }

    @ModifyArg(
            method = "render("
                    + "Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;"
                    + "Lnet/minecraft/registry/RegistryKey;"
                    + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                    + "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I"
                    + "Lnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel("
                            + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
                            + "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;"
                            + "IIILnet/minecraft/client/texture/Sprite;I"
                            + "Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
            ),
            index = 5
    )
    private int fevervisual$replaceArmorTrimOverlayUv(int overlay) {
        if (!HitColorRenderContext.isHurt()) {
            return overlay;
        }
        HitColor module = HitColor.getModule();
        if (module == null || !module.isEnabled()) {
            return overlay;
        }
        return OverlayTexture.getUv(0.0F, true);
    }

    @Unique
    private int fevervisual$applyHitColor(int color) {
        if (!HitColorRenderContext.isHurt()) {
            return color;
        }

        HitColor module = HitColor.getModule();
        if (module == null || !module.isEnabled()) {
            return color;
        }

        ColorRGBA hit = module.getHitColor();
        if (hit == null) {
            return color;
        }
        float strength = MathHelper.clamp(hit.getAlpha() / 255.0F, 0.0F, 1.0F);
        if (strength <= 0.0F) {
            return color;
        }

        int a = (color >>> 24) & 0xFF;
        int br = (color >> 16) & 0xFF;
        int bg = (color >> 8) & 0xFF;
        int bb = color & 0xFF;

        int r = MathHelper.clamp(Math.round(br + (hit.getRed() - br) * strength), 0, 255);
        int g = MathHelper.clamp(Math.round(bg + (hit.getGreen() - bg) * strength), 0, 255);
        int b = MathHelper.clamp(Math.round(bb + (hit.getBlue() - bb) * strength), 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
