package fever.visual.utility.render.female;

import fever.visual.FeverVisual;
import fever.visual.systems.event.RenderStateEntityCache;
import fever.visual.systems.modules.modules.visuals.FemaleGender;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class FemaleGenderArmorLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final float DEG_TO_RAD = (float)(Math.PI / 180.0D);

   private final EquipmentModelLoader equipmentModelLoader;
   private WildfireModelRenderer.BreastModelBox leftArmorBreast =
      new WildfireModelRenderer.BreastModelBox(64, 32, 16, 17, -4.0F, 0.0F, 0.0F, 4, 5, 3, 0.0F, false);
   private WildfireModelRenderer.BreastModelBox rightArmorBreast =
      new WildfireModelRenderer.BreastModelBox(64, 32, 20, 17, 0.0F, 0.0F, 0.0F, 4, 5, 3, 0.0F, false);
   private float previousDepthKey;

   public FemaleGenderArmorLayer(
      FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context,
      EquipmentModelLoader equipmentModelLoader
   ) {
      super(context);
      this.equipmentModelLoader = equipmentModelLoader;
   }

   @Override
   public void render(
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      int light,
      PlayerEntityRenderState state,
      float limbAngle,
      float limbDistance
   ) {
      FemaleGender module = FeverVisual.getInstance().getModuleManager() == null
         ? null
         : FeverVisual.getInstance().getModuleManager().getModuleSafe(FemaleGender.class);
      if (module == null || !module.isEnabled()) {
         return;
      }

      if (state.invisible && !state.hasOutline()) {
         return;
      }

      ItemStack chestStack = state.equippedChestStack;
      if (chestStack == null || chestStack.isEmpty()) {
         return;
      }

      EquippableComponent equippable = chestStack.get(DataComponentTypes.EQUIPPABLE);
      if (equippable == null || equippable.slot() != EquipmentSlot.CHEST || equippable.assetId().isEmpty()) {
         return;
      }

      PlayerEntity player = this.fevervisual$getRenderedPlayer(state);
      if (player == null) {
         return;
      }

      float baseSize = Math.clamp(module.getSizeFor(player), 0.0F, 1.6F);
      if (baseSize <= 0.01F) {
         return;
      }

      float breastOffsetX = 0.0F;
      float breastOffsetY = 0.0F;
      float breastOffsetZ = 0.0F;
      float outwardAngle = 8.0F;
      float zOffset = 0.0625F - baseSize * 0.0625F;
      float breastSize = Math.min(baseSize * 1.5F, 0.7F);
      if (baseSize > 0.7F) {
         breastSize = baseSize;
      }
      breastSize += 0.5F * Math.abs(baseSize - 0.7F) * 2.0F;

      this.fevervisual$resizeBox(baseSize);

      int dyeColor = chestStack.isIn(ItemTags.DYEABLE) ? DyedColorComponent.getColor(chestStack, -1) : -1;
      boolean glint = chestStack.hasGlint();
      RegistryKey<EquipmentAsset> asset = equippable.assetId().get();

      for (EquipmentModel.Layer layer : this.equipmentModelLoader.get(asset).getLayers(EquipmentModel.LayerType.HUMANOID)) {
         int layerColor = layer.dyeable().map((dye) -> {
            int defaultColor = dye.colorWhenUndyed().map(ColorHelper::fullAlpha).orElse(-1);
            return dyeColor != -1 ? dyeColor : defaultColor;
         }).orElse(-1);

         RenderLayer armorLayer = RenderLayers.armorCutoutNoCull(layer.getFullTextureId(EquipmentModel.LayerType.HUMANOID));
         int color = ColorHelper.fullAlpha(layerColor);
         this.fevervisual$submitBreasts(
            queue, matrices, armorLayer, light, color, state, breastSize, zOffset, outwardAngle,
            breastOffsetX, breastOffsetY, breastOffsetZ
         );
         if (glint) {
            this.fevervisual$submitBreasts(
               queue, matrices, RenderLayers.armorEntityGlint(), light, -1, state, breastSize, zOffset, outwardAngle,
               breastOffsetX, breastOffsetY, breastOffsetZ
            );
         }
      }
   }

   private void fevervisual$submitBreasts(
      OrderedRenderCommandQueue queue,
      MatrixStack matrices,
      RenderLayer layer,
      int light,
      int color,
      PlayerEntityRenderState state,
      float breastSize,
      float zOffset,
      float outwardAngle,
      float breastOffsetX,
      float breastOffsetY,
      float breastOffsetZ
   ) {
      WildfireModelRenderer.ModelBox capturedLeft = this.leftArmorBreast;
      WildfireModelRenderer.ModelBox capturedRight = this.rightArmorBreast;
      queue.submitCustom(matrices, layer, (entry, consumer) -> {
         MatrixStack renderMatrices = new MatrixStack();
         renderMatrices.multiplyPositionMatrix(entry.getPositionMatrix());
         this.getContextModel().setAngles(state);
         this.fevervisual$renderBreastSide(
            capturedLeft, renderMatrices, consumer, light, OverlayTexture.DEFAULT_UV, color, state, true,
            breastSize, zOffset, outwardAngle, breastOffsetX, breastOffsetY, breastOffsetZ
         );
         this.fevervisual$renderBreastSide(
            capturedRight, renderMatrices, consumer, light, OverlayTexture.DEFAULT_UV, color, state, false,
            breastSize, zOffset, outwardAngle, breastOffsetX, breastOffsetY, breastOffsetZ
         );
      });
   }

   private void fevervisual$resizeBox(float breastSize) {
      float reducer = -1.0F;
      if (breastSize < 0.84F) {
         ++reducer;
      }
      if (breastSize < 0.72F) {
         ++reducer;
      }

      float depthKey = 4.0F - reducer;
      if (this.previousDepthKey != depthKey) {
         int depth = (int)depthKey;
         this.leftArmorBreast = new WildfireModelRenderer.BreastModelBox(64, 32, 16, 17, -4.0F, 0.0F, 0.0F, 4, 5, depth, 0.0F, false);
         this.rightArmorBreast = new WildfireModelRenderer.BreastModelBox(64, 32, 20, 17, 0.0F, 0.0F, 0.0F, 4, 5, depth, 0.0F, false);
         this.previousDepthKey = depthKey;
      }
   }

   private void fevervisual$renderBreastSide(
      WildfireModelRenderer.ModelBox breast,
      MatrixStack matrices,
      VertexConsumer consumer,
      int light,
      int overlay,
      int color,
      PlayerEntityRenderState state,
      boolean left,
      float breastSize,
      float zOffset,
      float outwardAngle,
      float breastOffsetX,
      float breastOffsetY,
      float breastOffsetZ
   ) {
      matrices.push();
      PlayerEntityModel model = this.getContextModel();
      if (state.baby) {
         matrices.scale(state.ageScale, state.ageScale, state.ageScale);
         matrices.translate(0.0F, 0.75F, 0.0F);
      }

      ModelPart body = model.body;
      matrices.translate(body.originX * 0.0625F, body.originY * 0.0625F, body.originZ * 0.0625F);
      if (body.roll != 0.0F || body.yaw != 0.0F || body.pitch != 0.0F) {
         matrices.multiply((new Quaternionf()).rotationZYX(body.roll, body.yaw, body.pitch));
      }

      matrices.translate((left ? breastOffsetX : -breastOffsetX) * 0.0625F, 0.05625F + breastOffsetY * 0.0625F, zOffset - 0.125F + breastOffsetZ * 0.0625F);
      matrices.translate(-0.125F * (left ? 1.0F : -1.0F), 0.0F, 0.0F);
      matrices.multiply((new Quaternionf())
         .rotationY((left ? outwardAngle : -outwardAngle) * DEG_TO_RAD)
         .rotateX(-35.0F * breastSize * DEG_TO_RAD));
      matrices.translate(0.125F * (left ? 1.0F : -1.0F), 0.0F, 0.0F);

      matrices.translate(left ? 0.001F : -0.001F, 0.015F, -0.015F);
      matrices.scale(1.05F, 1.0F, 1.0F);

      fevervisual$renderBox(breast, matrices, consumer, light, overlay, color);
      matrices.pop();
   }

   @Nullable
   private PlayerEntity fevervisual$getRenderedPlayer(PlayerEntityRenderState state) {
      if (RenderStateEntityCache.get(state) instanceof PlayerEntity player) {
         return player;
      }
      return null;
   }

   private static void fevervisual$renderBox(
      WildfireModelRenderer.ModelBox model,
      MatrixStack matrices,
      VertexConsumer consumer,
      int light,
      int overlay,
      int color
   ) {
      Matrix4f position = matrices.peek().getPositionMatrix();
      Matrix3f normal = matrices.peek().getNormalMatrix();

      for (WildfireModelRenderer.TexturedQuad quad : model.quads) {
         Vector3f transformedNormal = (new Vector3f(quad.normal.x, quad.normal.y, quad.normal.z)).mul(normal);
         float normalX = transformedNormal.x;
         float normalY = transformedNormal.y;
         float normalZ = transformedNormal.z;

         for (WildfireModelRenderer.PositionTextureVertex vertex : quad.vertexPositions) {
            Vector4f pos = (new Vector4f(vertex.x() / 16.0F, vertex.y() / 16.0F, vertex.z() / 16.0F, 1.0F)).mul(position);
            consumer.vertex(pos.x(), pos.y(), pos.z(), color, vertex.u(), vertex.v(), overlay, light, normalX, normalY, normalZ);
         }
      }
   }
}
