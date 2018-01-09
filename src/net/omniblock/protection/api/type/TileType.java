package net.omniblock.protection.api.type;

import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.EnchantingTable;
import org.bukkit.block.Furnace;

public enum TileType {

	CHEST,
	DOUBLE_CHEST,
	DOOR,
	FURNACE,
	ENCHANTING_TABLE,
	
	;
	
	public static TileType getTileType(BlockState state) {
		
		if(state instanceof Chest)
			return CHEST;
			
		if(state instanceof DoubleChest)
			return DOUBLE_CHEST;
		
		if(state.getBlock().getType().name().contains("DOOR"))
			return DOOR;
		
		if(state instanceof Furnace)
			return FURNACE;
		
		if(state instanceof EnchantingTable)
			return ENCHANTING_TABLE;
		
		return null;
		
	}
	
}
