package net.omniblock.protection.api.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	protected List<Block> protectionStructure;
	
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
			
		switch(tileType) {
		
			case CHEST: {
				
				
				break;
				
			}
				
			case DOOR: {
				
				
				break;
				
			}
				
			case DOUBLE_CHEST: {
				
				break;
				
			}
				
			case ENCHANTING_TABLE: {
				
				break;
				
			}
				
			case FURNACE: {
				
				break;
				
			}
			
		}
		
		
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
			put("protectionsigns." + uniqueID + ".type", tileType);
			
		}};
	}
	
	public BlockState getTileEntity() {
		return tileEntity;
	}
	
	public Sign getProtectionSign() {
		return protectionSign;
	}
	
}
