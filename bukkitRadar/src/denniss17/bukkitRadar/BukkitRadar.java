package denniss17.bukkitRadar;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import denniss17.bukkitRadar.radars.BaseRadar;
import denniss17.bukkitRadar.radars.MobRadar;
import denniss17.bukkitRadar.radars.OreRadar;
import denniss17.bukkitRadar.radars.PlayerRadar;
import denniss17.bukkitRadar.utils.ChatStyler;
import denniss17.bukkitRadar.utils.VersionChecker;

public class BukkitRadar extends JavaPlugin implements Runnable {
	public static VersionChecker versionChecker;
	public static final int PROJECT_ID = 55957;
	
	private Set<BaseRadar> currentRadars;
	
	private BukkitTask timerTask;
	
	public enum RadarType{
		PLAYER_RADAR, MOB_RADAR, ORE_RADAR
	}
	
	public void onEnable(){
		// Register PlayerListener
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
		
		if(getConfig().getBoolean("check_updates")){
			versionChecker = new VersionChecker(this, PROJECT_ID);
			versionChecker.activate(getConfig().getInt("check_updates_interval")*60*20);
		}		
		
		currentRadars = new HashSet<BaseRadar>();
		
		BukkitRadarCommands executor = new BukkitRadarCommands(this);
		this.getCommand("bukkitradar").setExecutor(executor);
		
		File file = new File(getDataFolder(), "config.yml");
		if(!file.exists()){
			saveDefaultConfig();
		}
	}
	
	public void onDisable(){
		for(BaseRadar radar : currentRadars){
			radar.destroy();
		}
		currentRadars.clear();
		stopTimer();
	}
	
	public void reload() {
		for(BaseRadar radar : currentRadars){
			radar.preReload();
		}
		reloadConfig();
		for(BaseRadar radar : currentRadars){
			radar.reload();
		}
	}

	public void updateRadars(){
		for(BaseRadar radar: currentRadars){
			radar.updateRadar();
		}
	}
	
	public void showRadar(Player player, RadarType type){
		BaseRadar current = getRadarByPlayer(player);
		if(current!=null){
			if(current.getType().equals(type)){
				// Radar is already of correct type
				return;
			}else{
				// Destroy radar
				current.destroy();
				currentRadars.remove(current);
			}
		}
		
		// Create new radar
		BaseRadar radar = null;
		switch(type){
		case MOB_RADAR:
			radar = new MobRadar(this, player);
			break;
		case ORE_RADAR:
			radar = new OreRadar(this, player);
			break;
		case PLAYER_RADAR:
			radar = new PlayerRadar(this, player);
			break;
		}
		
		// Start new radar
		radar.init();
		currentRadars.add(radar);
		startTimer();
	}

	public BaseRadar getRadarByPlayer(Player player){
		for(BaseRadar radar: currentRadars){
			if(radar.getPlayer().equals(player)) return radar;
		}
		return null;
	}
	
	public void hideRadar(Player player){
		BaseRadar current = getRadarByPlayer(player);
		if(current!=null){
			current.destroy();
			currentRadars.remove(current);
		}
		if(currentRadars.size()==0){
			stopTimer();
		}
	}
	
	public void startTimer(){
		if(timerTask==null){
			int interval = getConfig().getInt("radar_update_interval");
			timerTask = getServer().getScheduler().runTaskTimer(this, this, interval, interval);
		}
	}
	
	public void stopTimer(){
		if(timerTask!=null){
			timerTask.cancel();
			timerTask = null;
		}
	}

	@Override
	public void run() {
		updateRadars();		
	}

	public void sendConfigMessage(CommandSender sender, String key) {
		sender.sendMessage(ChatStyler.setTotalStyle(getConfig().getString("messages."+key)));		
	}
}
