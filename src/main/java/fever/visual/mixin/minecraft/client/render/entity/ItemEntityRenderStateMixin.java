package fever.visual.mixin.minecraft.client.render.entity;

import fever.visual.utility.mixins.ItemEntityRenderStateAddition;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements ItemEntityRenderStateAddition {
   @Unique
   private boolean fevervisual$onGround;

   @Unique
   @Override
   public void fevervisual$setOnGround(boolean onGround) {
      this.fevervisual$onGround = onGround;
   }

   @Unique
   @Override
   public boolean fevervisual$isOnGround() {
      return this.fevervisual$onGround;
   }
}
