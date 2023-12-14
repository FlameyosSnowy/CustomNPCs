package dev.foxikle.customnpcs.internal;

import dev.foxikle.customnpcs.actions.Action;
import dev.foxikle.customnpcs.actions.ActionType;
import dev.foxikle.customnpcs.actions.conditions.Conditional;
import dev.foxikle.customnpcs.internal.interfaces.InternalNPC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * The class that deals with all file related things
 */
public class FileManager {

    private final CustomNPCs plugin;

    /**
     * <p> Gets the file manager object.
     * </p>
     * @param plugin The instance of the Main class
     */
    public FileManager (CustomNPCs plugin){
        this.plugin = plugin;
    }

    /**
     * <p> Creates the files the plugin needs to run
     * </p>
     */
    public boolean createFiles(){
        if (!new File("plugins/CustomNPCs/npcs.yml").exists()) {
            plugin.saveResource("npcs.yml", false);
        } else if (!new File("plugins/CustomNPCs/config.yml").exists()) {
            plugin.saveResource("config.yml", false);
        }
        File file = new File("plugins/CustomNPCs/config.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        if(!yml.contains("Skins")){
            plugin.getLogger().warning("Adding Skin Section to config.");
            plugin.saveResource("config.yml", true);
        }

        int version = yml.getInt("CONFIG_VERSION");
        if(version == 0) { // doesn't exist?
            plugin.getLogger().log(Level.WARNING, "Outdated Config version! Converting config (" + version + " -> 2).");
            yml.set("CONFIG_VERSION", 1);
            yml.setComments("CONFIG_VERSION", List.of(" DO NOT, under ANY circumstances modify the 'CONFIG_VERSION' field. Doing so can cause catastrophic data loss.", ""));
            yml.set("ClickText", "&e&lCLICK");
            yml.setComments("ClickText", List.of("ClickText -> The hologram displayed above the NPC if it is interactable", " NOTE: Due to Minecraft limitatations, this cannot be more than 16 characters INCLUDING color and format codes.", " (But not the &)", ""));
            yml.set("DisplayClickText", true);
            yml.setComments("DisplayClickText", List.of(" DisplayClickText -> Should the plugin display a hologram above the NPC's head if it is interactable?", ""));
            try {
                yml.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (version < 2) { // prior to 1.4-pre2
            plugin.getLogger().log(Level.WARNING, "Outdated Config version! Converting config (" + version + " -> 2).");
            yml.set("CONFIG_VERSION", 2);
            yml.set("AlertOnUpdate", true);
            try {
                yml.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if(version < 3) { // prior to 1.5.2-pre1
            plugin.getLogger().log(Level.WARNING, "Outdated Config version! Converting config (" + version + " -> 3).");
            yml.set("CONFIG_VERSION", 3);
            yml.set("ClickText", plugin.getMiniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(yml.getString("ClickText"))));
            try {
                yml.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * <p> Adds an NPC to the `npcs.yml` file.
     * </p>
     * @param npc The NPC to store
     */
    public void addNPC(InternalNPC npc){
        File file = new File("plugins/CustomNPCs/npcs.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        yml.createSection(npc.getUniqueID().toString());
        ConfigurationSection section = yml.getConfigurationSection(npc.getUniqueID().toString());

        List<String> actions = new ArrayList<>();
        npc.getActions().forEach(action -> actions.add(action.toJson()));

        section.addDefault("value", npc.getValue());
        section.addDefault("signature", npc.getSignature());
        section.addDefault("skin", npc.getSkinName());
        section.addDefault("clickable", npc.isClickable());
        section.addDefault("location", npc.getSpawnLoc());
        section.addDefault("actions", actions);
        section.addDefault("handItem", npc.getHandItem());
        section.addDefault("offhandItem", npc.getItemInOffhand());
        section.addDefault("headItem", npc.getHeadItem());
        section.addDefault("chestItem", npc.getChestItem());
        section.addDefault("legsItem", npc.getLegsItem());
        section.addDefault("feetItem", npc.getBootsItem());
        section.addDefault("name", npc.getHologramName());
        section.addDefault("world", npc.getWorld().getName());
        section.addDefault("direction", npc.getFacingDirection());
        yml.options().copyDefaults(true);
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("An error occoured saving the npcs.yml file after creating a new section. Please report the following stacktrace to Foxikle.");
            e.printStackTrace();
        }
    }

    /**
     * <p> Gets the NPC of the specified UUID
     * </p>
     * @param uuid The NPC to load from the file
     */
    public void loadNPC(UUID uuid){
        File file = new File("plugins/CustomNPCs/npcs.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection(uuid.toString());
        if(section == null) throw new IllegalArgumentException("NPC uuid cannot be null.");
        List<Action> actions = new ArrayList<>();
        if(yml.getString("version") == null) { // Config is from before 1.3-pre4
            yml.set("version", "1.3");
            // save updating the version
            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("An error occoured saving the npcs.yml file after updating version number. Please report the following stacktrace to Foxikle.");
                e.printStackTrace();
            }
            plugin.getLogger().info("Adding delay to old actions.");
            for (UUID u : getNPCIds()) {
                ConfigurationSection s = yml.getConfigurationSection(u.toString());
                List<String> strings = s.getStringList("actions");
                List<String> convertedActions = new ArrayList<>();
                for (String string : strings) {
                    ArrayList<String> split = new ArrayList<>(Arrays.stream(string.split("%::%")).toList());
                    String sub = split.get(0);
                    split.remove(0);
                    int delay = 0;
                    Action acttion = new Action(ActionType.valueOf(sub), split, delay, Conditional.SelectionMode.ONE, new ArrayList<>());
                    convertedActions.add(acttion.toJson());
                    actions.add(acttion);
                }
                s.set("actions", convertedActions);
            }
            // save after adding actions
            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("An error occoured saving the npcs.yml file after saving a list of converted actions. Please report the following stacktrace to Foxikle.");
                e.printStackTrace();
            }
        } else if (yml.getString("version").equalsIgnoreCase("1.3")) {
            yml.set("version", "1.4");
            plugin.getLogger().warning("Old Actions found. Converting to json.");
            List<String> legacyActions = section.getStringList("actions");
            List<String> newActions = new ArrayList<>();
            legacyActions.forEach(s -> {
                if(s != null) {
                    Action a = Action.of(s); // going to be converted the old way
                    if(a != null) {
                        newActions.add(a.toJson());
                    }
                }
            });
            section.set("actions", newActions);
            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("An error occoured whilst saving the converted actions. Pleaes report the following stacktrace to Foxikle. \n" + Arrays.toString(e.getStackTrace()));
            }
        }
        if(section.getConfigurationSection("actions") == null) { // meaning it does not exist
            if (section.getString("command") != null) { // if there is a legacy command
                Bukkit.getLogger().info("Converting legacy commands to Actions.");
                String command = section.getString("command");
                Action action = new Action(ActionType.RUN_COMMAND, new ArrayList<>(Arrays.stream(command.split(" ")).toList()), 0, Conditional.SelectionMode.ONE, new ArrayList<>());
                actions.add(action);
                section.set("actions", actions);
                section.set("command", null);
                try {
                    yml.save(file);
                } catch (IOException e) {
                    plugin.getLogger().severe("An error occoured saving the npcs.yml file after converting legacy commands to actions. Please report the following stacktrace to Foxikle.");
                    e.printStackTrace();
                }
            }
        }
        if(section.getString("name").contains("§")) {
            section.set("name", plugin.getMiniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(section.getString("name").replace("§", "&"))));
            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("An error occoured saving the npcs.yml file after converting legacy names to minimessage. Please report the following stacktrace to Foxikle.");
                e.printStackTrace();
            }
        }

        World world = Bukkit.getWorld(section.getString("world"));
        if(world == null) {
            plugin.getLogger().severe("NPC with an invalid world detected! World: '" + section.getString("world") + "'");
            return;
        }

        InternalNPC npc = plugin.createNPC(Bukkit.getWorld(section.getString("world")), section.getLocation("location"), section.getItemStack("handItem"), section.getItemStack("offhandItem"), section.getItemStack("headItem"), section.getItemStack("chestItem"), section.getItemStack("legsItem"), section.getItemStack("feetItem"), section.getBoolean("clickable"), true, section.getString("name"), uuid, section.getString("value"), section.getString("signature"), section.getString("skin"), section.getDouble("direction"), null,  section.getStringList("actions"), false);
        npc.createNPC();
    }

    /**
     * <p> Gets the set of stored UUIDs.
     * </p>
     * @return the set of stored NPC uuids.
     */
    public Set<UUID> getNPCIds(){
        File file = new File("plugins/CustomNPCs/npcs.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        Set<UUID> uuids = new HashSet<>();
        for (String str: yml.getKeys(false)) {
            if(!str.equalsIgnoreCase("version"))
                uuids.add(UUID.fromString(str));
        }
        return uuids;
    }

    /**
     * <p> Removes the specified NPC from storage
     * </p>
     * @param uuid The NPC uuid to remove
     */
    public void remove(UUID uuid){
        File file = new File("plugins/CustomNPCs/npcs.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        yml.set(uuid.toString(), null);
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("An error occoured saving the npcs.yml file after removing an npc. Please report the following stacktrace to Foxikle.");
            e.printStackTrace();
        }
    }
}
