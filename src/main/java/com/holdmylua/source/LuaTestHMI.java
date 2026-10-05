package com.holdmylua.source;

import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

public final class LuaTestHMI {
    public static final String MOD_ID = "fevervisual";
    public static Matrix4f matricesMain = new Matrix4f();
    public static Matrix4f matricesOff = new Matrix4f();
    public static float tickProgress = 0.0F;
    public static MinecraftClient client = MinecraftClient.getInstance();
    public static float prevTime = 0.0F;
    public static float deltaTime = 0.0F;

    private LuaTestHMI() {
    }
}
