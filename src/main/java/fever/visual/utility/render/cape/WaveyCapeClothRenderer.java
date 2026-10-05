package fever.visual.utility.render.cape;

import fever.visual.systems.modules.modules.visuals.Cape;
import fever.visual.utility.game.PlatformUtility;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class WaveyCapeClothRenderer {

    private static final int PART_COUNT = 16;
    private static final float CAPE_LEFT = -0.3125F;
    private static final float CAPE_RIGHT = 0.3125F;
    private static final float CAPE_FRONT = -0.0625F;
    private static final float CAPE_BACK = 0.0F;
    private static final float CAPE_HEIGHT = 1.0F;
    private static final float SEGMENT_HEIGHT = CAPE_HEIGHT / PART_COUNT;
    private static final Map<Integer, ClothState> STATES = new HashMap<>();

    private WaveyCapeClothRenderer() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void render(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            PlayerEntityRenderState state,
            Identifier capeTexture,
            Cape module,
            AbstractClientPlayerEntity player,
            float tickDelta
    ) {
        if (player == null) {
            return;
        }

        tickDelta = MathHelper.clamp(tickDelta, 0.0F, 1.0F);
        ClothState cloth = STATES.computeIfAbsent(player.getId(), ignored -> new ClothState(player.getId()));
        updateSimulation(cloth, player, module);

        Matrix4f[] positionMatrices = new Matrix4f[PART_COUNT];
        for (int part = 0; part < PART_COUNT; part++) {
            matrices.push();
            applySimulationTransform(matrices, player, module, cloth.simulation, tickDelta, part);
            positionMatrices[part] = new Matrix4f(matrices.peek().getPositionMatrix());
            matrices.pop();
        }

        queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(capeTexture),
                (entry, capeConsumer) -> renderSmoothCape(capeConsumer, positionMatrices, light));

        if ((state.id & 15) == 0) {
            cleanup(cloth.lastSeenAge);
        }
    }

    private static void updateSimulation(ClothState cloth, AbstractClientPlayerEntity player, Cape module) {
        cloth.lastSeenAge = player.age;

        boolean reinitialized = cloth.simulation.init(PART_COUNT);
        if (reinitialized || cloth.lastSimulatedAge == Integer.MIN_VALUE) {
            cloth.simulation.applyMovement(1.0F, 1.0F, 0.0F);
            for (int i = 0; i < 5; i++) {
                simulateStep(cloth.simulation, player, module);
            }
            cloth.lastSimulatedAge = player.age;
            return;
        }

        int steps = MathHelper.clamp(player.age - cloth.lastSimulatedAge, 0, 4);
        if (steps == 0 && PlatformUtility.isLunarClient()) {
            long now = System.currentTimeMillis();
            if (cloth.lastSimulatedAtMs <= 0L) {
                cloth.lastSimulatedAtMs = now;
            }
            steps = MathHelper.clamp((int)((now - cloth.lastSimulatedAtMs) / 50L), 0, 4);
            if (steps > 0) {
                cloth.lastSimulatedAtMs += steps * 50L;
            }
        }
        for (int i = 0; i < steps; i++) {
            simulateStep(cloth.simulation, player, module);
        }

        if (steps > 0) {
            cloth.lastSimulatedAge = player.age;
            if (!PlatformUtility.isLunarClient()) {
                cloth.lastSimulatedAtMs = System.currentTimeMillis();
            }
        }
    }

    private static void simulateStep(Simulation3D simulation, AbstractClientPlayerEntity player, Cape module) {
        simulation.configure(module.getPhysicsStiffness(), module.getPhysicsDamping());
        float motionMul = Math.max(0.2F, module.getMotionInfluence());
        float gravityMul = Math.max(0.0F, module.getGravityInfluence());

        double d = player.lastX - player.getX();
        double m = player.lastZ - player.getZ();

        float yaw = player.bodyYaw;
        double sin = MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE);
        double cos = -MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE);

        float heightMul = 6.0F * motionMul;
        float strafeMul = 2.0F * motionMul;

        boolean underwater = player.isSubmergedInWater();
        if (underwater) {
            heightMul *= 2.0F;
            simulation.gravity = 1.8F * gravityMul;
        } else {
            simulation.gravity = 6.0F + 3.0F * gravityMul;
        }

        double fallHack = MathHelper.clamp((player.lastY - player.getY()) * 10.0D, 0.0D, 1.0D);

        Vec2 strafe = new Vec2((float) (player.getX() - player.lastX), (float) (player.getZ() - player.lastZ));
        strafe.rotateDegrees(-player.getYaw());

        double changeX = d * sin + m * cos + fallHack + ((player.isInSneakingPose() && !simulation.sneaking) ? 3.0D : 0.0D);
        double changeY = (player.getY() - player.lastY) * heightMul + ((player.isInSneakingPose() && !simulation.sneaking) ? 1.0D : 0.0D);
        double changeZ = -strafe.x * strafeMul;

        simulation.sneaking = player.isInSneakingPose();
        simulation.gravityDir.set(0.0F, -1.0F, 0.0F);

        float amplifiedX = (float) (changeX * 1.15D);
        float amplifiedY = (float) (changeY * 1.05D);
        float amplifiedZ = (float) (changeZ * 1.15D);

        simulation.applyMovement(amplifiedX, amplifiedY, amplifiedZ);
        simulation.simulate();
    }

    private static void applySimulationTransform(
            MatrixStack matrices,
            AbstractClientPlayerEntity player,
            Cape module,
            Simulation3D simulation,
            float tickDelta,
            int part
    ) {
        matrices.translate(0.0D, 0.0D, 0.125D);

        float x = simulation.points.get(part).getLerpX(tickDelta) - simulation.points.get(0).getLerpX(tickDelta);
        if (x > 0.0F) {
            x = 0.0F;
        }

        float y = simulation.points.get(0).getLerpY(tickDelta) - part - simulation.points.get(part).getLerpY(tickDelta);
        float z = simulation.points.get(0).getLerpZ(tickDelta) - simulation.points.get(part).getLerpZ(tickDelta);

        float partRotation = getPartRotation(tickDelta, part, simulation);
        float wind = getNaturalWindSwing(part, player.isSubmergedInWater(), module);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + wind));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        matrices.translate(-z / PART_COUNT, y / PART_COUNT, x / PART_COUNT);

        float offset = 0.48F / 16.0F;
        matrices.translate(0.0F, offset, -offset);
        matrices.translate(0.0F, part * SEGMENT_HEIGHT, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-partRotation));
        matrices.translate(0.0F, -part * SEGMENT_HEIGHT, 0.0F);
        matrices.translate(0.0F, -offset, offset);
    }

    private static float getPartRotation(float tickDelta, int part, Simulation3D simulation) {
        if (part == PART_COUNT - 1) {
            return getPartRotation(tickDelta, part - 1, simulation);
        }
        Vec3 a = simulation.points.get(part).getLerpedPos(tickDelta);
        Vec3 b = simulation.points.get(part + 1).getLerpedPos(tickDelta);
        Vec3 angle = b.copy().sub(a);
        return (float) (Math.toDegrees(Math.atan2(angle.x, angle.y)) + 180.0D);
    }

    private static float getNaturalWindSwing(int part, boolean underwater, Cape module) {
        float amplitude = module.getWaveAmplitude();
        if (amplitude <= 0.01F) {
            return 0.0F;
        }

        float speed = Math.max(0.05F, module.getWaveSpeed());
        long highlightedPart = (long) ((System.currentTimeMillis() / (underwater ? 9.0D : 3.0D)) * speed) % 360L;
        float relativePart = (float) (part + 1) / PART_COUNT;
        return (float) (Math.sin(Math.toRadians(relativePart * 360.0F - highlightedPart)) * (amplitude * 1.5F));
    }

    private static void renderSmoothCape(VertexConsumer buffer, Matrix4f[] positionMatrices, int light) {
        Vector3f[] frontNormalVecs = new Vector3f[PART_COUNT];
        Vector3f[] backNormalVecs = new Vector3f[PART_COUNT];

        for (int part = 0; part < PART_COUNT; part++) {
            int prev = Math.max(part - 1, 0);
            float y0 = part * SEGMENT_HEIGHT;
            float y1 = (part + 1) * SEGMENT_HEIGHT;

            frontNormalVecs[part] = getNormalVec(
                    positionMatrices[prev], positionMatrices[prev], positionMatrices[part],
                    new Vector3f(CAPE_RIGHT, y0, CAPE_FRONT),
                    new Vector3f(CAPE_LEFT, y0, CAPE_FRONT),
                    new Vector3f(CAPE_RIGHT, y1, CAPE_FRONT),
                    false
            );

            backNormalVecs[part] = getNormalVec(
                    positionMatrices[prev], positionMatrices[prev], positionMatrices[part],
                    new Vector3f(CAPE_RIGHT, y1, CAPE_BACK),
                    new Vector3f(CAPE_LEFT, y1, CAPE_BACK),
                    new Vector3f(CAPE_RIGHT, y0, CAPE_BACK),
                    false
            );
        }

        for (int part = 0; part < PART_COUNT; part++) {
            int prev = Math.max(part - 1, 0);
            float y0 = part * SEGMENT_HEIGHT;
            float y1 = (part + 1) * SEGMENT_HEIGHT;

            if (part == 0) {
                float minU = 0.015625F;
                float maxU = 0.171875F;
                float minV = 0.0F;
                float maxV = 0.03125F;
                Vector3f normal = getNormalVec(
                        positionMatrices[0], positionMatrices[0], positionMatrices[0],
                        new Vector3f(CAPE_RIGHT, 0.0F, CAPE_BACK),
                        new Vector3f(CAPE_LEFT, 0.0F, CAPE_BACK),
                        new Vector3f(CAPE_RIGHT, 0.0F, CAPE_BACK - CAPE_FRONT),
                        false
                );
                addVertex(buffer, positionMatrices[0], CAPE_RIGHT, 0.0F, CAPE_BACK, maxU, maxV, light, normal);
                addVertex(buffer, positionMatrices[0], CAPE_LEFT, 0.0F, CAPE_BACK, minU, maxV, light, normal);
                addVertex(buffer, positionMatrices[0], CAPE_LEFT, 0.0F, CAPE_FRONT, minU, minV, light, normal);
                addVertex(buffer, positionMatrices[0], CAPE_RIGHT, 0.0F, CAPE_FRONT, maxU, minV, light, normal);
            }

            if (part == PART_COUNT - 1) {
                float minU = 0.171875F;
                float maxU = 0.328125F;
                float minV = 0.0F;
                float maxV = 0.03125F;
                Vector3f normal = getNormalVec(
                        positionMatrices[part], positionMatrices[part], positionMatrices[part],
                        new Vector3f(CAPE_RIGHT, 1.0F, CAPE_FRONT),
                        new Vector3f(CAPE_LEFT, 1.0F, CAPE_FRONT),
                        new Vector3f(CAPE_RIGHT, 1.0F, CAPE_BACK),
                        false
                );
                addVertex(buffer, positionMatrices[part], CAPE_RIGHT, 1.0F, CAPE_FRONT, maxU, minV, light, normal);
                addVertex(buffer, positionMatrices[part], CAPE_LEFT, 1.0F, CAPE_FRONT, minU, minV, light, normal);
                addVertex(buffer, positionMatrices[part], CAPE_LEFT, 1.0F, CAPE_BACK, minU, maxV, light, normal);
                addVertex(buffer, positionMatrices[part], CAPE_RIGHT, 1.0F, CAPE_BACK, maxU, maxV, light, normal);
            }

            float minV = 0.03125F * (part + 1);
            float maxV = minV + 0.03125F;

            float leftMinU = 0.0F;
            float leftMaxU = 0.015625F;
            Vector3f leftNormal = getNormalVec(
                    positionMatrices[part], positionMatrices[part], positionMatrices[prev],
                    new Vector3f(CAPE_LEFT, y1, CAPE_BACK),
                    new Vector3f(CAPE_LEFT, y1, CAPE_FRONT),
                    new Vector3f(CAPE_LEFT, y0, CAPE_BACK),
                    false
            );
            addVertex(buffer, positionMatrices[part], CAPE_LEFT, y1, CAPE_BACK, leftMinU, maxV, light, leftNormal);
            addVertex(buffer, positionMatrices[part], CAPE_LEFT, y1, CAPE_FRONT, leftMaxU, maxV, light, leftNormal);
            addVertex(buffer, positionMatrices[prev], CAPE_LEFT, y0, CAPE_FRONT, leftMaxU, minV, light, leftNormal);
            addVertex(buffer, positionMatrices[prev], CAPE_LEFT, y0, CAPE_BACK, leftMinU, minV, light, leftNormal);

            float rightMinU = 0.171875F;
            float rightMaxU = 0.1875F;
            Vector3f rightNormal = getNormalVec(
                    positionMatrices[part], positionMatrices[part], positionMatrices[prev],
                    new Vector3f(CAPE_RIGHT, y1, CAPE_FRONT),
                    new Vector3f(CAPE_RIGHT, y1, CAPE_BACK),
                    new Vector3f(CAPE_RIGHT, y0, CAPE_FRONT),
                    false
            );
            addVertex(buffer, positionMatrices[part], CAPE_RIGHT, y1, CAPE_FRONT, rightMinU, maxV, light, rightNormal);
            addVertex(buffer, positionMatrices[part], CAPE_RIGHT, y1, CAPE_BACK, rightMaxU, maxV, light, rightNormal);
            addVertex(buffer, positionMatrices[prev], CAPE_RIGHT, y0, CAPE_BACK, rightMaxU, minV, light, rightNormal);
            addVertex(buffer, positionMatrices[prev], CAPE_RIGHT, y0, CAPE_FRONT, rightMinU, minV, light, rightNormal);

            float frontMinU = 0.015625F;
            float frontMaxU = 0.171875F;
            Vector3f normalTopFront = frontNormalVecs[part].add(frontNormalVecs[prev], new Vector3f()).div(2.0F);
            Vector3f normalBottomFront = frontNormalVecs[part].add(frontNormalVecs[Math.min(part + 1, PART_COUNT - 1)], new Vector3f()).div(2.0F);
            addVertex(buffer, positionMatrices[prev], CAPE_RIGHT, y0, CAPE_FRONT, frontMaxU, minV, light, normalTopFront);
            addVertex(buffer, positionMatrices[prev], CAPE_LEFT, y0, CAPE_FRONT, frontMinU, minV, light, normalTopFront);
            addVertex(buffer, positionMatrices[part], CAPE_LEFT, y1, CAPE_FRONT, frontMinU, maxV, light, normalBottomFront);
            addVertex(buffer, positionMatrices[part], CAPE_RIGHT, y1, CAPE_FRONT, frontMaxU, maxV, light, normalBottomFront);

            float backMinU = 0.1875F;
            float backMaxU = 0.34375F;
            Vector3f normalTopBack = backNormalVecs[part].add(backNormalVecs[prev], new Vector3f()).div(2.0F);
            Vector3f normalBottomBack = backNormalVecs[part].add(backNormalVecs[Math.min(part + 1, PART_COUNT - 1)], new Vector3f()).div(2.0F);
            addVertex(buffer, positionMatrices[prev], CAPE_RIGHT, y0, CAPE_BACK, backMinU, minV, light, normalTopBack);
            addVertex(buffer, positionMatrices[prev], CAPE_LEFT, y0, CAPE_BACK, backMaxU, minV, light, normalTopBack);
            addVertex(buffer, positionMatrices[part], CAPE_LEFT, y1, CAPE_BACK, backMaxU, maxV, light, normalBottomBack);
            addVertex(buffer, positionMatrices[part], CAPE_RIGHT, y1, CAPE_BACK, backMinU, maxV, light, normalBottomBack);
        }
    }

    private static Vector3f getNormalVec(
            Matrix4f matrix1,
            Matrix4f matrix2,
            Matrix4f matrix3,
            Vector3f point1,
            Vector3f point2,
            Vector3f point3,
            boolean inverse
    ) {
        Vector3f p1 = transform(matrix1, point1);
        Vector3f p2 = transform(matrix2, point2);
        Vector3f p3 = transform(matrix3, point3);

        p2.sub(p1);
        p3.sub(p1);
        p2.cross(p3);
        if (p2.lengthSquared() < 1.0E-6F) {
            return new Vector3f(0.0F, 0.0F, 1.0F);
        }

        p2.normalize();
        if (inverse) {
            p2.mul(-1.0F);
        }
        return p2;
    }

    private static Vector3f transform(Matrix4f matrix, Vector3f vector) {
        Vector4f vec4 = new Vector4f(vector.x, vector.y, vector.z, 1.0F).mul(matrix);
        return new Vector3f(vec4.x, vec4.y, vec4.z);
    }

    private static void addVertex(
            VertexConsumer buffer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light,
            Vector3f normal
    ) {
        buffer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal.x, normal.y, normal.z);
    }

    private static void cleanup(int currentAge) {
        Iterator<Map.Entry<Integer, ClothState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ClothState> entry = iterator.next();
            if (currentAge - entry.getValue().lastSeenAge > 400) {
                iterator.remove();
            }
        }
    }

    private static final class ClothState {
        private final int id;
        private final Simulation3D simulation = new Simulation3D();
        private int lastSeenAge;
        private int lastSimulatedAge = Integer.MIN_VALUE;
        private long lastSimulatedAtMs;

        private ClothState(int id) {
            this.id = id;
        }
    }

    private static final class Simulation3D {
        private final List<Point> points = new ArrayList<>();
        private final List<Stick> sticks = new ArrayList<>();
        private final Vec3 gravityDir = new Vec3(0.0F, -1.0F, 0.0F);
        private float gravity = 14.0F;
        private int iterations = 3;
        private float stiffness = 0.22F;
        private float damping = 0.82F;
        private boolean sneaking;

        private void configure(float stiffness, float damping) {
            this.stiffness = MathHelper.clamp(stiffness, 0.05F, 0.45F);
            this.damping = MathHelper.clamp(damping, 0.55F, 0.95F);
            float normalized = (this.stiffness - 0.05F) / 0.40F;
            this.iterations = MathHelper.clamp(2 + Math.round(normalized * 4.0F), 2, 6);
        }

        private boolean init(int partCount) {
            if (this.points.size() == partCount) {
                return false;
            }

            this.points.clear();
            this.sticks.clear();

            for (int i = 0; i < partCount; i++) {
                Point point = new Point();
                point.position.set(-i, -i, 0.0F);
                point.previous.set(-i, -i, 0.0F);
                point.locked = i == 0;
                this.points.add(point);

                if (i > 0) {
                    this.sticks.add(new Stick(this.points.get(i - 1), point, 1.0F));
                }
            }

            return true;
        }

        private void applyMovement(float x, float y, float z) {
            Point base = this.points.get(0);
            base.previous.copy(base.position);
            base.position.add(x, y, z);
        }

        private void simulate() {
            applyGravity();
            preventClipping();
            preventSelfClipping();
            applyConstraints();
            preventSelfClipping();
            preventHardBends();
            limitLength();
            relaxToRest();
        }

        private void applyGravity() {
            float deltaTime = 0.05F;
            Vec3 down = this.gravityDir.copy().mul(this.gravity * deltaTime);
            for (Point point : this.points) {
                if (point.locked) {
                    continue;
                }
                float velocityX = (point.position.x - point.previous.x) * this.damping;
                float velocityY = (point.position.y - point.previous.y) * this.damping;
                float velocityZ = (point.position.z - point.previous.z) * this.damping;
                point.previous.copy(point.position);
                point.position.add(velocityX, velocityY, velocityZ).add(down);
            }
        }

        private void applyConstraints() {
            for (int i = 0; i < this.iterations; i++) {
                for (int s = this.sticks.size() - 1; s >= 0; s--) {
                    Stick stick = this.sticks.get(s);
                    Vec3 center = stick.a.position.copy().add(stick.b.position).div(2.0F);
                    Vec3 dir = stick.a.position.copy().sub(stick.b.position).normalize();

                    if (!stick.a.locked) {
                        stick.a.position = center.copy().add(dir.copy().mul(stick.length / 2.0F));
                    }

                    if (!stick.b.locked) {
                        stick.b.position = center.copy().sub(dir.copy().mul(stick.length / 2.0F));
                    }
                }
            }
        }

        private void limitLength() {
            for (Stick stick : this.sticks) {
                Vec3 dir = stick.a.position.copy().sub(stick.b.position).normalize();
                if (!stick.b.locked) {
                    stick.b.position = stick.a.position.copy().sub(dir.mul(stick.length));
                }
            }
        }

        private void preventClipping() {
            Point base = this.points.get(0);
            for (int i = 1; i < this.points.size(); i++) {
                Point point = this.points.get(i);

                if (point.position.x - base.position.x > 0.0F) {
                    point.position.x = base.position.x;
                }

                float t = (float) i / (float) this.points.size();
                float maxZ = t * t * 5.0F;
                float zDiff = base.position.z - point.position.z;
                if (zDiff > maxZ) {
                    point.position.z = base.position.z - maxZ;
                }
                if (zDiff < -maxZ) {
                    point.position.z = base.position.z + maxZ;
                }
            }
        }

        private void preventSelfClipping() {
            boolean clipped;
            int runs = 0;
            do {
                clipped = false;

                for (int a = 0; a < this.points.size(); a++) {
                    for (int b = a + 1; b < this.points.size(); b++) {
                        Point pA = this.points.get(a);
                        Point pB = this.points.get(b);

                        Vec3 delta = pA.position.copy().sub(pB.position);
                        if (delta.lengthSquared() >= 0.99F) {
                            continue;
                        }

                        clipped = true;
                        runs++;

                        delta.normalize();
                        Vec3 center = pA.position.copy().add(pB.position).div(2.0F);
                        if (!pA.locked) {
                            pA.position = center.copy().add(delta.copy().mul(0.5F));
                        }
                        if (!pB.locked) {
                            pB.position = center.copy().sub(delta.copy().mul(0.5F));
                        }
                    }
                }
            } while (clipped && runs < 32);
        }

        private void preventHardBends() {
            float maxBend = 20.0F;
            for (int i = 1; i < this.points.size() - 2; i++) {
                Vec3 middle = this.points.get(i).position;
                Vec3 prev = this.points.get(i - 1).position;
                Vec3 next = this.points.get(i + 1).position;

                float abx = prev.x - middle.x;
                float aby = prev.y - middle.y;
                float cbx = prev.x - next.x;
                float cby = prev.y - next.y;

                float dot = abx * cbx + aby * cby;
                float cross = abx * cby - aby * cbx;
                double angle = Math.toDegrees(Math.atan2(cross, dot));

                if (angle < -maxBend) {
                    this.points.get(i + 1).position = rotateAround(middle, prev, (float) (-maxBend * 2.0F));
                }
                if (angle > maxBend) {
                    this.points.get(i + 1).position = rotateAround(middle, prev, (float) (maxBend * 2.0F));
                }
            }
        }

        private Vec3 rotateAround(Vec3 middle, Vec3 prev, float degrees) {
            Vec3 dir = middle.copy().sub(prev);
            dir.rotateDegrees(degrees).add(middle);
            return dir;
        }

        private void relaxToRest() {
            if (this.points.size() < 2) {
                return;
            }

            Point base = this.points.get(0);
            float baseX = base.position.x;
            float baseZ = base.position.z;

            float stiffnessScale = MathHelper.clamp(this.stiffness / 0.22F, 0.2F, 2.0F);
            float xRecovery = MathHelper.clamp(0.08F * stiffnessScale, 0.02F, 0.16F);
            float zRecovery = MathHelper.clamp(0.12F * stiffnessScale, 0.03F, 0.24F);

            for (int i = 1; i < this.points.size(); i++) {
                Point point = this.points.get(i);
                float progress = (float) i / (float) (this.points.size() - 1);

                float restX = baseX - progress * 0.03F;
                float restZ = baseZ;

                // Soft recovery force: cape returns to idle pose instead of freezing in bent state.
                point.position.x = MathHelper.lerp(xRecovery, point.position.x, restX);
                point.position.z = MathHelper.lerp(zRecovery, point.position.z, restZ);
            }
        }
    }

    private static final class Stick {
        private final Point a;
        private final Point b;
        private final float length;

        private Stick(Point a, Point b, float length) {
            this.a = a;
            this.b = b;
            this.length = length;
        }
    }

    private static final class Point {
        private Vec3 position = new Vec3(0.0F, 0.0F, 0.0F);
        private Vec3 previous = new Vec3(0.0F, 0.0F, 0.0F);
        private boolean locked;

        private float getLerpX(float delta) {
            return MathHelper.lerp(delta, this.previous.x, this.position.x);
        }

        private float getLerpY(float delta) {
            return MathHelper.lerp(delta, this.previous.y, this.position.y);
        }

        private float getLerpZ(float delta) {
            return MathHelper.lerp(delta, this.previous.z, this.position.z);
        }

        private Vec3 getLerpedPos(float delta) {
            return new Vec3(getLerpX(delta), getLerpY(delta), getLerpZ(delta));
        }
    }

    private static final class Vec2 {
        private float x;
        private float y;

        private Vec2(float x, float y) {
            this.x = x;
            this.y = y;
        }

        private void rotateDegrees(float degrees) {
            float rad = degrees * MathHelper.RADIANS_PER_DEGREE;
            float cos = MathHelper.cos(rad);
            float sin = MathHelper.sin(rad);
            float ox = this.x;
            float oy = this.y;
            this.x = cos * ox - sin * oy;
            this.y = sin * ox + cos * oy;
        }
    }

    private static final class Vec3 {
        private float x;
        private float y;
        private float z;

        private Vec3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private Vec3 set(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        private Vec3 copy() {
            return new Vec3(this.x, this.y, this.z);
        }

        private Vec3 copy(Vec3 other) {
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            return this;
        }

        private Vec3 add(float x, float y, float z) {
            this.x += x;
            this.y += y;
            this.z += z;
            return this;
        }

        private Vec3 add(Vec3 other) {
            return add(other.x, other.y, other.z);
        }

        private Vec3 sub(Vec3 other) {
            this.x -= other.x;
            this.y -= other.y;
            this.z -= other.z;
            return this;
        }

        private Vec3 mul(float value) {
            this.x *= value;
            this.y *= value;
            this.z *= value;
            return this;
        }

        private Vec3 div(float value) {
            if (Math.abs(value) > 1.0E-6F) {
                this.x /= value;
                this.y /= value;
                this.z /= value;
            }
            return this;
        }

        private float lengthSquared() {
            return this.x * this.x + this.y * this.y + this.z * this.z;
        }

        private Vec3 normalize() {
            float lenSq = lengthSquared();
            if (lenSq < 1.0E-6F) {
                return this.set(0.0F, 0.0F, 0.0F);
            }
            float inv = MathHelper.inverseSqrt(lenSq);
            this.x *= inv;
            this.y *= inv;
            this.z *= inv;
            return this;
        }

        private Vec3 rotateDegrees(float degrees) {
            float rad = degrees * MathHelper.RADIANS_PER_DEGREE;
            float cos = MathHelper.cos(rad);
            float sin = MathHelper.sin(rad);
            float ox = this.x;
            float oy = this.y;
            this.x = cos * ox - sin * oy;
            this.y = sin * ox + cos * oy;
            return this;
        }
    }
}
