package com.gmail.wintersj7.laststand;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class LastStand extends JavaPlugin {
	
	// playerList represents all players currently "down" awaiting revival.
	private static ArrayList<Player> playerList = new ArrayList<Player>();
	
		
	private BukkitTask deathTicker;

	
	@Override
    public void onEnable(){
		new LastStandListener(this);
		this.saveDefaultConfig();
		deathTicker = new DyingTickerAsyncTask(this).runTaskTimerAsynchronously(this,0,100);
    }
 
    @Override
    public void onDisable() {
    	deathTicker.cancel();
    }
    
	public static ArrayList<Player> getPlayerList() {
		return playerList;
	}

	public static void setPlayerList(ArrayList<Player> playerList) {
		LastStand.playerList = playerList;
	}
    
	public static void addPlayer(Player player) {
		if (!LastStand.playerList.contains(player))
			LastStand.playerList.add(player);
	}
	
	public static void removePlayer(Player player) {
		LastStand.playerList.remove(player);
	}
	
	public static boolean isDown(Player player){
		return LastStand.playerList.contains(player);
	}
	
	public static boolean playerListIsEmpty(){
		return LastStand.playerList.isEmpty();
	}
 
	public static void resetPlayer(Player player){
		LastStand.removePlayer(player);
		player.removePotionEffect(PotionEffectType.BLINDNESS);
		player.setWalkSpeed((float)0.2);
		player.setCanPickupItems(true);
		player.setSneaking(false);
	}
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("laststand.revive") || cmd.getName().equalsIgnoreCase("ls.revive")) { 
			if (args.length < 1)
				return false;
			
			Player target = (Bukkit.getServer().getPlayer(args[0]));
			if (target == null) {
				sender.sendMessage(args[0] + " is not online!");
				return true;
		    } else if (!LastStand.isDown(target)) {
		    	 sender.sendMessage(args[0] + " is not dying and therefore cannot be revived!");
		    	 return true;
		    }
		    else {
		    	LastStand.resetPlayer(target);
		    	target.setHealth(20);
		    	target.setFoodLevel(20);
		    	
		    	if (args.length < 2) {
		    		sender.sendMessage(args[0] + " was removed from LastStand mode with notification.");
		    		target.sendMessage("You were removed from LastStand mode by admin.");
		    		return true;
		    	}
		    	
		    	if (args[1].equalsIgnoreCase("quiet")){
		    		sender.sendMessage(args[0] + " was removed from LastStand mode without notification.");
		    	}
		    	else if (args[1].equalsIgnoreCase("silent")){
		    		// send no messages
		    	}
		    	else {
		    		sender.sendMessage(args[0] + " was removed from LastStand mode with notification.");
		    		target.sendMessage("You were removed from LastStand mode by admin.");
		    	}
		    	
		    	return true;
		    }
		    	 
		} 
		else if (cmd.getName().equalsIgnoreCase("laststand.reload") || cmd.getName().equalsIgnoreCase("ls.reload")) { 
			reloadConfig();
			sender.sendMessage("LastStand configs reloaded.");
			return true;
		}
		
		return false;
	} // end onCommand
	
} // end LastStand class


class DyingTickerAsyncTask extends BukkitRunnable {
// Remains in the background, firing off synchronous DyingTickTasks
// for any player who is "down".
    private final LastStand laststand;
 
    public DyingTickerAsyncTask(LastStand laststand) {
        this.laststand = laststand;
    }
    
    public void run() {
   	
    	if (LastStand.playerListIsEmpty() != true) {
    		for (int i = 0; i < LastStand.getPlayerList().size(); i++) {	
    			Player player = LastStand.getPlayerList().get(i);
    			
    			new DyingTickTask(laststand, player).runTask(laststand);
    		}
    	}
    }
 
}

class DyingTickTask extends BukkitRunnable {
	 
    private final LastStand laststand;
    private Player player;
    
    public DyingTickTask(LastStand laststand, Player player) {
        this.laststand = laststand;
        this.player = player;
    }
 
