package tr.kozalaksmp.achievements;

import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class AdvancementRewards implements Listener, CommandExecutor {
    private static final int PAGE_SIZE = 28;
    private static final int[] ENTRY_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private static final String[] CATEGORIES = {"story", "nether", "end", "adventure", "husbandry"};
    private static final String[] CATEGORY_NAMES = {"Minecraft", "Nether", "The End", "Macera", "Çiftçilik"};
    private static final org.bukkit.Material[] CATEGORY_ICONS = {
            org.bukkit.Material.GRASS_BLOCK, org.bukkit.Material.NETHERRACK,
            org.bukkit.Material.END_STONE, org.bukkit.Material.IRON_SWORD,
            org.bukkit.Material.WHEAT
    };
    private static final int[] CATEGORY_SLOTS = {11, 13, 15, 29, 33};
    private final JavaPlugin plugin;
    private final File ledgerFile;
    private final YamlConfiguration ledger;

    private static final class PageHolder implements InventoryHolder {
        private final int page;
        private final int category;
        private Inventory inventory;
        private PageHolder(int category, int page) { this.category = category; this.page = page; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }

    public AdvancementRewards(JavaPlugin plugin) {
        this.plugin = plugin;
        ledgerFile = new File(plugin.getDataFolder(), "advancement-rewards.yml");
        File previous = new File(plugin.getDataFolder().getParentFile(), "KozalakSettings/advancement-rewards.yml");
        if (!ledgerFile.exists() && previous.isFile()) {
            try { java.nio.file.Files.copy(previous.toPath(), ledgerFile.toPath()); }
            catch (IOException e) { throw new IllegalStateException("Eski odul kayitlari aktarilamadi", e); }
        }
        ledger = YamlConfiguration.loadConfiguration(ledgerFile);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        java.util.Objects.requireNonNull(plugin.getCommand("basarimlar")).setExecutor(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> rewardCompleted(player), 20L);
        }
    }

    private List<Advancement> advancements() {
        List<Advancement> result = new ArrayList<>();
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            NamespacedKey key = advancement.getKey();
            if (!key.getNamespace().equals("minecraft")) continue;
            if (advancement.getDisplay() == null) continue;
            result.add(advancement);
        }
        result.sort(Comparator.comparing(a -> a.getKey().toString()));
        return result;
    }

    // Individually curated rewards: time, resources, exploration, luck and skill.
    // New vanilla IDs are logged so they can be evaluated rather than silently
    // receiving the same category-wide reward as unrelated advancements.
    private static final java.util.Map<String, Integer> REWARDS = java.util.Map.ofEntries(
            java.util.Map.entry("story/root", 5),
            java.util.Map.entry("story/mine_stone", 5),
            java.util.Map.entry("story/upgrade_tools", 7),
            java.util.Map.entry("story/smelt_iron", 10),
            java.util.Map.entry("story/obtain_armor", 12),
            java.util.Map.entry("story/lava_bucket", 15),
            java.util.Map.entry("story/iron_tools", 15),
            java.util.Map.entry("story/deflect_arrow", 18),
            java.util.Map.entry("story/form_obsidian", 20),
            java.util.Map.entry("story/mine_diamond", 24),
            java.util.Map.entry("story/enter_the_nether", 30),
            java.util.Map.entry("story/shiny_gear", 35),
            java.util.Map.entry("story/enchant_item", 28),
            java.util.Map.entry("story/cure_zombie_villager", 65),
            java.util.Map.entry("story/follow_ender_eye", 45),
            java.util.Map.entry("story/enter_the_end", 55),
            java.util.Map.entry("nether/root", 5),
            java.util.Map.entry("nether/return_to_sender", 95),
            java.util.Map.entry("nether/find_bastion", 35),
            java.util.Map.entry("nether/obtain_ancient_debris", 55),
            java.util.Map.entry("nether/fast_travel", 100),
            java.util.Map.entry("nether/find_fortress", 35),
            java.util.Map.entry("nether/obtain_crying_obsidian", 18),
            java.util.Map.entry("nether/distract_piglin", 12),
            java.util.Map.entry("nether/ride_strider", 30),
            java.util.Map.entry("nether/ride_strider_in_overworld_lava", 145),
            java.util.Map.entry("nether/explore_nether", 190),
            java.util.Map.entry("nether/loot_bastion", 45),
            java.util.Map.entry("nether/use_lodestone", 40),
            java.util.Map.entry("nether/netherite_armor", 125),
            java.util.Map.entry("nether/get_wither_skull", 65),
            java.util.Map.entry("nether/obtain_blaze_rod", 35),
            java.util.Map.entry("nether/charge_respawn_anchor", 35),
            java.util.Map.entry("nether/summon_wither", 95),
            java.util.Map.entry("nether/brew_potion", 25),
            java.util.Map.entry("nether/all_potions", 155),
            java.util.Map.entry("nether/all_effects", 300),
            java.util.Map.entry("nether/create_beacon", 90),
            java.util.Map.entry("nether/create_full_beacon", 170),
            java.util.Map.entry("nether/uneasy_alliance", 180),
            java.util.Map.entry("end/root", 5),
            java.util.Map.entry("end/kill_dragon", 95),
            java.util.Map.entry("end/dragon_egg", 105),
            java.util.Map.entry("end/enter_end_gateway", 65),
            java.util.Map.entry("end/respawn_dragon", 110),
            java.util.Map.entry("end/dragon_breath", 45),
            java.util.Map.entry("end/find_end_city", 75),
            java.util.Map.entry("end/elytra", 85),
            java.util.Map.entry("end/levitate", 135),
            java.util.Map.entry("adventure/root", 5),
            java.util.Map.entry("adventure/sleep_in_bed", 8),
            java.util.Map.entry("adventure/trade", 15),
            java.util.Map.entry("adventure/kill_a_mob", 10),
            java.util.Map.entry("adventure/shoot_arrow", 12),
            java.util.Map.entry("adventure/throw_trident", 35),
            java.util.Map.entry("adventure/summon_iron_golem", 35),
            java.util.Map.entry("adventure/totem_of_undying", 65),
            java.util.Map.entry("adventure/adventuring_time", 260),
            java.util.Map.entry("adventure/kill_all_mobs", 245),
            java.util.Map.entry("adventure/sniper_duel", 110),
            java.util.Map.entry("adventure/very_very_frightening", 95),
            java.util.Map.entry("adventure/arbalistic", 220),
            java.util.Map.entry("adventure/two_birds_one_arrow", 130),
            java.util.Map.entry("adventure/bullseye", 120),
            java.util.Map.entry("adventure/hero_of_the_village", 75),
            java.util.Map.entry("adventure/voluntary_exile", 35),
            java.util.Map.entry("adventure/trade_at_world_height", 130),
            java.util.Map.entry("adventure/play_jukebox_in_meadows", 45),
            java.util.Map.entry("adventure/walk_on_powder_snow_with_leather_boots", 18),
            java.util.Map.entry("adventure/light_ning_rod", 35),
            java.util.Map.entry("adventure/lightning_rod_with_villager_no_fire", 50),
            java.util.Map.entry("adventure/avoid_vibration", 35),
            java.util.Map.entry("adventure/fall_from_world_height", 100),
            java.util.Map.entry("adventure/read_power_from_chiseled_bookshelf", 15),
            java.util.Map.entry("adventure/craft_decorated_pot_using_only_sherds", 50),
            java.util.Map.entry("adventure/salvage_sherd", 25),
            java.util.Map.entry("adventure/trim_with_any_armor_pattern", 35),
            java.util.Map.entry("adventure/trim_with_all_exclusive_armor_patterns", 195),
            java.util.Map.entry("adventure/use_lodestone", 40),
            java.util.Map.entry("adventure/spyglass_at_parrot", 25),
            java.util.Map.entry("adventure/spyglass_at_ghast", 45),
            java.util.Map.entry("adventure/spyglass_at_dragon", 100),
            java.util.Map.entry("adventure/ride_a_boat_with_a_goat", 30),
            java.util.Map.entry("adventure/use_wind_charge", 30),
            java.util.Map.entry("adventure/under_lock_and_key", 45),
            java.util.Map.entry("adventure/blowback", 110),
            java.util.Map.entry("adventure/overoverkill", 130),
            java.util.Map.entry("adventure/who_needs_rockets", 100),
            java.util.Map.entry("adventure/heart_transplanter", 90),
            java.util.Map.entry("husbandry/root", 5),
            java.util.Map.entry("husbandry/plant_seed", 7),
            java.util.Map.entry("husbandry/breed_an_animal", 12),
            java.util.Map.entry("husbandry/tame_an_animal", 15),
            java.util.Map.entry("husbandry/fishy_business", 12),
            java.util.Map.entry("husbandry/tactical_fishing", 22),
            java.util.Map.entry("husbandry/safely_harvest_honey", 25),
            java.util.Map.entry("husbandry/silk_touch_nest", 35),
            java.util.Map.entry("husbandry/wax_on", 25),
            java.util.Map.entry("husbandry/wax_off", 28),
            java.util.Map.entry("husbandry/axolotl_in_a_bucket", 25),
            java.util.Map.entry("husbandry/kill_axolotl_target", 65),
            java.util.Map.entry("husbandry/make_a_sign_glow", 15),
            java.util.Map.entry("husbandry/ride_a_boat_with_a_goat", 30),
            java.util.Map.entry("husbandry/balanced_diet", 245),
            java.util.Map.entry("husbandry/bred_all_animals", 205),
            java.util.Map.entry("husbandry/complete_catalogue", 190),
            java.util.Map.entry("husbandry/froglights", 120),
            java.util.Map.entry("husbandry/leash_all_frog_variants", 75),
            java.util.Map.entry("husbandry/obtain_netherite_hoe", 90),
            java.util.Map.entry("husbandry/break_diamond_hoe", 90),
            java.util.Map.entry("husbandry/eat_cake", 10),
            java.util.Map.entry("husbandry/breed_all_frogs", 70),
            java.util.Map.entry("husbandry/tadpole_in_a_bucket", 20),
            java.util.Map.entry("husbandry/plant_any_sniffer_seed", 40),
            java.util.Map.entry("husbandry/feed_snifflet", 30),
            java.util.Map.entry("husbandry/obtain_sniffer_egg", 55),
            java.util.Map.entry("husbandry/allay_deliver_item_to_player", 55),
            java.util.Map.entry("husbandry/allay_deliver_cake_to_note_block", 80),
            java.util.Map.entry("husbandry/repair_wolf_armor", 35),
            java.util.Map.entry("husbandry/remove_wolf_armor", 20),
            java.util.Map.entry("husbandry/whole_pack", 135),
            java.util.Map.entry("husbandry/place_dried_ghast_in_water", 30),
            java.util.Map.entry("husbandry/dried_ghast_in_water", 30),
            java.util.Map.entry("husbandry/happy_ghast", 55),
            java.util.Map.entry("husbandry/craft_harness", 20)
    );
    private final java.util.Set<String> unknownAdvancements = new java.util.HashSet<>();

    private int points(Advancement advancement) {
        String key = advancement.getKey().getKey();
        Integer reward = REWARDS.get(key);
        if (reward != null) return reward;
        if (unknownAdvancements.add(key))
            plugin.getLogger().warning("Yeni veya fiyatlandirilmamis basarim: " + key
                    + " (gecici 5 Puan). Listeye ozel deger ekleyin.");
        return 5;
    }

    private String ledgerKey(Player player, Advancement advancement) {
        return "players." + player.getUniqueId() + "." + advancement.getKey().getNamespace()
                + "." + advancement.getKey().getKey().replace('/', '.');
    }

    private boolean completed(Player player, Advancement advancement) {
        return player.getAdvancementProgress(advancement).isDone();
    }

    private void saveLedger() throws IOException {
        ledger.save(ledgerFile);
    }

    private boolean givePoints(UUID uuid, int amount) {
        org.bukkit.plugin.Plugin points = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (points == null || !points.isEnabled()) return false;
        try {
            Object api = points.getClass().getMethod("getAPI").invoke(points);
            Method give = api.getClass().getMethod("give", UUID.class, int.class);
            Object result = give.invoke(api, uuid, amount);
            return result instanceof Boolean success && success;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "PlayerPoints API ile odul verilemedi.", ex);
            return false;
        }
    }

    private void reward(Player player, Advancement advancement, boolean announce) {
        if (!player.isOnline() || !completed(player, advancement)) return;
        String path = ledgerKey(player, advancement);
        // PENDING is persisted before transfer to avoid duplicate payment after a crash.
        // A pending entry is intentionally not retried automatically: staff can audit it.
        if (ledger.contains(path)) return;
        ledger.set(path, "pending");
        try {
            saveLedger();
        } catch (IOException ex) {
            ledger.set(path, null);
            plugin.getLogger().log(Level.SEVERE, "Basarim odul kaydi yazilamadi; odul verilmedi.", ex);
            return;
        }
        int amount = points(advancement);
        if (!givePoints(player.getUniqueId(), amount)) {
            ledger.set(path, null);
            try { saveLedger(); } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Basarim odul kaydi temizlenemedi.", ex);
            }
            return;
        }
        ledger.set(path, "paid");
        try {
            saveLedger();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Basarim odulu verildi fakat kayit tamamlanamadi: " + path, ex);
        }
        if (announce) {
            player.sendMessage(Component.text("Basarim odulu: +" + amount + " Puan!", NamedTextColor.GREEN));
        }
    }

    private void rewardCompleted(Player player) {
        if (!player.isOnline()) return;
        for (Advancement advancement : advancements()) {
            if (completed(player, advancement)) reward(player, advancement, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        if (!advancement.getKey().getNamespace().equals("minecraft") || advancement.getDisplay() == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> reward(event.getPlayer(), advancement, true));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> rewardCompleted(event.getPlayer()), 40L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut oyuncular icindir.");
            return true;
        }
        rewardCompleted(player);
        openCategories(player);
        return true;
    }

    private List<Advancement> inCategory(int category) {
        String prefix = CATEGORIES[category] + "/";
        List<Advancement> result = new ArrayList<>();
        for (Advancement advancement : advancements()) {
            if (advancement.getKey().getKey().startsWith(prefix)) result.add(advancement);
        }
        return result;
    }

    private int completedCount(Player player, List<Advancement> list) {
        int count = 0;
        for (Advancement advancement : list) if (completed(player, advancement)) count++;
        return count;
    }

    private void openCategories(Player player) {
        PageHolder holder = new PageHolder(-1, 0);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("✦ Maceracının Günlüğü ✦", NamedTextColor.DARK_GREEN));
        holder.inventory = inventory;
        decorate(inventory);
        inventory.setItem(4, navigation(org.bukkit.Material.WRITABLE_BOOK, "Maceracının Günlüğü"));
        List<Advancement> all = new ArrayList<>();
        for (int i = 0; i < CATEGORIES.length; i++) {
            List<Advancement> entries = inCategory(i);
            all.addAll(entries);
            int done = completedCount(player, entries);
            ItemStack item = new ItemStack(CATEGORY_ICONS[i]);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("✦ " + CATEGORY_NAMES[i] + " ✦", NamedTextColor.GOLD));
            meta.lore(List.of(
                    Component.text("Tamamlanan: " + done + "/" + entries.size(), NamedTextColor.GREEN),
                    Component.text("İlerleme: %" + (entries.isEmpty() ? 0 : done * 100 / entries.size()), NamedTextColor.AQUA),
                    Component.text("✧ Bu bölümü keşfet", NamedTextColor.YELLOW)));
            item.setItemMeta(meta);
            inventory.setItem(CATEGORY_SLOTS[i], item);
        }
        int totalDone = completedCount(player, all);
        ItemStack summary = navigation(org.bukkit.Material.NETHER_STAR,
                "Toplam ilerleme: " + totalDone + "/" + all.size());
        inventory.setItem(49, summary);
        inventory.setItem(45, navigation(org.bukkit.Material.SPRUCE_SAPLING, "Kozalak SMP • Macera seni bekliyor"));
        player.openInventory(inventory);
    }

    private void openPage(Player player, int category, int requestedPage) {
        List<Advancement> all = inCategory(category);
        int maxPage = Math.max(0, (all.size() - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(maxPage, requestedPage));
        PageHolder holder = new PageHolder(category, page);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("✦ " + CATEGORY_NAMES[category] + " • " + (page + 1) + "/" + (maxPage + 1), NamedTextColor.DARK_GREEN));
        holder.inventory = inventory;
        decorate(inventory);
        inventory.setItem(4, navigation(org.bukkit.Material.WRITABLE_BOOK, CATEGORY_NAMES[category] + " • Macera sayfası"));
        int start = page * PAGE_SIZE;
        for (int i = start; i < Math.min(all.size(), start + PAGE_SIZE); i++) {
            Advancement advancement = all.get(i);
            AdvancementDisplay display = advancement.getDisplay();
            if (display == null) continue;
            ItemStack item = display.icon().clone();
            ItemMeta meta = item.getItemMeta();
            boolean done = completed(player, advancement);
            meta.displayName(display.title().colorIfAbsent(done ? NamedTextColor.GREEN : NamedTextColor.RED));
            meta.lore(List.of(
                    display.description(),
                    Component.text("Ödül: " + points(advancement) + " Puan", NamedTextColor.GOLD),
                    Component.text(done ? "✓ Tamamlandı" : "✗ Tamamlanmadı",
                            done ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.text(done && "paid".equals(ledger.getString(ledgerKey(player, advancement)))
                            ? "Ödül alındı" : done ? "Ödül bekleniyor" : "Henüz kazanılmadı",
                            NamedTextColor.GRAY)));
            item.setItemMeta(meta);
            inventory.setItem(ENTRY_SLOTS[i - start], item);
        }
        if (page > 0) inventory.setItem(45, navigation(org.bukkit.Material.ARROW, "Önceki sayfa"));
        inventory.setItem(48, navigation(org.bukkit.Material.SPRUCE_DOOR, "Günlüğe dön"));
        inventory.setItem(49, navigation(org.bukkit.Material.BOOK,
                "İlerleme: " + completedCount(player, all) + "/" + all.size()));
        if (page < maxPage) inventory.setItem(53, navigation(org.bukkit.Material.ARROW, "Sonraki sayfa"));
        player.openInventory(inventory);
    }

    private void decorate(Inventory inventory) {
        ItemStack border = new ItemStack(org.bukkit.Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.DARK_GREEN));
        border.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || column == 0 || column == 8 || row == 5)
                inventory.setItem(slot, border);
        }
        inventory.setItem(0, navigation(org.bukkit.Material.SPRUCE_SAPLING, "Kozalak SMP"));
        inventory.setItem(8, navigation(org.bukkit.Material.SPRUCE_LOG, "Ormanın izinde"));
    }

    private ItemStack navigation(org.bukkit.Material material, String title) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.AQUA));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PageHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (holder.category == -1) {
            for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
                if (slot == CATEGORY_SLOTS[i]) {
                    openPage(player, i, 0);
                    return;
                }
            }
            return;
        }
        if (slot == 48) openCategories(player);
        else if (slot == 45 && holder.page > 0) openPage(player, holder.category, holder.page - 1);
        else if (slot == 53 && (holder.page + 1) * PAGE_SIZE < inCategory(holder.category).size())
            openPage(player, holder.category, holder.page + 1);
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PageHolder) event.setCancelled(true);
    }
}
