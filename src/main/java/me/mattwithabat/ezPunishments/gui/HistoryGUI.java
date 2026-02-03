package me.mattwithabat.ezPunishments.gui;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoryGUI implements Listener {

    private final EzPunishments plugin;
    private final String targetName;
    private final List<Punishment> punishments;
    private final Inventory inventory;
    private final int page;
    private final int maxPage;
    private final Map<Integer, Punishment> slotToPunishment = new HashMap<>();

    private static final int ITEMS_PER_PAGE = 45;

    public HistoryGUI(EzPunishments plugin, String targetName, List<Punishment> punishments) {
        this(plugin, targetName, punishments, 0);
    }

    public HistoryGUI(EzPunishments plugin, String targetName, List<Punishment> punishments, int page) {
        this.plugin = plugin;
        this.targetName = targetName;
        this.punishments = punishments;
        this.page = page;
        this.maxPage = Math.max(0, (punishments.size() - 1) / ITEMS_PER_PAGE);

        String title = plugin.getMenuConfig().getString("history.title", "&6Punishment History: {player}")
                .replace("{player}", targetName)
                .replace("{page}", String.valueOf(page + 1))
                .replace("{maxpage}", String.valueOf(maxPage + 1));
        title = ChatColor.translateAlternateColorCodes('&', title);

        int rows = plugin.getMenuConfig().getInt("history.rows", 6);
        this.inventory = Bukkit.createInventory(null, rows * 9, title);

        populate();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void populate() {
        inventory.clear();
        slotToPunishment.clear();

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, punishments.size());

        ConfigurationSection itemsSection = plugin.getMenuConfig().getConfigurationSection("history.items");

        for (int i = startIndex; i < endIndex; i++) {
            Punishment p = punishments.get(i);
            int slot = i - startIndex;

            ItemStack item = createPunishmentItem(p, itemsSection);
            inventory.setItem(slot, item);
            slotToPunishment.put(slot, p);
        }

        if (page > 0) {
            ItemStack prevPage = createNavigationItem("previous-page");
            int prevSlot = plugin.getMenuConfig().getInt("history.navigation.previous-page.slot", 48);
            inventory.setItem(prevSlot, prevPage);
        }

        if (page < maxPage) {
            ItemStack nextPage = createNavigationItem("next-page");
            int nextSlot = plugin.getMenuConfig().getInt("history.navigation.next-page.slot", 50);
            inventory.setItem(nextSlot, nextPage);
        }

        ItemStack filler = createFillerItem();
        if (filler != null) {
            for (int i = 45; i < 54; i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler);
                }
            }
        }
    }

    private ItemStack createPunishmentItem(Punishment punishment, ConfigurationSection section) {
        String typeKey = punishment.getType().name().toLowerCase().replace("_", "-");
        ConfigurationSection typeSection = null;

        if (section != null) {
            typeSection = section.getConfigurationSection(typeKey);
            if (typeSection == null) {
                typeSection = section.getConfigurationSection("default");
            }
        }

        Material material = Material.PAPER;
        if (typeSection != null) {
            String matName = typeSection.getString("material", "PAPER");
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        if (!punishment.isActive()) {
            String inactiveMat = plugin.getMenuConfig().getString("history.inactive-material", "GRAY_DYE");
            try {
                material = Material.valueOf(inactiveMat.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName = typeSection != null
                    ? typeSection.getString("name", "&c{type}")
                    : "&c{type}";
            displayName = replacePlaceholders(displayName, punishment);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            List<String> lore = typeSection != null
                    ? typeSection.getStringList("lore")
                    : getDefaultLore();

            lore = lore.stream()
                    .map(line -> replacePlaceholders(line, punishment))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private List<String> getDefaultLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Punisher: &f{punisher}");
        lore.add("&7Reason: &f{reason}");
        lore.add("&7Date: &f{date}");
        lore.add("&7Duration: &f{duration}");
        lore.add("&7Status: {status}");
        return lore;
    }

    private String replacePlaceholders(String text, Punishment p) {
        String status = p.isActive() ? (p.hasExpired() ? "&eExpired" : "&aActive") : "&cRemoved";

        return text
                .replace("{type}", formatType(p.getType().name()))
                .replace("{punisher}", p.getPunisherName())
                .replace("{reason}", p.getReason())
                .replace("{date}", TimeUtil.formatDate(p.getCreatedAt()))
                .replace("{duration}", p.isPermanent() ? "Permanent" : TimeUtil.formatDuration(p.getExpiresAt() - p.getCreatedAt()))
                .replace("{remaining}", TimeUtil.formatRemaining(p.getExpiresAt()))
                .replace("{status}", status)
                .replace("{id}", p.getId())
                .replace("{player}", p.getTargetName());
    }

    private String formatType(String type) {
        String formatted = type.replace("_", " ");
        StringBuilder result = new StringBuilder();
        for (String word : formatted.split(" ")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private ItemStack createNavigationItem(String key) {
        ConfigurationSection section = plugin.getMenuConfig().getConfigurationSection("history.navigation." + key);
        Material material = Material.ARROW;
        String name = key.equals("previous-page") ? "&ePrevious Page" : "&eNext Page";

        if (section != null) {
            String matName = section.getString("material", "ARROW");
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
            name = section.getString("name", name);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        String matName = plugin.getMenuConfig().getString("history.filler.material", "BLACK_STAINED_GLASS_PANE");
        if (matName.equalsIgnoreCase("NONE")) return null;

        Material material;
        try {
            material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        int prevSlot = plugin.getMenuConfig().getInt("history.navigation.previous-page.slot", 48);
        int nextSlot = plugin.getMenuConfig().getInt("history.navigation.next-page.slot", 50);

        if (slot == prevSlot && page > 0) {
            HandlerList.unregisterAll(this);
            new HistoryGUI(plugin, targetName, punishments, page - 1).open(player);
        } else if (slot == nextSlot && page < maxPage) {
            HandlerList.unregisterAll(this);
            new HistoryGUI(plugin, targetName, punishments, page + 1).open(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
}
