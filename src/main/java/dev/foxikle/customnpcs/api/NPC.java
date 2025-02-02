package dev.foxikle.customnpcs.api;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.foxikle.customnpcs.internal.CustomNPCs;
import dev.foxikle.customnpcs.internal.InternalNpc;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * A class providing a 'PaperAPI friendly' NPC object requring no NMS to use within a project.
 */
public class NPC {

    private final InternalNpc npc;

    /**
     * The intended way to create an NPC
     * By default, this npc has no actions
     * By default, this npc has no skin
     * By default, this npc has no name
     * By default, this npc is not interactable
     * By default, this npc is not resilient
     * By default, this npc has a yaw of 0
     * By default, this npc has tunnelvision
     * @param world The world for the NPC to be created in
     * @author Foxikle
     * @since 1.5-pre5
     * @throws NullPointerException if the world is null
     * @throws IllegalStateException if the API is not initilized
     */
    public NPC(@NotNull World world){
        if(NPCApi.plugin == null) throw new IllegalStateException("You must initialize the api before using it!");
        if(world == null) throw new NullPointerException("world cannot be null.");
        UUID uuid = UUID.randomUUID();
        GameProfile profile = new GameProfile(uuid, uuid.toString().substring(0, 16));
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel nmsWorld = ((CraftWorld) world).getHandle();
        this.npc = new InternalNpc(NPCApi.plugin, nmsServer, nmsWorld, profile, new Location(world, 0, 0, 0), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), false, false, "", profile.getId(), "",  "", null, 0, null,  new ArrayList<>(), true);
    }

    private NPC(InternalNpc npc) {
        this.npc = npc;
    }

    /**
     * <p>Sets the display name of the NPC
     * This supports SERIALIZED MiniMessage.
     * </p>
     * @param name the new name
     * @return the NPC with the modified name
     * @since 1.5.2-pre3
     */
    public NPC setName(@NotNull String name){
        Preconditions.checkArgument(name != null, "name cannot be null.");
        npc.setName(name);
        return this;
    }

    /**
     * <p>Sets the display name of the NPC
     * </p>
     * @param name of the NPC as an Adventure Component
     * @return the NPC with the modified name
     * @since 1.5.2-pre3
     */
    public NPC setName(@NotNull Component name){
        Preconditions.checkArgument(name != null, "name cannot be null.");
        npc.setName(NPCApi.plugin.getMiniMessage().serialize(name));
        return this;
    }

    /**
     * <p>Sets the location of the NPC
     * </p>
     * @param loc the new location for the NPC
     * @return the NPC with the modified location
     * @since 1.5.2-pre3
     */
    public NPC setPostion(@NotNull Location loc){
        Preconditions.checkArgument(loc != null, "loc cannot be null.");
        npc.setSpawnLoc(loc);
        return this;
    }

    /**
     *
     * @param tunnelVision if the npc should have TunnelVision (Not looking at players)
     * @return the npc with the modified TunnelVision
     */
    public NPC setTunnelVision(boolean tunnelVision){
        npc.setTunnelVision(tunnelVision);
        return this;
    }

    /**
     * <p>Sets the leggings the NPC is wearing
     * </p>
     * @param skinName the name to reference the skin by.
     * @param signature the encoded signature of the skin.
     * @param value the encoded value of the skin
     * @return the NPC with the modified skin
     * @see <a href="ttps://mineskin.org/">Use this site by InventiveTalent to find value and signature</a>
     * @since 1.5.2-pre3
     * @throws NullPointerException if any argument is null
     */
    public NPC setSkin(@NotNull String skinName, @NotNull String signature, @NotNull String value){
        Preconditions.checkArgument(signature != null, "signature cannot be null.");
        Preconditions.checkArgument(value != null, "value cannot be null.");
        Preconditions.checkArgument(skinName != null && skinName.length() != 0, "skinName cannot be null or empty");
        GameProfile profile = npc.getGameProfile();
        npc.setSkinName(skinName);
        npc.setValue(value);
        npc.setSignature(signature);
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", new Property("textures", value, signature));
        return this;
    }

    /**
     * <p>Sets the helmet the NPC is wearing
     * </p>
     * @param item the item to be used
     * @return the NPC with the modified helmet
     * @since 1.5.2-pre3
     */
    public NPC setHelmet(ItemStack item){
        Preconditions.checkArgument(item != null, "item cannot be null.");
        npc.setHeadItem(item);
        return this;
    }

    /**
     * <p>Sets the chestplate the NPC is wearing
     * </p>
     * @param item the item to be used
     * @return the NPC with the modified chestplate
     * @since 1.5.2-pre3
     */
    public NPC setChestplate(ItemStack item){
        Preconditions.checkArgument(item != null, "item cannot be null.");
        npc.setChestItem(item);
        return this;
    }

    /**
     * <p>Sets the leggings the NPC is wearing
     * </p>
     * @param item the item to be used
     * @return the NPC with the modified pair leggings
     * @since 1.5.2-pre3
     */
    public NPC setLeggings(ItemStack item){
        Preconditions.checkArgument(item != null, "item cannot be null.");
        npc.setLegsItem(item);
        return this;
    }

    /**
     * <p>Sets the boots the NPC is wearing
     * </p>
     * @param item the item to be used
     * @return the NPC with the modified pair of boots
     * @since 1.5.2-pre3
     */
    public NPC setBoots(ItemStack item){
        Preconditions.checkArgument(item != null, "item cannot be null.");
        npc.setBootsItem(item);
        return this;
    }

    /**
     * <p>Sets the item in the NPC's hand
     * </p>
     * @param item the item to be used
     * @return the NPC with the modified hand item
     * @since 1.5.2-pre3
     */
    public NPC setHandItem(ItemStack item){
        Preconditions.checkArgument(item != null, "item cannot be null.");
        npc.setHandItem(item);
        return this;
    }

    /**
     * <p>Sets the item in the NPC's offhand
     * </p>
     * @param item the item to be used
     * @return the NPC with the modified offhand item
     * @since 1.5.2-pre3
     */
    public NPC setOffhandItem(ItemStack item){
        Preconditions.checkArgument(item != null, "item cannot be null.");
        npc.setOffhandItem(item);
        return this;
    }

    /**
     * <p>Sets the NPC's interactability
     * </p>
     * @param interactable should the NPC be interactable?
     * @return the NPC with the modified interacability
     * @since 1.5.2-pre3
     */
    public NPC setInteractable(boolean interactable){
        npc.setClickable(interactable);
        return this;
    }

    /**
     * <p>Sets the NPC's persistence (staying on server restart/reload).
     * </p>
     * @param resilient should the NPC persist on reloads/server restarts?
     * @return the NPC with the modified resiliency
     * @since 1.5.2-pre3
     */
    public NPC setResilient(boolean resilient){
        npc.setResilient(resilient);
        return this;
    }

    /**
     * <p>Sets the NPC's facing direction
     * </p>
     * @param heading the heading the NPC should face.
     * @return the NPC with the modified heading
     * @since 1.5.2-pre3
     */
    public NPC setHeading(double heading){
        npc.setDirection(heading);
        return this;
    }

    /**
     * <p>Sets the NPC's actions to the specified Collection
     * </p>
     * @param actions the colection of actions
     * @return the NPC with the modified set of actions
     * @see Action
     * @since 1.5.2-pre3
     */
    public NPC setActions(Collection<Action> actions){
        npc.setActions(actions);
        return this;
    }

    /**
     * Move the npc to the specified location. Takes into account pitch and yaw
     * @param location the location to move to
     */
    public void moveTo(Location location){
        npc.moveTo(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
    }

    /**
     * Swings the NPC's arm
     * @since 1.5.2-pre3
     */
    public void swingArm(){
        npc.swing(InteractionHand.MAIN_HAND);
    }

    /**
     * Injects the npc into the player's connection. This should be handled by the plugin, but this is here for more control.
     * @param player the player to inject
     * @since 1.5.2-pre3
     * @see Player
     */
    public void injectPlayer(Player player) {
        npc.injectPlayer(player);
    }

    /**
     * Points the NPC's head in the direction of an entity
     * @param e The entity to look at
     * @param atHead If the npc should look head (true), or feet (false)
     */
    public void lookAt(Entity e, boolean atHead){
        npc.lookAt(EntityAnchorArgument.Anchor.EYES, ((CraftEntity) e).getHandle(), atHead ? EntityAnchorArgument.Anchor.EYES : EntityAnchorArgument.Anchor.FEET);
    }

    /**
     * Points the NPC's head at a location
     * @param location the location to look at
     */
    public void lookAt(Location location) {
        npc.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(location.x(), location.y(), location.z()));
    }

    /**
     * <p>Creates the NPC.
     * </p>
     * @return UUID the NPC's uuid for later reference
     * @since 1.5.2-pre3
     */
    public UUID create(){
        npc.createNPC();
        return npc.getUUID();
    }

    /**
     * <p>Permanantly deletes an NPC. They will NOT reappear on the next reload or server restart.
     * The NPC will NOT be despawned. Only applicable if the NPC is resilient.
     * </p>
     * @since 1.5.2-pre3
     * @throws IllegalStateException if the NPC is not resilient
     */
    public void delete() {
        if(!npc.isResilient()) throw new IllegalStateException("The NPC must be resilient to be deleted!");
        npc.delete();
    }

    /**
     * <p>Temporarily removes an NPC. They will reappear on the next reload or server restart IF it is resilient.
     * </p>
     * @since 1.5.2-pre3
     */
    public void remove() {
        npc.remove();
    }
}