    public void run() {
    	
    	final int MAX_TIME = laststand.getConfig().getInt("time");
    	
    	//player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, PotionType.INSTANT_HEAL);
    	if (laststand.getConfig().getBoolean("ve_smoke"))
    		player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
    	
    	if (laststand.getConfig().getBoolean("ve_ender_pearl"))
    		player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 0);
    	
    	if (laststand.getConfig().getBoolean("ve_damage")) {
    		player.setHealth(2);
    		player.damage(1);
    	}
    	
    	if (laststand.getConfig().getBoolean("ve_blindness")) {
    		PotionEffect pe = new PotionEffect(PotionEffectType.BLINDNESS,MAX_TIME,1);
    		player.addPotionEffect(pe, true);
    	}
    	
    	if (player.getFoodLevel() <= 0) {
    		//player.setHealth(0);
    		player.damage(player.getMaxHealth());
    		//LastStand.resetPlayer(player);
    		
    	}
    	else {
    		// Periodic server message if enabled.
    		if (laststand.getConfig().getBoolean("msg_do_broadcast_periodic")) {
  			  String temp = laststand.getConfig().getString("msg_broadcast_periodic");
  			  temp = temp.replace("<name>", player.getDisplayName());
  			  player.getServer().broadcastMessage(temp);
  			}
    		
    		// Periodic message to players in specified range if enabled.
    		if (laststand.getConfig().getBoolean("range_do_periodic")) {
  			  double rangexz = laststand.getConfig().getDouble("range_msgs_xz_blocks");
  			  double rangey = laststand.getConfig().getDouble("range_msgs_y_blocks");
  			  
  			  String temp = laststand.getConfig().getString("range_msg_periodic");
  			  temp = temp.replace("<name>", player.getDisplayName());
  			  
  			  // Don't forget to also message the player (not included in getNearbyEntities)
  			  player.sendMessage(temp);
	    		  
  			  ArrayList<Entity> near_entities = (ArrayList<Entity>) player.getNearbyEntities(rangexz, rangey, rangexz);
  			  for (int i = 0; i < near_entities.size(); i++) {
  				if (near_entities.get(i) instanceof Player) {
  					Player one_player = (Player) near_entities.get(i);
  					one_player.sendMessage(temp);
  					
  				} 
  			  }
  			}
    	}
        
    	player.setFoodLevel(player.getFoodLevel() - 1);
    }
 
}


class LastStandListener implements Listener {
		
	private final LastStand laststand;
	 
    public LastStandListener(LastStand laststand) {
        this.laststand = laststand;
        laststand.getServer().getPluginManager().registerEvents(this, laststand);
    }

