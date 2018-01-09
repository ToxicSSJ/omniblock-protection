package net.omniblock.protection.api.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.omniblock.network.handlers.base.sql.util.Resolver;
import net.omniblock.network.library.utils.LocationUtils;
import net.omniblock.packets.util.Lists;
import net.omniblock.protection.api.ProtectionManager;
import net.omniblock.protection.api.config.ConfigType;
import net.omniblock.protection.api.type.ProtectionZoneType;
import net.omniblock.protection.base.object.PlayerMembersWrapper;
import net.omniblock.regions.Regions;
import net.omniblock.regions.RegionsPlugin;
import net.omniblock.regions.object.RegionAlterator;

public class ProtectedZone {

	public static final RegionAlterator PROTECTED_ZONE_ALTERATOR = new RegionAlterator("protection.zone");
	
	public ProtectedRegion protectedRegion;
	public ProtectionZoneType protectionType;
	
	public String playerNetworkID;
	public String uniqueID;
	
	public Block protectionBlock;
	
	public ProtectedZone(String uniqueID, ProtectionZoneType protectionType, Block protectionBlock, ProtectedRegion protectedRegion, String playerNetworkID){
		
		if(!Regions.HANDLER.hasAlterator(protectedRegion, PROTECTED_ZONE_ALTERATOR))
			Regions.HANDLER.setAlterator(protectedRegion, PROTECTED_ZONE_ALTERATOR);
		
		this.uniqueID = uniqueID;
		this.protectedRegion = protectedRegion;
		this.protectionType = protectionType;
		
		this.playerNetworkID = playerNetworkID;
		this.protectionBlock = protectionBlock;
		
		return;
		
	}
	
	public void load() {
		
		List<String> toRemove = Lists.newArrayList();
		
		PlayerMembersWrapper members = ProtectionManager.getMembers(playerNetworkID, true);
		List<String> membersNetworkIDs = members.getMembers(false);
		
		for(String memberName : protectedRegion.getMembers().getPlayers()) {
			
			boolean delete = true;
			
			for(int i = 0; i < membersNetworkIDs.size(); i++) {
				
				String memberNetworkID = membersNetworkIDs.get(i);
				
				if(memberNetworkID.equals(Resolver.getNetworkIDByName(memberName)))
					delete = false;
				
			}
			
			if(delete)
				toRemove.add(memberName);
					
		}
		
		toRemove.forEach(playerName -> {
			protectedRegion.getMembers().removePlayer(playerName);
		});
		
		saveConfig();
		
	}
	
	public void destroy() {
		
		RegionsPlugin.getWGPlugin().getRegionManager(protectionBlock.getWorld()).removeRegion(protectedRegion.getId());
		
		ConfigType.PROTECTION_DATA.getConfig().set("protectedzones." + uniqueID, null);
		ConfigType.PROTECTION_DATA.getConfigObject().save();
		return;
		
	}
	
	public boolean isOwner(Player player) {
		
		String cacheNetworkID = Resolver.getNetworkIDByName(player.getName());
		
		if(cacheNetworkID.equals(playerNetworkID))
			return true;
		
		return false;
		
	}
	
	public boolean isOwner(String param, boolean isNetworkID) {
		
		String cacheNetworkID = isNetworkID ? param : Resolver.getNetworkIDByName(param);
		
		if(cacheNetworkID.equals(playerNetworkID))
			return true;
		
		return false;
		
	}
	
	public void saveConfig() {
		
		for(Map.Entry<String, Object> entry : getConfigData().entrySet())
			ConfigType.PROTECTION_DATA.getConfig().set(entry.getKey(), entry.getValue());
		
		ConfigType.PROTECTION_DATA.getConfigObject().save();
		return;
		
	}
	
	public ProtectedRegion getRegion() {
		return protectedRegion;
	}
	
	public Block getBlock() {
		return protectionBlock;
	}
	
	public String getPlayerNetworkID() {
		return playerNetworkID;
	}
	
	public String getID() {
		return uniqueID;
	}
	
	@SuppressWarnings("serial")
	public Map<String, Object> getConfigData() {
		return new HashMap<String, Object>(){{
			
			put("protectedzones." + uniqueID + ".regionID", protectedRegion.getId());
			put("protectedzones." + uniqueID + ".location", LocationUtils.serializeLocation(protectionBlock.getLocation()));
			put("protectedzones." + uniqueID + ".protectionType", protectionType.name());
			put("protectedzones." + uniqueID + ".playerNetworkID", playerNetworkID);
			
		}};
	}
	
}
