package fever.visual.systems.modules.modules.visuals;

import fever.visual.framework.base.CustomDrawContext;
import fever.visual.framework.msdf.Font;
import fever.visual.framework.msdf.Fonts;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.HudRenderEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.SelectSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.game.EntityVisibility;
import fever.visual.framework.objects.BorderRadius;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Name Tags", category = ModuleCategory.VISUALS, desc = "Custom projected entity name tags")
public class NameTags extends BaseModule {
    private static NameTags instance;

    private final SelectSetting targets = new SelectSetting(this, "Targets").min(1);
    private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "Players").select();
    private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "Mobs");
    private final SelectSetting.Value self = new SelectSetting.Value(this.targets, "Self");
    private final BooleanSetting health = new BooleanSetting(this, "Health").enable();
    private final BooleanSetting background = new BooleanSetting(this, "Background").enable();
    private final SliderSetting scale = new SliderSetting(this, "Scale").min(0.65F).max(1.6F).step(0.05F).currentValue(1.0F);
    private final SliderSetting distance = new SliderSetting(this, "Distance").min(8.0F).max(192.0F).step(4.0F).currentValue(96.0F);
    private final List<Tag> tags = new ArrayList<>();
    private long projectedAt;

    public NameTags() {
        instance = this;
    }

    private final EventListener<Render3DEvent> onWorldRender = event -> {
        this.tags.clear();
        if (mc.player == null || mc.world == null || event.getCamera() == null) return;
        this.projectedAt = System.currentTimeMillis();
        Matrix4f viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(event.getPositionMatrix());
        Vec3d camera = event.getCamera().getCameraPos();
        float tickDelta = event.getTickDelta();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !this.shouldRender(living)) continue;
            if (living.squaredDistanceTo(mc.player) > this.distance.getCurrentValue() * this.distance.getCurrentValue()) continue;
            if (!EntityVisibility.isRenderedAndVisible(living, tickDelta)) continue;
            double x = MathHelper.lerp(tickDelta, living.lastRenderX, living.getX()) - camera.x;
            double y = MathHelper.lerp(tickDelta, living.lastRenderY, living.getY()) + living.getHeight() + 0.35D - camera.y;
            double z = MathHelper.lerp(tickDelta, living.lastRenderZ, living.getZ()) - camera.z;
            Vector4f clip = new Vector4f((float)x, (float)y, (float)z, 1.0F);
            viewProjection.transform(clip);
            if (clip.w <= 0.001F) continue;
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            if (Math.abs(ndcX) > 1.15F || Math.abs(ndcY) > 1.15F) continue;
            float screenX = (ndcX * 0.5F + 0.5F) * mc.getWindow().getScaledWidth();
            float screenY = (0.5F - ndcY * 0.5F) * mc.getWindow().getScaledHeight();
            this.tags.add(new Tag(living, screenX, screenY));
        }
    };

    private final EventListener<HudRenderEvent> onHudRender = event -> {
        if (System.currentTimeMillis() - this.projectedAt > 100L || mc.world == null || mc.player == null) {
            this.tags.clear();
            return;
        }
        CustomDrawContext context = event.getContext();
        float tagScale = this.scale.getCurrentValue();
        Font font = Fonts.MEDIUM.getFont(7.0F * tagScale);
        for (Tag tag : this.tags) {
            String name = tag.entity.getDisplayName().getString();
            String suffix = this.health.isEnabled() ? "  " + Math.round(tag.entity.getHealth() * 10.0F) / 10.0F + " HP" : "";
            String text = name + suffix;
            float textWidth = font.width(text);
            float width = textWidth + 6.0F * tagScale;
            float height = font.height() + 4.0F * tagScale;
            float x = tag.x - width * 0.5F;
            float y = tag.y - height;
            if (this.background.isEnabled()) {
                context.drawRoundedRect(x, y, width, height, BorderRadius.all(3.0F), new ColorRGBA(12, 14, 18, 190));
                context.drawRoundedBorder(x, y, width, height, 0.7F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, 35));
            }
            ColorRGBA color = tag.entity.hurtTime > 0 ? new ColorRGBA(255, 115, 115, 255) : ColorRGBA.WHITE;
            context.drawText(font, text, tag.x - textWidth * 0.5F, y + 2.0F * tagScale, color);
        }
    };

    private boolean shouldRender(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved() || isInvisible(entity)) return false;
        if (entity == mc.player) return this.self.isSelected() && mc.options.getPerspective() != Perspective.FIRST_PERSON;
        if (entity instanceof PlayerEntity) return this.players.isSelected() && !entity.isSpectator();
        return entity instanceof MobEntity && this.mobs.isSelected() && !entity.isSpectator();
    }

    private static boolean isInvisible(LivingEntity entity) {
        return entity.hasStatusEffect(StatusEffects.INVISIBILITY) || entity.isInvisible()
                || mc.player != null && entity.isInvisibleTo(mc.player);
    }

    public static boolean shouldHideVanilla(Entity entity) {
        NameTags module = instance;
        return module != null && module.isEnabled() && entity instanceof LivingEntity living
                && module.shouldRender(living)
                && EntityVisibility.isRenderedAndVisible(living, mc.getRenderTickCounter().getTickProgress(false));
    }

    @Override
    public void onDisable() {
        this.tags.clear();
        this.projectedAt = 0L;
    }

    private record Tag(LivingEntity entity, float x, float y) {
    }
}
