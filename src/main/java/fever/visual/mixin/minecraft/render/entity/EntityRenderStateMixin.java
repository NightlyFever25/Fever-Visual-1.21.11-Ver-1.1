package fever.visual.mixin.minecraft.render.entity;

import fever.visual.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements EntityRenderStateAddition {
   @Unique
   private Entity fevervisual$entity;

   @Unique
   @Override
   public void fevervisual$setEntity(Entity entity) {
      this.fevervisual$entity = entity;
   }

   @Unique
   @Override
   public Entity fevervisual$getEntity() {
      return this.fevervisual$entity;
   }
}
