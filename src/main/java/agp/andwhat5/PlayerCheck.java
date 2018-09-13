package agp.andwhat5;

import agp.andwhat5.config.AGPConfig;
import agp.andwhat5.config.structs.DataStruc;
import agp.andwhat5.config.structs.GymStruc;
import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.enums.EnumPokemon;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static agp.andwhat5.config.structs.GymStruc.EnumStatus.*;

public class PlayerCheck {
    //TODO: Redesign

    //private final static String devLink = "https://pastebin.com/raw/SXepayjB";
    //private final static String scrubLink = "https://pastebin.com/raw/gqXKbgad";
    private static List<UUID> devs = Lists.newArrayList(
    		UUID.fromString("e978a5b2-3ea7-4f10-acde-1c220967c338") /*AnDwHaT5*/,
    		UUID.fromString("88333268-79b6-4537-8066-48d255a6a0f9") /*Sy1veon*/,
    		UUID.fromString("07aa849d-43e5-4da1-b2f9-5d8ac69f4d1a") /*ClientHax*/);
    private static List<UUID> scrubs = Lists.newArrayList(
    		UUID.fromString("0eb8e4fa-f8dc-4648-989b-98ac5bd417a3") /*HackoJacko*/);
    
    //The best of eastereggs.
    int eventCounter = 0;

    private boolean isDeveloper(Player player) {
        return devs.contains(player.getUniqueId());
    }

