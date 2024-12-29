package com.marmok.fixshulker;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShulkerProtectListener implements Listener {

    private final FixShulker plugin;
    private final List<String> blockedPatterns;

    public ShulkerProtectListener(FixShulker plugin) {
        this.plugin = plugin;
        this.blockedPatterns = plugin.getConfig().getStringList("blocked_patterns");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Player player = event.getPlayer();

        if (item.getType().toString().endsWith("SHULKER_BOX") && hasMaliciousNBT(item)) {
            // Очищаем вредоносные данные
            ItemStack cleanedItem = cleanShulkerNBT(item);

            // Заменяем блок на очищенный
            event.setCancelled(true); // Отменяем первоначальное размещение
            Block block = event.getBlockPlaced();
            block.setType(Material.AIR); // Удаляем вредоносный блок

            // Устанавливаем новый шалкер с очищенными данными
            block.setType(item.getType());
            BlockState state = block.getState();
            if (state instanceof ShulkerBox) {
                ((ShulkerBox) state).update();
            }

            // Обновляем инвентарь игрока
            player.getInventory().remove(item);
            player.getInventory().addItem(cleanedItem);

            player.sendMessage("§aШалкер был очищен от вредоносных данных.");
            notifyAdmins(player, "установил шалкер с вредоносными данными, но они были очищены.");
        }
    }

    private boolean hasMaliciousNBT(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        try {
            NBTItem nbtItem = new NBTItem(item);
            if (nbtItem.hasKey("BlockEntityTag")) {
                NBTCompound blockEntityTag = nbtItem.getCompound("BlockEntityTag");
                if (blockEntityTag != null && blockEntityTag.hasKey("CustomName")) {
                    String customName = blockEntityTag.getString("CustomName");
                    for (String pattern : blockedPatterns) {
                        if (customName.contains(pattern)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка проверки NBT: " + e.getMessage());
        }
        return false;
    }

    private ItemStack cleanShulkerNBT(ItemStack item) {
        NBTItem nbtItem = new NBTItem(item);

        if (nbtItem.hasKey("BlockEntityTag")) {
            NBTCompound blockEntityTag = nbtItem.getCompound("BlockEntityTag");
            if (blockEntityTag != null) {
                // Удаляем ключи translate и with
                if (blockEntityTag.hasKey("CustomName")) {
                    blockEntityTag.removeKey("CustomName");
                }
                blockEntityTag.getKeys().forEach(key -> {
                    if (key.equalsIgnoreCase("translate") || key.equalsIgnoreCase("with")) {
                        blockEntityTag.removeKey(key);
                    }
                });
            }
        }
        return nbtItem.getItem();
    }

    private void notifyAdmins(Player player, String action) {
        String message = String.format("§cИгрок %s %s.", player.getName(), action);
        Bukkit.getOnlinePlayers().stream()
                .filter(onlinePlayer -> onlinePlayer.hasPermission("fixshulker.notify"))
                .forEach(admin -> admin.sendMessage(message));
        plugin.getLogger().warning(message);
    }
}