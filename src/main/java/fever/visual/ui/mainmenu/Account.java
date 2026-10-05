package fever.visual.ui.mainmenu;

public class Account {
    private String username;
    private boolean favorite;
    private long lastUsed;

    public Account(String username) {
        this.username = username;
        this.favorite = false;
        this.lastUsed = 0;
    }

    public Account(String username, boolean favorite, long lastUsed) {
        this.username = username;
        this.favorite = favorite;
        this.lastUsed = lastUsed;
    }

    public String getUsername() {
        return username;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}