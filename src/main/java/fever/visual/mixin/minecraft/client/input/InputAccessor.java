package fever.visual.mixin.minecraft.client.input;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
   @Accessor("movementVector")
   Vec2f getMovementVector();

   @Accessor("movementVector")
   void setMovementVector(Vec2f movement);

   @Accessor("playerInput")
   PlayerInput getInput();

   @Accessor("playerInput")
   void setInput(PlayerInput var1);
}
