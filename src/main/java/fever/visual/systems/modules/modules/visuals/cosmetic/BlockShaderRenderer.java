package fever.visual.systems.modules.modules.visuals.cosmetic;

import fever.visual.utility.render.compat.RenderSystem;
import fever.visual.FeverVisual;
import fever.visual.framework.shader.GlProgram;
import fever.visual.systems.modules.modules.visuals.Zoom;
import fever.visual.utility.colors.ColorRGBA;
import fever.visual.utility.render.compat.BufferRenderer;
import fever.visual.utility.render.compat.ShaderProgramKeys;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class BlockShaderRenderer {
    public static void renderShaderBox(Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, ColorRGBA color, ShaderMode mode, int fillAlpha, float speed) {
        if (!mode.hasShader()) {
            renderNormalBox(matrix, minX, minY, minZ, maxX, maxY, maxZ, color, fillAlpha);
        } else {
            renderWithShader(matrix, minX, minY, minZ, maxX, maxY, maxZ, color, mode, fillAlpha, speed);
        }
    }

    private static void renderNormalBox(Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, ColorRGBA color, int fillAlpha) {
        int c = replaceAlpha(color.getRGB(), fillAlpha);
        int r = c >> 16 & 0xFF;
        int g = c >> 8 & 0xFF;
        int b = c & 0xFF;
        int a = c >> 24 & 0xFF;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);


        buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);


        buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderWithShader(Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, ColorRGBA color, ShaderMode mode, int fillAlpha, float speed) {
        try {
            GlProgram shader = mode.getShader();
            if (shader == null) {
                FeverVisual.LOGGER.warn("Shader key is null for mode: {}", mode.getShaderName());
                renderNormalBox(matrix, minX, minY, minZ, maxX, maxY, maxZ, color, fillAlpha);
                return;
            }

            shader.use();
            shader.findUniform("time").set((float) (System.currentTimeMillis() % 100000L) / 1000.0f * speed);
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            shader.findUniform("screenSize").set((float) mc.getWindow().getWidth(), (float) mc.getWindow().getHeight());
            shader.findUniform("baseColor").set(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            shader.findUniform("alpha").set((float) fillAlpha / 255.0f);
            if (mode.usesZoom()) {
                Zoom zoomModule = FeverVisual.getInstance().getModuleManager().getModule(Zoom.class);
                float zoomFactor = 1.0f;
                if (zoomModule != null && zoomModule.isEnabled()) {
                    zoomFactor = zoomModule.getZoomMultiplier();
                }
                shader.findUniform("zoomFactor").set(zoomFactor);
            }

            BufferBuilder buffer = Tessellator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            int c = replaceAlpha(color.getRGB(), fillAlpha);
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            int a = c >> 24 & 0xFF;


            buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).texture(0.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).texture(1.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).texture(1.0f, 1.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).texture(0.0f, 1.0f).color(r, g, b, a);


            buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).texture(0.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).texture(0.0f, 1.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).texture(1.0f, 1.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).texture(1.0f, 0.0f).color(r, g, b, a);


            buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).texture(0.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).texture(1.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).texture(1.0f, 1.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).texture(0.0f, 1.0f).color(r, g, b, a);


            buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).texture(0.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).texture(1.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).texture(1.0f, 1.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).texture(0.0f, 1.0f).color(r, g, b, a);


            buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).texture(0.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).texture(1.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).texture(1.0f, 1.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).texture(0.0f, 1.0f).color(r, g, b, a);


            buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).texture(0.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).texture(1.0f, 0.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).texture(1.0f, 1.0f).color(r, g, b, a);
            buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).texture(0.0f, 1.0f).color(r, g, b, a);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (Exception e) {
            FeverVisual.LOGGER.error("Failed to render with shader: {}", mode.getShaderName(), e);
            renderNormalBox(matrix, minX, minY, minZ, maxX, maxY, maxZ, color, fillAlpha);
        }
    }

    private static int replaceAlpha(int color, int alpha) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        return alpha << 24 | r << 16 | g << 8 | b;
    }

    public enum ShaderMode {
        COBWEB("Паутина", "block_cobweb"),
        NEBULA("Туманность", "block_nebula"),
        PLASMA("Плазма", "block_plasma"),
        STARFIELD("Звёздное поле", "block_starfield"),
        FIREWORKS("Геометрия", "block_fireworks"),
        GALAXY("Галактика", "block_galaxy"),
        STARS("Аква", "block_stars");

        private final String displayName;
        private final String shaderName;
        private final GlProgram shader;

        ShaderMode(String displayName, String shaderName) {
            this.displayName = displayName;
            this.shaderName = shaderName;
            if (shaderName != null) {
                this.shader = new GlProgram(
                    Identifier.of("fevervisual", "shadersblock/shadersforblock/" + shaderName),
                    VertexFormats.POSITION_TEXTURE_COLOR
                );
            } else {
                this.shader = null;
            }
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getShaderName() {
            return shaderName;
        }

        public GlProgram getShader() {
            return shader;
        }

        public boolean usesZoom() {
            return this == FIREWORKS || this == GALAXY || this == STARS;
        }

        public boolean hasShader() {
            return shaderName != null;
        }
    }
}
