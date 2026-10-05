package fever.visual.mixin.minecraft.client.render.entity;

import fever.visual.FeverVisual;
import fever.visual.systems.event.RenderStateEntityCache;
import fever.visual.systems.modules.modules.visuals.BadTrip;
import fever.visual.systems.modules.modules.visuals.ChinaHatModule;
import fever.visual.systems.modules.modules.visuals.CustomModels;
import fever.visual.systems.modules.modules.visuals.FriendMarkers;
import fever.visual.systems.modules.modules.visuals.ModelChanger;
import fever.visual.systems.modules.modules.visuals.SelfTag;
import fever.visual.systems.modules.modules.visuals.cosmetic.CustomModelType;
import fever.visual.systems.modules.modules.visuals.cosmetic.CustomModelsRenderer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fever.visual.utility.interfaces.IMinecraft.mc;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Unique
   private static final String FEVER_VISUAL_RENDER_METHOD =
      "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;"
         + "Lnet/minecraft/client/util/math/MatrixStack;"
         + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
         + "Lnet/minecraft/client/render/state/CameraRenderState;)V";

   @Shadow
   @Nullable
   protected abstract RenderLayer getRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline);

   @Shadow
   protected M model;

   @Unique private static final FriendMarkers FRIEND_MARKERS_MODULE = FeverVisual.getInstance().getModuleManager().getModule(FriendMarkers.class);
   @Unique private static final ModelChanger MODEL_CHANGER_MODULE = FeverVisual.getInstance().getModuleManager().getModule(ModelChanger.class);
   @Unique private static final BadTrip BAD_TRIP_MODULE = FeverVisual.getInstance().getModuleManager().getModule(BadTrip.class);
   @Unique private static final CustomModels CUSTOM_MODELS_MODULE = FeverVisual.getInstance().getModuleManager().getModule(CustomModels.class);
   @Unique private static final ChinaHatModule CHINA_HAT_MODULE = FeverVisual.getInstance().getModuleManager().getModule(ChinaHatModule.class);

   @Unique private boolean fevervisual$scaleHead;
   @Unique private boolean fevervisual$scaleModel;
   @Unique private boolean fevervisual$badTrip;
   @Unique private boolean fevervisual$skipArmor;
   @Unique private boolean fevervisual$pushedTransform;
   @Unique private boolean fevervisual$chinaHatSubmitted;
   @Unique private float fevervisual$modelScale = 1.0F;
   @Unique private float fevervisual$modelOffset;
   @Unique private float fevervisual$badTripX = 1.0F;
   @Unique private float fevervisual$badTripY = 1.0F;
   @Unique private float fevervisual$badTripZ = 1.0F;
   @Unique private S fevervisual$currentState;

   @Inject(method = "updateRenderState", at = @At("TAIL"))
   private void fevervisual$storeEntity(LivingEntity entity, S state, float tickProgress, CallbackInfo ci) {
      RenderStateEntityCache.put(state, entity);
      if (entity == mc.player && SelfTag.isActive() && state.displayName == null && !mc.options.getPerspective().isFirstPerson()) {
         state.displayName = entity.getDisplayName();
         state.nameLabelPos = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw(tickProgress));
      }
   }

   @Inject(method = FEVER_VISUAL_RENDER_METHOD, at = @At("HEAD"))
   private void fevervisual$beginRender(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
      this.fevervisual$currentState = state;
      this.fevervisual$scaleHead = false;
      this.fevervisual$scaleModel = false;
      this.fevervisual$badTrip = false;
      this.fevervisual$skipArmor = false;
      this.fevervisual$pushedTransform = false;
      this.fevervisual$chinaHatSubmitted = false;
      this.fevervisual$modelScale = 1.0F;
      this.fevervisual$modelOffset = 0.0F;
      this.fevervisual$badTripX = 1.0F;
      this.fevervisual$badTripY = 1.0F;
      this.fevervisual$badTripZ = 1.0F;

      LivingEntity entity = RenderStateEntityCache.get(state);
      if (!(entity instanceof PlayerEntity player)) {
         return;
      }

      boolean self = player == mc.player;
      String name = player.getName().getString();
      boolean friend = FeverVisual.getInstance().getFriendManager().isFriend(name);
      boolean invisibleOtherPlayer = !self && this.fevervisual$isInvisible(player);
      this.fevervisual$scaleHead = FRIEND_MARKERS_MODULE != null
         && FRIEND_MARKERS_MODULE.isEnabled()
         && FRIEND_MARKERS_MODULE.getHeads() != null
         && FRIEND_MARKERS_MODULE.getHeads().isSelected()
         && friend
         && !invisibleOtherPlayer;

      if (MODEL_CHANGER_MODULE != null && MODEL_CHANGER_MODULE.isEnabled() && !invisibleOtherPlayer) {
         this.fevervisual$modelScale = MODEL_CHANGER_MODULE.getScaleForPlayer(name, self);
         this.fevervisual$scaleModel = this.fevervisual$modelScale != 1.0F;
         this.fevervisual$modelOffset = (1.0F - this.fevervisual$modelScale) * 1.5F;
      }

      if (BAD_TRIP_MODULE != null && BAD_TRIP_MODULE.isEnabled() && BAD_TRIP_MODULE.shouldAffectPlayer(player)) {
         this.fevervisual$badTrip = true;
         BAD_TRIP_MODULE.updateScales();
         this.fevervisual$badTripX = BadTrip.getCurrentScaleX();
         this.fevervisual$badTripY = BadTrip.getCurrentScaleY();
         this.fevervisual$badTripZ = BadTrip.getCurrentScaleZ();
      }

      this.fevervisual$skipArmor = CUSTOM_MODELS_MODULE != null
         && CUSTOM_MODELS_MODULE.isEnabled()
         && CUSTOM_MODELS_MODULE.shouldApplyTo(player)
         && (self || friend);
   }

   @ModifyExpressionValue(
      method = FEVER_VISUAL_RENDER_METHOD,
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;")
   )
   private RenderLayer fevervisual$renderLayer(RenderLayer original) {
      LivingEntity entity = RenderStateEntityCache.get(this.fevervisual$currentState);
      CustomModelType type = entity != null && CUSTOM_MODELS_MODULE != null && CUSTOM_MODELS_MODULE.shouldApplyTo(entity)
         ? CUSTOM_MODELS_MODULE.getSelectedTypeFor(entity)
         : null;
      if (type != null) {
         Identifier texture = type.getTexture();
         return RenderLayers.entityTranslucent(texture, true);
      }
      return original;
   }

   @Redirect(
      method = FEVER_VISUAL_RENDER_METHOD,
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V")
   )
   @SuppressWarnings({"rawtypes", "unchecked"})
   private void fevervisual$renderFeature(FeatureRenderer feature, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, EntityRenderState state, float limbAngle, float limbDistance) {
      if (!this.fevervisual$skipArmor || !(feature instanceof ArmorFeatureRenderer || feature instanceof ElytraFeatureRenderer)) {
         feature.render(matrices, queue, light, state, limbAngle, limbDistance);
      }
   }

   @Inject(
      method = FEVER_VISUAL_RENDER_METHOD,
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;setupTransforms(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V")
   )
   private void fevervisual$applyTransform(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
      if (!this.fevervisual$scaleModel && !this.fevervisual$badTrip) {
         return;
      }

      float x = this.fevervisual$modelScale * this.fevervisual$badTripX;
      float y = this.fevervisual$modelScale * this.fevervisual$badTripY;
      float z = this.fevervisual$modelScale * this.fevervisual$badTripZ;
      float offset = this.fevervisual$modelOffset;
      if (this.fevervisual$badTrip && this.fevervisual$badTripY != 1.0F) {
         offset += (1.0F - this.fevervisual$badTripY) * 0.8F;
      }

      matrices.push();
      matrices.translate(0.0F, -offset, 0.0F);
      matrices.scale(x, y, z);
      matrices.translate(0.0F, offset / y, 0.0F);
      this.fevervisual$pushedTransform = true;
      if (MODEL_CHANGER_MODULE != null && this.fevervisual$scaleModel) {
         ModelChanger.setCurrentScale(this.fevervisual$modelScale);
         ModelChanger.setCurrentOffset(this.fevervisual$modelOffset);
      }
   }

   @Redirect(
      method = FEVER_VISUAL_RENDER_METHOD,
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V")
   )
   @SuppressWarnings({"unchecked", "rawtypes"})
   private void fevervisual$submitModel(
      OrderedRenderCommandQueue queue,
      Model model,
      Object state,
      MatrixStack matrices,
      RenderLayer layer,
      int light,
      int overlay,
      int color,
      Sprite sprite,
      int outlineColor,
      ModelCommandRenderer.CrumblingOverlayCommand crumbling
   ) {
      LivingEntity entity = RenderStateEntityCache.get(this.fevervisual$currentState);
      CustomModelType customType = entity != null && CUSTOM_MODELS_MODULE != null && CUSTOM_MODELS_MODULE.shouldApplyTo(entity)
         ? CUSTOM_MODELS_MODULE.getSelectedTypeFor(entity)
         : null;
      boolean enlargedHead = this.fevervisual$scaleHead;
      if (customType == null && !enlargedHead) {
         queue.submitModel(model, state, matrices, layer, light, overlay, color, sprite, outlineColor, crumbling);
         this.fevervisual$renderChinaHat(queue, model, state, matrices, layer, light);
         return;
      }

      queue.submitCustom(matrices, layer, (entry, vertices) -> {
         MatrixStack renderMatrices = new MatrixStack();
         renderMatrices.multiplyPositionMatrix(entry.getPositionMatrix());
         EntityModel entityModel = (EntityModel)model;
         entityModel.setAngles(state);
         ModelPart head = entityModel instanceof BipedEntityModel<?> biped ? biped.head : null;
         float oldX = head == null ? 1.0F : head.xScale;
         float oldY = head == null ? 1.0F : head.yScale;
         float oldZ = head == null ? 1.0F : head.zScale;
         if (head != null && enlargedHead) {
            head.xScale *= 1.5F;
            head.yScale *= 1.5F;
            head.zScale *= 1.5F;
         }

         boolean renderedCustom = customType != null
            && CustomModelsRenderer.render(customType, entityModel, renderMatrices, vertices, light, overlay, color);
         if (!renderedCustom) {
            entityModel.render(renderMatrices, vertices, light, overlay, color);
         }
         if (head != null) {
            head.xScale = oldX;
            head.yScale = oldY;
            head.zScale = oldZ;
         }
      });
      this.fevervisual$renderChinaHat(queue, model, state, matrices, layer, light);
   }

   @Unique
   @SuppressWarnings({"rawtypes", "unchecked"})
   private void fevervisual$renderChinaHat(
      OrderedRenderCommandQueue queue,
      Model model,
      Object state,
      MatrixStack matrices,
      RenderLayer layer,
      int light
   ) {
      LivingEntity entity = RenderStateEntityCache.get(this.fevervisual$currentState);
      if (!(entity instanceof AbstractClientPlayerEntity player)
         || CHINA_HAT_MODULE == null
         || !CHINA_HAT_MODULE.shouldRenderForPlayer(player)
         || !(model instanceof EntityModel entityModel)
         || !(entityModel instanceof ModelWithHead headModel)) {
         return;
      }

      queue.submitCustom(matrices, layer, (entry, vertices) -> {
         MatrixStack renderMatrices = new MatrixStack();
         renderMatrices.multiplyPositionMatrix(entry.getPositionMatrix());
         entityModel.setAngles(state);
         entityModel.getRootPart().applyTransform(renderMatrices);
         headModel.applyTransform(renderMatrices);
         CHINA_HAT_MODULE.renderHeadAnchored(player, renderMatrices);
      });
      this.fevervisual$chinaHatSubmitted = true;
   }

   @Inject(method = FEVER_VISUAL_RENDER_METHOD, at = @At("RETURN"))
   private void fevervisual$finishRender(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
      this.fevervisual$renderInvisibleSelfChinaHat(state, matrices, queue);
      if (this.fevervisual$pushedTransform) {
         matrices.pop();
      }
      if (MODEL_CHANGER_MODULE != null && this.fevervisual$scaleModel) {
         ModelChanger.setCurrentScale(1.0F);
         ModelChanger.setCurrentOffset(0.0F);
      }
      this.fevervisual$currentState = null;
      this.fevervisual$skipArmor = false;
      this.fevervisual$pushedTransform = false;
      this.fevervisual$chinaHatSubmitted = false;
   }

   @Unique
   @SuppressWarnings({"rawtypes", "unchecked"})
   private void fevervisual$renderInvisibleSelfChinaHat(S state, MatrixStack matrices, OrderedRenderCommandQueue queue) {
      if (this.fevervisual$chinaHatSubmitted
         || CHINA_HAT_MODULE == null
         || this.model == null
         || !(this.model instanceof ModelWithHead headModel)
         || !(RenderStateEntityCache.get(state) instanceof AbstractClientPlayerEntity player)
         || player != mc.player
         || !CHINA_HAT_MODULE.shouldRenderForPlayer(player)) {
         return;
      }

      RenderLayer layer = this.getRenderLayer(state, true, true, false);
      if (layer == null) {
         layer = RenderLayers.entityTranslucent(FeverVisual.id("textures/particles/triangle.png"), true);
      }

      EntityModel entityModel = this.model;
      queue.submitCustom(matrices, layer, (entry, vertices) -> {
         MatrixStack renderMatrices = new MatrixStack();
         renderMatrices.multiplyPositionMatrix(entry.getPositionMatrix());
         entityModel.setAngles(state);
         entityModel.getRootPart().applyTransform(renderMatrices);
         headModel.applyTransform(renderMatrices);
         CHINA_HAT_MODULE.renderHeadAnchored(player, renderMatrices);
      });
      this.fevervisual$chinaHatSubmitted = true;
   }

   @Unique
   private boolean fevervisual$isInvisible(LivingEntity entity) {
      if (entity.hasStatusEffect(StatusEffects.INVISIBILITY)) {
         return true;
      }
      if (entity.isInvisible()) {
         return true;
      }
      return mc.player != null && entity.isInvisibleTo(mc.player);
   }
}
