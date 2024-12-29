package com.marmok.fixshulker;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShulkerProtectListener implements Listener {

    private final FixShulker plugin;
    private final int maxCustomNameLength;
    private final int maxNBTDepth;
    private final List<String> blockedPatterns;

    public ShulkerProtectListener(FixShulker plugin) {
        this.plugin = plugin;
        this.maxCustomNameLength = plugin.getConfig().getInt("max_custom_name_length", 100);
        this.maxNBTDepth = plugin.getConfig().getInt("max_nbt_depth", 10);
        this.blockedPatterns = plugin.getConfig().getStringList("blocked_patterns");
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (isMaliciousItem(item)) {
            event.setCancelled(true);
            event.getItem().remove();
            notifyAdmins(event.getPlayer(), "пытался подобрать");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && isMaliciousItem(clickedItem)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            player.getInventory().remove(clickedItem);
            clearCursorIfMalicious(player);
            player.sendMessage("§cЭтот предмет был удален из вашего инвентаря из-за вредоносных данных.");
            notifyAdmins(player, "использовал");
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isMaliciousItem(item)) {
                player.getInventory().remove(item);
                player.sendMessage("§cВредоносный предмет был удален из вашего инвентаря.");
                notifyAdmins(player, "открыл инвентарь с");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (isMaliciousItem(item)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.getInventory().remove(item);
            clearCursorIfMalicious(player);
            player.sendMessage("§cВы не можете установить этот предмет, так как он содержит вредоносные данные.");
            notifyAdmins(player, "пытался установить");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && isMaliciousItem(item)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.getInventory().remove(item);
            clearCursorIfMalicious(player);
            player.sendMessage("§cЭтот предмет был удален из вашего инвентаря из-за вредоносных данных.");
            notifyAdmins(player, "пытался использовать");
        }
    }

    private boolean isMaliciousItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return false; // Предмет невалиден
        }

        try {
            NBTItem nbtItem = new NBTItem(item);
            if (nbtItem.hasKey("BlockEntityTag")) {
                NBTCompound blockEntityTag = nbtItem.getCompound("BlockEntityTag");
                if (blockEntityTag != null) {
                    // Проверка длины CustomName
                    if (blockEntityTag.hasKey("CustomName")) {
                        String customName = blockEntityTag.getString("CustomName");
                        if (customName.length() > maxCustomNameLength) {
                            return true;
                        }
                        for (String pattern : blockedPatterns) {
                            if (customName.contains(pattern)) {
                                return true;
                            }
                        }
                    }
                    // Проверка глубины вложенности
                    if (getNBTDepth(blockEntityTag, 0) > maxNBTDepth) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка проверки NBT: " + e.getMessage());
        }

        return false;
    }

    private int getNBTDepth(NBTCompound compound, int depth) {
        if (depth > maxNBTDepth) return depth;

        int maxDepth = depth;
        for (String key : compound.getKeys()) {
            if (compound.getCompound(key) != null) {
                maxDepth = Math.max(maxDepth, getNBTDepth(compound.getCompound(key), depth + 1));
            }
        }
        return maxDepth;
    }

    private void clearCursorIfMalicious(Player player) {
        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem != null && isMaliciousItem(cursorItem)) {
            player.setItemOnCursor(null);
            player.updateInventory();
        }
    }

    private void notifyAdmins(Player player, String action) {
        String message = String.format("§cИгрок %s %s вредоносный предмет!", player.getName(), action);
        Bukkit.getOnlinePlayers().stream()
                .filter(onlinePlayer -> onlinePlayer.hasPermission("fixshulker.notify"))
                .forEach(admin -> admin.sendMessage(message));
        plugin.getLogger().warning(message);
    }
}