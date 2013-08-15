package ramirez57.Slenderman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Slenderman2 {

	Plugin plugin;
	Server mc;
	Enderman slenderman;
	FileConfiguration config;
	boolean canBeSeen;
	Player prey;
	int catchTime;
	int teleportCooldown;
	int shockCooldown;
	boolean caught;
	int capturing;
	Random random;
	List<Location> teleportLocations;
	List<String> worlds;
	Chunk[] chunks;
	Chunk chunk;
	Location location;
	int tick;
	boolean teleported;
	/*TO DO
	 * Make slenderism
	 * Skin
	 * etc..
	 */
	
	public Slenderman2(Plugin pl) {
		tick = 0;
		random = new Random();
		this.plugin = pl;
		mc = plugin.getServer();
		config = plugin.getConfig();
		canBeSeen = false;
		prey = null;
		slenderman = null;
		caught=false;
		teleported=false;
		capturing = 0;
		shockCooldown=0;
		teleportCooldown=0;
		teleportLocations = new ArrayList<Location>();
		worlds = config.getStringList("worlds");
		for(String world : worlds) {
			if(mc.getWorld(world) == null) {
				this.plugin.getLogger().log(Level.WARNING, world + " does not exist. Please change this in the configuration file.");
				worlds.remove(world);
			}
		}
		mc.getScheduler().scheduleSyncRepeatingTask(pl, new Runnable() {
			public void run() {
				loop();
			}
		}, 60L, 1L);
	}
	
	public void loop() {
		PlayerTick();
		SlendermanTick();
		if(tick>=1000)tick=0;
		tick++;
 	}
	
	public void PlayerTick() {
		if(prey != null && slenderman != null) {
			if(shockCooldown > 0) {
				shockCooldown--;
			}
			if(config.getBoolean("drums") && config.getBoolean("sounds") && tick % 40 == 0) {
				prey.playSound(prey.getLocation(),Sound.NOTE_BASS_DRUM,1.0F,1.0F);
			}
			int dist = (int)slenderman.getLocation().distance(prey.getLocation());
			boolean visible = prey.getTargetBlock(null, dist).getType() == Material.AIR;
			if(this.canBeSeen) {
				if(visible) {
					if(tick % (dist*1.5) == 0 && prey.getHealth() > 1) {
						prey.damage(1);
						this.capturing++;
					}
					if(dist <= 11D) {
						this.capturing++;
					}
					if(dist <= 6.2D && shockCooldown == 0) {
						shock(prey);
						prey.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,300,0));
						prey.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER,300,0));
						shockCooldown=300;
					}
				} else {
					if(this.capturing>0) this.capturing--;
				}
			} else {
				if(this.capturing>0) this.capturing--;
				if(this.capturing < 4) {
					prey.removePotionEffect(PotionEffectType.CONFUSION);
				}
			}
			if(this.capturing >= 4 && this.capturing <32) {
				Static(prey);
			}
			if(dist < 1.02D && capturing<33) {
				this.capturing = 33;
			}
			if(this.capturing >= 32) {
				Location loc = prey.getLocation();
				loc.setYaw(0F);loc.setPitch(-45F);
				prey.teleport(loc);
				loc.setYaw(180F);loc.setPitch(30F);
				slenderman.teleport(loc.add(0,0,1));
				this.capturing+=2;
				Static(prey);
			}
			if(this.capturing >= 100) {
				Catch(prey);
			}
		}
	}
	
	public void SlendermanTick() {
		chunks = mc.getWorld(worlds.get(random.nextInt(worlds.size()))).getLoadedChunks();
		chunk = chunks[random.nextInt(chunks.length)];
		location = new Location(chunk.getWorld(),((chunk.getX()*16)+random.nextInt(16)),0,((chunk.getZ()*16)+random.nextInt(16)));
		location.setY(getFirstClearY(location.getWorld(), location.getBlockX(),location.getBlockZ()));
		if(teleportLocations.size() >= 50) {
			teleportLocations.remove(0);
		}
		teleportLocations.add(location);
		if(slenderman == null) SpawnSlenderman(location);
		slenderman.setNoDamageTicks(100);
		//plugin.getLogger().info("" + teleportCooldown);
		if(this.teleportCooldown > 0 && tick % 10 == 0) {
			this.teleportCooldown--;
		}
		if(this.prey == null) {
			teleport();
			for (Player pl : mc.getOnlinePlayers()) {
				if(pl.getWorld().equals(slenderman.getWorld()) && slenderman.getLocation().distance(pl.getLocation()) <= (config.getDouble("aggro") / (double)pl.getHealth()) && isValidPrey(pl)) { prey = pl;break; }
			}
		} else {
			if(!isValidPrey(prey)) { Reset(); return; }
			this.canBeSeen = false;
			if(this.canSee(prey, slenderman.getLocation())) {
				this.canBeSeen = true;
			}
			if(slenderman.getTarget() == null) {
				if(random.nextFloat() > getHostility()) {
					if(random.nextFloat() > 0.5F) {
						//teleport in view
						teleportInViewOfPlayer(prey);
					} else {
						teleportWithinPlayerRadius(prey, 60D - 54D * getHostility());
					}
				} else if(random.nextFloat() > 0.3F * getHostility()) {
					teleportWithinPlayerRadius(prey, 60D - 54D * getHostility());
				}
			} else if (!this.canBeSeen) {
				if(random.nextFloat() > (float)Math.sqrt(getHostility())) {
					if(random.nextFloat() < 0.5F * getHostility()) {
						teleportInViewOfPlayer(prey);
					} else {
						teleportWithinPlayerRadius(prey, 60D - 54D * getHostility());
					}
				} else {
					//"jump" to players
					slenderman.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30,(int)(10 / prey.getHealth())));
				}
			}
			
			if(!this.canBeSeen || this.capturing>=32) slenderman.setTarget(prey); else slenderman.setTarget(null);
			if(!isValidPrey(prey)) { Reset();return; }
			if(slenderman.getLocation().distance(prey.getLocation()) >= ((config.getDouble("aggro")*40 / (double)prey.getHealth())/prey.getFoodLevel())) {Reset();return;}
			ShowSlenderman(prey,false);
		}
	}
	
	public void Static(Player pl) {
		if(config.getBoolean("static")) {
			if(prey.hasPotionEffect(PotionEffectType.CONFUSION)) {
				prey.removePotionEffect(PotionEffectType.CONFUSION);
			} else {
				prey.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,1000,1));
			}
			if(config.getBoolean("sounds")) {
				pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
			}
		}
	}
	
	public void Catch(Player pl) {
		shock(pl);
		caught=true;
		slenderman.teleport(pl.getTargetBlock(new HashSet<Byte>(1), 2).getLocation());
		if(config.getString("slender_dimension") == "") {
			pl.damage(100000);
		} else {
			pl.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,config.getInt("slow_dur"),1));
			pl.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,config.getInt("confusion_dur"),1));
			pl.teleport(mc.getWorld(config.getString("slender_dimension")).getSpawnLocation());
		}
		mc.broadcastMessage(pl.getDisplayName() + " has been taken by the Slenderman.");
		Reset();
		return;
	}
	
	public void Reset() {
		prey = null;
		//slenderman.remove();
		//slenderman = null;
		teleportCooldown=0;
		shockCooldown=0;
		capturing=0;
	}
	
	public void SlenderSound(Player pl, Location loc, Sound sound, float vol, float pitch) {
		if(config.getBoolean("sounds")) pl.playSound(loc, sound, vol, pitch);
	}
	
	public void teleport() {
		if(teleportLocations.size() > 0) teleport(teleportLocations.get(random.nextInt(teleportLocations.size())));
	}
	
	public void teleport(Location loc) {
		//plugin.getLogger().info("" + teleportCooldown);
		if(teleportCooldown == 0) {
			respawn(loc);
			teleportCooldown = (int) Math.ceil(50.0D * Math.abs(getHostility()-1));
		}
	}
	
	public void teleportInViewOfPlayer(Player pl) {
		this.teleport(pl.getTargetBlock(null, 100).getLocation());
	}
	
	public void teleportWithinPlayerRadius(Player pl, double radius) {
		this.teleportWithinPlayerRadius(pl, radius, false);
	}
	
	public void teleportWithinPlayerRadius(Player pl, double radius, boolean force) {
		if(force) {
			Location loc =pl.getLocation().add((random.nextInt((int) (radius*2))-radius),0,(random.nextInt((int) (radius*2))-radius));
			loc.setY(getFirstClearY(loc.getWorld(), loc.getBlockX(), loc.getBlockZ())+0.5);
		} else {
			for(int i=0;i<teleportLocations.size();i++) {
				location = teleportLocations.get(i);
				if(location.distance(pl.getLocation()) <= radius && checkClear(location)) {teleport(location);return;}
			}
			teleport();
		}
	}
	
	public void SpawnSlenderman(Location loc) {
		slenderman = (Enderman) loc.getWorld().spawnEntity(loc, EntityType.ENDERMAN);
		slenderman.setNoDamageTicks(100);
		slenderman.setCarriedMaterial(new MaterialData(Material.IRON_HELMET));
	}
	
	public float getHostility() {
		if(prey == null) return 0.0F;
		float f1 = (float)prey.getHealth() / 20F;
		return Math.abs(f1-1);
	}
	
	public int getFirstClearY(World world, int x, int z) {
		int ret;
		for (ret=1;world.getBlockAt(x,ret,z).getType() != Material.AIR;ret++);
		return ret;
	}
	
	public void searchPrey() {
		Player[] players;
		players = mc.getOnlinePlayers();
		if(players.length > 0) {
			if(random.nextInt(config.getInt("chance")) == 1) {
				prey = players[random.nextInt(players.length)];
				System.out.println("" + slenderman.getLocation().distance(prey.getLocation()));
				if(config.getStringList("worlds").contains(prey.getWorld().getName()) && slenderman.getLocation().distance(prey.getLocation()) <= 32 && isValidPrey(prey)) {
					if(config.getBoolean("anytime")){
						return;
					} else if(prey.getWorld().getTime() < 12500L) {
						prey = null;
						return;
					} else {
						return;
					}
				} else {
					prey = null;
					return;
				}
			}
		}
	}
	
	public boolean canSee(Player src, Location dest) {
		double viewThreshold = 1.099557428756428D;
		return canSee(src,dest,viewThreshold);
	}
	
	public boolean canSee(Player src, Location dest, double viewThreshold) {
		if ((src == null) || (dest == null)) {
			return false;
		}
		return getAngle(src, dest) < viewThreshold;
	}
	
	public double getAngle(Player player, Location slender) {
		return getAngle(player, slender.getX(), slender.getY(), slender.getZ());
	}
	
	public double getAngle(Player player, double x, double y, double z) {
		Location cam = player.getLocation();
		float f1 = (float) Math.cos(-cam.getYaw() * 0.01745329F - 3.141593F);
		float f3 = (float) Math.sin(-cam.getYaw() * 0.01745329F - 3.141593F);
		float f5 = (float) -Math.cos(-cam.getPitch() * 0.01745329F);
		float f7 = (float) Math.sin(-cam.getPitch() * 0.01745329F);
		
		double lookx = f3 * f5;
		double looky = f7;
		double lookz = f1 * f5;
		
		double dx = x - cam.getX();
		double dy = y - cam.getY();
		double dz = z - cam.getZ();
		
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		
		double dot = dx / len * lookx + dy / len * looky + dz / len * lookz;
		
		double angle = Math.acos(dot);
		
		return angle;
	}
	
	public void shock(Player entity) {
		if(config.getBoolean("sounds")) {
		entity.playSound(entity.getLocation(), Sound.NOTE_PIANO, 2.0F, 1.0F);
		entity.playSound(entity.getLocation(), Sound.NOTE_PIANO, 1.0F, 0.8F);
		entity.playSound(entity.getLocation(), Sound.NOTE_PIANO, 1.0F, 0.5F);
		entity.playSound(entity.getLocation(), Sound.NOTE_PIANO, 1.0F, 0.2F); }
	}
	
	public void target(Player entity) {
		this.Reset();
		this.teleportWithinPlayerRadius(entity, ((config.getDouble("aggro")*40 / (double)entity.getHealth())/entity.getFoodLevel()), true);
		prey = entity;
	}
	
	public boolean checkClear(Location loc) {
		if(loc.getWorld().getBlockAt(loc).getType() == Material.AIR && loc.getWorld().getBlockAt(loc.add(0,1,0)).getType() == Material.AIR && loc.getWorld().getBlockAt(loc.add(0,1,0)).getType() == Material.AIR) {
			loc.subtract(0,2,0);return true;
		} else {
			loc.subtract(0,2,0);return false;
		}
	}
	
	public boolean visibleTo(Player pl) {
		return canSee(pl, slenderman.getLocation()) && pl.getTargetBlock(null,(int) pl.getLocation().distance(slenderman.getLocation())).getType() == Material.AIR;
	}
	
	public boolean isValidPrey(Player pl) {
		return pl.getGameMode() != GameMode.CREATIVE && !pl.hasPermission("slenderman.exempt") && !pl.isDead() && pl.isOnline() && worlds.contains(pl.getWorld().getName());
	}
	
	public void respawn(Location loc) {
		slenderman.remove();
		SpawnSlenderman(loc);
	}
	
	public void ShowSlenderman(Player pl, boolean show) {
		location = slenderman.getLocation();
		for(int i=0;i<3;i++) {
			if(show) {
				if(i==2) {
					pl.sendBlockChange(location,Material.WOOL, (byte)0);
				} else {
					pl.sendBlockChange(location, Material.WOOL, (byte)15);
				}
			} else {
				pl.sendBlockChange(location, location.getBlock().getType(), location.getBlock().getData());
			}
			location.add(0,1,0);
		}
	}
}
