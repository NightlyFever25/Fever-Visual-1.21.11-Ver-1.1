package fever.visual.systems.modules.modules.visuals;

import fever.visual.utility.render.compat.BufferRenderer;

import fever.visual.utility.render.compat.GlStateManager;
import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.systems.event.EventListener;
import fever.visual.systems.event.impl.render.Render3DEvent;
import fever.visual.systems.modules.api.ModuleCategory;
import fever.visual.systems.modules.api.ModuleInfo;
import fever.visual.systems.modules.impl.BaseModule;
import fever.visual.systems.setting.settings.BooleanSetting;
import fever.visual.systems.setting.settings.ColorSetting;
import fever.visual.systems.setting.settings.ModeSetting;
import fever.visual.systems.setting.settings.SliderSetting;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.colors.Colors;
import fever.visual.utility.render.ColorUtility;
import fever.visual.utility.render.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;
//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)
@ModuleInfo(name = "Spheres", category = ModuleCategory.VISUALS, desc = "Сферы")
@Environment(EnvType.CLIENT)
public class Spheres extends BaseModule {

    private static final Identifier BLOOM_TEX = FeverVisual.id("textures/bloom.png");
    private static final float[] PHASES = {0.0f, 2.0943952f, 4.1887903f};
    private static final float[] STATIC_OFFSETS_DEG = {180.0f, -90.0f, 360.0f};
    private static final int SPHERE_STACKS = 12;
    private static final int SPHERE_SLICES = 20;
    private static final float[] SPHERE_NORMALS = createSphereNormals();
    private final ModeSetting sphereMode = new ModeSetting(this, "modules.settings.sphere.mode", "Режим движения сфер");
    private final ModeSetting.Value modeStatic = new ModeSetting.Value(sphereMode, "modules.settings.static", "Статичный");
    private final ModeSetting.Value modeOrbit = new ModeSetting.Value(sphereMode, "modules.settings.orbit", "Орбита").select();
    private final ModeSetting.Value modeSpiral = new ModeSetting.Value(sphereMode, "modules.settings.spiral", "Спираль");
    private final ModeSetting.Value modeSpiralV2 = new ModeSetting.Value(sphereMode, "modules.settings.spiral_v2", "Спираль V2");
    private final ModeSetting.Value modeWaves = new ModeSetting.Value(sphereMode, "modules.settings.waves", "Волны");

    private final ModeSetting sphereEffect = new ModeSetting(this, "modules.settings.sphere.effect", "Доп. эффект");
    private final ModeSetting.Value effectNone = new ModeSetting.Value(sphereEffect, "modules.settings.none", "Нет");
    private final ModeSetting.Value effectTrail = new ModeSetting.Value(sphereEffect, "modules.settings.trail", "След").select();
    private final ModeSetting.Value effectFireTrail = new ModeSetting.Value(sphereEffect, "modules.settings.fire_trail", "Огненный след");

    private final SliderSetting sphereSize = new SliderSetting(this, "modules.settings.sphere.size", "Размер сферы").min(0.05f).max(0.22f).step(0.01f).currentValue(0.12f);
    private final SliderSetting sphereRadius = new SliderSetting(this, "modules.settings.sphere.radius", "Радиус орбиты сфер").min(0.18f).max(1.25f).step(0.01f).currentValue(0.42f);
    private final SliderSetting sphereHeight = new SliderSetting(this, "modules.settings.sphere.height", "Смещение по Y").min(-1.2f).max(3.5f).step(0.01f).currentValue(0.05f);
    private final SliderSetting sphereSpeed = new SliderSetting(this, "modules.settings.sphere.speed", "Скорость анимации").min(0.2f).max(4.0f).step(0.01f).currentValue(1.85f);
    private final SliderSetting sphereBreathe = new SliderSetting(this, "modules.settings.sphere.breathe", "Пульсация радиуса").min(0.0f).max(0.3f).step(0.01f).currentValue(0.08f);
    private final SliderSetting spiralHeight = new SliderSetting(this, "modules.settings.sphere.spiral_height", "Размах спирали по Y").min(0.2f).max(6.0f).step(0.01f).currentValue(2);
    private final SliderSetting waveAmp = new SliderSetting(this, "modules.settings.sphere.wave_amp", "Амплитуда волн по Y").min(0.0f).max(1.2f).step(0.01f).currentValue(0.22f);
    private final SliderSetting waveFreq = new SliderSetting(this, "modules.settings.sphere.wave_freq", "Частота волн").min(0.6f).max(8.0f).step(0.01f).currentValue(2.4f);
    private final SliderSetting trailLen = new SliderSetting(this, "modules.settings.sphere.trail_len", "Длина следа").min(6.0f).max(48.0f).step(1.0f).currentValue(18.0f);
    private final SliderSetting trailPower = new SliderSetting(this, "modules.settings.sphere.trail_power", "Сила свечения").min(0.2f).max(2.2f).step(0.01f).currentValue(1.1f);

