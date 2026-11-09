package tr.kozalaksmp.achievements;
import org.bukkit.plugin.java.JavaPlugin;
public final class KozalakAchievements extends JavaPlugin {
 @Override public void onEnable() {
  if (!getDataFolder().exists() && !getDataFolder().mkdirs()) throw new IllegalStateException("Veri klasoru olusturulamadi");
  saveDefaultConfig();
  new AdvancementRewards(this);
 }
}
