package com.marmok.fixshulker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public final class FixShulker extends JavaPlugin implements TabExecutor, TabCompleter {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Регистрация слушателя
        getServer().getPluginManager().registerEvents(new ShulkerProtectListener(this), this);

        // Регистрация команды
        if (getCommand("fixshulkerreload") != null) {
            getCommand("fixshulkerreload").setExecutor(this);
            getCommand("fixshulkerreload").setTabCompleter(this);
        } else {
            getLogger().severe("Команда /fixshulkerreload не зарегистрирована в plugin.yml!");
        }

        getLogger().info("FixShulker включен.");
    }

    @Override
    public void onDisable() {
        getLogger().info("FixShulker отключен.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fixshulkerreload")) {
            if (sender.hasPermission("fixshulker.reload")) {
                reloadConfig();
                sender.sendMessage("§aКонфигурация FixShulker была успешно перезагружена.");
                getLogger().info("Конфигурация была перезагружена.");
            } else {
                sender.sendMessage("§cУ вас нет прав на выполнение этой команды.");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("fixshulkerreload")) {
            return Collections.emptyList();
        }
        return null;
    }
}