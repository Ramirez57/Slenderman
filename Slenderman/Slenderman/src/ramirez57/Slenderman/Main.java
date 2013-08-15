package ramirez57.Slenderman;

import java.io.File;
//import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	Server mc;
	PluginManager pluginmgr;
	Logger log;
	File configFile;
	Slenderman2 slender;
	FileConfiguration config;
	Player player;
	
	public void onEnable() {
		mc = this.getServer();
		log = this.getLogger();
		pluginmgr = this.getServer().getPluginManager();
		slender = new Slenderman2(this);
		config = this.getConfig();
		configFile = new File(this.getDataFolder(), "config.yml");
		config.options().copyDefaults(true);
		this.saveConfig();
		pluginmgr.registerEvents(this, this);
		slender_init();
	}
	
	public void onDisable() {
		if(slender.slenderman != null) slender.slenderman.remove();
	}
	
	public void slender_init() {
		/*slender.x = 0;
		slender.y = 0;
		slender.z = 64;
		slender.plugin=this;
		slender.config=this.config;
		slender.log=this.getLogger();
		slender.mc=this.getServer();
		slender.random=new Random();
		slender.tasktick=0;
		slender.init_chase = true;
		slender.just_shocked = false;
		slender.caught=false;
		mc.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				slender.stalk();
			}
		}, 60L, 10L);*/
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]) {
		boolean isPlayer = sender instanceof Player;
		if(cmd.getName().equalsIgnoreCase("slenderman")) {
			if(args.length < 1) {
				return false;
			}
			if(args[0].equalsIgnoreCase("reload")) {
				config = YamlConfiguration.loadConfiguration(configFile);
				log.info("Configuration reloaded.");
				if(isPlayer) sender.sendMessage(ChatColor.GREEN + "Configurations reloaded.");
				return true;
			}
			if(args[0].equalsIgnoreCase("target")) {
				if(args.length > 1){
					player = mc.getPlayer(args[1]);
					if(!player.isOnline()) {
						sender.sendMessage("That player is not online.");
						return true;
					}
					if(player.isDead()) {
						sender.sendMessage("That player is dead.");
						return true;
					}
					if(!config.getStringList("worlds").contains(player.getWorld().getName())) {
						sender.sendMessage("That player is in a world Slenderman cannot go to.");
						return true;
					}
					slender.target(player);
					return true;
				}
			}
			if(args[0].equalsIgnoreCase("location")) {
				sender.sendMessage(slender.slenderman.getLocation().toString());
				return true;
			}
		}
		return false;
	}
	
	@EventHandler
	public void slendernoattack(EntityDamageByEntityEvent e) {
		if(e.getDamager() == slender.slenderman && e.getEntity() instanceof Player) {
			e.setCancelled(true);
		}
		if(e.getEntity() == slender.slenderman) {
			e.setCancelled(true);
		}
	}
}
