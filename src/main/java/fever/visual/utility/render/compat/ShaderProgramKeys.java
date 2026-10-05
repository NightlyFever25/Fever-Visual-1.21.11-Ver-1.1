package fever.visual.utility.render.compat;

import net.minecraft.util.Identifier;
public final class ShaderProgramKeys {
   public static final Key POSITION_COLOR = new Key(Identifier.ofVanilla("core/position_color"), false);
   public static final Key POSITION_TEX = new Key(Identifier.ofVanilla("core/position_tex"), true);
   public static final Key POSITION_TEX_COLOR = new Key(Identifier.ofVanilla("core/position_tex_color"), true);

   private ShaderProgramKeys() {
   }

   public record Key(Identifier shader, boolean textured) {
   }
}
