package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.game.EntityJumpEvent;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.interfaces.IMinecraft;
import fever.visual.utility.math.MathUtility;
import net.minecraft.block.BlockState;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(name = "Jump Effect", category = ModuleCategory.VISUALS, desc = "modules.descriptions.jump_effect")
public class JumpEffect extends BaseModule implements IMinecraft {
    private static final int MAX_ACTIVE_WAVES = 3;

    private final List<WaveEffect> waveEffects = new ArrayList<>();

    private final SliderSetting radius = new SliderSetting(this, "modules.settings.jump_effect.radius")
            .min(2.0f).max(8.0f).step(0.5f).currentValue(4.0f);

    private final SliderSetting speed = new SliderSetting(this, "modules.settings.jump_effect.speed")
            .min(300.0f).max(2000.0f).step(50.0f).currentValue(800.0f);

    private final BooleanSetting useTheme = new BooleanSetting(this, "modules.settings.jump_effect.use_theme").enable();

    private final ColorSetting color1 = new ColorSetting(this, "modules.settings.jump_effect.color1", () -> this.useTheme.isEnabled())
            .color(new ColorRGBA(100, 200, 255, 255));

    private final ColorSetting color2 = new ColorSetting(this, "modules.settings.jump_effect.color2", () -> this.useTheme.isEnabled())
            .color(new ColorRGBA(255, 100, 200, 255));

    public JumpEffect() {
    }

    private final EventListener<EntityJumpEvent> onJump = event -> {
        if (mc.player == null || event.getEntity() != mc.player) return;

        BlockPos pos = mc.player.getBlockPos().down();
        if (waveEffects.size() >= MAX_ACTIVE_WAVES) {
            waveEffects.remove(0);
        }
        waveEffects.add(new WaveEffect(pos, System.currentTimeMillis()));
    };

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (waveEffects.isEmpty() || mc.world == null) return;

        Iterator<WaveEffect> iterator = waveEffects.iterator();
        while (iterator.hasNext()) {
            WaveEffect wave = iterator.next();
            if (wave.isExpired()) {
                iterator.remove();
                continue;
            }
            wave.render(event);
        }
    };

    private class WaveEffect {
        private final BlockPos centerPos;
        private final long startTime;
        private final long duration;
        private final int maxRadius;

        public WaveEffect(BlockPos centerPos, long startTime) {
            this.centerPos = centerPos;
            this.startTime = startTime;
            this.duration = (long) speed.getCurrentValue();
            this.maxRadius = (int) Math.ceil(radius.getCurrentValue());
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > duration;
        }

        public void render(Render3DEvent event) {
            if (mc.world == null) return;

            long elapsed = System.currentTimeMillis() - startTime;
            float progress = (float) elapsed / duration;
            float currentRadius = easeOutCubic(progress) * maxRadius;
            ColorRGBA waveColor1 = getWaveColor1();
            ColorRGBA waveColor2 = getWaveColor2();
            float fadeInDuration = 0.15f;
            float fadeOutStart = 0.75f;
            float globalAlpha;

            if (progress < fadeInDuration) {
                globalAlpha = progress / fadeInDuration;
            } else if (progress >= fadeOutStart) {
                float fadeOutProgress = (progress - fadeOutStart) / (1f - fadeOutStart);
                globalAlpha = 1f - easeInCubic(fadeOutProgress);
            } else {
                globalAlpha = 1f;
            }

            MatrixStack matrices = event.getMatrices();
            Camera camera = event.getCamera();
            Vec3d cameraPos = camera.getCameraPos();

            matrices.push();

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            int rendered = 0;
            int maxPerFrame = 400;

            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int z = -maxRadius; z <= maxRadius; z++) {
                    if (rendered >= maxPerFrame) break;

                    BlockPos blockPos = centerPos.add(x, 0, z);
                    double distanceFromCenter = Math.sqrt(x * x + z * z);

                    if (distanceFromCenter > currentRadius + 0.5f) continue;
                    if (distanceFromCenter < currentRadius - 2.5f) continue;

                    BlockState state = mc.world.getBlockState(blockPos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getOutlineShape(mc.world, blockPos);
                    if (shape.isEmpty()) continue;

                    rendered++;

                    float waveProgress = (float) (distanceFromCenter / maxRadius);
                    float localAlpha = 1.0f - Math.abs((float)distanceFromCenter - currentRadius) / 2.5f;
                    localAlpha = Math.max(0, Math.min(1, localAlpha));

                    float pulseOffset = waveProgress * 2f;
                    float pulse = (float) Math.sin((progress * Math.PI * 4) - pulseOffset);
                    pulse = (pulse + 1f) / 2f;
                    localAlpha *= (0.5f + pulse * 0.5f);
                    localAlpha *= globalAlpha;

                    if (localAlpha > 0.02f) {
                        int gradientColor = getGradientColor(
                                waveColor1,
                                waveColor2,
                                waveProgress
                        );

                        ColorRGBA finalColor = ColorRGBA.fromInt(gradientColor).withAlpha(localAlpha * 180);

                        try {
                            drawBlockShape(matrices, buffer, blockPos, shape, finalColor, cameraPos);
                        } catch (Exception ignored) {}
                    }
                }
            }
            BuiltBuffer builtBuffer = buffer.endNullable();
            if (builtBuffer != null) {
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }

            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            matrices.pop();
        }

        private void drawBlockShape(MatrixStack matrices, BufferBuilder buffer, BlockPos pos,
                                    VoxelShape shape, ColorRGBA color, Vec3d cameraPos) {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            double renderX = pos.getX() - cameraPos.getX();
            double renderY = pos.getY() - cameraPos.getY();
            double renderZ = pos.getZ() - cameraPos.getZ();
            shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                float x1 = (float)(renderX + minX);
                float y1 = (float)(renderY + minY);
                float z1 = (float)(renderZ + minZ);
                float x2 = (float)(renderX + maxX);
                float y2 = (float)(renderY + maxY);
                float z2 = (float)(renderZ + maxZ);
                int rgb = color.getRGB();
                float offset = 0.002f;
                y2 += offset;
                buffer.vertex(matrix, x1, y2, z1).color(rgb);
                buffer.vertex(matrix, x1, y2, z2).color(rgb);
                buffer.vertex(matrix, x2, y2, z2).color(rgb);
                buffer.vertex(matrix, x2, y2, z1).color(rgb);
            });
        }

        private int getGradientColor(ColorRGBA c1, ColorRGBA c2, float t) {
            float smoothT = (float) (Math.sin((t - 0.5f) * Math.PI) * 0.5f + 0.5f);

            int r = (int) MathUtility.interpolate(c1.getRed(), c2.getRed(), smoothT);
            int g = (int) MathUtility.interpolate(c1.getGreen(), c2.getGreen(), smoothT);
            int b = (int) MathUtility.interpolate(c1.getBlue(), c2.getBlue(), smoothT);
            int a = (int) MathUtility.interpolate(c1.getAlpha(), c2.getAlpha(), smoothT);

            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        private float easeOutCubic(float x) {
            return 1f - (float) Math.pow(1f - x, 3);
        }

        private float easeInCubic(float x) {
            return x * x * x;
        }
    }

    private ColorRGBA getWaveColor1() {
        if (this.useTheme.isEnabled()) {
                return Colors.getAccentColor();

        }
        return this.color1.getColor();
    }

    private ColorRGBA getWaveColor2() {
        if (this.useTheme.isEnabled()) {
                return Colors.getAccentColor();

        }
        return this.color2.getColor();
    }
}
