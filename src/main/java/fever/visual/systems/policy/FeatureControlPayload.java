package fever.visual.systems.policy;

import java.nio.charset.StandardCharsets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FeatureControlPayload(String json) implements CustomPayload {
   public static final Identifier CHANNEL = Identifier.of("liteapi", "feature-control");
   public static final CustomPayload.Id<FeatureControlPayload> ID = new CustomPayload.Id<>(CHANNEL);
   public static final PacketCodec<PacketByteBuf, FeatureControlPayload> CODEC = PacketCodec.ofStatic(
         FeatureControlPayload::write,
         FeatureControlPayload::read
   );
   private static final int MAX_BYTES = 262_144;

   private static void write(PacketByteBuf buffer, FeatureControlPayload payload) {
      buffer.writeBytes(payload.json.getBytes(StandardCharsets.UTF_8));
   }

   private static FeatureControlPayload read(PacketByteBuf buffer) {
      try {
         int length = buffer.readableBytes();
         if (length <= 0 || length > MAX_BYTES) {
            if (length > 0) {
               buffer.skipBytes(length);
            }
            return new FeatureControlPayload("");
         }
         byte[] bytes = new byte[length];
         buffer.readBytes(bytes);
         return new FeatureControlPayload(new String(bytes, StandardCharsets.UTF_8));
      } catch (RuntimeException exception) {
         if (buffer.isReadable()) {
            buffer.skipBytes(buffer.readableBytes());
         }
         return new FeatureControlPayload("");
      }
   }

   @Override
   public Id<? extends CustomPayload> getId() {
      return ID;
   }
}
