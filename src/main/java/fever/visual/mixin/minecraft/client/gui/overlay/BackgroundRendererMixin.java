package fever.visual.mixin.minecraft.client.gui.overlay;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.visuals.CustomFog;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.PlatformUtility;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {
   @Shadow
   @Final
   private GpuBuffer emptyBuffer;

   @ModifyArgs(
      method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
      )
   )
   private void fevervisual$modifyFogBuffer(
      Args args,
      Camera camera,
      int viewDistance,
      RenderTickCounter renderTickCounter,
      float skyDarkness,
      ClientWorld world
   ) {
      CustomFog customFogModule = FeverVisual.getInstance().getModuleManager().getModule(CustomFog.class);
      if (customFogModule.shouldModifyFog(camera)) {
         boolean compatClient = PlatformUtility.isLabyMod() || PlatformUtility.isLunarClient();
         CustomFog.setCompatWorldFogBufferActive(compatClient);
         float maximumDistance = compatClient ? Math.max(1.0F, viewDistance * 16.0F) : args.get(6);
         float start = customFogModule.getFogStart(maximumDistance);
         float end = customFogModule.getFogEnd(maximumDistance);
         ColorRGBA color = customFogModule.getFogColorValue();
         float r = color.getRed() / 255.0F;
         float g = color.getGreen() / 255.0F;
         float b = color.getBlue() / 255.0F;
         float a = customFogModule.getColorBlend();
         args.set(2, new Vector4f(r, g, b, a));
         args.set(3, start);
         args.set(4, end);
         args.set(5, start);
         args.set(6, end);
         args.set(7, end);
         args.set(8, end);
      }
   }

   @Inject(
      method = "getFogBuffer(Lnet/minecraft/client/render/fog/FogRenderer$FogType;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void fevervisual$disableWorldFogBuffer(FogRenderer.FogType fogType, CallbackInfoReturnable<GpuBufferSlice> cir) {
      if (fogType == FogRenderer.FogType.WORLD && CustomFog.shouldDisableWorldFogBuffer()) {
         cir.setReturnValue(this.emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE));
      }
   }

   @Inject(
      method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
      at = @At("RETURN"),
      cancellable = true
   )
   private void fevervisual$modifyReturnedFogColor(
      Camera camera,
      int viewDistance,
      RenderTickCounter renderTickCounter,
      float skyDarkness,
      ClientWorld world,
      CallbackInfoReturnable<Vector4f> cir
   ) {
      CustomFog customFogModule = FeverVisual.getInstance().getModuleManager().getModule(CustomFog.class);
      if (customFogModule.shouldModifyFog(camera)) {
         ColorRGBA color = customFogModule.getFogColorValue();
         cir.setReturnValue(new Vector4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 1.0F));
      }
   }
}
