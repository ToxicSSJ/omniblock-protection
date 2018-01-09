package net.omniblock.protection.base.object;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.sk89q.worldedit.entity.Player;

import net.omniblock.network.handlers.base.sql.util.Resolver;
import net.omniblock.packets.util.Lists;
import net.omniblock.protection.base.ProtectionBase;

public class PlayerMembersWrapper {

	protected String playerNetworkID;
	protected List<String> playerMembersIDs = Lists.newArrayList();
	
	public PlayerMembersWrapper(String playerNetworkID, String compactedMembers) {
		
		this.playerNetworkID = playerNetworkID;
		
		if(compactedMembers.contains(";"))
			for(String cacheMemberID : compactedMembers.split(";"))
				if(Resolver.hasNetworkID(cacheMemberID))
					playerMembersIDs.add(cacheMemberID);
		
		ProtectionBase.setMembers(this);
		return;
		
	}
	
	public boolean isOwner(Player player) {
		return isOwner(player.getName(), false);
	}
	
	public boolean isOwner(String param, boolean isNetworkID) {
		
		String cacheNetworkID = isNetworkID ? param : Resolver.getNetworkIDByName(param);
		
		if(playerNetworkID.equals(cacheNetworkID))
			return true;
		
		return false;
		
	}
	
	public boolean isMember(Player player) {
		return isMember(player.getName(), false);
	}
	
	public boolean isMember(String param, boolean isNetworkID) {
		
		String cacheNetworkID = isNetworkID ? param : Resolver.getNetworkIDByName(param);
		
		if(playerNetworkID.equals(cacheNetworkID))
			return true;
			
		return playerMembersIDs.contains(cacheNetworkID);
	}
	
	public void addMember(Player player) {
		addMember(player.getName(), false);
	}
	
	public void addMember(String param, boolean isNetworkID) {
		
		String cacheNetworkID = isNetworkID ? param : Resolver.getNetworkIDByName(param);
		
		if(playerMembersIDs.contains(cacheNetworkID))
			return;
		
		playerMembersIDs.add(cacheNetworkID);
		ProtectionBase.setMembers(this);
		return;
		
	}
	
	public void removeMember(Player player) {
		removeMember(player.getName(), false);
	}
	
	public void removeMember(String param, boolean isNetworkID) {
		
		String cacheNetworkID = isNetworkID ? param : Resolver.getNetworkIDByName(param);
		
		if(!playerMembersIDs.contains(cacheNetworkID))
			return;
		
		playerMembersIDs.remove(cacheNetworkID);
		ProtectionBase.setMembers(this);
		return;
		
	}
	
	public String getPlayerNetworkID() {
		return playerNetworkID;
	}
	
	public List<String> getMembers(boolean useNames) {
		
		if(useNames)
			return new ArrayList<String>() {
			
			private static final long serialVersionUID = 1L;

			{
				
				for(String cacheNetworkID : playerMembersIDs)
					add(Resolver.getLastNameByNetworkID(cacheNetworkID));
				
			}};
		
		return new ArrayList<String>(playerMembersIDs);
			
	}
	
	public String getData() {
		
		if(playerMembersIDs.size() == 1)
			return playerMembersIDs.get(0);
			
		if(playerMembersIDs.size() >= 2)
			return StringUtils.join(playerMembersIDs, ';');
		
		return "";
		
	}
	
}
