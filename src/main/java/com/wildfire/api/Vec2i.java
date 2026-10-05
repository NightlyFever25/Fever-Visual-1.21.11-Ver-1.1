package com.wildfire.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Vec2i(int x, int y) {
   public static final Codec<Vec2i> CODEC = RecordCodecBuilder.create(instance ->
      instance.group(
         Codec.INT.fieldOf("x").forGetter(Vec2i::x),
         Codec.INT.fieldOf("y").forGetter(Vec2i::y)
      ).apply(instance, Vec2i::new)
   );

   public Vec2i add(int x, int y) {
      return new Vec2i(this.x + x, this.y + y);
   }
}
