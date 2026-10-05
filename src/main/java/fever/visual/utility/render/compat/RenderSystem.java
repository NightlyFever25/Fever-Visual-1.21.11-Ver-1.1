package fever.visual.utility.render.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import fever.visual.framework.shader.GlProgram;
import java.util.Arrays;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
public final class RenderSystem {
   private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

   private RenderSystem() {
   }

   public static void enableBlend() {
      STATE.get().blend = BlendFunction.TRANSLUCENT;
   }

   public static void disableBlend() {
      STATE.get().blend = null;
   }

   public static void defaultBlendFunc() {
      STATE.get().blend = BlendFunction.TRANSLUCENT;
   }

   public static void blendFunc(GlStateManager.SrcFactor source, GlStateManager.DstFactor dest) {
      STATE.get().blend = new BlendFunction(source.modern(), dest.modern());
   }

   public static void blendFuncSeparate(
      GlStateManager.SrcFactor sourceColor,
      GlStateManager.DstFactor destColor,
      GlStateManager.SrcFactor sourceAlpha,
      GlStateManager.DstFactor destAlpha
   ) {
      STATE.get().blend = new BlendFunction(sourceColor.modern(), destColor.modern(), sourceAlpha.modern(), destAlpha.modern());
   }

   public static void enableCull() {
      STATE.get().cull = true;
   }

   public static void disableCull() {
      STATE.get().cull = false;
   }

   public static void enableDepthTest() {
      STATE.get().depthTest = DepthTestFunction.LEQUAL_DEPTH_TEST;
   }

   public static void disableDepthTest() {
      STATE.get().depthTest = DepthTestFunction.NO_DEPTH_TEST;
   }

   public static void depthFunc(int function) {
      STATE.get().depthTest = switch (function) {
         case 513 -> DepthTestFunction.LESS_DEPTH_TEST;
         case 514 -> DepthTestFunction.EQUAL_DEPTH_TEST;
         case 516 -> DepthTestFunction.GREATER_DEPTH_TEST;
         case 519 -> DepthTestFunction.NO_DEPTH_TEST;
         default -> DepthTestFunction.LEQUAL_DEPTH_TEST;
      };
   }

   public static void depthMask(boolean writeDepth) {
      STATE.get().writeDepth = writeDepth;
   }

   public static void lineWidth(float width) {
      STATE.get().lineWidth = width;
   }

   public static void setScissor(ScreenRect scissor) {
      STATE.get().scissor = scissor;
   }

   public static void setShader(ShaderProgramKeys.Key shader) {
      STATE.get().shader = shader;
      STATE.get().program = null;
   }

   public static void setShader(Object shader) {
      if (shader instanceof ShaderProgramKeys.Key key) {
         setShader(key);
      } else if (shader instanceof GlProgram program) {
         useProgram(program);
      }
   }

   public static Object getShader() {
      State state = STATE.get();
      return state.program != null ? state.program : state.shader;
   }

   public static void useProgram(GlProgram program) {
      STATE.get().program = program;
   }

   public static void setShaderTexture(int slot, Identifier texture) {
      if (slot == 0) {
         STATE.get().texture = texture;
      }
   }

   public static void setShaderTexture(int slot, GpuTexture texture) {
      if (slot == 0) {
         STATE.get().texture = texture;
      }
   }

   public static void setShaderTexture(int slot, GpuTextureView texture) {
      if (slot == 0) {
         STATE.get().texture = texture;
      }
   }

   public static void setShaderTexture(int slot, int texture) {
      if (slot == 0 && texture == 0) {
         STATE.get().texture = null;
      }
   }

   public static void setShaderColor(float red, float green, float blue, float alpha) {
      float[] color = STATE.get().shaderColor;
      color[0] = red;
      color[1] = green;
      color[2] = blue;
      color[3] = alpha;
   }

   public static float[] getShaderColor() {
      return Arrays.copyOf(STATE.get().shaderColor, 4);
   }

   public static float getShaderAlpha() {
      return STATE.get().shaderColor[3];
   }

   public static void copyShaderColor(Vector4f target) {
      float[] color = STATE.get().shaderColor;
      target.set(color[0], color[1], color[2], color[3]);
   }

   public static ScreenRect getScissor() {
      return STATE.get().scissor;
   }

   public static Tessellator renderThreadTesselator() {
      return Tessellator.getInstance();
   }

   public static void recordRenderCall(Runnable runnable) {
      runnable.run();
   }

   public static void activeTexture(int texture) {
   }

   static State state() {
      return STATE.get();
   }

   public static final class State {
      ShaderProgramKeys.Key shader = ShaderProgramKeys.POSITION_COLOR;
      GlProgram program;
      Object texture;
      BlendFunction blend = BlendFunction.TRANSLUCENT;
      DepthTestFunction depthTest = DepthTestFunction.NO_DEPTH_TEST;
      boolean cull;
      boolean writeDepth;
      float lineWidth = 1.0F;
      ScreenRect scissor;
      final float[] shaderColor = {1.0F, 1.0F, 1.0F, 1.0F};

      public BlendFunction blend() {
         return this.blend;
      }

      public DepthTestFunction depthTest() {
         return this.depthTest;
      }

      public boolean cull() {
         return this.cull;
      }

      public boolean writeDepth() {
         return this.writeDepth;
      }
   }
}
