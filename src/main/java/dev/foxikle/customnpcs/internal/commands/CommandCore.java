package dev.foxikle.customnpcs.internal.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.foxikle.customnpcs.api.Action;
import dev.foxikle.customnpcs.internal.CustomNPCs;
import dev.foxikle.customnpcs.internal.menu.MenuCore;
import dev.foxikle.customnpcs.internal.InternalNpc;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bukkit.ChatColor.RED;
/**
 * The class to handle the core command
 */
public class CommandCore implements CommandExecutor, TabCompleter {
    /**
     * The instance of the main class
     */
    private final CustomNPCs plugin;

    /**
     * Creates the command handler
     * @param plugin the instance of the Main Class
     */
    public CommandCore(CustomNPCs plugin) {
        this.plugin = plugin;
    }
    /**
     * <p>The generic handler for any command
     * </p>
     * @param command The command used
     * @param sender The sender of the command
     * @param label The label of the command (/label args[])
     * @param args The arguments of the commands
     * @return if the command was handled
     * @since 1.3-pre5
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player player) {
            if (args.length == 0) {
                player.performCommand("npc help");
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    if(!player.hasPermission("customnpcs.commands.help")){
                        player.sendMessage(RED + "You lack the propper permissions to execute this.");
                        return true;
                    }
                    player.sendMessage(ChatColor.translateAlternateColorCodes('§', """
                            §2§m                      §r§3§l Custom NPCs §r§7[§8v${version}§7] §r§2§m                      \s
                            §r                                 §r§6By Foxikle \n
                            
                            """).replace("${version}", plugin.getDescription().getVersion()));


                    player.sendMessage(getHelpComponent());
                } else if (args[0].equalsIgnoreCase("manage")) {
                    if(!player.hasPermission("customnpcs.commands.manage")){
                        player.sendMessage(RED + "You lack the propper permissions to manage npcs.");
                        return true;
                    }
                    if(plugin.getNPCs().isEmpty()) {
                        player.sendMessage(RED + "There are no npcs to manage!");
                        return true;
                    }
                    player.sendMessage(ChatColor.translateAlternateColorCodes('§', """
                            §2§m                           §r§3§l Manage NPCs  §r§2§m                           \s
                            §r                                 \n
                            
                            """));
                    Component message = Component.empty();
                    for (InternalNpc npc : plugin.getNPCs()) {
                        if (npc.isResilient()) {
                            Component name = Component.text("  ")
                                    .append(plugin.getMiniMessage().deserialize(npc.getHologramName())
                                            .hoverEvent(HoverEvent.showText(Component.text("Click to copy UUID", NamedTextColor.GREEN)))
                                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, npc.getUUID().toString()))
                                    ).append(Component.text( " [EDIT]", NamedTextColor.YELLOW, TextDecoration.BOLD)
                                            .hoverEvent(HoverEvent.showText(Component.text("Click to edit NPC", NamedTextColor.DARK_AQUA)))
                                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/npc edit " + npc.getUUID()))
                                    ).append(Component.text(" [DELETE]", NamedTextColor.RED, TextDecoration.BOLD)
                                            .hoverEvent(HoverEvent.showText(Component.text("Click to permanantly delete NPC", NamedTextColor.DARK_RED, TextDecoration.BOLD)))
                                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc delete " + npc.getUUID()))
                                    ).appendNewline();
                            message = message.append(name);
                        }
                    }
                    player.sendMessage(message);


                } else if (args[0].equalsIgnoreCase("new")) {
                    player.performCommand("npc create");
                } else if (args[0].equalsIgnoreCase("list")) {
                    player.performCommand("npc manage");
                } else if (args[0].equalsIgnoreCase("clear_holograms")) {
                    if(player.hasPermission("customnpcs.commands.removeHolograms")){
                        AtomicInteger stands = new AtomicInteger();
                        player.getWorld().getEntities().forEach(entity -> {
                            if(entity.getScoreboardTags().contains("npcHologram")){
                                entity.remove();
                                stands.getAndIncrement();
                            }
                        });
                        player.sendMessage((stands.get() == 1) ? ChatColor.GREEN + "Successfully removed " + stands.get() + " npc hologram." : ChatColor.GREEN + "Successfully removed " + stands.get() + " npc holograms.");
                    } else {
                        player.sendMessage(RED + "You lack the propper permissions to remove npc holograms.");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("create")) {
                    if(!player.hasPermission("customnpcs.create")){
                        player.sendMessage(RED + "You lack the propper permissions to create npcs.");
                        return true;
                    }
                    UUID uuid = UUID.randomUUID();
                    GameProfile profile = new GameProfile(uuid, uuid.toString().substring(0, 16));
                    profile.getProperties().removeAll("textures");
                    profile.getProperties().put("textures", new Property("textures", null, null));
                    MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
                    ServerLevel nmsWorld = ((CraftWorld) player.getWorld()).getHandle();
                    InternalNpc npc = new InternalNpc(plugin, nmsServer, nmsWorld, profile,  player.getLocation(), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), true, true,  "not set", uuid, "", "", "not set", 180, null, new ArrayList<>(), false);
                    MenuCore mc = new MenuCore(npc, plugin);
                    plugin.menuCores.put(player, mc);
                    plugin.pages.put(player, 0);
                    player.openInventory(mc.getMainMenu());
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if(!player.hasPermission("customnpcs.commands.reload")){
                        player.sendMessage(RED + "You lack the propper permissions to reload npcs.");
                        return true;
                    }
                    player.sendMessage(ChatColor.YELLOW + "Reloading NPCs!");
                    plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
                    plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
                    plugin.reloadConfig();
                    try {
                        Bukkit.getScoreboardManager().getMainScoreboard().getTeam("npc").unregister();
                    } catch (IllegalArgumentException ignored) {}
                    HandlerList.unregisterAll(plugin);
                    List<InternalNpc> npcs = new ArrayList<>(plugin.npcs.values());
                    for (InternalNpc npc : npcs) {
                        plugin.npcs.remove(npc.getUUID());
                        npc.remove();
                    }
                    plugin.npcs.clear();
                    plugin.holograms.clear();
                    plugin.onDisable();
                    plugin.onEnable();
                    player.sendMessage(ChatColor.GREEN + "NPCs successfully reloaded.");
                }
            } else if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("setsound")) {
                    if(plugin.soundWaiting.contains(player)) {
                        try{
                            Sound.valueOf(args[1]);
                        } catch (IllegalArgumentException ex) {
                            player.sendMessage(RED + "Unrecognised sound, please use tab completions.");
                            return true;
                        }
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.soundWaiting.remove(player);
                            List<String> argsCopy = plugin.editingActions.get(player).getArgsCopy();
                            Action action = plugin.editingActions.get(player);
                            List<String> currentArgs = action.getArgs();
                            currentArgs.clear();
                            currentArgs.add(0, argsCopy.get(0));
                            currentArgs.add(1, argsCopy.get(1));
                            currentArgs.add(2, args[1]);
                            player.sendMessage(ChatColor.GREEN + "Successfully set sound to be '" + ChatColor.RESET + args[1] + ChatColor.GREEN + "'");
                            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(plugin.menuCores.get(player).getActionCustomizerMenu(action)));
                        });
                    } else {
                        player.sendMessage(ChatColor.RED + "Unccessfully set NPC sound. I wasn't waiting for a response. Please contact Foxikle if you think this is a mistake.");
                    }
                } else {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(args[1]);
                    } catch (IllegalArgumentException ignored) {
                        player.sendMessage(RED + "Invalid UUID provided.");
                        return false;
                    }
                    if (args[0].equalsIgnoreCase("delete")) {
                        if (!player.hasPermission("customnpcs.delete")) {
                            player.sendMessage(RED + "You lack the propper permissions to delete npcs.");
                            return true;
                        }
                        if (plugin.npcs.keySet().contains(uuid)) {
                            InternalNpc npc = plugin.getNPCByID(uuid);
                            npc.remove();
                            npc.delete();
                            plugin.npcs.remove(npc.getUUID());
                            player.sendMessage(ChatColor.GREEN + "Successfully deleted the NPC: " + npc.getHologramName());

                        } else {
                            player.sendMessage(RED + "The UUID provided does not match any NPC.");
                        }
                    } else if (args[0].equalsIgnoreCase("edit")) {
                        if (!player.hasPermission("customnpcs.edit")) {
                            player.sendMessage(RED + "You lack the propper permissions to edit npcs.");
                            return true;
                        }
                        if (plugin.npcs.containsKey(uuid)) {
                            InternalNpc npc = plugin.getNPCByID(uuid);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                GameProfile profile = new GameProfile(uuid, uuid.toString().substring(0, 16));
                                profile.getProperties().removeAll("textures");
                                profile.getProperties().put("textures", new Property("textures", null, null));
                                MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
                                ServerLevel nmsWorld = ((CraftWorld) player.getWorld()).getHandle();
                                List<String> actionStrs = new ArrayList<>();
                                npc.getActions().forEach(action -> actionStrs.add(action.toJson()));
                                InternalNpc newNpc = new InternalNpc(plugin, nmsServer, nmsWorld, profile, npc.getSpawnLoc(), npc.getHandItem(), npc.getItemInOffhand(), npc.getHeadItem(), npc.getChestItem(), npc.getLegsItem(), npc.getBootsItem(), npc.isClickable(), npc.isResilient(), npc.getHologramName(), uuid, npc.getValue(), npc.getSignature(), npc.getSkinName(), npc.getFacingDirection(), null, actionStrs, false);
                                MenuCore mc = new MenuCore(newNpc, plugin);
                                plugin.menuCores.put(player, mc);
                                plugin.pages.put(player, 0);
                                player.openInventory(mc.getMainMenu());
                            }, 1);
                        } else {
                            player.sendMessage(RED + "The UUID provided does not match any NPC.");
                        }
                    } else {
                        sender.sendMessage(RED + "Unrecognised sub-command. Use '/npc help' for a list of supported commands.");
                    }
                }
            }
        } else if(args[0].equalsIgnoreCase("reload")) {
            if(args.length >= 2) {
                if (args[1].equalsIgnoreCase("silent")) {
                    try {
                        Bukkit.getScoreboardManager().getMainScoreboard().getTeam("npc").unregister();
                    } catch (IllegalArgumentException ignored) {
                    }
                    HandlerList.unregisterAll(plugin);
                    List<InternalNpc> npcs = new ArrayList<>(plugin.npcs.values());
                    for (InternalNpc npc : npcs) {
                        plugin.npcs.remove(npc.getUUID());
                        npc.remove();
                    }
                    plugin.npcs.clear();
                    plugin.holograms.clear();
                    plugin.onEnable();
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Reloading NPCs!");
                try {
                    Bukkit.getScoreboardManager().getMainScoreboard().getTeam("npc").unregister();
                } catch (IllegalArgumentException ignored) {}
                HandlerList.unregisterAll(plugin);
                List<InternalNpc> npcs = new ArrayList<>(plugin.npcs.values());
                for (InternalNpc npc : npcs) {
                    plugin.npcs.remove(npc.getUUID());
                    npc.remove();
                }
                plugin.npcs.clear();
                plugin.holograms.clear();
                plugin.onEnable();
                sender.sendMessage(ChatColor.GREEN + "NPCs successfully reloaded.");
            }
        }
        return false;
    }

    @NotNull
    private Component getHelpComponent() {
        Component component = Component.empty();
        component = component.append(Component.text("\n\n- /npc help  ", NamedTextColor.GOLD)
        .hoverEvent(HoverEvent.showText(Component.text("Displays this message.", NamedTextColor.WHITE)))
        .append(Component.text("Displays this message.", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("- /npc manage  ", NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Displays the current NPCs with buttons to edit or delete them.", NamedTextColor.WHITE)))
        )
        .append(Component.text("Displays the current NPCs", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("- /npc create  ", NamedTextColor.GOLD)
            .hoverEvent(HoverEvent.showText(Component.text("Opens the NPC customizer", NamedTextColor.WHITE)))
        )
        .append(Component.text("Creates a new NPC", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("- /npc delete <UUID>  ", NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Permanantly deletes the NPC", NamedTextColor.DARK_RED)))
        )
        .append(Component.text("Permanantly deletes the NPC  ", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("- /npc edit <UUID>  ", NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Brings up the NPC edit dialogue", NamedTextColor.WHITE)))
        )
        .append(Component.text("Edits the specified NPC", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("- /npc create  ", NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Opens the NPC customizer", NamedTextColor.WHITE)))
        )
        .append(Component.text("Creates a new NPC  ", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("- /npc clear_holograms  ", NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Deletes ALL NPC holograms. (Includes holograms without NPCs correlated to them)", NamedTextColor.WHITE)))
        )
        .append(Component.text("Forcfully deletes NPC holograms", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("- /npc reload  ", NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Reloads the plugin and config", NamedTextColor.WHITE)))
        )
        .append(Component.text("Reloads CustomNPCs", NamedTextColor.AQUA))
        .appendNewline()
        .append(Component.text("                                                                                 ", NamedTextColor.DARK_GREEN, TextDecoration.STRIKETHROUGH)));
        return component;
    }

    /**
     * <p>The generic handler for any tab completion
     * </p>
     * @param command The command used
     * @param sender The sender of the command
     * @param label The label of the command (/label args[])
     * @param args The arguments of the commands
     * @return the options to tab-complete
     * @since 1.3-pre5
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if(args.length == 1){
            list.add("help");
            list.add("manage");
            list.add("create");
            list.add("delete");
            list.add("edit");
            list.add("reload");
            list.add("clear_holograms");
            if(plugin.soundWaiting.contains((Player) sender)) list.add("setsound");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setsound")) {
                for (Sound sound : Sound.values()) {
                    list.add(sound.name());
                }
                return list;
            }
            plugin.npcs.keySet().forEach(uuid -> list.add(uuid.toString()));
        }
        return list;
    }
}
