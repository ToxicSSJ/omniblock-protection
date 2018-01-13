package net.omniblock.protection.api.type;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.EnchantingTable;
import org.bukkit.block.Furnace;

public enum TileType {

	CHEST,
	DOOR,
	DOOR_BLOCK,
	DOUBLE_CHEST,
	DOUBLE_TRAPPED_CHEST,
	ENCHANTING_TABLE,
	FURNACE,
	TRAPPED_CHEST,
	
	;
	
	public static TileType getTileType(BlockState state) {
		
		if(state.getBlock().getType().equals(Material.TRAPPED_CHEST)){
			
			Location tileLoc = state.getLocation();
			
			tileLoc.add(-1, 0, 0);
			if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST) return DOUBLE_TRAPPED_CHEST;
			
			tileLoc.add(2, 0, 0);
			if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST) return DOUBLE_TRAPPED_CHEST;
			
			tileLoc.add(-1, 0, -1);
			if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST) return DOUBLE_TRAPPED_CHEST;
			
			tileLoc.add(0, 0, 2);
			if(tileLoc.getBlock().getType() == Material.TRAPPED_CHEST)return DOUBLE_TRAPPED_CHEST;
			
			return TRAPPED_CHEST;
		}
		
		
		if(state instanceof Chest){
			
			Location tileLoc = state.getLocation();
			
			tileLoc.add(-1, 0, 0);
			if(tileLoc.getBlock().getType() == Material.CHEST) return DOUBLE_CHEST;
			
			tileLoc.add(2, 0, 0);
			if(tileLoc.getBlock().getType() == Material.CHEST) return DOUBLE_CHEST;
			
			tileLoc.add(-1, 0, -1);
			if(tileLoc.getBlock().getType() == Material.CHEST) return DOUBLE_CHEST;
			
			tileLoc.add(0, 0, 2);
			if(tileLoc.getBlock().getType() == Material.CHEST)return DOUBLE_CHEST;
			
			return CHEST;
		}
		
		if(state instanceof Furnace)
			return FURNACE;
		
		if(state instanceof EnchantingTable)
			return ENCHANTING_TABLE;
		
		if(state.getBlock().getType().name().contains("DOOR"))
			return DOOR;
		
		Location detectDoor = state.getBlock().getLocation();
		detectDoor.add(0, -1, 0);
		if(detectDoor.getBlock().getType().name().contains("DOOR")){
			return DOOR_BLOCK;
		}
		
		return null;
		
	}
	
}
