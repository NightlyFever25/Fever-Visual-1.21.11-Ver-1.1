package fever.visual.mixin.accessors;

import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
   @Accessor("buffers")
   BufferBuilderStorage buffers();

   @Accessor("lightmapTextureManager")
   LightmapTextureManager lightmapTextureManager();

   @Accessor("fogRenderer")
   FogRenderer fogRenderer();

   @Invoker("getProjectionMatrix")
   Matrix4f invokeGetProjectionMatrix(float tickProgress);
}
