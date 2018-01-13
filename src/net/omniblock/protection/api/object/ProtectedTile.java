package net.omniblock.protection.api.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import net.omniblock.network.library.utils.LocationUtils;
import net.omniblock.protection.api.config.ConfigType;
import net.omniblock.protection.api.type.TileType;

public class ProtectedTile {

	protected String uniqueID;
	protected String protectorID;
	
	protected Sign protectionSign;
	protected List<Block> protectionStructure = new ArrayList<Block>();
	
	protected BlockState tileEntity;
	protected TileType tileType;
	
	public ProtectedTile(String uniqueID, String protectorID, Sign protectionSign, TileType tileType, BlockState tileEntity) {
		
		this.uniqueID = uniqueID;
		this.protectorID = protectorID;
		
		this.protectionSign = protectionSign;
		
		this.tileType = tileType;
		this.tileEntity = tileEntity;
		
		return;
		
	}
	
	public void load() {
		
		TileType loadType = TileType.getTileType(tileEntity);
		
		if(loadType != tileType)
			this.tileType = loadType;
			
		protectionStructure.add(tileEntity.getBlock());
		
		Location tileLoc = tileEntity.getLocation();
		
		switch(tileType) {
		
			case TRAPPED_CHEST: {
				
				break;
				
			}
		
			case CHEST: {
				
				break;
				
			}
				
			case DOOR: {
				
				tileLoc.add(0, 1, 0);
					if(TileType.getTileType(tileLoc.getBlock().getState()) == TileType.DOOR){
						
						protectionStructure.add(tileLoc.getBlock());
						Location baseLoc = tileLoc.add(0, -2, 0);
						protectionStructure.add(baseLoc.getBlock());
						break;
				
					}
				
				tileLoc.add(0, -2, 0);
				if(TileType.getTileType(tileLoc.getBlock().getState()) == TileType.DOOR){
					
					protectionStructure.add(tileLoc.getBlock());
					Location baseLoc = tileLoc.add(0, -1, 0);
					protectionStructure.add(baseLoc.getBlock());
					break;
				
				}
					
				break;
			}
			
			case DOOR_BLOCK: {
				
				for(int i = 0; i < 3; i++){
					tileLoc.add(0, -1, 0);
					protectionStructure.add(tileLoc.getBlock());
				}
				
				break;
			}
				
			case DOUBLE_CHEST: {
				tileLoc.add(-1, 0, 0);
				if(tileLoc.getBlock().getType() == Material.CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				tileLoc.add(2, 0, 0);
				if(tileLoc.getBlock().getType() == Material.CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				tileLoc.add(-1, 0, -1);
				if(tileLoc.getBlock().getType() == Material.CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				tileLoc.add(0, 0, 2);
				if(tileLoc.getBlock().getType() == Material.CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				break;
				
			}
			
			case DOUBLE_TRAPPED_CHEST: {
				tileLoc.add(-1, 0, 0);
				if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				tileLoc.add(2, 0, 0);
				if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				tileLoc.add(-1, 0, -1);
				if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				tileLoc.add(0, 0, 2);
				if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST){
					
					protectionStructure.add(tileLoc.getBlock());
					break;
					
				}
				
				break;
				
			}
				
			case ENCHANTING_TABLE: {
				
				break;
				
			}
				
			case FURNACE: {
				
				break;
				
			}
			
		}
		
		saveSign();
	}
	
	public void destroy(){
		
		protectionStructure = new ArrayList<Block>();
		ConfigType.PROTECTION_DATA.getConfig().set("protectedsigns." + uniqueID, null);
		ConfigType.PROTECTION_DATA.getConfigObject().save();
		
		return;
	}
	
	public void saveSign() {
		
		for(Map.Entry<String, Object> entry : getConfigData().entrySet())
			ConfigType.PROTECTION_DATA.getConfig().set(entry.getKey(), entry.getValue());
		
		ConfigType.PROTECTION_DATA.getConfigObject().save();
		return;
		
	}
	
	@SuppressWarnings("serial")
	public Map<String, Object> getConfigData() {
		return new HashMap<String, Object>(){{
			
			put("protectionsigns." + uniqueID + ".protectorID", protectorID);
			put("protectionsigns." + uniqueID + ".location", LocationUtils.serializeLocation(protectionSign.getLocation()));
			put("protectionsigns." + uniqueID + ".type", tileType.name());
			
		}};
	}
	
	public String getUniqueID(){
		return uniqueID;
	}
	
	public BlockState getTileEntity() {
		return tileEntity;
	}
	
	public Sign getProtectionSign() {
		return protectionSign;
	}
	
	public String getProtectorID() {
		return protectorID;
	}
	
	public List<Block> getStructure(){
		return protectionStructure;
	}
	public void setTileType(TileType type){
		
		this.tileType = type;
		
		return;
	}
}
