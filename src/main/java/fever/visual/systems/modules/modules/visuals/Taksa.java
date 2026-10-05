package fever.visual.systems.modules.modules.visuals;

import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.AttackEvent;
import fever.visual.systems.event.impl.player.ClientPlayerTickEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.modules.modules.visuals.littlePet.TaksaBrain;
import fever.visual.systems.modules.modules.visuals.littlePet.TaksaModel;
import fever.visual.utility.interfaces.IMinecraft;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Taksa", category = ModuleCategory.VISUALS, desc = "modules.descriptions.taksa")
public class Taksa extends BaseModule implements IMinecraft {

    private final TaksaBrain brain = new TaksaBrain();
    private final TaksaModel model = new TaksaModel();

    private final EventListener<AttackEvent> onAttack = event -> {
        if (event.getEntity() instanceof LivingEntity living) {
            this.brain.setAttackTarget(living);
        }
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.player == null || mc.world == null) return;
        this.brain.setEntity(mc.player);
        this.brain.tick();
    };

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (mc.player == null || mc.world == null) return;

        Vec3d petPos = this.brain.getPos();
        Vec3d cam = event.getCamera().getCameraPos();
        MatrixStack matrices = event.getMatrices();

        matrices.push();
        matrices.translate(petPos.x - cam.x, petPos.y - cam.y, petPos.z - cam.z);
        int light = LightmapTextureManager.pack(
                mc.world.getLightLevel(net.minecraft.world.LightType.BLOCK, BlockPos.ofFloored(petPos)),
                mc.world.getLightLevel(net.minecraft.world.LightType.SKY,   BlockPos.ofFloored(petPos))
        );
        int overlay = net.minecraft.client.render.OverlayTexture.DEFAULT_UV;
        RenderLayer layer = RenderLayers.entityCutoutNoCull(TaksaModel.TEXTURE);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        var vc = immediate.getBuffer(layer);

        this.model.render(matrices, vc, light, overlay, this.brain,
                (float) mc.player.age + event.getTickDelta());

        immediate.draw(layer);
        matrices.pop();
    };

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) this.brain.setEntity(mc.player);
    }

    @Override
    public void onDisable() {
        this.brain.setEntity(null);
        this.brain.setAttackTarget(null);
        super.onDisable();
    }
}