    public LastStand getLastStand(){
    	return this.laststand;
    }
    
    
    // Resurrection event.
    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent evt) {
    	 	
    	if (evt.getRightClicked() instanceof Player){
    		
    		Player target = (Player) evt.getRightClicked();
    		boolean has_permission = evt.getPlayer().hasPermission("laststand.revive_other");

    		
    		if (LastStand.isDown(target) && !LastStand.isDown(evt.getPlayer())){
    			
    			final ArrayList<String> ALLOWED_ITEMS = (ArrayList<String>) laststand.getConfig().getStringList("items_to_revive");
    	    	boolean any = ALLOWED_ITEMS.contains("ANY");
    	    	ItemStack handItem = evt.getPlayer().getInventory().getItemInHand();
    	    	
    	    	if (!has_permission)
        			evt.getPlayer().sendMessage("You don't have permission to revive someone.");
    	    	else if (ALLOWED_ITEMS.contains(handItem.getType().toString()) || any) {
    	    		
    	    	  if (ALLOWED_ITEMS.contains(handItem.getType().toString()) && getLastStand().getConfig().getBoolean("items_used_up")) {
    	    		  if (handItem.getAmount() > 1)
    	    			  handItem.setAmount(handItem.getAmount() - 1);
    	    		  else
    	    			  evt.getPlayer().getInventory().setItemInHand(null);
    	    	  }
    	    	 
    			  evt.setCancelled(true);
    			  target.setHealth(laststand.getConfig().getInt("revived_hp"));
    			  LastStand.resetPlayer(target);
        		
        		  if (getLastStand().getConfig().getBoolean("msg_do_broadcast_revival")) {			 		
      			  	String temp = "someone";
      			  	temp = getLastStand().getConfig().getString("msg_broadcast_revival");
      			  	temp = temp.replace("<name1>", target.getDisplayName());
      			  	temp = temp.replace("<name2>", evt.getPlayer().getDisplayName());
      			  	target.getServer().broadcastMessage(temp);
      			  }
        		  
        		  if (laststand.getConfig().getBoolean("range_do_revival")) {
          			  double rangexz = laststand.getConfig().getDouble("range_msgs_xz_blocks");
          			  double rangey = laststand.getConfig().getDouble("range_msgs_y_blocks");
          			  
          			  String temp = "someone";
      			  	  temp = getLastStand().getConfig().getString("range_msg_revival");
      			  	  temp = temp.replace("<name1>", target.getDisplayName());
      			  	  temp = temp.replace("<name2>", evt.getPlayer().getDisplayName());
          			  
          			  // Don't forget to also message the player (not included in getNearbyEntities)
          			  target.sendMessage(temp);
        	    		  
          			  ArrayList<Entity> near_entities = (ArrayList<Entity>) target.getNearbyEntities(rangexz, rangey, rangexz);
          			  for (int i = 0; i < near_entities.size(); i++) {
          				if (near_entities.get(i) instanceof Player) {
          					Player one_player = (Player) near_entities.get(i);
          					one_player.sendMessage(temp);
          					
          				} 
          			  }
          			}
        		  
        		  
        		  
    	    	}
    			else {
    				if (getLastStand().getConfig().getBoolean("msg_do_display_no_item"))
    					evt.getPlayer().sendMessage(getLastStand().getConfig().getString("msg_no_item"));
    			}
    		}
    	}
    }
    
    
    // Initiate last stand event.
    // Priority is an issue because other damage cancelling effects, such as safe falling
    // in MagicSpells' Leap don't always work in time to prevent Last Stand.
	@EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent evt) {
    	
		if (!laststand.getConfig().getStringList("worlds_supported_list").contains(evt.getEntity().getWorld().getName())) {
			// Do nothing
			//((Player) evt.getEntity()).sendMessage(evt.getEntity().getWorld().getName());
		}
			
		else if (evt.getEntity() instanceof Player){
    		Player player = (Player)evt.getEntity();
    		double damage = evt.getDamage();
    		
    		boolean suicide = evt.getCause().toString().equals("SUICIDE");
    		boolean projectile = evt.getCause().toString().equals("PROJECTILE");
    		boolean has_permission = player.hasPermission("laststand.dying_mode");
    		
    		//player.getServer().broadcastMessage("Damage cause: " + evt.getCause().toString());
    		
    		// Fall damage checker
    		if (LastStand.isDown(player)) {
    			if (evt.getCause().equals(DamageCause.FALL) && laststand.getConfig().getBoolean("falling_hurts")) {		
    				evt.setCancelled(true);	
    				player.setFoodLevel((int) (player.getFoodLevel() - evt.getDamage()));
    				player.setHealth(2);
    				player.damage(1);
    			}
    			else if (evt.getCause().equals(DamageCause.FIRE_TICK) && laststand.getConfig().getBoolean("burning_hurts")) {		
    				evt.setCancelled(true);	
    				player.setFoodLevel((int) (player.getFoodLevel() - evt.getDamage()));
    				player.setHealth(2);
    				player.damage(1);
    			}
    			else if (evt.getCause().equals(DamageCause.FIRE) && laststand.getConfig().getBoolean("fire_hurts")) {		
    				evt.setCancelled(true);	
    				player.setFoodLevel((int) (player.getFoodLevel() - evt.getDamage()));
    				player.setHealth(2);
    				player.damage(1);
    			}
    			/*else if (evt.getCause().equals(DamageCause.LAVA) && laststand.getConfig().getBoolean("lava_hurts")) {		
    				evt.setCancelled(true);	
    				player.setFoodLevel((int) (player.getFoodLevel() - evt.getDamage()));
    				player.setHealth(2);
    				player.damage(1);
    			}*/
    			else if (evt.getCause().equals(DamageCause.DROWNING) && laststand.getConfig().getBoolean("drowning_hurts")) {		
    				evt.setCancelled(true);	
    				player.setFoodLevel((int) (player.getFoodLevel() - evt.getDamage()));
    				player.setHealth(2);
    				player.damage(1);
    			}
    			else if (evt.getCause().equals(DamageCause.BLOCK_EXPLOSION) && laststand.getConfig().getBoolean("exploding_hurts")) {		
    				evt.setCancelled(true);	
    				player.setFoodLevel((int) (player.getFoodLevel() - evt.getDamage()));
    				player.setHealth(2);
    				player.damage(1);
    			}
    			else if (evt.getCause().equals(DamageCause.SUFFOCATION) && laststand.getConfig().getBoolean("suffocation_hurts")) {		
    				evt.setCancelled(true);	
    				player.setFoodLevel((int) (player.getFoodLevel() - evt.getDamage()));
    				player.setHealth(2);
    				player.damage(1);
    			}    			
    			else if (evt.getCause().equals(DamageCause.VOID)) {		
    				evt.setCancelled(false);	
    			}
    			else if (suicide)
    				evt.setCancelled(false);
    			else if (projectile && laststand.getConfig().getBoolean("player_kill_on")) {
    				if (laststand.getConfig().getBoolean("player_kill_on")) {
    					damage = evt.getDamage() * (laststand.getConfig().getDouble("player_kill_mitigation") / 100);
    					if (damage < 1.0)
    						damage = 1.0;
    					
    					evt.setCancelled(true);	
    					player.setFoodLevel((int) (player.getFoodLevel() - damage));
    					player.setHealth(2);
    					player.damage(1);
    				}
    			}
    			
    			else
    				evt.setCancelled(true);
    			
    		}
    		
    		boolean skipLS = false;
    		if (!LastStand.isDown(player) && (player.getHealth() - damage) <= 0 && has_permission){
    			if (suicide)
    				skipLS = true;
    			
    			if (evt.getCause().equals(DamageCause.FALL) && laststand.getConfig().getBoolean("falling_kills")) 		
    				skipLS = true;
    			
    			if (evt.getCause().equals(DamageCause.FIRE_TICK) && laststand.getConfig().getBoolean("burning_kills")) 		
    				skipLS = true;
    			
    			if (evt.getCause().equals(DamageCause.FIRE) && laststand.getConfig().getBoolean("fire_kills")) 		
    				skipLS = true;
    			
    			if (evt.getCause().equals(DamageCause.LAVA)) 		
    				skipLS = true;
    			
    			if (evt.getCause().equals(DamageCause.DROWNING) && laststand.getConfig().getBoolean("drowning_kills")) 		
    				skipLS = true;
    			
    			if (evt.getCause().equals(DamageCause.SUFFOCATION) && laststand.getConfig().getBoolean("suffocation_kills")) 		
    				skipLS = true;
    			
    			if (evt.getCause().equals(DamageCause.VOID)) 		
    				skipLS = true;
    			
    			if (player.getGameMode().equals(GameMode.CREATIVE))
    				skipLS = true;
    		}
    		
    		
    		// Determine if the damage is a killing blow
    		if (!LastStand.isDown(player) && (player.getHealth() - damage) <= 0 && !skipLS && has_permission){
    		
    			// Check cause
    			
    			
    			// Prevent the player from dropping items/dying;
    			// Make the player invulnerable/cancel all damage;
    			// Cancel the damage and set health to 1.
    			evt.setCancelled(true);
    			player.setHealth(2);
    			player.damage(1);
    			
    			// Disallow player to pick up items.
    			// Note this doesn't seem to actually work as of 1.6; now we listen for PlayerPickupItem instead
    			player.setCanPickupItems(false);
    			
    			String temp = "someone";
    			
    			if (getLastStand().getConfig().getBoolean("msg_do_display_start")) {			 		
    			  // Display message to player.
    			  temp = getLastStand().getConfig().getString("msg_start_dying");
    			  temp = temp.replace("<name>", player.getDisplayName());
    			  player.sendMessage(temp);
    			}
    			
    			if (getLastStand().getConfig().getBoolean("msg_do_broadcast_start")) {
    			  // Display server message (con).
    			  temp = getLastStand().getConfig().getString("msg_broadcast_start");
    			  temp = temp.replace("<name>", player.getDisplayName());
    			  player.getServer().broadcastMessage(temp);
    			}
    			
    			if (getLastStand().getConfig().getBoolean("range_do_start")) {
    			  double rangexz = getLastStand().getConfig().getDouble("range_msgs_xz_blocks");
    			  double rangey = getLastStand().getConfig().getDouble("range_msgs_y_blocks");
    			  
    			  temp = getLastStand().getConfig().getString("range_msg_start_dying");
	    		  temp = temp.replace("<name>", player.getDisplayName());
	    		  
    			  ArrayList<Entity> near_entities = (ArrayList<Entity>) player.getNearbyEntities(rangexz, rangey, rangexz);
    			  for (int i = 0; i < near_entities.size(); i++) {
    				if (near_entities.get(i) instanceof Player) {
    					Player one_player = (Player) near_entities.get(i);
    					one_player.sendMessage(temp);
    					
    				} // else {
    					//player.getServer().broadcastMessage(near_entities.get(i).getType().getName() + " found.");
    				//}
    			  }
    			}
    			
    			
    			// Save players items.
    	    	// Remove all player items except sword, bow, and arrows (con).
    			// Instead, make inventory inaccessible (done by inventoryBlocker event handler)	
    			List<String> FINAL_ITEMS = laststand.getConfig().getStringList("items_useable_list");
    			
    	    	if (!FINAL_ITEMS.contains(player.getInventory().getItemInHand().getType().toString()))
    	    	{
    	    		if (!player.getInventory().getItemInHand().getType().toString().equals("AIR")) {
    	    			if (laststand.getConfig().getBoolean("msg_do_display_drop")) {
    	    				player.sendMessage(laststand.getConfig().getString("msg_drop_item"));
    	    				player.getWorld().dropItemNaturally(player.getLocation(), player.getInventory().getItemInHand());
    	    				player.getInventory().clear(player.getInventory().getHeldItemSlot());
    	    			}
    	    		}
    	    	}
    	    	
    	    	// Make the player invisible to monsters.
    			// (New targeters done by on target event handler)
    	    	// Resets all monsters' targets within 20 blocks
    	    	ArrayList<Entity> mobs = (ArrayList<Entity>) player.getNearbyEntities(20, 20, 20);
    	    	for (int i = 0; i < mobs.size(); i++) {
    	    		if (mobs.get(i) instanceof Monster){
    	    			Monster monster = (Monster)mobs.get(i);
    	    			monster.setTarget(null);
    	    		}
    	    	}
    	    	// Set flag indicating player can be revived.
    			LastStand.addPlayer(player);
    			
    	    	// Change player model to sitting or crawling.
    	    	if (laststand.getConfig().getBoolean("ve_sneak")) {
    	    		player.setSneaking(true);
    	    	}
    			
    			// Slow the player's movement speed (con).
    			player.setWalkSpeed((float)laststand.getConfig().getDouble("walk_speed"));
    			
    			// Set exp bar as timer expiring (con).
    			player.setFoodLevel(laststand.getConfig().getInt("time")/5);
    					
    			// Add blindness 1
    			if (laststand.getConfig().getBoolean("ve_blindness")) {
    				PotionEffect pe = new PotionEffect(PotionEffectType.BLINDNESS,laststand.getConfig().getInt("time"),0);
            		player.addPotionEffect(pe, true);
    			}
    			
    		}
    		
    		// Kill player instantly if something lowered their food level to 0
    		// AND they are already dying! (Not if they are simply starving.)
    		if (player.getFoodLevel() <= 0 && LastStand.isDown(player)) {
				player.setHealth(0);	
				LastStand.resetPlayer(player);
			}
    	}	
    }
	
	
	// PvP option
	@EventHandler (priority = EventPriority.HIGHEST)
	public void pvpOption (EntityDamageByEntityEvent event) { 
		if (!laststand.getConfig().getStringList("worlds_supported_list").contains(event.getEntity().getWorld().getName())) {
			// Do nothing
		}
			
		else if (laststand.getConfig().getBoolean("player_kill_on")) {
		if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
			Player victim = (Player) event.getEntity();
			Player damager = (Player) event.getDamager();
			
			if (LastStand.isDown(victim)) {
				double damage;
				
				if (laststand.getConfig().getDouble("player_kill_mitigation") < 1 || laststand.getConfig().getDouble("player_kill_mitigation") > 100){
					damage = event.getDamage();
				} else {
					damage = event.getDamage() * (laststand.getConfig().getDouble("player_kill_mitigation") / 100);
					if (damage < 1)
						damage = 1;
				}
				
				victim.setFoodLevel((int) (victim.getFoodLevel() - (int) damage));
				victim.setHealth(2);
				victim.damage(1);
				
				if (victim.getFoodLevel() <= 0) {
					victim.setHealth(0);
					LastStand.resetPlayer(victim);
					
					String msg = laststand.getConfig().getString("msg_broadcast_player_kill");
					msg = msg.replaceAll("<name1>", victim.getDisplayName());
					msg = msg.replaceAll("<name2>", damager.getDisplayName());
					if (laststand.getConfig().getBoolean("msg_do_broadcast_player_kill"))
						victim.getServer().broadcastMessage(msg);
					
					
					if (laststand.getConfig().getBoolean("range_do_player_kill")) {
	          			  double rangexz = laststand.getConfig().getDouble("range_msgs_xz_blocks");
	          			  double rangey = laststand.getConfig().getDouble("range_msgs_y_blocks");
	          			  
	          			  msg = laststand.getConfig().getString("range_msg_player_kill");
	          			  msg = msg.replaceAll("<name1>", victim.getDisplayName());
						  msg = msg.replaceAll("<name2>", damager.getDisplayName());
	          			  
	          			  // Don't forget to also message the player (not included in getNearbyEntities)
	          			  victim.sendMessage(msg);
	        	    		  
	          			  ArrayList<Entity> near_entities = (ArrayList<Entity>) victim.getNearbyEntities(rangexz, rangey, rangexz);
	          			  for (int i = 0; i < near_entities.size(); i++) {
	          				if (near_entities.get(i) instanceof Player) {
	          					Player one_player = (Player) near_entities.get(i);
	          					one_player.sendMessage(msg);
	          					
	          				} 
	          			  }
	          		}
				}
			}
		}
		} // end pvp check
	}
	
	
	// Monsters' player-targeting disabler.
	@EventHandler
	public void monsterInvisibility(EntityTargetEvent event) {
	    if (event.getTarget() instanceof Player) {
	    	
	    	Player target = (Player) event.getTarget();
	    	
	    	if (LastStand.isDown(target)){
	    		if (event.getEntity() instanceof Monster){
	    			Monster mob = (Monster) event.getEntity();
	    			event.setCancelled(true); 
		    		mob.setTarget(null);
	    		}
	    	}
		}
	}
	
	
	// Skeleton projectile blocker
	@EventHandler
	public void monsterProjectileImmunity(EntityShootBowEvent event) {
	    if (event.getEntity() instanceof Skeleton) {
	    	
	    	Skeleton skelly = (Skeleton) event.getEntity();
	    	
	    	if (skelly.getTarget() instanceof Player){
	    		Player target = (Player) skelly.getTarget();
	    			
	    		if (LastStand.isDown(target)){
	    			event.setCancelled(true);
	    		}
	    	}
	    }
	}
	
	// Consumption blocker.
		@EventHandler
		public void noConsume(PlayerItemConsumeEvent event) {
		    if (event.getPlayer() instanceof Player) {
		    	
		    	Player target = (Player) event.getPlayer();
		    	
		    	if (LastStand.isDown(target)){
		    		event.setCancelled(true); 
			    	
		    		
		    	}
			}
		}
	
	// Inventory blocker.
	@EventHandler
	public void inventoryBlocker(InventoryClickEvent event) {
	    	
		Player player = (Player) event.getWhoClicked();
	    	
	    if (LastStand.isDown(player)){
	    	event.setCancelled(true);
	    	player.closeInventory();
	    	if (laststand.getConfig().getBoolean("msg_do_display_inventory"))
	    		player.sendMessage(laststand.getConfig().getString("msg_inventory_access"));
	    }
		
	}
	
	
	// Item use blocker.
	@EventHandler
	public void itemBlocker(PlayerItemHeldEvent event) {
	    	
		Player player = (Player) event.getPlayer();
	    	
	    if (LastStand.isDown(player)){
	    	try {
	    		String lastItem = player.getInventory().getItem(event.getNewSlot()).getType().toString();
	    		
	    		final ArrayList<String> FINAL_ITEMS = (ArrayList<String>) laststand.getConfig().getStringList("items_useable_list");
	    		
	    		if (!FINAL_ITEMS.contains(lastItem)) {
	    			event.setCancelled(true);
	    			if (laststand.getConfig().getBoolean("msg_do_display_switch"))
	    	    		player.sendMessage(laststand.getConfig().getString("msg_switch_item"));
	    		}
	    	}
	    	catch (Exception e){
	    		
	    	}
	    }
			
	}
	
	// Logout blocker.
	@EventHandler
	public void logoutBlocker(PlayerQuitEvent event) {
		    	
		Player player = (Player) event.getPlayer();    	
		
	    if (LastStand.isDown(player)){
	    	World world = player.getWorld();
	    	
	    	if (laststand.getConfig().getBoolean("items_do_drop")) {
	    	  ItemStack[] items = player.getInventory().getContents();
	    	  for (int i = 0; i < items.length; i++)
	    	  {
	    		if (items[i] != null)
	    			world.dropItemNaturally(player.getLocation(), items[i]);
	    	  }
	    	  
	    	}

	    	if (laststand.getConfig().getBoolean("msg_do_display_dc"))
	    		player.sendMessage(laststand.getConfig().getString("msg_dc"));
	    	
	    	if (getLastStand().getConfig().getBoolean("msg_do_broadcast_dc")) {
	    		String temp = "someone";
	    		temp = getLastStand().getConfig().getString("msg_broadcast_dc");
	    		temp = temp.replace("<name>", player.getDisplayName());
	    		player.getServer().broadcastMessage(temp);
	    	}
	    	
	    	if (laststand.getConfig().getBoolean("range_do_dc")) {
    			  double rangexz = laststand.getConfig().getDouble("range_msgs_xz_blocks");
    			  double rangey = laststand.getConfig().getDouble("range_msgs_y_blocks");
    			  
    			  String temp = laststand.getConfig().getString("range_msg_dc");
    			  temp = temp.replace("<name>", player.getDisplayName());
    			  
    			  ArrayList<Entity> near_entities = (ArrayList<Entity>) player.getNearbyEntities(rangexz, rangey, rangexz);
    			  for (int i = 0; i < near_entities.size(); i++) {
    				if (near_entities.get(i) instanceof Player) {
    					Player one_player = (Player) near_entities.get(i);
    					one_player.sendMessage(temp);
    					
    				} 
    			  }
    			}
	    	
	    	player.getInventory().clear();
	    	player.setHealth(0);

	    	LastStand.resetPlayer(player);
	    }
		
	}
	
	// This method developed because player.setCanPickupItems (false) stopped working in 1.6
	@EventHandler 
	public void pickupBlocker(PlayerPickupItemEvent event) {
		Player player = (Player) event.getPlayer();	
		if (LastStand.isDown(player))
			event.setCancelled(true);		
	}

	
	// A safety net to catch players who somehow respawn in LastStand mode, and reset them.
	@EventHandler 
	public void respawnSafety(PlayerRespawnEvent event) {
		Player player = (Player) event.getPlayer();
		if (LastStand.isDown(player))
			LastStand.resetPlayer(player);			
	}
	
	
	// A safety net to catch players who teleport (instead of respawn?), and reset them.
	@EventHandler 
	public void teleportSafety(PlayerTeleportEvent event) {
		Player player = (Player) event.getPlayer();
		if (LastStand.isDown(player) && laststand.getConfig().getBoolean("teleport_restore"))
			LastStand.resetPlayer(player);			
	}
	
	// A death listener to catch all players that die and reset them.
	// This should take care of players getting stuck in laststand mode.
	@EventHandler 
	public void deathResetter(EntityDeathEvent event) {
		if (event.getEntity() instanceof Player)
			LastStand.resetPlayer((Player)event.getEntity());
					
	}
	
	
	
}