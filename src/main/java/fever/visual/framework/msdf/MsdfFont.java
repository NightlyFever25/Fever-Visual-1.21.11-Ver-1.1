package fever.visual.framework.msdf;

import fever.visual.utility.render.compat.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.other.NameProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class MsdfFont {
   private final String name;
   private final AbstractTexture texture;
   private final FontData.AtlasData atlas;
   private final FontData.MetricsData metrics;
   private final Int2ObjectMap<MsdfGlyph> glyphs;
   private final Int2ObjectMap<Int2FloatMap> kernings;
   private static final int MAX_WIDTH_CACHE_SIZE = 8192;
   private final Long2FloatOpenHashMap widthCache = new Long2FloatOpenHashMap();
   private final Int2ObjectMap<Font> fontCache = new Int2ObjectOpenHashMap<>();
   private NameProtect nameProtectModule;

   private MsdfFont(
      String name,
      AbstractTexture texture,
      FontData.AtlasData atlas,
      FontData.MetricsData metrics,
      Int2ObjectMap<MsdfGlyph> glyphs,
      Int2ObjectMap<Int2FloatMap> kernings
   ) {
      this.name = name;
      this.texture = texture;
      this.atlas = atlas;
      this.metrics = metrics;
      this.glyphs = glyphs;
      this.kernings = kernings;
      this.widthCache.defaultReturnValue(Float.NaN);
   }

   public GpuTexture getTextureId() {
      return this.texture.getGlTexture();
   }

   public void applyGlyphs(
      Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color
   ) {
      int prevChar = -1;
      boolean skipNext = false;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = this.glyphs.get(c);
            if (glyph != null) {
               Int2FloatMap kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  x += kerning.get(c) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
               prevChar = c;
            }
         }
      }
   }

   public float getWidthOld(String text, float size) {
      return this.calculateWidth(this.prepareText(text), size);
   }

   private float calculateWidth(String text, float size) {
      int prevChar = -1;
      float width = 0.0F;
      boolean skipNext = false;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = this.glyphs.get(c);
            if (glyph != null) {
               Int2FloatMap kerning = this.kernings.get(prevChar);
               if (kerning != null) {
                  width += kerning.get(c) * size;
               }

               width += glyph.getWidth(size) + 0.25F;
               prevChar = c;
            }
         }
      }

      return width;
   }

   private static long widthKey(String s, float size, boolean np) {
      int h = s.hashCode();
      return h & 4294967295L ^ (long)Float.floatToIntBits(size) << 32 ^ (np ? -7046029254386353131L : 0L);
   }

   public float getWidth(String text, float size) {
      text = text.replace("і", "i").replace("І", "I");
      NameProtect nameProtect = this.getNameProtectModule();
      boolean np = nameProtect != null && nameProtect.isEnabled();
      if (np) {
         text = nameProtect.patchName(text);
      }

      long key = widthKey(text, size, np);
      float cached = this.widthCache.get(key);
      if (!Float.isNaN(cached)) {
         return cached;
      } else {
         float w = this.calculateWidth(text, size);
         if (this.widthCache.size() >= MAX_WIDTH_CACHE_SIZE) {
            this.widthCache.clear();
         }
         this.widthCache.put(key, w);
         return w;
      }
   }

   public void clearWidthCache() {
      this.widthCache.clear();
   }

   public float getTextWidth(Text text, float size) {
      return this.getWidth(text.getString(), size);
   }

   public Font getFont(float size) {
      int key = Float.floatToIntBits(size);
      Font font = this.fontCache.get(key);
      if (font == null) {
         font = new Font(this, size);
         this.fontCache.put(key, font);
      }
      return font;
   }

   private String prepareText(String text) {
      text = text.replace("і", "i").replace("І", "I");
      NameProtect nameProtect = this.getNameProtectModule();
      return nameProtect != null && nameProtect.isEnabled() ? nameProtect.patchName(text) : text;
   }

   private NameProtect getNameProtectModule() {
      if (this.nameProtectModule == null
         && FeverVisual.getInstance() != null
         && FeverVisual.getInstance().getModuleManager() != null) {
         this.nameProtectModule = FeverVisual.getInstance().getModuleManager().getModuleSafe(NameProtect.class);
      }
      return this.nameProtectModule;
   }

   public String getName() {
      return this.name;
   }

   public FontData.AtlasData getAtlas() {
      return this.atlas;
   }

   public FontData.MetricsData getMetrics() {
      return this.metrics;
   }

   public static MsdfFont.Builder builder() {
      return new MsdfFont.Builder();
   }

   public static class Builder {
      private String name = "?";
      private Identifier dataIdentifer;
      private Identifier atlasIdentifier;

      private Builder() {
      }

      public MsdfFont.Builder name(String name) {
         this.name = name;
         return this;
      }

      public MsdfFont.Builder data(String dataFileName) {
         this.dataIdentifer = Identifier.of(FeverVisual.MOD_ID, "fonts/msdf/" + dataFileName + ".json");
         return this;
      }

      public MsdfFont.Builder atlas(String atlasFileName) {
         this.atlasIdentifier = Identifier.of(FeverVisual.MOD_ID, "fonts/msdf/" + atlasFileName + ".png");
         return this;
      }

      public MsdfFont build() {
         FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
         AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier);
         if (data == null) {
            throw new RuntimeException(
               "Failed to read font data file: "
                  + this.dataIdentifer.toString()
                  + "; Are you sure this is json file? Try to check the correctness of its syntax."
            );
         } else {
            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            Int2ObjectMap<MsdfGlyph> glyphs = new Int2ObjectOpenHashMap<>(data.glyphs().size());
            data.glyphs().forEach(glyphData -> glyphs.put(glyphData.unicode(), new MsdfGlyph(glyphData, aWidth, aHeight)));
            Int2ObjectMap<Int2FloatMap> kernings = new Int2ObjectOpenHashMap<>();
            data.kernings().forEach(kerning -> {
               Int2FloatMap map = kernings.get(kerning.leftChar());
               if (map == null) {
                  map = new Int2FloatOpenHashMap();
                  kernings.put(kerning.leftChar(), map);
               }

               map.put(kerning.rightChar(), kerning.advance());
            });
            return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings);
         }
      }
   }
}