    private boolean isScrub(Player player) {
        return scrubs.contains(player.getUniqueId());
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join e) {
    	Player player = e.getTargetEntity();
        if (isDeveloper(player)) {
            Utils.sendToAll(TextFormatting.AQUA + "\u2605AGP-R Dev\u2605 " +
                    TextFormatting.UNDERLINE + player.getName() +
                    TextFormatting.RESET + TextFormatting.AQUA + " has joined.", false);
        }
        if (isScrub(player)) {
            Utils.sendToAll(TextFormatting.GREEN + "\u2605AGP-R Helper" +
                    "\u2605 " + TextFormatting.UNDERLINE +
                    player.getName() + TextFormatting.RESET +
                    TextFormatting.GREEN + " has joined.", false);
        }
        boolean isLeader = Utils.isAnyLeader(player);
        if(!isLeader)
        	return;
        DataStruc.gcon.GymData.stream().forEach(g -> {if(g.PlayerLeaders.contains(player.getUniqueId())) g.OnlineLeaders.add(player.getUniqueId());});       
        if (AGPConfig.Announcements.announceLeaderJoin) 
        {
        	for (GymStruc g : DataStruc.gcon.GymData) 
        	{
        		if (g.PlayerLeaders.contains(player.getUniqueId())) 
        		{
                    Utils.getGym(g.Name).OnlineLeaders.add(player.getUniqueId());
        		}
        	}
        	Utils.sendToAll(AGPConfig.Announcements.leaderJoinMessage.replace("{leader}", player.getName()), true);
        }
        
        if(AGPConfig.General.autoOpen)
        {
        	List<String> gymNames = new ArrayList<>();
        	for(GymStruc gym : DataStruc.gcon.GymData)
        	{
        		if(gym.PlayerLeaders.contains(player.getUniqueId()) && gym.OnlineLeaders.isEmpty())
        		{
        			gym.Status = OPEN;
        			gymNames.add(gym.Name);
        		}
        	}
        	
        	if(AGPConfig.Announcements.openAnnouncement)
        	{
        		if(!gymNames.isEmpty())
        		{
        			if(gymNames.size() == 1)
        			{
        				Utils.sendToAll("The " + gymNames.get(0) + " gym has opened!", true);
        			}
        			else
        			if(gymNames.size() == 2)
        			{
        				Utils.sendToAll("The &b" + gymNames.get(0) + " &7and &b" + gymNames.get(1) + " &7gyms have opened!", true);
        			}
        			else
        			{
        				Utils.sendToAll("Multiple gyms have opened! Use &b/GymList &7to see all open gyms.", true);
        			}
        		}
        	}
        }
    }
    
    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect e) {
    	if(!Utils.isAnyLeader(e.getTargetEntity()))
    		return;
    	
    	Player player = e.getTargetEntity();
        if (AGPConfig.Announcements.announceLeaderQuit) {
            Utils.sendToAll(AGPConfig.Announcements.leaderQuitMessage.replace("{leader}", player.getName()), true);

            List<String> closedGyms = new ArrayList<>();
            List<String> npcGyms = new ArrayList<>();
            
            for (GymStruc gs : DataStruc.gcon.GymData) {
                if (gs.OnlineLeaders.contains(player.getUniqueId())) {
                    gs.OnlineLeaders.remove(player.getUniqueId());
                    if (gs.Status == OPEN) {
                        if (gs.OnlineLeaders.isEmpty()) {
                            if (gs.NPCAmount > 0) {
                                if (AGPConfig.General.offlineNPC) {
                                    gs.Status = NPC;
                                    npcGyms.add(gs.Name);
                                } else {
                                    gs.Status = CLOSED;
                                    closedGyms.add(gs.Name);
                                }
                            } else {
                                gs.Status = CLOSED;
                                npcGyms.add(gs.Name);
                            }
                            gs.Queue.clear();
                        }
                    }
                }
            }

            if(AGPConfig.Announcements.closeAnnouncement)
            {
            	if(!closedGyms.isEmpty())
            	{
            		if(closedGyms.size() == 1)
            		{
            			Utils.sendToAll("&7The &b" + closedGyms.get(0) + " &7gym has closed.", true);
            		}
            		else
            		if(closedGyms.size() == 2)
            		{
            			Utils.sendToAll("&7The &b" + closedGyms.get(0) + " &7and &b" + closedGyms.get(1) + " &7gyms have closed.", true);
            		}
            		else
            		{
            			Utils.sendToAll("&7Multiple gyms have closed. Use &b/GymList &7to see what gyms are currently open.", true);
            		}
            	}
            	
            	if(!npcGyms.isEmpty())
            	{
            		if(npcGyms.size() == 1)
            		{
            			Utils.sendToAll("&7The &b" + closedGyms.get(0) + " &7gym is now being run by NPCs.", true);
            		}
            		else
            		if(npcGyms.size() == 2)
            		{
            			Utils.sendToAll("&7The &b" + closedGyms.get(0) + " &7and &b" + closedGyms.get(1) + " &7gyms are now being run by NPCs.", true);
            		}
            		else
            		{
            			Utils.sendToAll("&7Multiple gyms are being run by NPCs. Use &b/GymList &7to see what gyms are currently open.", true);
            		}
            	}
            }
        }

    }

    @Listener
    public void onPlayerInteractWithKarp(InteractEntityEvent.Secondary.MainHand event, @Root Player player) {
        Entity targetEntity = event.getTargetEntity();

        Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        if(!itemInHand.isPresent()) {
            return;
        }

        if (targetEntity instanceof EntityPixelmon) {
            EntityPixelmon pixelmon = (EntityPixelmon) targetEntity;
            if (pixelmon.baseStats.pokemon == EnumPokemon.Magikarp) {
                if (player.get(Keys.IS_SNEAKING).get()) {
                    if (eventCounter == 3) {
                        eventCounter = 0;

                        if (itemInHand.get().getType().getName().toLowerCase().contains("fish")) {
                            Task.builder()
                                    .interval(1, TimeUnit.SECONDS)
                                    .execute(new JumpThread(pixelmon))
                                    .submit(AGP.getInstance());

                            player.sendMessage(Utils.toText("&bMagikarp &7is appalled you would attempt to feed it &bFish&7. &bMagikarp &7is leaving...", true));
                        }
                    } else {
                        eventCounter++;
                    }
                }
            }
        }
    }

}

class JumpThread implements Runnable {
    private EntityPixelmon p;
    private float startingblock = 0;
    private float i = 0;

    JumpThread(EntityPixelmon pixelmon) {
        p = pixelmon;
        startingblock = (float) p.posY;
        i = startingblock;
    }

    @Override
    public void run() {
        if(i < startingblock + 20) {
            i+= 0.5;
            p.setPosition(p.posX, i, p.posZ);

            ParticleEffect effect = ParticleEffect.builder()
                    .type(ParticleTypes.WATER_SPLASH)
                    .quantity(50)
                    .build();
            ((Entity)p).getWorld().spawnParticles(effect, ((Entity)p).getLocation().getPosition().add(0, -0.5, 0));

        }
    }
}
