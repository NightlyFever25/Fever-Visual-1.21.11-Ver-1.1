package fever.visual.ui.mainmenu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fever.visual.mixin.minecraft.client.IMinecraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountManager {

    private final List<Account> accounts = new ArrayList<>();
    private Account current;
    private final File file;

    public AccountManager() {
        File configsFolder = new File(MinecraftClient.getInstance().runDirectory, "fevervisual");
        if (!configsFolder.exists()) {
            configsFolder.mkdir();
        }
        this.file = new File(configsFolder, "accounts.json");
        load();
    }

    public void add(Account account) {
        accounts.add(account);
        save();
    }

    public void remove(Account account) {
        accounts.remove(account);
        if (account == current) current = null;
        save();
    }

    public void addAccount(String username) {
        Account account = new Account(username);
        add(account);
    }

    public void removeAccount(Account account) {
        remove(account);
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public Account getCurrent() {
        return current;
    }

    public void select(Account account) {
        this.current = account;
        account.setLastUsed(System.currentTimeMillis());
        changeSession(account.getUsername());
        save();
    }

    public void toggleFavorite(Account account) {
        account.setFavorite(!account.isFavorite());
        save();
    }

    public Account getAccountByName(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }
        return null;
    }

    public void clearAll() {
        accounts.clear();
        current = null;
        save();
    }

    private void changeSession(String username) {
        MinecraftClient mc = MinecraftClient.getInstance();

        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));

        Session newSession = new Session(
                username,
                uuid,
                "",
                Optional.empty(),
                Optional.empty()
        );

        ((IMinecraftClient) mc).setSession(newSession);
    }

    private void save() {
        try {
            if (!file.exists() && !file.createNewFile()) {
                throw new IOException("Failed to create accounts file: " + file.getAbsolutePath());
            }

            JsonObject json = new JsonObject();
            JsonArray accountsArray = new JsonArray();

            for (Account account : accounts) {
                JsonObject accountObject = new JsonObject();
                accountObject.addProperty("username", account.getUsername());
                accountObject.addProperty("favorite", account.isFavorite());
                accountObject.addProperty("lastUsed", account.getLastUsed());
                accountsArray.add(accountObject);
            }

            json.add("accounts", accountsArray);
            if (current != null) {
                json.addProperty("current", current.getUsername());
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (json.has("accounts")) {
                JsonArray accountsArray = json.getAsJsonArray("accounts");

                for (JsonElement element : accountsArray) {
                    JsonObject accountObject = element.getAsJsonObject();
                    String username = accountObject.get("username").getAsString();
                    boolean favorite = accountObject.has("favorite") && accountObject.get("favorite").getAsBoolean();
                    long lastUsed = accountObject.has("lastUsed") ? accountObject.get("lastUsed").getAsLong() : 0;
                    accounts.add(new Account(username, favorite, lastUsed));
                }
            }

            if (json.has("current")) {
                String currentUsername = json.get("current").getAsString();
                for (Account account : accounts) {
                    if (account.getUsername().equals(currentUsername)) {
                        this.current = account;
                        changeSession(account.getUsername());
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
