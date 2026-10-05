package com.holdmyitems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

public class HoldMyItems implements ModInitializer {
   public static final String MOD_ID = "hold-my-items";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   private static double prevTime = 0.0D;
   public static double deltaTime = 0.0D;
   private static boolean initialized;

   public void onInitialize() {
      if (initialized) {
         return;
      }
      initialized = true;
      Identifier packId = Identifier.of("fevervisual", "handmyitems");
      FabricLoader.getInstance().getModContainer("fevervisual").ifPresent(container ->
              ResourceLoader.registerBuiltinPack(packId, container, Text.of("HoldMyItems"), PackActivationType.DEFAULT_ENABLED)
      );
      WorldRenderEvents.START_MAIN.register((context) -> {
         double currentTime = GLFW.glfwGetTime();
         deltaTime = currentTime - prevTime;
         prevTime = currentTime;
         if (MinecraftClient.getInstance().isPaused()) {
            deltaTime = 0.0D;
         } else {
            deltaTime = Math.min(0.05, deltaTime);
         }
      });
      LOGGER.info("HoldMyItems initialized as part of Fever Visual!");
   }
}
