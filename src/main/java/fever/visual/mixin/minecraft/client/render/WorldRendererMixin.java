package fever.visual.mixin.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import fever.visual.FeverVisual;
import fever.visual.systems.event.RenderedEntityTracker;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.modules.visuals.GlowEsp;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
   @Inject(method = "render", at = @At("HEAD"), require = 0)
   private void fevervisual$beginEntityVisibilityFrame(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f frustumMatrix,
      GpuBufferSlice fog,
      org.joml.Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci
   ) {
      GlowEsp.prepareFrame(tickCounter.getTickProgress(false));
      RenderedEntityTracker.beginFrame();
   }

   @Inject(method = "render", at = @At("RETURN"), require = 0)
   private void fevervisual$renderWorld(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f frustumMatrix,
      GpuBufferSlice fog,
      org.joml.Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci
   ) {
      if (FeverVisual.mc.world == null || FeverVisual.mc.player == null) {
         return;
      }

      Profilers.get().swap(FeverVisual.MOD_ID + "_renderWorld");
      MatrixStack matrices = new MatrixStack();
      matrices.multiplyPositionMatrix(positionMatrix);
      FeverVisual.getInstance().getEventManager().triggerEvent(new Render3DEvent(
         matrices,
         new Matrix4f(positionMatrix),
         new Matrix4f(projectionMatrix),
         camera,
         tickCounter.getTickProgress(true)
      ));
   }
}