    private final ModeSetting sphereColorMode = new ModeSetting(this, "modules.settings.sphere.color_mode");
    private final ModeSetting.Value sphereColorTheme = new ModeSetting.Value(this.sphereColorMode, "modules.settings.sphere.color_mode.theme").select();
    private final ModeSetting.Value sphereColorCustom = new ModeSetting.Value(this.sphereColorMode, "modules.settings.sphere.color_mode.custom");
    private final ColorSetting sphereColor1 = new ColorSetting(this, "modules.settings.sphere.color1", () -> !this.sphereColorCustom.isSelected()).color(ColorRGBA.fromInt(-3644216)).alpha(true);
    private final ColorSetting sphereColor1Second = new ColorSetting(this, "modules.settings.sphere.color1_second", () -> !this.sphereColorCustom.isSelected()).color(new ColorRGBA(100, 160, 255, 255)).alpha(true);
    private final ColorSetting sphereColor2 = new ColorSetting(this, "modules.settings.sphere.color2", () -> !this.sphereColorCustom.isSelected()).color(ColorRGBA.fromInt(-6881336)).alpha(true);
    private final ColorSetting sphereColor2Second = new ColorSetting(this, "modules.settings.sphere.color2_second", () -> !this.sphereColorCustom.isSelected()).color(new ColorRGBA(255, 120, 170, 255)).alpha(true);
    private final ColorSetting sphereColor3 = new ColorSetting(this, "modules.settings.sphere.color3", () -> !this.sphereColorCustom.isSelected()).color(ColorRGBA.fromInt(-1768489016)).alpha(true);
    private final ColorSetting sphereColor3Second = new ColorSetting(this, "modules.settings.sphere.color3_second", () -> !this.sphereColorCustom.isSelected()).color(new ColorRGBA(255, 210, 90, 255)).alpha(true);

    private final BooleanSetting sphereSelf = new BooleanSetting(this, "modules.settings.sphere.self", "Рисовать на себе").enabled(true);
    private final BooleanSetting sphereFriends = new BooleanSetting(this, "modules.settings.sphere.friends", "Рисовать на друзьях").enabled(true);
    private final BooleanSetting sphereFirstPerson = new BooleanSetting(this, "modules.settings.sphere.first_person", "Рисовать от 1-го лица").enabled(true);

