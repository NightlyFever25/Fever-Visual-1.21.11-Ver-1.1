package com.jagrosh.discordipc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class IPCClient {
   private final long clientId;
   private RandomAccessFile pipe;
   private IPCListener listener;
   private volatile boolean connected = false;

   public IPCClient(long clientId) {
      this.clientId = clientId;
   }

   public void setListener(IPCListener listener) {
      this.listener = listener;
   }

   public void connect() {
      Thread thread = new Thread(() -> {
         try {
            for (int i = 0; i < 10; i++) {
               try {
                  this.pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                  break;
               } catch (IOException var9) {
                  if (i == 9) {
                     throw var9;
                  }
               }
            }

            JsonObject handshake = new JsonObject();
            handshake.addProperty("v", 1);
            handshake.addProperty("client_id", String.valueOf(this.clientId));
            this.write(0, handshake.toString());
            byte[] header = new byte[8];
            this.pipe.readFully(header);
            ByteBuffer headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            int opcode = headerBuf.getInt();
            int length = headerBuf.getInt();
            byte[] data = new byte[length];
            this.pipe.readFully(data);
            String response = new String(data);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (json.has("evt") && "READY".equals(json.get("evt").getAsString())) {
               this.connected = true;
               if (this.listener != null) {
                  this.listener.onReady(this);
               }
            }
         } catch (Exception var10) {
         }
      }, "Discord-IPC");
      thread.setDaemon(true);
      thread.start();
   }

   public void sendRichPresence(RichPresence presence) {
      if (this.connected && this.pipe != null) {
         try {
            JsonObject activity = new JsonObject();
            if (presence.getState() != null) {
               activity.addProperty("state", presence.getState());
            }

            if (presence.getDetails() != null) {
               activity.addProperty("details", presence.getDetails());
            }

            if (presence.getStartTimestamp() > 0L) {
               JsonObject timestamps = new JsonObject();
               timestamps.addProperty("start", presence.getStartTimestamp());
               if (presence.getEndTimestamp() > 0L) {
                  timestamps.addProperty("end", presence.getEndTimestamp());
               }

               activity.add("timestamps", timestamps);
            }

            JsonObject assets = new JsonObject();
            if (presence.getLargeImageKey() != null) {
               assets.addProperty("large_image", presence.getLargeImageKey());
               if (presence.getLargeImageText() != null) {
                  assets.addProperty("large_text", presence.getLargeImageText());
               }
            }

            if (presence.getSmallImageKey() != null) {
               assets.addProperty("small_image", presence.getSmallImageKey());
               if (presence.getSmallImageText() != null) {
                  assets.addProperty("small_text", presence.getSmallImageText());
               }
            }

            if (assets.size() > 0) {
               activity.add("assets", assets);
            }

            if (presence.getButtons() != null && presence.getButtons().length > 0) {
               JsonArray buttons = new JsonArray();
               int max = Math.min(2, presence.getButtons().length);

               for (int i = 0; i < max; i++) {
                  RichPresence.Button button = presence.getButtons()[i];
                  if (button != null && button.getLabel() != null && button.getUrl() != null) {
                     JsonObject buttonObj = new JsonObject();
                     buttonObj.addProperty("label", button.getLabel());
                     buttonObj.addProperty("url", button.getUrl());
                     buttons.add(buttonObj);
                  }
               }

               if (buttons.size() > 0) {
                  activity.add("buttons", buttons);
               }
            }

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);
            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");
            payload.add("args", args);
            payload.addProperty("nonce", UUID.randomUUID().toString());
            this.write(1, payload.toString());
         } catch (Exception var9) {
            System.err.println("[DiscordIPC] Error sending presence: " + var9.getMessage());
         }
      }
   }

   private void write(int opcode, String data) throws IOException {
      byte[] bytes = data.getBytes();
      ByteBuffer buffer = ByteBuffer.allocate(8 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
      buffer.putInt(opcode);
      buffer.putInt(bytes.length);
      buffer.put(bytes);
      this.pipe.write(buffer.array());
   }

   public void close() {
      this.connected = false;

      try {
         if (this.pipe != null) {
            this.pipe.close();
         }
      } catch (IOException var2) {
      }
   }

   public boolean isConnected() {
      return this.connected;
   }
}
