package denniss17.bukkitRadar.radars;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;

import denniss17.bukkitRadar.BukkitRadar;
import denniss17.bukkitRadar.BukkitRadar.RadarType;
import denniss17.bukkitRadar.utils.ChatStyler;

public class OreRadar extends BaseRadar {
	private Map<Material, Integer> oresDistances;
	
	// Local variables, used in calculation
	private int distance;
	private Block block;
	private Set<Material> toRemove;
	
	public static Map<Material, String> oresShowed;

	public OreRadar(BukkitRadar plugin, Player player) {
		super(plugin, player, plugin.getConfig().getInt("oreradar.radius"));
		this.type = RadarType.ORE_RADAR;
		this.toRemove = new HashSet<Material>();
		this.oresDistances = new HashMap<Material, Integer>();
		if(oresShowed == null){
			loadOresShowed();
		}
	}
	
	private void loadOresShowed(){
		oresShowed = new HashMap<Material, String>();
		if(plugin.getConfig().contains("oreradar.blocks")){
			ConfigurationSection section = plugin.getConfig().getConfigurationSection("oreradar.blocks");
			for(Entry<String, Object> entry : section.getValues(false).entrySet()){
				try{
					oresShowed.put(Material.valueOf(entry.getKey()), entry.getValue().toString());
				}catch(IllegalArgumentException e){
					plugin.getLogger().warning("Material '" + entry.getKey() + "' in orerader.blocks (config.yml) is not a valid material!");
				}
			}
		}
	}

	@Override
	public void init() {
		this.objective = player.getScoreboard().getObjective("oreradar");
		if(this.objective==null){
			this.objective = player.getScoreboard().registerNewObjective("oreradar", "dummy");
		}
		this.objective.setDisplayName(parseObjectiveName(plugin.getConfig().getString("oreradar.name")));
		this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
	
	@Override
	public void preReload(){
		// Clear scoreboard before reload
		for(Entry<Material, Integer> entry: oresDistances.entrySet()){
			// Remove from scoreboard
			this.removeCustomScore(ChatStyler.setMessageColor(oresShowed.get(entry.getKey())));
		}
		oresDistances.clear();
	}
	
	@Override
	public void reload(){
		loadOresShowed();
		this.radius = plugin.getConfig().getInt("oreradar.radius");
		this.objective.setDisplayName(parseObjectiveName(plugin.getConfig().getString("oreradar.name")));
	}

	@Override
	public void updateRadar() {
		// Reset counts
		for(Material type: oresDistances.keySet()){
			oresDistances.put(type, -1);
		}
		
		Location l = player.getLocation();
		Set<Material> materials = oresShowed.keySet();
				
		for(int x=l.getBlockX()-radius; x<l.getBlockX()+radius; x++){
			for(int y=l.getBlockY()-radius; y<l.getBlockY()+radius; y++){
				for(int z=l.getBlockZ()-radius; z<l.getBlockZ()+radius; z++){
					block = player.getWorld().getBlockAt(x,y,z);
					if(materials.contains(block.getType())){
						distance = (int)l.distance(block.getLocation());
						if(distance<=radius){
							if(oresDistances.containsKey(block.getType())){
								int current = oresDistances.get(block.getType());
								if(current==-1 || distance < current){
									oresDistances.put(block.getType(), distance);
								}
							}else{
								oresDistances.put(block.getType(), distance);
							}
						}
					}
				}
			}
		}
		
		// Remove score from list or send score
		for(Entry<Material, Integer> entry: oresDistances.entrySet()){
			if(entry.getValue()==-1){
				// Remove from scoreboard
				this.removeCustomScore(ChatStyler.setMessageColor(oresShowed.get(entry.getKey())));
				// Remove from mapping
				// Don't remove directly: causes ConcurrentModificationException
				toRemove.add(entry.getKey());
			}else{
				// Send score
				this.sendCustomScore(ChatStyler.setMessageColor(oresShowed.get(entry.getKey())), entry.getValue());
			}
		}
		// Clean up
		for(Material type : toRemove){
			oresDistances.remove(type);
		}
		toRemove.clear();
	}

	@Override
	public void destroy() {
		this.objective.setDisplaySlot(null);		
	}
	
}
