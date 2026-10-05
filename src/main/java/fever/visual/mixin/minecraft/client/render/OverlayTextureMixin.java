package fever.visual.mixin.minecraft.client.render;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.systems.modules.modules.visuals.HitColor;
import fever.visual.utility.render.OverlayTextureReloadBridge;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OverlayTexture.class)
public abstract class OverlayTextureMixin {
    @Unique
    private static final int FEVER_VISUAL_DEFAULT_HIT_OVERLAY = -1291911168;
    @Unique
    private final Runnable fevervisual$reloadCallback = this::fevervisual$reloadOverlay;

    @Shadow
    @Final
    private NativeImageBackedTexture texture;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fevervisual$onInit(CallbackInfo ci) {
        OverlayTextureReloadBridge.register(this.fevervisual$reloadCallback);
        this.fevervisual$reloadOverlay();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void fevervisual$onClose(CallbackInfo ci) {
        OverlayTextureReloadBridge.unregister(this.fevervisual$reloadCallback);
    }

    @Unique
    private int fevervisual$getTargetOverlayColor() {
        HitColor module = HitColor.getModule();
        if (module != null && module.isEnabled()) {
            return module.getOverlayArgb();
        }
        return FEVER_VISUAL_DEFAULT_HIT_OVERLAY;
    }

    @Unique
    private void fevervisual$reloadOverlay() {
        NativeImage nativeImage = this.texture.getImage();
        if (nativeImage == null) {
            return;
        }

        int color = this.fevervisual$getTargetOverlayColor();
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 16; ++x) {
                nativeImage.setColorArgb(x, y, color);
            }
        }

        this.texture.upload();
    }
}
