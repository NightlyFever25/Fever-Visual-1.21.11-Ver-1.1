package com.wildfire.api;

import org.jetbrains.annotations.NotNull;

public interface IBreastArmorTexture {
   Vec2i DEFAULT_TEXTURE_SIZE = new Vec2i(64, 32);
   Vec2i DEFAULT_DIMENSIONS = new Vec2i(4, 5);
   Vec2i DEFAULT_LEFT_UV = new Vec2i(16, 17);
   Vec2i DEFAULT_RIGHT_UV = DEFAULT_LEFT_UV.add(DEFAULT_DIMENSIONS.x(), 0);

   default @NotNull Vec2i textureSize() {
      return DEFAULT_TEXTURE_SIZE;
   }

   default @NotNull Vec2i dimensions() {
      return DEFAULT_DIMENSIONS;
   }

   default @NotNull Vec2i leftUv() {
      return DEFAULT_LEFT_UV;
   }

   default @NotNull Vec2i rightUv() {
      return DEFAULT_RIGHT_UV;
   }
}
