package fever.visual.mixin.minecraft.client.render.entity;

import fever.visual.utility.render.female.FemaleGenderArmorLayer;
import fever.visual.utility.render.female.FemaleGenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class FemaleGenderPlayerEntityRendererMixin extends LivingEntityRenderer<PlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
   private FemaleGenderPlayerEntityRendererMixin(EntityRendererFactory.Context context, PlayerEntityModel model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @SuppressWarnings("unchecked")
   @Inject(method = "<init>", at = @At("TAIL"))
   private void fevervisual$addFemaleGenderLayer(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
      this.addFeature(new FemaleGenderLayer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this));
      this.addFeature(new FemaleGenderArmorLayer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this, context.getEquipmentModelLoader()));
   }
}
