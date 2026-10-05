package fever.visual.utility.render.compat;

import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;

public final class GlStateManager {
   private GlStateManager() {
   }

   public enum SrcFactor {
      CONSTANT_ALPHA(SourceFactor.CONSTANT_ALPHA),
      CONSTANT_COLOR(SourceFactor.CONSTANT_COLOR),
      DST_ALPHA(SourceFactor.DST_ALPHA),
      DST_COLOR(SourceFactor.DST_COLOR),
      ONE(SourceFactor.ONE),
      ONE_MINUS_CONSTANT_ALPHA(SourceFactor.ONE_MINUS_CONSTANT_ALPHA),
      ONE_MINUS_CONSTANT_COLOR(SourceFactor.ONE_MINUS_CONSTANT_COLOR),
      ONE_MINUS_DST_ALPHA(SourceFactor.ONE_MINUS_DST_ALPHA),
      ONE_MINUS_DST_COLOR(SourceFactor.ONE_MINUS_DST_COLOR),
      ONE_MINUS_SRC_ALPHA(SourceFactor.ONE_MINUS_SRC_ALPHA),
      ONE_MINUS_SRC_COLOR(SourceFactor.ONE_MINUS_SRC_COLOR),
      SRC_ALPHA(SourceFactor.SRC_ALPHA),
      SRC_ALPHA_SATURATE(SourceFactor.SRC_ALPHA_SATURATE),
      SRC_COLOR(SourceFactor.SRC_COLOR),
      ZERO(SourceFactor.ZERO);

      private final SourceFactor modern;

      SrcFactor(SourceFactor modern) {
         this.modern = modern;
      }

      public SourceFactor modern() {
         return this.modern;
      }
   }

   public enum DstFactor {
      CONSTANT_ALPHA(DestFactor.CONSTANT_ALPHA),
      CONSTANT_COLOR(DestFactor.CONSTANT_COLOR),
      DST_ALPHA(DestFactor.DST_ALPHA),
      DST_COLOR(DestFactor.DST_COLOR),
      ONE(DestFactor.ONE),
      ONE_MINUS_CONSTANT_ALPHA(DestFactor.ONE_MINUS_CONSTANT_ALPHA),
      ONE_MINUS_CONSTANT_COLOR(DestFactor.ONE_MINUS_CONSTANT_COLOR),
      ONE_MINUS_DST_ALPHA(DestFactor.ONE_MINUS_DST_ALPHA),
      ONE_MINUS_DST_COLOR(DestFactor.ONE_MINUS_DST_COLOR),
      ONE_MINUS_SRC_ALPHA(DestFactor.ONE_MINUS_SRC_ALPHA),
      ONE_MINUS_SRC_COLOR(DestFactor.ONE_MINUS_SRC_COLOR),
      SRC_ALPHA(DestFactor.SRC_ALPHA),
      SRC_COLOR(DestFactor.SRC_COLOR),
      ZERO(DestFactor.ZERO);

      private final DestFactor modern;

      DstFactor(DestFactor modern) {
         this.modern = modern;
      }

      public DestFactor modern() {
         return this.modern;
      }
   }
}
