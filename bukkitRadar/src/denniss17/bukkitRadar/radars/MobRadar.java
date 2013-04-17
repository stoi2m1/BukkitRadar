package denniss17.bukkitRadar.radars;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;

import denniss17.bukkitRadar.BukkitRadar;
import denniss17.bukkitRadar.BukkitRadar.RadarType;

public class MobRadar extends BaseRadar {
	private Set<EntityType> toRemove;
	private Map<EntityType, Integer> entityCounts;
	
	public static Set<EntityType> mobsShowed;

	public MobRadar(BukkitRadar plugin, Player player) {
		super(plugin, player, plugin.getConfig().getInt("mobradar.radius"));
		this.type = RadarType.MOB_RADAR;
		this.entityCounts = new HashMap<EntityType, Integer>();
		this.toRemove = new HashSet<EntityType>();
		if(mobsShowed == null){
			loadMobsShowed();
		}
	}
	
	private void loadMobsShowed(){
		mobsShowed = new HashSet<EntityType>();
		for(String entityname : plugin.getConfig().getStringList("mobradar.mobs")){
			try{
				mobsShowed.add(EntityType.valueOf(entityname));
			}catch(IllegalArgumentException e){
				plugin.getLogger().warning("Entity '" + entityname + "' in mobradar.mobs (config.yml) is not a valid entityname!");
			}
		}
	}

	@Override
	public void init() {
		this.objective = player.getScoreboard().getObjective("mobradar");
		if(this.objective==null){
			this.objective = player.getScoreboard().registerNewObjective("mobradar", "dummy");
		}
		this.objective.setDisplayName(parseObjectiveName(plugin.getConfig().getString("mobradar.name")));
		this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
	
	@Override
	public void reload(){
		loadMobsShowed();
		this.radius = plugin.getConfig().getInt("mobradar.radius");
		this.objective.setDisplayName(parseObjectiveName(plugin.getConfig().getString("mobradar.name")));
	}

	@Override
	public void updateRadar() {
		// Reset counts
		for(EntityType type: entityCounts.keySet()){
			entityCounts.put(type, 0);
		}
		
		// Count entities
		List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
		for(Entity entity : entities){
			if(mobsShowed.contains(entity.getType())){
				if(entityCounts.containsKey(entity.getType())){
					entityCounts.put(entity.getType(), entityCounts.get(entity.getType())+1);
				}else{
					entityCounts.put(entity.getType(), 1);
				}
			}
		}
		
		// Remove score from list or send score
		for(Entry<EntityType, Integer> entry: entityCounts.entrySet()){
			if(entry.getValue()==0){
				// Remove from scoreboard
				this.removeCustomScore(entry.getKey().toString());
				// Remove from mapping
				// Don't remove directly: causes ConcurrentModificationException
				toRemove.add(entry.getKey());
			}else{
				// Send score
				this.sendCustomScore(entry.getKey().toString().toLowerCase(), entry.getValue());
			}
		}
		// Clean up
		for(EntityType type : toRemove){
			entityCounts.remove(type);
		}
		toRemove.clear();
	}

	@Override
	public void destroy() {
		this.objective.setDisplaySlot(null);		
	}
	
}
