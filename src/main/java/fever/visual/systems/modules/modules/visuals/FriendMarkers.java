//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)
package fever.visual.systems.modules.modules.visuals;


import lombok.Generated;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.render.CrystalRenderer;
import fever.visual.utility.render.RenderUtility;
import fever.visual.utility.render.Utils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import fever.visual.utility.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Friend Markers", desc = "Выделяет друзей", category = ModuleCategory.DISPLAY)
public class FriendMarkers extends BaseModule {

    private final ModeSetting setting = new ModeSetting(this, "modules.settings.friends_markers.setting");
    private final ModeSetting.Value heads = new ModeSetting.Value(this.setting, "modules.settings.friends_markers.heads");
    private final ModeSetting.Value sims = new ModeSetting.Value(this.setting, "modules.settings.friends_markers.sims");

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (sims.isSelected()) {
            RenderUtility.setupRender3D(true);
            MatrixStack ms = event.getMatrices();
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getCameraPos();
            ColorRGBA color = new ColorRGBA(52.0F, 199.0F, 89.0F);
            BufferBuilder builder = CrystalRenderer.createBuffer();

            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                if (!FeverVisual.getInstance().getFriendManager().isFriend(player.getName().getString())) {
                    continue;
                }

                if (isInvisible(player)) {
                    continue;
                }

                ms.push();
                RenderUtility.prepareMatrices(ms, Utils.getInterpolatedPos(player, event.getTickDelta()));
                float size = 0.1F;
                CrystalRenderer.render(ms, builder, 0.0F, player.getHeight() + 0.4F, 0.0F, size, color.withAlpha(255.0F));
                ms.pop();
            }

            BuiltBuffer built = builder.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }

            RenderUtility.endRender3D();
        }
    };

    private boolean isInvisible(AbstractClientPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            return true;
        }
        if (player.isInvisible()) {
            return true;
        }
        if (mc.player != null && player.isInvisibleTo(mc.player)) {
            return true;
        }
        return false;
    }

    @Generated
    public ModeSetting.Value getHeads() {
        return this.heads;
    }
}
