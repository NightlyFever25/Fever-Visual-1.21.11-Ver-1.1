package fever.visual.mixin.minecraft.client.render.entity.feature;

import fever.visual.systems.modules.modules.visuals.Cape;
import fever.visual.utility.render.cape.WaveyCapeClothRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CapeFeatureRenderer.class, priority = 2000)
public abstract class CapeFeatureRendererMixin {

    @Shadow
    @Final
    private EquipmentModelLoader equipmentModelLoader;

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void fevervisual$renderWaveyCape(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            PlayerEntityRenderState state,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci
    ) {
        Cape module = Cape.getModule();
        if (module == null || !module.isEnabled() || !module.isClothPhysicsEnabled()) {
            return;
        }

        if (state == null || state.invisible || !state.capeVisible) {
            return;
        }

        SkinTextures skinTextures = state.skinTextures;
        if (skinTextures == null) {
            return;
        }

        Identifier capeTexture = skinTextures.cape() == null ? null : skinTextures.cape().texturePath();
        if (capeTexture == null) {
            return;
        }

        if (this.fevervisual$hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.WINGS)) {
            return;
        }

        AbstractClientPlayerEntity player = this.fevervisual$findPlayerById(state.id);
        if (player == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.player.getId() == state.id) {
                player = client.player;
            }
        }

        if (player == null || !module.shouldRenderForPlayer(player)) {
            return;
        }

        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        tickDelta = MathHelper.clamp(tickDelta, 0.0F, 1.0F);

        ci.cancel();
        matrices.push();
        if (this.fevervisual$hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.HUMANOID)) {
            matrices.translate(0.0F, -0.053125F, 0.06875F);
        }

        WaveyCapeClothRenderer.render(matrices, queue, light, state, capeTexture, module, player, tickDelta);
        matrices.pop();
    }

    @Unique
    private AbstractClientPlayerEntity fevervisual$findPlayerById(int id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.getId() == id) {
                return player;
            }
        }
        return null;
    }

    @Unique
    private boolean fevervisual$hasCustomModelForLayer(ItemStack stack, EquipmentModel.LayerType layerType) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) {
            return false;
        }
        EquipmentModel equipmentModel = this.equipmentModelLoader.get(equippable.assetId().get());
        return !equipmentModel.getLayers(layerType).isEmpty();
    }
}
