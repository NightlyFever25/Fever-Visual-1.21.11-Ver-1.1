package fever.visual.systems.modules.modules.visuals.littlePet;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class TaksaModel {

    public static final Identifier TEXTURE = Identifier.of("fevervisual", "textures/taksa.png");

    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart back;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;
    private final ModelPart leftEar;
    private final ModelPart rightEar;

    public TaksaModel() {
        ModelData modelData = getModelData();
        ModelPart root = TexturedModelData.of(modelData, 60, 36).createModel();

        this.head = root.getChild("head");
        this.neck = root.getChild("neck");
        this.body = root.getChild("body");
        this.chest = this.body.getChild("chest");
        this.back  = this.body.getChild("back");
        this.frontLeftLeg  = root.getChild("frontLeftLeg");
        this.frontRightLeg = root.getChild("frontRightLeg");
        this.leftBackLeg   = root.getChild("leftBackLeg");
        this.rightBackLeg  = root.getChild("rightBackLeg");
        this.tail     = root.getChild("tail");
        this.leftEar  = this.head.getChild("leftEar");
        this.rightEar = this.head.getChild("rightEar");
    }

    public static ModelData getModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData headData = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F)
                        .uv(21, 0).cuboid(-1.5F, 0.0F, -7.0F, 3.0F, 3.0F, 3.0F),
                ModelTransform.origin(0.0F, 10.5F, -6.8F));

        headData.addChild("leftEar",
                ModelPartBuilder.create()
                        .uv(32, 4).cuboid(0.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F)
                        .uv(34, 1).cuboid(0.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.0F),
                ModelTransform.origin(3.0F, 3.0F, -2.0F));

        headData.addChild("rightEar",
                ModelPartBuilder.create()
                        .uv(32, 4).cuboid(-1.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F)
                        .uv(34, 1).cuboid(-1.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.0F),
                ModelTransform.origin(-3.0F, 3.0F, -2.0F));

        root.addChild("neck",
                ModelPartBuilder.create()
                        .uv(15, 7).cuboid(-2.95F, -1.0F, -4.0F, 5.9F, 5.0F, 6.0F),
                ModelTransform.of(0.0F, 10.5F, -5.0F, -0.43633232F, 0.0F, 0.0F));

        ModelPartData bodyData = root.addChild("body",
                ModelPartBuilder.create(),
                ModelTransform.origin(0.0F, 13.5F, -5.0F));

        bodyData.addChild("chest",
                ModelPartBuilder.create()
                        .uv(32, 13).cuboid(-4.0F, -3.5F, -3.0F, 8.0F, 7.0F, 6.0F),
                ModelTransform.origin(0.0F, 0.0F, 3.0F));

        bodyData.addChild("back",
                ModelPartBuilder.create()
                        .uv(3, 19).cuboid(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 11.0F),
                ModelTransform.origin(0.0F, -0.5F, 5.5F));

        root.addChild("frontLeftLeg",
                ModelPartBuilder.create()
                        .uv(42, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                ModelTransform.origin(1.5F, 16.0F, -3.0F));

        root.addChild("frontRightLeg",
                ModelPartBuilder.create()
                        .uv(42, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                ModelTransform.origin(-1.5F, 16.0F, -3.0F));

        root.addChild("leftBackLeg",
                ModelPartBuilder.create()
                        .uv(52, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                ModelTransform.origin(1.5F, 16.0F, 9.0F));

        root.addChild("rightBackLeg",
                ModelPartBuilder.create()
                        .uv(52, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                ModelTransform.origin(-1.5F, 16.0F, 9.0F));

        root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(2, 12).cuboid(-1.0F, 2.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                ModelTransform.of(0.0F, 9.0F, 10.0F, 0.3926991F, 0.0F, 0.0F));

        return data;
    }

    public void setAngles(float ageInTicks, TaksaBrain brain) {
        this.head.yaw   = brain.getYaw()   * ((float) Math.PI / 180);
        this.head.pitch = brain.getPitch() * ((float) Math.PI / 180);

        float swing    = brain.limbSwing;
        float swingAmt = brain.limbSwingAmount;

        this.frontLeftLeg.pitch  = MathHelper.cos(swing * 0.6662F) * 1.4F * swingAmt;
        this.frontRightLeg.pitch = MathHelper.cos((float)(swing * 0.6662F + Math.PI)) * 1.4F * swingAmt;
        this.leftBackLeg.pitch   = MathHelper.cos((float)(swing * 0.6662F + Math.PI)) * 1.4F * swingAmt;
        this.rightBackLeg.pitch  = MathHelper.cos(swing * 0.6662F) * 1.4F * swingAmt;

        if (brain.isLay()) {
            this.frontLeftLeg.pitch  = (float) Math.toRadians(-90.0);
            this.frontRightLeg.pitch = (float) Math.toRadians(-90.0);
            this.leftBackLeg.pitch   = (float) Math.toRadians(90.0);
            this.rightBackLeg.pitch  = (float) Math.toRadians(90.0);
            this.frontLeftLeg.yaw    = (float) Math.toRadians(-22.0);
            this.frontRightLeg.yaw   = (float) Math.toRadians(22.0);
            this.leftBackLeg.yaw     = (float) Math.toRadians(22.0);
            this.rightBackLeg.yaw    = (float) Math.toRadians(-22.0);
        } else {
            this.frontLeftLeg.yaw  = 0.0F;
            this.frontRightLeg.yaw = 0.0F;
            this.leftBackLeg.yaw   = 0.0F;
            this.rightBackLeg.yaw  = 0.0F;
        }

        this.tail.pitch = (float) Math.toRadians(brain.isLay() ? 45.0 : 22.0);
        this.tail.roll  = (float) (Math.toRadians(-22.5) + 0.39269908169872414 + Math.cos(ageInTicks * 0.15F) * 0.3);

        if (brain.isLay()) {
            this.leftEar.pitch  = (float) Math.toRadians(-30.0);
            this.rightEar.pitch = (float) Math.toRadians(-30.0);
        } else {
            this.leftEar.pitch  = 0.0F;
            this.rightEar.pitch = 0.0F;
        }
    }

    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, TaksaBrain brain, float ageInTicks) {
        matrices.push();
        matrices.translate(0.0F, 1.2F - (brain.isLay() ? 0.3F : 0.0F), 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(brain.getBody()));

        this.setAngles(ageInTicks, brain);

        this.head.render(matrices, vertexConsumer, light, overlay);
        this.neck.render(matrices, vertexConsumer, light, overlay);
        this.body.render(matrices, vertexConsumer, light, overlay);
        this.frontLeftLeg.render(matrices, vertexConsumer, light, overlay);
        this.frontRightLeg.render(matrices, vertexConsumer, light, overlay);
        this.leftBackLeg.render(matrices, vertexConsumer, light, overlay);
        this.rightBackLeg.render(matrices, vertexConsumer, light, overlay);
        this.tail.render(matrices, vertexConsumer, light, overlay);

        matrices.pop();
    }
}
