package fever.visual.systems.modules.modules.visuals.cosmetic;
//Да давай пасти бездарность :)
//Made by FeverVisuals (NightlyFever)

import fever.visual.mixin.accessors.BipedEntityModelAccessor;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public final class CustomModelsRenderer {

    private static final RabbitModel RABBIT_MODEL = new RabbitModel(RabbitModel.createModel());
    private static final DemonModel DEMON_MODEL = new DemonModel(DemonModel.createModel());
    private static final FreddyModel FREDDY_MODEL = new FreddyModel(FreddyModel.createModel());
    private static final AmogusModel AMOGUS_MODEL = new AmogusModel(AmogusModel.createModel());

    private CustomModelsRenderer() {}

    public static boolean render(CustomModelType type, EntityModel<?> baseModel, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        if (type == null || !(baseModel instanceof BipedEntityModel)) {
            return false;
        }
        BipedEntityModelAccessor accessor = (BipedEntityModelAccessor) baseModel;
        ModelPart head = accessor.fevervisual$getHead();
        ModelPart leftArm = accessor.fevervisual$getLeftArm();
        ModelPart rightArm = accessor.fevervisual$getRightArm();
        ModelPart leftLeg = accessor.fevervisual$getLeftLeg();
        ModelPart rightLeg = accessor.fevervisual$getRightLeg();

        CustomModel model = getModel(type.getModelKey());
        if (model == null) return false;

        model.applyAngles(head, leftArm, rightArm, leftLeg, rightLeg);

        matrices.push();
        applyTransform(type.getModelKey(), matrices);
        model.render(matrices, vertices, light, overlay, color);
        matrices.pop();
        return true;
    }

    private static CustomModel getModel(CustomModelType.ModelKey key) {
        return switch (key) {
            case RABBIT -> RABBIT_MODEL;
            case DEMON -> DEMON_MODEL;
            case FREDDY -> FREDDY_MODEL;
            case AMOGUS -> AMOGUS_MODEL;
        };
    }

    private static void applyTransform(CustomModelType.ModelKey key, MatrixStack matrices) {
        switch (key) {
            case RABBIT -> {
                matrices.scale(1.25f, 1.25f, 1.25f);
                matrices.translate(0.0f, -0.3f, 0.0f);
            }
            case FREDDY -> {
                matrices.scale(0.75f, 0.65f, 0.75f);
                matrices.translate(0.0f, 0.85f, 0.0f);
            }
            case AMOGUS -> {
                matrices.translate(0.0f, -0.5f, 0.0f);
            }
        }
    }

    private static void copyAngles(ModelPart source, ModelPart target, float extraPitch, float extraYaw, float extraRoll) {
        target.pitch = source.pitch + extraPitch;
        target.yaw = source.yaw + extraYaw;
        target.roll = source.roll + extraRoll;
    }

    private interface CustomModel {
        void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg);
        void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color);
    }


    private static final class RabbitModel
            implements CustomModel {
        private final ModelPart root;
        private final ModelPart rabbitHead;
        private final ModelPart rabbitLarm;
        private final ModelPart rabbitRarm;
        private final ModelPart rabbitLleg;
        private final ModelPart rabbitRleg;

        private RabbitModel(ModelPart root) {
            this.root = root;
            ModelPart rabbitBone = root.getChild("rabbit_bone");
            this.rabbitHead = rabbitBone.getChild("rabbit_head");
            this.rabbitLarm = rabbitBone.getChild("rabbit_larm");
            this.rabbitRarm = rabbitBone.getChild("rabbit_rarm");
            this.rabbitLleg = rabbitBone.getChild("rabbit_lleg");
            this.rabbitRleg = rabbitBone.getChild("rabbit_rleg");
        }

        @Override
        public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            CustomModelsRenderer.copyAngles(head, this.rabbitHead, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(leftArm, this.rabbitLarm, 0.0f, 0.0f, -0.0873f);
            CustomModelsRenderer.copyAngles(rightArm, this.rabbitRarm, 0.0f, 0.0f, 0.0873f);
            CustomModelsRenderer.copyAngles(leftLeg, this.rabbitLleg, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(rightLeg, this.rabbitRleg, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
            this.root.render(matrices, vertices, light, overlay, color);
        }

        private static ModelPart createModel() {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();
            ModelPartData rabbitBone = root.addChild("rabbit_bone", ModelPartBuilder.create().uv(28, 45).cuboid(-5.0f, -13.0f, -5.0f, 10.0f, 11.0f, 8.0f), ModelTransform.origin((float)0.0f, (float)24.0f, (float)0.0f));
            rabbitBone.addChild("rabbit_rleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f), ModelTransform.origin((float)-3.0f, (float)-2.0f, (float)-1.0f));
            rabbitBone.addChild("rabbit_larm", ModelPartBuilder.create().uv(0, 0).cuboid(0.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f), ModelTransform.of((float)5.0f, (float)-13.0f, (float)-1.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
            rabbitBone.addChild("rabbit_rarm", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f), ModelTransform.of((float)-5.0f, (float)-13.0f, (float)-1.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
            rabbitBone.addChild("rabbit_lleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f), ModelTransform.origin((float)3.0f, (float)-2.0f, (float)-1.0f));
            rabbitBone.addChild("rabbit_head", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0f, 0.0f, -4.0f, 6.0f, 1.0f, 6.0f).uv(56, 0).cuboid(-5.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f).uv(56, 0).mirrored().cuboid(3.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f).mirrored(false).uv(0, 45).cuboid(-4.0f, -11.0f, -4.0f, 8.0f, 11.0f, 8.0f).uv(46, 0).cuboid(1.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f).uv(46, 0).cuboid(-4.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f), ModelTransform.origin((float)0.0f, (float)-14.0f, (float)-1.0f));
            return TexturedModelData.of((ModelData)modelData, (int)64, (int)64).createModel();
        }
    }

    private static final class DemonModel
            implements CustomModel {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart leftArm;
        private final ModelPart rightArm;
        private final ModelPart leftLeg;
        private final ModelPart rightLeg;

        private DemonModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("demon_head");
            this.leftArm = root.getChild("demon_left_arm");
            this.rightArm = root.getChild("demon_right_arm");
            this.leftLeg = root.getChild("demon_left_leg");
            this.rightLeg = root.getChild("demon_right_leg");
        }

        @Override
        public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            CustomModelsRenderer.copyAngles(head, this.head, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(leftArm, this.leftArm, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(rightArm, this.rightArm, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(leftLeg, this.leftLeg, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(rightLeg, this.rightLeg, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
            this.root.render(matrices, vertices, light, overlay, color);
        }

        private static ModelPart createModel() {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();
            ModelPartData head = root.addChild("demon_head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0f, -4.0f, -3.0f, 8.0f, 8.0f, 8.0f, new Dilation(0.3f)), ModelTransform.origin((float)0.0f, (float)-6.0f, (float)-1.0f));
            head.addChild("demon_left_horn", ModelPartBuilder.create().uv(32, 8).cuboid(13.4346f, -5.2071f, 2.7071f, 6.0f, 2.0f, 2.0f, new Dilation(0.1f)).uv(0, 0).cuboid(17.4346f, -10.4071f, 2.7071f, 2.0f, 5.0f, 2.0f, new Dilation(0.1f)), ModelTransform.of((float)-8.0f, (float)8.0f, (float)0.0f, (float)-0.3927f, (float)0.3927f, (float)-0.5236f));
            head.addChild("demon_right_horn", ModelPartBuilder.create().uv(32, 8).mirrored().cuboid(-19.4346f, -5.2071f, 2.7071f, 6.0f, 2.0f, 2.0f, new Dilation(0.1f)).mirrored(false).uv(0, 0).mirrored().cuboid(-19.4346f, -10.4071f, 2.7071f, 2.0f, 5.0f, 2.0f, new Dilation(0.1f)).mirrored(false), ModelTransform.of((float)8.0f, (float)8.0f, (float)0.0f, (float)-0.3927f, (float)-0.3927f, (float)0.5236f));
            ModelPartData body = root.addChild("demon_body", ModelPartBuilder.create().uv(0, 16).cuboid(-4.5f, -1.7028f, 1.4696f, 8.0f, 12.0f, 4.0f), ModelTransform.of((float)0.5f, (float)-0.1f, (float)-3.5f, (float)0.1745f, (float)0.0f, (float)0.0f));
            body.addChild("demon_left_wing", ModelPartBuilder.create().uv(40, 12).cuboid(-7.0072f, -0.5972f, 0.7515f, 12.0f, 13.0f, 0.0f), ModelTransform.of((float)8.25f, (float)-2.0f, (float)10.0f, (float)0.0873f, (float)-0.829f, (float)0.1745f));
            body.addChild("demon_right_wing", ModelPartBuilder.create().uv(40, 12).mirrored().cuboid(-4.9928f, -0.5972f, 0.7515f, 12.0f, 13.0f, 0.0f).mirrored(false), ModelTransform.of((float)-9.25f, (float)-2.0f, (float)10.0f, (float)0.0873f, (float)0.829f, (float)-0.1745f));
            root.addChild("demon_left_arm", ModelPartBuilder.create().uv(24, 16).cuboid(-1.1f, -1.05f, 0.0f, 4.0f, 14.0f, 4.0f), ModelTransform.of((float)5.4f, (float)-1.25f, (float)-2.0f, (float)0.0f, (float)0.0f, (float)-0.2182f));
            root.addChild("demon_right_arm", ModelPartBuilder.create().uv(24, 16).mirrored().cuboid(-2.9f, -1.05f, 0.0f, 4.0f, 14.0f, 4.0f).mirrored(false), ModelTransform.of((float)-5.4f, (float)-1.25f, (float)-2.0f, (float)0.0f, (float)0.0f, (float)0.2182f));
            ModelPartData leftLeg = root.addChild("demon_left_leg", ModelPartBuilder.create().uv(48, 22).cuboid(-3.25f, -2.25f, -1.0f, 4.0f, 9.0f, 4.0f), ModelTransform.origin((float)3.0f, (float)10.0f, (float)0.0f));
            ModelPartData leftLeg1 = leftLeg.addChild("demon_left_leg1", ModelPartBuilder.create().uv(34, 34).cuboid(0.95f, 4.6f, 8.0511f, 3.0f, 5.0f, 3.0f), ModelTransform.of((float)-1.7f, (float)-0.1f, (float)-3.55f, (float)-0.5236f, (float)0.0f, (float)0.0f));
            leftLeg1.addChild("demon_left_bone2", ModelPartBuilder.create().uv(26, 0).cuboid(-0.7f, -1.15f, 9.3f, 4.0f, 2.0f, 4.0f).uv(40, 0).cuboid(-0.7f, -1.15f, 7.3f, 4.0f, 2.0f, 2.0f), ModelTransform.of((float)1.4f, (float)15.0f, (float)0.25f, (float)0.5236f, (float)0.0f, (float)0.0f));
            ModelPartData leftBone3 = leftLeg1.addChild("demon_left_bone3", ModelPartBuilder.create(), ModelTransform.of((float)-1.0f, (float)0.0f, (float)-2.0f, (float)0.0f, (float)-0.0873f, (float)-0.2618f));
            leftBone3.addChild("demon_left_bone7", ModelPartBuilder.create().uv(16, 34).cuboid(-0.7911f, -10.1159f, 8.0029f, 4.0f, 4.0f, 5.0f).uv(0, 32).cuboid(-0.7911f, -15.1159f, 4.0029f, 4.0f, 9.0f, 4.0f), ModelTransform.origin((float)1.9f, (float)12.0f, (float)0.25f));
            ModelPartData rightLeg = root.addChild("demon_right_leg", ModelPartBuilder.create().uv(48, 22).mirrored().cuboid(-0.75f, -2.25f, -1.0f, 4.0f, 9.0f, 4.0f).mirrored(false), ModelTransform.origin((float)-3.0f, (float)10.0f, (float)0.0f));
            ModelPartData rightLeg3 = rightLeg.addChild("demon_right_leg3", ModelPartBuilder.create().uv(34, 34).mirrored().cuboid(-3.95f, 4.6f, 8.0511f, 3.0f, 5.0f, 3.0f).mirrored(false), ModelTransform.of((float)1.7f, (float)-0.1f, (float)-3.55f, (float)-0.5236f, (float)0.0f, (float)0.0f));
            rightLeg3.addChild("demon_right_bone4", ModelPartBuilder.create().uv(26, 0).mirrored().cuboid(-3.3f, -1.15f, 9.3f, 4.0f, 2.0f, 4.0f).mirrored(false).uv(40, 0).mirrored().cuboid(-3.3f, -1.15f, 7.3f, 4.0f, 2.0f, 2.0f).mirrored(false), ModelTransform.of((float)-1.4f, (float)15.0f, (float)0.25f, (float)0.5236f, (float)0.0f, (float)0.0f));
            ModelPartData rightBone5 = rightLeg3.addChild("demon_right_bone5", ModelPartBuilder.create(), ModelTransform.of((float)1.0f, (float)0.0f, (float)-2.0f, (float)0.0f, (float)0.0873f, (float)0.2618f));
            rightBone5.addChild("demon_right_bone6", ModelPartBuilder.create().uv(16, 34).mirrored().cuboid(-3.2089f, -10.1159f, 8.0029f, 4.0f, 4.0f, 5.0f).mirrored(false).uv(0, 32).mirrored().cuboid(-3.2089f, -15.1159f, 4.0029f, 4.0f, 9.0f, 4.0f).mirrored(false), ModelTransform.origin((float)-1.9f, (float)12.0f, (float)0.25f));
            return TexturedModelData.of((ModelData)modelData, (int)64, (int)64).createModel();
        }
    }

    private static final class FreddyModel
            implements CustomModel {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart leftArm;
        private final ModelPart rightArm;
        private final ModelPart leftLeg;
        private final ModelPart rightLeg;

        private FreddyModel(ModelPart root) {
            this.root = root;
            ModelPart freddyBody = root.getChild("freddy_body");
            this.head = freddyBody.getChild("freddy_head");
            this.leftArm = freddyBody.getChild("freddy_left_arm");
            this.rightArm = freddyBody.getChild("freddy_right_arm");
            this.leftLeg = freddyBody.getChild("freddy_left_leg");
            this.rightLeg = freddyBody.getChild("freddy_right_leg");
        }

        @Override
        public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            CustomModelsRenderer.copyAngles(head, this.head, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(leftArm, this.leftArm, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(rightArm, this.rightArm, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(leftLeg, this.leftLeg, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(rightLeg, this.rightLeg, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
            this.root.render(matrices, vertices, light, overlay, color);
        }

        private static ModelPart createModel() {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();
            ModelPartData freddyBody = root.addChild("freddy_body", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0f, -14.0f, -1.0f, 2.0f, 24.0f, 2.0f), ModelTransform.origin((float)0.0f, (float)-9.0f, (float)0.0f));
            freddyBody.addChild("freddy_torso", ModelPartBuilder.create().uv(8, 0).cuboid(-6.0f, -9.0f, -4.0f, 12.0f, 18.0f, 8.0f), ModelTransform.of((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0174533f, (float)0.0f, (float)0.0f));
            ModelPartData rightArm = freddyBody.addChild("freddy_right_arm", ModelPartBuilder.create().uv(48, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 10.0f, 2.0f), ModelTransform.of((float)-6.5f, (float)-8.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.2617994f));
            rightArm.addChild("freddy_right_arm_pad", ModelPartBuilder.create().uv(70, 10).cuboid(-2.5f, 0.0f, -2.5f, 5.0f, 9.0f, 5.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            ModelPartData rightArm2 = rightArm.addChild("freddy_right_arm2", ModelPartBuilder.create().uv(90, 20).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f), ModelTransform.of((float)0.0f, (float)9.6f, (float)0.0f, (float)-0.17453292f, (float)0.0f, (float)0.0f));
            rightArm2.addChild("freddy_right_arm_pad2", ModelPartBuilder.create().uv(0, 26).cuboid(-2.5f, 0.0f, -2.5f, 5.0f, 7.0f, 5.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            rightArm2.addChild("freddy_right_hand", ModelPartBuilder.create().uv(20, 26).cuboid(-2.0f, 0.0f, -2.5f, 4.0f, 4.0f, 5.0f), ModelTransform.of((float)0.0f, (float)8.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.05235988f));
            ModelPartData leftArm = freddyBody.addChild("freddy_left_arm", ModelPartBuilder.create().uv(62, 10).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 10.0f, 2.0f), ModelTransform.of((float)6.5f, (float)-8.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.2617994f));
            leftArm.addChild("freddy_left_arm_pad", ModelPartBuilder.create().uv(38, 54).cuboid(-2.5f, 0.0f, -2.5f, 5.0f, 9.0f, 5.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            ModelPartData leftArm2 = leftArm.addChild("freddy_left_arm2", ModelPartBuilder.create().uv(90, 48).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f), ModelTransform.of((float)0.0f, (float)9.6f, (float)0.0f, (float)-0.17453292f, (float)0.0f, (float)0.0f));
            leftArm2.addChild("freddy_left_arm_pad2", ModelPartBuilder.create().uv(0, 58).cuboid(-2.5f, 0.0f, -2.5f, 5.0f, 7.0f, 5.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            leftArm2.addChild("freddy_left_hand", ModelPartBuilder.create().uv(58, 56).cuboid(-1.0f, 0.0f, -2.5f, 4.0f, 4.0f, 5.0f), ModelTransform.of((float)0.0f, (float)8.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.05235988f));
            ModelPartData rightLeg = freddyBody.addChild("freddy_right_leg", ModelPartBuilder.create().uv(90, 8).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 10.0f, 2.0f), ModelTransform.origin((float)-3.3f, (float)12.5f, (float)0.0f));
            rightLeg.addChild("freddy_right_leg_pad", ModelPartBuilder.create().uv(73, 33).cuboid(-3.0f, 0.0f, -3.0f, 6.0f, 9.0f, 6.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            ModelPartData rightLeg2 = rightLeg.addChild("freddy_right_leg2", ModelPartBuilder.create().uv(20, 35).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f), ModelTransform.of((float)0.0f, (float)9.6f, (float)0.0f, (float)0.03490659f, (float)0.0f, (float)0.0f));
            rightLeg2.addChild("freddy_right_leg_pad2", ModelPartBuilder.create().uv(0, 39).cuboid(-2.5f, 0.0f, -3.0f, 5.0f, 7.0f, 6.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            rightLeg2.addChild("freddy_right_foot", ModelPartBuilder.create().uv(22, 39).cuboid(-2.5f, 0.0f, -6.0f, 5.0f, 3.0f, 8.0f), ModelTransform.of((float)0.0f, (float)8.0f, (float)0.0f, (float)-0.03490659f, (float)0.0f, (float)0.0f));
            ModelPartData leftLeg = freddyBody.addChild("freddy_left_leg", ModelPartBuilder.create().uv(54, 10).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 10.0f, 2.0f), ModelTransform.origin((float)3.3f, (float)12.5f, (float)0.0f));
            leftLeg.addChild("freddy_left_leg_pad", ModelPartBuilder.create().uv(48, 39).cuboid(-3.0f, 0.0f, -3.0f, 6.0f, 9.0f, 6.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            ModelPartData leftLeg2 = leftLeg.addChild("freddy_left_leg2", ModelPartBuilder.create().uv(72, 48).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f), ModelTransform.of((float)0.0f, (float)9.6f, (float)0.0f, (float)0.03490659f, (float)0.0f, (float)0.0f));
            leftLeg2.addChild("freddy_left_leg_pad2", ModelPartBuilder.create().uv(16, 50).cuboid(-2.5f, 0.0f, -3.0f, 5.0f, 7.0f, 6.0f), ModelTransform.origin((float)0.0f, (float)0.5f, (float)0.0f));
            leftLeg2.addChild("freddy_left_foot", ModelPartBuilder.create().uv(72, 50).cuboid(-2.5f, 0.0f, -6.0f, 5.0f, 3.0f, 8.0f), ModelTransform.of((float)0.0f, (float)8.0f, (float)0.0f, (float)-0.03490659f, (float)0.0f, (float)0.0f));
            freddyBody.addChild("freddy_crotch", ModelPartBuilder.create().uv(56, 0).cuboid(-5.5f, 0.0f, -3.5f, 11.0f, 3.0f, 7.0f), ModelTransform.origin((float)0.0f, (float)9.5f, (float)0.0f));
            ModelPartData head = freddyBody.addChild("freddy_head", ModelPartBuilder.create().uv(39, 22).cuboid(-5.5f, -8.0f, -4.5f, 11.0f, 8.0f, 9.0f), ModelTransform.origin((float)0.0f, (float)-13.0f, (float)-0.5f));
            head.addChild("freddy_jaw", ModelPartBuilder.create().uv(49, 65).cuboid(-5.0f, 0.0f, -4.5f, 10.0f, 3.0f, 9.0f), ModelTransform.of((float)0.0f, (float)0.5f, (float)0.0f, (float)0.08726646f, (float)0.0f, (float)0.0f));
            head.addChild("freddy_nose", ModelPartBuilder.create().uv(17, 67).cuboid(-4.0f, -2.0f, -3.0f, 8.0f, 4.0f, 3.0f), ModelTransform.origin((float)0.0f, (float)-2.0f, (float)-4.5f));
            ModelPartData earRight = head.addChild("freddy_ear_right", ModelPartBuilder.create().uv(8, 0).cuboid(-1.0f, -3.0f, -0.5f, 2.0f, 3.0f, 1.0f), ModelTransform.of((float)-4.5f, (float)-5.5f, (float)0.0f, (float)0.05235988f, (float)0.0f, (float)-1.0471976f));
            earRight.addChild("freddy_ear_right_pad", ModelPartBuilder.create().uv(85, 0).cuboid(-2.0f, -5.0f, -1.0f, 4.0f, 4.0f, 2.0f), ModelTransform.origin((float)0.0f, (float)-1.0f, (float)0.0f));
            ModelPartData earLeft = head.addChild("freddy_ear_left", ModelPartBuilder.create().uv(40, 0).cuboid(-1.0f, -3.0f, -0.5f, 2.0f, 3.0f, 1.0f), ModelTransform.of((float)4.5f, (float)-5.5f, (float)0.0f, (float)0.05235988f, (float)0.0f, (float)1.0471976f));
            earLeft.addChild("freddy_ear_left_pad", ModelPartBuilder.create().uv(40, 39).cuboid(-2.0f, -5.0f, -1.0f, 4.0f, 4.0f, 2.0f), ModelTransform.origin((float)0.0f, (float)-1.0f, (float)0.0f));
            ModelPartData hat = head.addChild("freddy_hat", ModelPartBuilder.create().uv(70, 24).cuboid(-3.0f, -0.5f, -3.0f, 6.0f, 1.0f, 6.0f), ModelTransform.of((float)0.0f, (float)-8.4f, (float)0.0f, (float)-0.0174533f, (float)0.0f, (float)0.0f));
            hat.addChild("freddy_hat2", ModelPartBuilder.create().uv(78, 61).cuboid(-2.0f, -4.0f, -2.0f, 4.0f, 4.0f, 4.0f), ModelTransform.of((float)0.0f, (float)0.1f, (float)0.0f, (float)-0.0174533f, (float)0.0f, (float)0.0f));
            return TexturedModelData.of((ModelData)modelData, (int)100, (int)80).createModel();
        }
    }

    private static final class AmogusModel
            implements CustomModel {
        private final ModelPart root;
        private final ModelPart leftLeg;
        private final ModelPart rightLeg;

        private AmogusModel(ModelPart root) {
            this.root = root;
            this.leftLeg = root.getChild("amogus_left_leg");
            this.rightLeg = root.getChild("amogus_right_leg");
        }

        @Override
        public void applyAngles(ModelPart head, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg) {
            CustomModelsRenderer.copyAngles(leftLeg, this.leftLeg, 0.0f, 0.0f, 0.0f);
            CustomModelsRenderer.copyAngles(rightLeg, this.rightLeg, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
            this.root.render(matrices, vertices, light, overlay, color);
        }

        private static ModelPart createModel() {
            ModelData modelData = new ModelData();
            ModelPartData root = modelData.getRoot();
            root.addChild("amogus_body", ModelPartBuilder.create().uv(34, 8).cuboid(-4.0f, 6.0f, -3.0f, 8.0f, 12.0f, 6.0f).uv(15, 10).cuboid(-3.0f, 9.0f, 3.0f, 6.0f, 8.0f, 3.0f).uv(26, 0).cuboid(-3.0f, 5.0f, -3.0f, 6.0f, 1.0f, 6.0f), ModelTransform.origin((float)0.0f, (float)0.0f, (float)0.0f));
            root.addChild("amogus_eye", ModelPartBuilder.create().uv(0, 10).cuboid(-3.0f, 7.0f, -4.0f, 6.0f, 4.0f, 1.0f), ModelTransform.origin((float)0.0f, (float)0.0f, (float)0.0f));
            root.addChild("amogus_left_leg", ModelPartBuilder.create().uv(0, 0).cuboid(2.9f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), ModelTransform.origin((float)-2.0f, (float)18.0f, (float)0.0f));
            root.addChild("amogus_right_leg", ModelPartBuilder.create().uv(13, 0).cuboid(-5.9f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f), ModelTransform.origin((float)2.0f, (float)18.0f, (float)0.0f));
            return TexturedModelData.of((ModelData)modelData, (int)64, (int)64).createModel();
        }
    }
}