    private int lastEditIdx = Integer.MIN_VALUE;
    private float allLR = 0.0f;
    private float allFB = 0.0f;
    private final float[] offLR = new float[3];
    private final float[] offFB = new float[3];
    private final Map<UUID, TrailState> sphereTrailMap = new HashMap<>();

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (mc.player == null || mc.world == null) return;
        onWorldRender(event);
    };

    @Override
    public void disable() {
        super.disable();
        deactivate();
    }

    public String getString(String key) {
        return switch (key) {
            case "mode" -> sphereMode.getValue().getName();
            case "effect" -> sphereEffect.getValue().getName();
            default -> "";
        };
    }

    public boolean getBool(String key) {
        return switch (key) {
            case "self" -> sphereSelf.isEnabled();
            case "friends" -> sphereFriends.isEnabled();
            case "firstPerson" -> sphereFirstPerson.isEnabled();
            default -> false;
        };
    }

    public float getFloat(String key) {
        return switch (key) {
            case "size" -> sphereSize.getCurrentValue();
            case "radius" -> sphereRadius.getCurrentValue();
            case "height" -> sphereHeight.getCurrentValue();
            case "speed" -> sphereSpeed.getCurrentValue();
            case "breathe" -> sphereBreathe.getCurrentValue();
            case "spiralHeight" -> spiralHeight.getCurrentValue();
            case "waveAmp" -> waveAmp.getCurrentValue();
            case "waveFreq" -> waveFreq.getCurrentValue();
            case "trailLen" -> trailLen.getCurrentValue();
            case "trailPower" -> trailPower.getCurrentValue();
            default -> 0.0f;
        };
    }

    public int getInt(String key) {
        return switch (key) {
            case "exort" -> this.getSphereColor(0, 0.0F).getRGB();
            case "wex" -> this.getSphereColor(1, 0.0F).getRGB();
            case "quas" -> this.getSphereColor(2, 0.0F).getRGB();
            default -> -1;
        };
    }

    public void deactivate() {
        sphereTrailMap.clear();
        lastEditIdx = Integer.MIN_VALUE;
        allLR = 0.0f;
        allFB = 0.0f;
        Arrays.fill(offLR, 0.0f);
        Arrays.fill(offFB, 0.0f);
    }

    public void onWorldRender(Render3DEvent e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        syncOffsetEditor();
        MatrixStack ms = e.getMatrices();
        float pt = e.getTickDelta();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        boolean camFirst = mc.options.getPerspective().isFirstPerson();
        boolean drawSelf = getBool("self");
        boolean drawFriends = getBool("friends");
        boolean drawFirst = getBool("firstPerson");

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.isRemoved()) continue;

            boolean isSelf = player == mc.player;

            if (isSelf) {
                if (!drawSelf || (camFirst && !drawFirst)) continue;
                renderSpheres(ms, camera, cameraPos, player, pt);
                continue;
            }

            if (!isFriend(player)) continue;

            if (!drawFriends) continue;
            if (isInvisible(player)) continue;
            if (player.squaredDistanceTo(cameraPos) > 4096.0
                    || !Utils.isInViewHemisphere(camera, player.getX(), player.getY() + player.getHeight() * 0.5, player.getZ(), 3.0)) continue;

            renderSpheres(ms, camera, cameraPos, player, pt);
        }
    }

    private boolean isFriend(PlayerEntity player) {
        if (FeverVisual.getInstance().getFriendManager() == null) {
            return false;
        }
        String playerName = player.getName().getString();
        return FeverVisual.getInstance().getFriendManager().isFriend(playerName);
    }

    private boolean isInvisible(PlayerEntity player) {
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

    private void renderSpheres(MatrixStack ms, Camera camera, Vec3d cameraPos, PlayerEntity player, float pt) {
        double px = MathHelper.lerp(pt, player.lastX, player.getX());
        double py = MathHelper.lerp(pt, player.lastY, player.getY());
        double pz = MathHelper.lerp(pt, player.lastZ, player.getZ());

        float bodyYawDeg = MathHelper.lerpAngleDegrees(pt, player.lastBodyYaw, player.bodyYaw);
        double bodyYawRad = Math.toRadians(bodyYawDeg);

        Vec3d forward = new Vec3d(-Math.sin(bodyYawRad), 0.0, Math.cos(bodyYawRad));
        Vec3d right = new Vec3d(forward.getZ(), 0.0, -forward.getX());

        double baseY = py + player.getHeight() * 0.5 + getFloat("height");
        double headY = py + player.getHeight() + getFloat("height");
        float baseR = getFloat("radius");
        float s = getFloat("size");

        float t = (float) (System.currentTimeMillis() % 1_000_000L) / 1000.0f;
        int baseCol0 = this.getSphereColor(0, t * 120.0F).getRGB();
        int baseCol1 = this.getSphereColor(1, t * 120.0F).getRGB();
        int baseCol2 = this.getSphereColor(2, t * 120.0F).getRGB();
        float bobAmp = MathHelper.clamp(getFloat("size") * 0.85f, 0.0f, 0.18f);

        boolean doTrail = !getString("effect").equals("none");
        boolean fireTrail = getString("effect").equals("fire_trail");
        int limit = Math.max(6, Math.min(48, (int) getFloat("trailLen")));
        float trailMul = getFloat("trailPower");

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.disableCull();

        for (int i = 0; i < 3; ++i) {
            float ph = PHASES[i];
            float breatheK = 1.0f + (float) (Math.sin(t * 2.1f + ph) * getFloat("breathe"));
            float rr = baseR * breatheK;
            float alphaMul = 1.0f;

            double sx, sy, sz;
            String mode = getString("mode");

            if (mode.equals("orbit")) {
                float ang = t * getFloat("speed") + ph + (float) bodyYawRad;
                sx = px + Math.cos(ang) * rr;
                sz = pz + Math.sin(ang) * rr;
                sy = baseY + Math.sin(t * 2.8f + ph) * bobAmp;
            } else if (mode.equals("spiral")) {
                double yTop = headY + getFloat("spiralHeight");
                double yBottom = py + getFloat("height") - getFloat("spiralHeight");
                double span = Math.max(0.001, yTop - yBottom);
                double prog = frac(t * (0.18f * getFloat("speed")) + (float) i * 0.33333334f);
                float ang = t * (2.1f * getFloat("speed")) + ph + (float) bodyYawRad;
                sx = px + Math.cos(ang) * rr;
                sz = pz + Math.sin(ang) * rr;
                sy = yTop - prog * span;
                alphaMul = spiralFade((float) ((sy - yBottom) / span));
            } else if (mode.equals("spiral_v2")) {
                double yTop = headY + getFloat("spiralHeight");
                double yBottom = py + getFloat("height") - getFloat("spiralHeight");
                double span = Math.max(0.001, yTop - yBottom);
                double prog = frac(t * (0.22f * getFloat("speed")) + (float) i * 0.33333334f);
                float rr2 = rr * (0.8f + 0.2f * (float) Math.sin(t * 1.7f + ph));
                float ang = t * (2.65f * getFloat("speed")) + ph * 1.15f + (float) bodyYawRad;
                sx = px + Math.cos(ang) * rr2;
                sz = pz + Math.sin(ang) * rr2;
                double base = yTop - prog * span;
                sy = base + Math.sin(t * 1.35f + ph) * (span * 0.04);
                alphaMul = spiralFade((float) ((sy - yBottom) / span));
            } else if (mode.equals("waves")) {
                float ang = t * getFloat("speed") + ph + (float) bodyYawRad;
                float w = (float) Math.sin(ang * getFloat("waveFreq") + t * 1.2f);
                sx = px + Math.cos(ang) * rr;
                sz = pz + Math.sin(ang) * rr;
                sy = headY + w * getFloat("waveAmp") + Math.sin(t * 2.8f + ph) * bobAmp;
            } else {
                double ang = Math.toRadians(bodyYawDeg + STATIC_OFFSETS_DEG[i]);
                sx = px + Math.cos(ang) * rr;
                sz = pz + Math.sin(ang) * rr;
                sy = baseY + Math.sin(t * 2.8f + ph) * bobAmp;
            }

            float addLR = allLR + offLR[i];
            float addFB = allFB + offFB[i];

            sx += right.getX() * addLR + forward.getX() * addFB;
            sz += right.getZ() * addLR + forward.getZ() * addFB;

            Vec3d viewDir = new Vec3d(cameraPos.getX() - sx, cameraPos.getY() - sy, cameraPos.getZ() - sz);
            double len = viewDir.length();
            viewDir = len > 1.0E-6 ? viewDir.multiply(1.0 / len) : new Vec3d(0.0, 0.0, 1.0);

            int col = withAlphaMul(i == 0 ? baseCol0 : i == 1 ? baseCol1 : baseCol2, alphaMul);

            if (doTrail) {
                pushSphereTrail(player.getUuid(), i, new Vec3d(sx, sy, sz), limit);
                renderSphereTrail(ms, camera, cameraPos, player.getUuid(), i, col, trailMul, fireTrail);
            }

            if (alphaMul > 0.0025f) {
                renderOne(ms, camera, cameraPos, sx, sy, sz, s, col, viewDir);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float spiralFade(float norm) {
        float p0 = MathHelper.clamp(norm, 0.0f, 1.0f);
        float edge = 0.14f;
        float f1 = MathHelper.clamp(p0 / edge, 0.0f, 1.0f);
        float f2 = MathHelper.clamp((1.0f - p0) / edge, 0.0f, 1.0f);
        float a = Math.min(f1, f2);
        return a * a * (3.0f - 2.0f * a);
    }

    private int withAlphaMul(int argb, float mul) {
        int a0 = (argb >>> 24) & 0xFF;
        int a = MathHelper.clamp((int) (a0 * mul), 0, 255);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private ColorRGBA getSphereColor(int sphereIndex, float index) {
        float offset = sphereIndex * 120.0F + index;
        if (!this.sphereColorCustom.isSelected()) {
            return Colors.getAccentColor(offset);
        }

        ColorSetting first = switch (sphereIndex) {
            case 1 -> this.sphereColor2;
            case 2 -> this.sphereColor3;
            default -> this.sphereColor1;
        };
        ColorSetting second = switch (sphereIndex) {
            case 1 -> this.sphereColor2Second;
            case 2 -> this.sphereColor3Second;
            default -> this.sphereColor1Second;
        };

        return first.getColorSafe().mix(second.getColorSafe(), this.getColorMix(offset));
    }

    private float getColorMix(float index) {
        float normalized = (index % 360.0F) / 180.0F;
        return normalized > 1.0F ? 2.0F - normalized : normalized;
    }

    private int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private void renderOne(MatrixStack ms, Camera camera, Vec3d cameraPos, double x, double y, double z, float r, int argb, Vec3d viewDir) {
        ms.push();
        ms.translate(x - cameraPos.getX(), y - cameraPos.getY(), z - cameraPos.getZ());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();

        drawSphere(ms, r, argb, viewDir);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, BLOOM_TEX);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.depthMask(false);

        float bloomSize = r * 6.0f;
        int a0 = (argb >>> 24) & 0xFF;
        int bloomA = Math.min(255, (int) (a0 * 0.62f));
        int bloom = (bloomA << 24) | (argb & 0xFFFFFF);

        drawBillboard(ms, camera, bloomSize, bloom);

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private void drawBillboard(MatrixStack ms, Camera camera, float size, int argb) {
        ms.push();
        ms.multiply(camera.getRotation());

        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float h = size * 0.5f;
        float r = ColorUtility.redf(argb);
        float g = ColorUtility.greenf(argb);
        float b = ColorUtility.bluef(argb);
        float a = ColorUtility.alphaf(argb);

        builder.vertex(m, -h, h, 0.0f).texture(0.0f, 1.0f).color(r, g, b, a);
        builder.vertex(m, h, h, 0.0f).texture(1.0f, 1.0f).color(r, g, b, a);
        builder.vertex(m, h, -h, 0.0f).texture(1.0f, 0.0f).color(r, g, b, a);
        builder.vertex(m, -h, -h, 0.0f).texture(0.0f, 0.0f).color(r, g, b, a);

        BuiltBuffer built = builder.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }

        ms.pop();
    }

    private void drawSphere(MatrixStack ms, float radius, int argb, Vec3d viewDir) {
        int a0 = (argb >>> 24) & 0xFF;
        int r0 = (argb >>> 16) & 0xFF;
        int g0 = (argb >>> 8) & 0xFF;
        int b0 = argb & 0xFF;

        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < SPHERE_NORMALS.length; i += 3) {
            float nx = SPHERE_NORMALS[i];
            float ny = SPHERE_NORMALS[i + 1];
            float nz = SPHERE_NORMALS[i + 2];
            builder.vertex(m, nx * radius, ny * radius, nz * radius).color(shade(nx, ny, nz, viewDir, r0, g0, b0, a0));
        }

        BuiltBuffer built = builder.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }

    private int shade(float nx, float ny, float nz, Vec3d viewDir, int r0, int g0, int b0, int a0) {
        double dot = nx * viewDir.getX() + ny * viewDir.getY() + nz * viewDir.getZ();
        dot = MathHelper.clamp(dot, 0.0, 1.0);

        double rim = 1.0 - dot;
        double rim2 = rim * rim;
        double rim4 = rim2 * rim2;

        double base = 0.4 + 0.6 * dot;
        double add = 0.22 * rim4;
        double light = Math.min(base + add, 1.0);

        int r = clamp255((int) (r0 * light));
        int g = clamp255((int) (g0 * light));
        int b = clamp255((int) (b0 * light));

        return (a0 << 24) | (r << 16) | (g << 8) | b;
    }

    private void pushSphereTrail(UUID uuid, int idx, Vec3d pos, int limit) {
        TrailState st = sphereTrailMap.computeIfAbsent(uuid, k -> new TrailState(6));
        ArrayDeque<TrailPoint> q = st.queues[MathHelper.clamp(idx, 0, st.queues.length - 1)];
        q.addFirst(new TrailPoint(pos, System.currentTimeMillis()));
        while (q.size() > limit) {
            q.removeLast();
        }
    }

    private void renderSphereTrail(MatrixStack ms, Camera camera, Vec3d cameraPos, UUID uuid, int idx, int argb, float mul, boolean fire) {
        TrailState st = sphereTrailMap.get(uuid);
        if (st == null) return;

        ArrayDeque<TrailPoint> q = st.queues[MathHelper.clamp(idx, 0, st.queues.length - 1)];
        if (q.isEmpty()) return;

        float a0 = ((argb >>> 24) & 0xFF) / 255.0f;
        float r0 = ((argb >>> 16) & 0xFF) / 255.0f;
        float g0 = ((argb >>> 8) & 0xFF) / 255.0f;
        float b0 = (argb & 0xFF) / 255.0f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, BLOOM_TEX);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        int n = q.size();
        int k = 0;
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (TrailPoint tp : q) {
            float tt = n <= 1 ? 0.0f : (float) k / (float) (n - 1);
            float fade = 1.0f - tt;
            fade *= fade;

            float a = MathHelper.clamp(a0 * (0.6f * fade) * mul, 0.0f, 1.0f);
            float ss = getFloat("size") * 4.8f * (0.55f + 0.7f * fade);

            float rr = r0;
            float gg = g0;
            float bb = b0;

            if (fire) {
                float heat = MathHelper.clamp(0.22f + 0.78f * fade, 0.0f, 1.0f);
                rr = rr * (1.0f - heat) + 1.0f * heat;
                gg = gg * (1.0f - heat) + 0.78f * heat;
                bb = bb * (1.0f - heat) + 0.32f * heat;
                a = MathHelper.clamp(a * 1.18f, 0.0f, 1.0f);
            }

            ms.push();
            ms.translate(
                    tp.pos.getX() - cameraPos.getX(),
                    tp.pos.getY() - cameraPos.getY(),
                    tp.pos.getZ() - cameraPos.getZ()
            );
            ms.multiply(camera.getRotation());

            Matrix4f m = ms.peek().getPositionMatrix();

            float h = ss * 0.5f;
            builder.vertex(m, -h, h, 0.0f).texture(0.0f, 1.0f).color(rr, gg, bb, a);
            builder.vertex(m, h, h, 0.0f).texture(1.0f, 1.0f).color(rr, gg, bb, a);
            builder.vertex(m, h, -h, 0.0f).texture(1.0f, 0.0f).color(rr, gg, bb, a);
            builder.vertex(m, -h, -h, 0.0f).texture(0.0f, 0.0f).color(rr, gg, bb, a);

            ms.pop();
            ++k;
        }

        BuiltBuffer built = builder.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private void syncOffsetEditor() {
        int idx = getEditIndex();
        if (idx != lastEditIdx) {
            lastEditIdx = idx;
            return;
        }

        float vLR = getFloatFromEditor("editLR");
        float vFB = getFloatFromEditor("editFB");

        if (idx < 0) {
            if (!eq(vLR, allLR) || !eq(vFB, allFB)) {
                allLR = vLR;
                allFB = vFB;
            }
        } else if (!eq(vLR, offLR[idx]) || !eq(vFB, offFB[idx])) {
            offLR[idx] = vLR;
            offFB[idx] = vFB;
        }
    }

    private float getFloatFromEditor(String key) {
        return 0.0f;
    }

    private boolean eq(float a, float b) {
        return Math.abs(a - b) <= 1.0E-4f;
    }

    private int getEditIndex() {
        String editor = getString("sphereEditor");
        if (editor == null) return -1;
        if (editor.equals("exort")) return 0;
        if (editor.equals("wex")) return 1;
        if (editor.equals("quas")) return 2;
        return -1;
    }

    private static class TrailState {
        final ArrayDeque<TrailPoint>[] queues;

        @SuppressWarnings("unchecked")
        TrailState(int queueCount) {
            queues = new ArrayDeque[queueCount];
            for (int i = 0; i < queueCount; i++) {
                queues[i] = new ArrayDeque<>();
            }
        }
    }

    private record TrailPoint(Vec3d pos, long time) {}

    private static float[] createSphereNormals() {
        float[] normals = new float[SPHERE_STACKS * SPHERE_SLICES * 6 * 3];
        int index = 0;

        for (int i = 0; i < SPHERE_STACKS; ++i) {
            double v0 = (double) i / SPHERE_STACKS;
            double v1 = (double) (i + 1) / SPHERE_STACKS;

            double phi0 = v0 * Math.PI - Math.PI / 2;
            double phi1 = v1 * Math.PI - Math.PI / 2;

            double sy0 = Math.sin(phi0);
            double sy1 = Math.sin(phi1);
            double sr0 = Math.cos(phi0);
            double sr1 = Math.cos(phi1);

            for (int j = 0; j < SPHERE_SLICES; ++j) {
                double u0 = (double) j / SPHERE_SLICES;
                double u1 = (double) (j + 1) / SPHERE_SLICES;

                double th0 = u0 * (Math.PI * 2);
                double th1 = u1 * (Math.PI * 2);

                float x00n = (float) (Math.cos(th0) * sr0);
                float z00n = (float) (Math.sin(th0) * sr0);
                float y00n = (float) sy0;

                float x10n = (float) (Math.cos(th1) * sr0);
                float z10n = (float) (Math.sin(th1) * sr0);
                float y10n = (float) sy0;

                float x01n = (float) (Math.cos(th0) * sr1);
                float z01n = (float) (Math.sin(th0) * sr1);
                float y01n = (float) sy1;

                float x11n = (float) (Math.cos(th1) * sr1);
                float z11n = (float) (Math.sin(th1) * sr1);
                float y11n = (float) sy1;

                index = putNormal(normals, index, x00n, y00n, z00n);
                index = putNormal(normals, index, x11n, y11n, z11n);
                index = putNormal(normals, index, x10n, y10n, z10n);
                index = putNormal(normals, index, x00n, y00n, z00n);
                index = putNormal(normals, index, x01n, y01n, z01n);
                index = putNormal(normals, index, x11n, y11n, z11n);
            }
        }

        return normals;
    }

    private static int putNormal(float[] normals, int index, float x, float y, float z) {
        normals[index++] = x;
        normals[index++] = y;
        normals[index++] = z;
        return index;
    }
}
