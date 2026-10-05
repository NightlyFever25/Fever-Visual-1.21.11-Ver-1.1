package fever.visual.mixin.minecraft.text;

import fever.visual.FeverVisual;
import fever.visual.systems.modules.modules.other.NameProtect;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
   @ModifyArg(
      method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
      index = 0,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
         ordinal = 0
      )
   )
   private static String patchName(String text) {
      NameProtect nameProtectModule = FeverVisual.getInstance().getModuleManager().getModule(NameProtect.class);
      // LabyMod formats HUD text during its early startup, before FeverVisual
      // has necessarily registered every module. Preserve vanilla text until
      // NameProtect becomes available instead of crashing the caller.
      return nameProtectModule != null && nameProtectModule.isEnabled()
         ? nameProtectModule.patchName(text)
         : text;
   }
}
