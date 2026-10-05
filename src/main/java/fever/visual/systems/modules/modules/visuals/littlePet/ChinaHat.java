package fever.visual.systems.modules.modules.visuals.littlePet;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import fever.visual.utility.render.compat.BufferRenderer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class ChinaHat {
    private static final int SEGMENTS = 32;
    private static final float[] UNIT_X = createCircleAxis(true);
    private static final float[] UNIT_Z = createCircleAxis(false);
    private static final float SETTING_MIN_RADIUS = 1.2F;
    private static final float SETTING_MAX_RADIUS = 2.1F;
    private static final float MAX_RADIUS = 1.2F;
    private static final float MIN_RADIUS = 0.3F;

    public static void renderAttachedToHead(
            MatrixStack matrices,
            float radius,
            float coneHeight,
            float yOffset,
            ColorRGBA hatColor,
            float alpha
    ) {
        if (matrices == null) {
            return;
        }

        ColorRGBA finalColor = hatColor != null ? hatColor : getDefaultColor();
        float mappedRadius = mapRadius(radius);
        float mappedHeight = mapHeight(coneHeight);

        matrices.push();
        matrices.translate(0.0D, -0.25D - yOffset, 0.0D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        drawConeHat(matrices, finalColor, mappedRadius, -mappedHeight, alpha, false);

        matrices.pop();
    }

    private static float mapRadius(float inputRadius) {
        float normalized = (inputRadius - SETTING_MIN_RADIUS) / (SETTING_MAX_RADIUS - SETTING_MIN_RADIUS);
        normalized = MathHelper.clamp(normalized, 0.0F, 1.0F);
        float eased = (float) Math.pow(normalized, 1.5);
        return MIN_RADIUS + eased * (MAX_RADIUS - MIN_RADIUS);
    }
    private static float mapHeight(float inputHeight) {
        float normalized = (inputHeight - 0.05F) / (0.8F - 0.05F);
        return 0.15F + normalized * 0.5F;
    }

    private static void drawConeHat(
            MatrixStack matrices,
            ColorRGBA color,
            float radius,
            float coneHeight,
            float alpha,
            boolean writeConeDepth
    ) {
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = Math.min(1.0F, alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(writeConeDepth);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(2.0F);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < SEGMENTS; i++) {
            float x1 = radius * UNIT_X[i];
            float z1 = radius * UNIT_Z[i];
            float x2 = radius * UNIT_X[i + 1];
            float z2 = radius * UNIT_Z[i + 1];

            buffer.vertex(matrix, x1, 0, z1).color(r, g, b, a);
            buffer.vertex(matrix, x2, 0, z2).color(r, g, b, a);
            buffer.vertex(matrix, 0, coneHeight, 0).color(r, g, b, a);

            buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
            buffer.vertex(matrix, x2, 0, z2).color(r, g, b, a);
            buffer.vertex(matrix, x1, 0, z1).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i <= SEGMENTS; i++) {
            buffer.vertex(matrix, radius * UNIT_X[i], 0, radius * UNIT_Z[i]).color(r, g, b, 1.0F);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static float[] createCircleAxis(boolean cosine) {
        float[] values = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / SEGMENTS;
            values[i] = (float)(cosine ? Math.cos(angle) : Math.sin(angle));
        }
        return values;
    }

    private static ColorRGBA getDefaultColor() {
        return FeverVisual.getInstance()
                .getThemeManager()
                .getCurrentTheme()
                .getAdditionalColor();
    }

}
