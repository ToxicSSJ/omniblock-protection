package net.omniblock.protection.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.Lists;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.omniblock.network.handlers.base.bases.type.RankBase;
import net.omniblock.network.handlers.base.sql.util.Resolver;
import net.omniblock.network.library.helpers.ItemBuilder;
import net.omniblock.network.library.helpers.actions.SimpleEventListener;
import net.omniblock.network.library.helpers.inventory.InventoryBuilder;
import net.omniblock.network.library.helpers.inventory.paginator.InventoryPaginator;
import net.omniblock.network.library.helpers.inventory.paginator.InventorySlotter;
import net.omniblock.network.library.helpers.inventory.paginator.InventoryPaginator.PaginatorStyle;
import net.omniblock.network.library.helpers.inventory.paginator.InventorySlotter.SlotLocatorType;
import net.omniblock.network.library.utils.InventoryUtils;
import net.omniblock.network.library.utils.LocationUtils;
import net.omniblock.network.library.utils.TextUtil;
import net.omniblock.network.library.utils.TileUtils;
import net.omniblock.protection.ProtectionPlugin;
import net.omniblock.protection.api.config.ConfigType;
import net.omniblock.protection.api.config.variables.LineRegex;
import net.omniblock.protection.api.object.ProtectedTile;
import net.omniblock.protection.api.object.ProtectedZone;
import net.omniblock.protection.api.type.ProtectionZoneType;
import net.omniblock.protection.api.type.TileType;
import net.omniblock.protection.base.ProtectionBase;
import net.omniblock.protection.base.object.PlayerMembersWrapper;
import net.omniblock.regions.RegionsPlugin;
import net.omniblock.survival.base.SurvivalBankBase;

public class ProtectionManager {

	protected static List<ProtectedZone> registeredZones = Lists.newArrayList();
	protected static List<ProtectedTile> registeredTiles = Lists.newArrayList();
	
	protected static List<PlayerMembersWrapper> registeredMembers = Lists.newArrayList();
	
	/**
	 * 
	 * Instalación del sistema de piedras de
	 * protección.
	 * Este metodo estatico debe ser ejecutado
	 * al iniciar el plugin.
	 * 
	 */
	public static void setup() {
		
		ProtectionPlugin.getInstance().getServer().getPluginManager().registerEvents(new ProtectionListener(), ProtectionPlugin.getInstance());
		
		//
		// Se cargarán a continuación todas las
		// protecciones tipo zona registradas dentro de la
		// configuración.
		//
		if(ConfigType.PROTECTION_DATA.getConfig().isSet("protectedzones"))
			if(ConfigType.PROTECTION_DATA.getConfig().isConfigurationSection("protectedzones"))
				for(String uniqueID : ConfigType.PROTECTION_DATA.getConfig().getConfigurationSection("protectedzones").getKeys(false)) {
					
					try {
						
						Block block = LocationUtils.deserializeLocation(ConfigType.PROTECTION_DATA.getConfig().getString("protectedzones." + uniqueID + ".location")).getBlock();
						ProtectedRegion region = RegionsPlugin.getWGPlugin().getRegionManager(block.getWorld()).getRegion(ConfigType.PROTECTION_DATA.getConfig().getString("protectedzones." + uniqueID + ".regionID"));
						
						ProtectionZoneType protectionType = ProtectionZoneType.valueOf(ConfigType.PROTECTION_DATA.getConfig().getString("protectedzones." + uniqueID + ".protectionType"));
						String playerNetworkID = ConfigType.PROTECTION_DATA.getConfig().getString("protectedzones." + uniqueID + ".playerNetworkID");
						
						ProtectedZone protectedZone = new ProtectedZone(
								uniqueID,
								protectionType,
								block,
								region,
								playerNetworkID);
						
						if(protectionType.getProtectionItem().getType().isBlock())
							if(block.getType() != protectionType.getProtectionItem().getType())
								block.setType(protectionType.getProtectionItem().getType());
						
						protectedZone.load();
						registerZone(protectedZone);
						continue;
						
					}  catch(Exception e) {
						
						ConfigType.PROTECTION_DATA.getConfig().set("protectedzones." + uniqueID, null);
						ConfigType.PROTECTION_DATA.getConfigObject().save();
						continue;
						
					}
					
				}
		
		//
		// Se cargarán a continuación todas las
		// protecciones tipo cartel registradas dentro de la
		// configuración.
		//
		if(ConfigType.PROTECTION_DATA.getConfig().isSet("protectionsigns"))
			if(ConfigType.PROTECTION_DATA.getConfig().isConfigurationSection("protectionsigns"))
				for(String uniqueID : ConfigType.PROTECTION_DATA.getConfig().getConfigurationSection("protectionsigns").getKeys(false)) {
				
					try {
						
						Block block = LocationUtils.deserializeLocation(ConfigType.PROTECTION_DATA.getConfig().getString("protectionsigns." + uniqueID + ".location")).getBlock();
						
						Sign sign = (Sign) block.getState();
						
						if(sign.getLines().length >= 2) {
							
							if(ConfigType.PROTECTION_DATA.getConfig().isSet("protectionsigns"))
								if(sign.getLine(0).equalsIgnoreCase(LineRegex.PROTECTION_PREFIX_TILE_UP)) {
									
									String protectorNetworkID = ConfigType.PROTECTION_DATA.getConfig().getString("protectionsigns." + uniqueID + ".protectorID");
									TileType tileType = TileType.valueOf(ConfigType.PROTECTION_DATA.getConfig().getString("protectionsigns." + uniqueID + ".type"));
									
									org.bukkit.material.Sign signMaterial = (org.bukkit.material.Sign) sign.getData();
									BlockFace attachedFace = signMaterial.getAttachedFace();
								
									BlockState tileEntity= sign.getBlock().getRelative(attachedFace).getState();
								
									
									if(tileEntity == null) {
										
										ConfigType.PROTECTION_DATA.getConfig().set("protectionsigns." + uniqueID, null);
										ConfigType.PROTECTION_DATA.getConfigObject().save();
										continue;
										
									}
									
									ProtectedTile protectedTile = new ProtectedTile(
											uniqueID,
											protectorNetworkID,
											sign,
											tileType,
											tileEntity);
									
									protectedTile.load();
									registeredTiles.add(protectedTile);
									continue;
									
								}
							
						}
						
					} catch(Exception e) {
						
						ConfigType.PROTECTION_DATA.getConfig().set("protectionsigns." + uniqueID, null);
						ConfigType.PROTECTION_DATA.getConfigObject().save();
						continue;
						
					}
				
			}
		
		//
		// Se cargarán a continuación todos
		// los carteles de protección colocados
		// para actualizarlos en caso de que
		// se hubiese cambiado el precio, el radio
		// o cualquier atributo.
		//
		if(ConfigType.PROTECTION_DATA.getConfig().isSet("buysigns"))
			for(String location : ConfigType.PROTECTION_DATA.getConfig().getStringList("buysigns")) {
				
				try {
					
					Location loc = LocationUtils.deserializeLocation(location);
					Sign sign = (Sign) loc.getBlock().getState();
					
					if(sign.getLines().length >= 4) {
						
						if(ConfigType.PROTECTION_DATA.getConfig().isSet("buysigns"))
							if(sign.getLine(0).equalsIgnoreCase(LineRegex.PROTECTION_PREFIX_SHOP_UP)) {
								
								String protectionName = ChatColor.stripColor(sign.getLine(2));
								
								for(ProtectionZoneType type : ProtectionZoneType.values())
									if(type.getShortname().equalsIgnoreCase(protectionName)) {
										
										sign.setLine(0, LineRegex.PROTECTION_PREFIX_SHOP_UP);
										sign.setLine(1, TextUtil.format("&l" + type.getRadius() + "x" + type.getRadius() + "x" + type.getRadius()));
										sign.setLine(2, TextUtil.format("&n" + type.getShortname()));
										sign.setLine(3, TextUtil.format("&l$&r " + type.getPrice()));
										continue;
										
									}
								
							}
						
					}
					
				} catch(Exception e) {}
				
			}
		
	}
	
	public static void openGUI(Player player) {
		
		PlayerMembersWrapper wrapper = getMembers(player.getName(), false);
		
		InventoryPaginator paginator = new InventoryPaginator(PaginatorStyle.COLOURED_ARROWS);
		InventorySlotter slotter = new InventorySlotter(SlotLocatorType.ROUND_SIX);
		
		InventoryBuilder cacheBuilder = new InventoryBuilder(TextUtil.format("&aMis Miembros"), 6 * 9, false);
		
		net.omniblock.network.library.helpers.inventory.InventoryBuilder.Action addMemberAction = new net.omniblock.network.library.helpers.inventory.InventoryBuilder.Action() {

			@Override
			public void click(ClickType click, Player player) {
				
				player.closeInventory();
				
				player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
				player.sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &aA continuación escribe el nombre del usuario al que añadirás a tus protecciones:"));
				
				new SimpleEventListener<AsyncPlayerChatEvent>(AsyncPlayerChatEvent.class, true) {

					@Override
					public boolean incomingEvent(AsyncPlayerChatEvent e) {
						
						if(e.getPlayer() == player) {
							
							e.setCancelled(true);
							
							if(e.getMessage().startsWith("/")) {

								player.sendMessage(TextUtil.format(
										"&8&lP&8rotecciones &b&l» &cERROR &7No debes escribir ningún comando durante la adición de un miembro, escribe el nombre del usuario que al que añadirás a tus protecciones:"));
								return false;

							}
							
							if(e.getMessage().length() >= 20) {

								player.sendMessage(TextUtil.format(
										"&8&lP&8rotecciones &b&l» &cERROR &7Ese nombre es demasiado extenso, escribe el nombre del usuario que al que añadirás a tus protecciones:"));
								return false;

							}
							
							if(e.getMessage().equals(e.getPlayer().getName())) {
								
								player.sendMessage(TextUtil.format(
										"&8&lP&8rotecciones &b&l» &cERROR &7Cuenta la leyenda que si te intentas añadir a tu misma protección podrías crear un bucle de proporciones incalculables!"));
								return true;
								
							}
							
							if(!Resolver.hasLastName(e.getMessage())) {
								
								player.sendMessage(TextUtil.format(
										"&8&lP&8rotecciones &b&l» &cERROR &7El usuario &6'" + e.getMessage() + "' &7no existe o nunca ha ingresado a Omniblock Network."));
								return true;
								
							}
							
							PlayerMembersWrapper wrapper = getMembers(player.getName(), false);
							String cacheNetworkID = Resolver.getNetworkIDByName(e.getMessage());
							
							if(wrapper.isMember(cacheNetworkID, true)) {
								
								player.sendMessage(TextUtil.format(
										"&8&lP&8rotecciones &b&l» &cERROR &7El usuario &6'" + e.getMessage() + "' &7ya es miembro de tus protecciones."));
								return true;
								
							}
							
							wrapper.addMember(cacheNetworkID, true);
							
							updateZones(wrapper.getPlayerNetworkID());
							updateSigns(wrapper.getPlayerNetworkID());
							
							player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
							
							player.sendMessage(TextUtil.format(
									"&8&lP&8rotecciones &b&l» &a¡Has añadido a &2'" + e.getMessage() + "' &aa tus protecciones correctamente!"));
							return true;
							
							
						} else {
							
							if(e.getRecipients().contains(player))
								e.getRecipients().remove(player);
							
						}

						return false;
						
					}

				};
				return;
				
			}
		
		};
		
		cacheBuilder.addItem(
				new ItemBuilder(Material.WOOL)
				.data(5)
				.amount(1)
				.name("&a&l+ &aAñadir Miembro")
				.lore("")
				.lore("&8- &7Al darle click a este icono")
				.lore("&7deberás escribir el nombre del miembro")
				.lore("&7que deseas añadir a tus protecciones")
				.lore("")
				.lore("&9&nRecuerda:&r &7Ten cuidado a quien añades")
				.lore("&7a tus protecciones, porque tendrá acceso")
				.lore("&7directo a todo lo que ya está protegido")
				.lore("&7e incluso todo lo que protejas a futuro.")
				.lore("&f¡Podrás borrar los miembros que has añadido")
				.lore("&fen cualquier momento!")
				.lore("")
				.build(), 49,
				addMemberAction);
		
		for(String memberName : wrapper.getMembers(true)) {
			
			if(!slotter.hasNext()) {
				
				paginator.addPage(cacheBuilder);
				slotter.reset();
				
				cacheBuilder = new InventoryBuilder(TextUtil.format("&aMis Miembros"), 6 * 9, false);
				
				cacheBuilder.addItem(
						new ItemBuilder(Material.WOOL)
						.data(5)
						.amount(1)
						.name("&a&l+ &aAñadir Miembro")
						.lore("")
						.lore("&8- &7Al darle click a este icono")
						.lore("&7deberás escribir el nombre del miembro")
						.lore("&7que deseas añadir a tus protecciones")
						.lore("")
						.lore("&9&nRecuerda:&r &7Ten cuidado a quien añades")
						.lore("&7a tus protecciones, porque tendrá acceso")
						.lore("&7directo a todo lo que ya está protegido")
						.lore("&7e incluso todo lo que protejas a futuro.")
						.lore("&f¡Podrás borrar los miembros que has añadido")
						.lore("&fen cualquier momento!")
						.lore("")
						.build(), 49,
						addMemberAction);
				
			}
			
			cacheBuilder.addItem(
					new ItemBuilder(Material.SKULL_ITEM)
						.data(3)
						.amount(1)
						.name(RankBase.getRank(memberName)
						.getCustomName(memberName, 'a'))
						.setSkullOwner(memberName)
						.lore("")
						.lore("&cRemover Miembro &8&l(Click derecho)")
						.lore("")
						.build(), slotter.next(),
						new net.omniblock.network.library.helpers.inventory.InventoryBuilder.Action() {

							@Override
							public void click(ClickType click, Player player) {
								
								if(click.isRightClick())
									if(wrapper.isMember(memberName, false)) {
										
										player.closeInventory();
										
										wrapper.removeMember(memberName, false);
										updateZones(wrapper.getPlayerNetworkID());
										
										player.sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &7Has eliminado a &c" + memberName + " &7de tus protecciones!"));
										return;
										
									}	
								
							}
						
						});
			continue;
			
		}
		
		if(!paginator.contains(cacheBuilder))
			paginator.addPage(cacheBuilder);
		
		paginator.openInventory(player);
		return;
		
	}
	
	public static void registerZone(ProtectedZone zone) {
		
		if(registeredZones.contains(zone))
			return;
		
		registeredZones.add(zone);
		return;
		
	}
	
	public static void removeZone(ProtectedZone zone) {
		
		if(!registeredZones.contains(zone))
			return;
		
		registeredZones.remove(zone);
		return;
		
	}
	
	public static PlayerMembersWrapper getMembers(String param, boolean isNetworkID) {
		
		for(PlayerMembersWrapper members : registeredMembers)
			if(members.isOwner(param, isNetworkID))
				return members;
		
		PlayerMembersWrapper wrapper = ProtectionBase.getMembers(param, isNetworkID);
		
		if(!registeredMembers.contains(wrapper))
			registeredMembers.add(wrapper);
			
		return wrapper;
		
	}
	
	public static ProtectedZone getProtectedZone(ProtectedRegion region) {
		
		for(ProtectedZone zone : registeredZones)
			if(zone.getRegion().getId().equals(region.getId()))
				return zone;
		
		return null;
		
	}
	
	public static void updateZones(String ownerNetworkID) {
		
		for(ProtectedZone zone : registeredZones)
			if(zone.isOwner(ownerNetworkID, true))
				zone.load();
		
		return;
		
	}
	
	public static void updateSigns(String ownerNetworkID) {
		
		// for(ProtectedZone zone : registeredZones)
			// if(zone.isOwner(ownerNetworkID, true))
				// zone.loadProtection();
		
		return;
		
	}
	
	public static class ProtectionListener implements Listener {
		
		public static List<Player> blacklist = Lists.newArrayList();
		
		@SuppressWarnings("deprecation")
		@EventHandler
		public void onPlace(BlockPlaceEvent e) {
			
			for(ProtectionZoneType type : ProtectionZoneType.values())
				if(type.getProtectionItem().getType().isBlock())
					if(type.getProtectionItem().isSimilar(e.getItemInHand()))
						if(e.getBlock().getWorld().getName().equalsIgnoreCase("world")) { // TODO CHANGE TO PvE
							
							String uniqueID = UUID.randomUUID().toString().substring(0, 10);
							
							Location pos1 = e.getBlock().getLocation().clone().add(type.getRadius(), type.getRadius(), type.getRadius());
							Location pos2 = e.getBlock().getLocation().clone().add(-type.getRadius(), -type.getRadius(), -type.getRadius());

							RegionManager manager = RegionsPlugin.getWGPlugin().getRegionManager(e.getBlock().getWorld());
							ProtectedCuboidRegion region = new ProtectedCuboidRegion(uniqueID, BukkitUtil.toVector(pos1.getBlock()), BukkitUtil.toVector(pos2.getBlock()));
							
							boolean canceled = false;
							
							ApplicableRegionSet regionSet = manager.getApplicableRegions(region);
							
							if(regionSet != null && regionSet.size() > 0)
								Checker : for(ProtectedRegion cacheRegion : regionSet.getRegions())
									if(!cacheRegion.getId().equalsIgnoreCase("__global__"))
										if(cacheRegion.isOwner(e.getPlayer().getName()) ||
												cacheRegion.isMember(e.getPlayer().getName())) {
											continue Checker;
										} else {
											
											ProtectedZone cacheZone = getProtectedZone(cacheRegion);
											
											if(cacheZone != null) 
												if(cacheZone.isOwner(e.getPlayer()))
													continue Checker;
												
											
											canceled = true;
											break Checker;
											
										}
							
							if(canceled) {
								
								e.setCancelled(true);
								
								e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &cERROR &7¡Tu protección hace contacto con la de otro jugador!"));
								return;
								
							}
							
							ProtectedZone protectedZone = new ProtectedZone(
									uniqueID,
									type,
									e.getBlockPlaced(),
									region,
									Resolver.getNetworkIDByName(e.getPlayer().getName()));
							
							manager.addRegion(region);
							
							protectedZone.load();
							registerZone(protectedZone);
							
							e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
							e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &a¡Has colocado tu &b'" + type.getName() + "'&a correctamente!"));
							return;
							
						} else {
							
							e.setCancelled(true);
							
							e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &cERROR &7¡Solo podrás usar las protecciones en el mundo PvE!"));
							return;
							
						}
			
			if(e.getBlock().getType() == Material.HOPPER) {
				
				Hopper hopper = (Hopper) e.getBlock().getState();
				
				Block block = 
						TileType.getTileType(e.getBlock().getRelative(BlockFace.UP).getState()) == null ?
								TileUtils.getBlockByHopper(hopper) :
								e.getBlock().getRelative(BlockFace.UP);
				
				//
				// Iterar todas las tiendas para buscar
				// si el cofre que es relativo al hopper
				// pertenece a alguna de ellas.
				//
				if(block != null)
					for(ProtectedTile protectedTile : registeredTiles) {
					
						if(protectedTile.getStructure().contains(block)){
							
							if(!Resolver.getLastNameByNetworkID(protectedTile.getProtectorID()).equals(e.getPlayer().getName())) {
								
								//
								// En caso de que el usuario no sea el dueño
								// de la tienda cancelar el evento y enviarle
								// un mensaje.
								//
								
								e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &cNo puedes colocar tolvas en protecciones que no sean tuyas!"));
								e.setBuild(false);
								e.setCancelled(true);
								return;
								
							} else return;
						}
							
					
					}
				
			}
			
			if(e.getBlock().getType() == Material.CHEST || e.getBlock().getType() == Material.TRAPPED_CHEST){
				
				Material material = e.getBlock().getType() == Material.CHEST ?
						Material.CHEST : Material.TRAPPED_CHEST;
				
				//Saber si el cofre que puse complementa uno doble
				
				Block checkChest = null;
				Location checkBlock =  e.getBlock().getLocation().add(-1, 0, 0);
				
				if(checkBlock.getBlock().getType().equals(material))
					checkChest = checkBlock.getBlock();

				checkBlock =  checkBlock.add(2, 0, 0);
				if(checkBlock.getBlock().getType().equals(material))
					checkChest = checkBlock.getBlock();

				checkBlock =  checkBlock.add(-1, 0, -1);
				if(checkBlock.getBlock().getType().equals(material))
					checkChest = checkBlock.getBlock();

				checkBlock =  checkBlock.add(0, 0, 2);
				if(checkBlock.getBlock().getType().equals(material))
					checkChest = checkBlock.getBlock();
				
				if(checkChest == null) return;
				
				
				//Si el cofre que complemento estaba protegido
				
				for(ProtectedTile protectedTile : registeredTiles)
					if(protectedTile.getStructure().contains(checkChest))
						if(!Resolver.getLastNameByNetworkID(protectedTile.getProtectorID()).equals(e.getPlayer().getName())){
							
							e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &cNo puedes colocar este cofre aquí."));
							e.setCancelled(true);
							return;
						
						}else{

							protectedTile.load();
							
							return;
						}
				
			}
			
			return;
			
		}
		
		@EventHandler
		public void onDestroy(BlockBreakEvent e) {
			
			//
			//POR CADA PROTECTEDTILE REGISTRADA
			//
			for(ProtectedTile protectedTile : registeredTiles){
				
				//SI EL BLOQUE QUE ROMPE ESTÁ PROTEGIDO
				if(protectedTile.getStructure().contains(e.getBlock())){
						e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &7Este bloque está protegido."));
						e.setCancelled(true);
						return;
					}
					
				//AL ROMPER UN CARTEL DE PROTECCION
				
				if(e.getBlock().equals(protectedTile.getProtectionSign().getBlock())){

					//si es admin o tiene permisos
					if(e.getPlayer().isOp() || e.getPlayer().hasPermission("protection.tile.adminbreak")){
							
						registeredTiles.remove(protectedTile);
						protectedTile.destroy();
						e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &7Has forzado la destrucción de la protección &c'" + protectedTile.getUniqueID() + "'&7 de &8" + Resolver.getLastNameByNetworkID(protectedTile.getProtectorID()) + "&7 correctamente!"));
						return;
						
					}
						
					//si si es su proteccion
					if(Resolver.getLastNameByNetworkID(protectedTile.getProtectorID()).equals(e.getPlayer().getName())){
						
						registeredTiles.remove(protectedTile);
						protectedTile.destroy();
						e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &7¡Has destruido tu proteccion!"));
						return;		
					}
						
					e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &7Ahh no hermano consiguete la tuya!"));
					e.setCancelled(true);
					return;
				}
			}
			
			
			
			for(ProtectionZoneType type : ProtectionZoneType.values())
				if(type.getProtectionItem().getType().isBlock())
					if(e.getBlock().getType() == type.getProtectionItem().getType())
						for(ProtectedZone zone : new ArrayList<ProtectedZone>(registeredZones))
							if(zone.protectionBlock.equals(e.getBlock())) {
								
								if(!zone.getPlayerNetworkID().equals(Resolver.getNetworkIDByName(e.getPlayer().getName()))) {
									
									e.setCancelled(true);
									
									e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &cERROR &7¡Esta protección no te pertenece!"));
									return;
									
								}
								
								zone.destroy();
								removeZone(zone);
								
								e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &6¡Has destruido tu &b'" + type.getName() + "'!"));
								return;
									
									
							}
			
			
			return;
			
		}
		
		@EventHandler
		public void onClick(PlayerInteractEvent e) {
			
			if(e.getClickedBlock() != null) {
				
				//
				// Con el fin de unicamente registrar
				// los eventos tipo click sobre un
				// bloque.
				//
				if(		e.getAction() == Action.RIGHT_CLICK_AIR ||
						e.getAction() == Action.LEFT_CLICK_AIR ||
						e.getAction() == Action.LEFT_CLICK_BLOCK ||
						e.getAction() == Action.PHYSICAL)
					return;
				
				//
				//Para si cliqueo un TileEntity protegido
				//Lo puse arriba para que no interfiera con la blacklist
				//
				if(TileType.getTileType(e.getClickedBlock().getState()) != null){
					for(ProtectedTile protectedTile : registeredTiles)
						if(protectedTile.getStructure().contains(e.getClickedBlock()))
							if(!(e.getPlayer().isOp() 
									|| Resolver.getLastNameByNetworkID(protectedTile.getProtectorID()).equals(e.getPlayer().getName()))){
								
								e.setCancelled(true);
								e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &cNo puedes interactuar con esto."));
								return;
								
							}
				}
				
				//
				// La blacklist es un sistema
				// para evitar que se repita muchas
				// veces el mismo evento ya que
				// PlayerInteractEvent se puede llamar
				// hasta 5 veces en 1 solo click.
				//
				if(blacklist.contains(e.getPlayer()))
					return;
				
				blacklist.add(e.getPlayer());
				
				new BukkitRunnable() {
					
					@Override
					public void run() {
						
						if(blacklist.contains(e.getPlayer()))
							blacklist.remove(e.getPlayer());
						
					}
					
				}.runTaskLater(ProtectionPlugin.getInstance(), 5L);
				
				//
				// Para detectar unicamente el click
				// sobre piedras de protección y abrir
				// la GUI de protecciones.
				//
				if(e.getClickedBlock().getType().isBlock())
					if(e.getClickedBlock().getType().isSolid())
						for(ProtectionZoneType type : ProtectionZoneType.values())
							if(e.getClickedBlock().getType() == type.getProtectionItem().getType()) {
								
								String playerNetworkID = Resolver.getNetworkIDByName(e.getPlayer().getName());
								
								for(ProtectedZone zone : registeredZones)
									if(zone.getBlock().equals(e.getClickedBlock()))
										if(zone.getPlayerNetworkID().equals(playerNetworkID)) {
											
											e.setCancelled(true);
											openGUI(e.getPlayer());
											
											e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 2, 10);
											return;
											
										}
								
								continue;
								
							}
				
				
				//
				// Para detectar unicamente el click
				// sobre carteles.
				//
				if(e.getClickedBlock().getState() instanceof Sign) {
					
					Sign sign = (Sign) e.getClickedBlock().getState();
					
					if(sign.getLine(0).equals(LineRegex.PROTECTION_PREFIX_TILE_UP)){
						
						//Añadir abrir gui para añadir miembros
						
						return;
					}
					
					//
					// Para detectar si el cartel mantiene
					// el formato de un cartel de compra de
					// protecciones.
					//
					if(!(sign.getLines().length >= 4)) //Gracias toxic por pensar en mi codigo y no privatizarlo con un if
						return;							// que me da flojera cambiar así que escribiré arriba c: xD
					
					//
					// Iterar todas las protecciones disponibles
					// para saber en base al cartel cual se
					// esta comprando.
					//
					if(ConfigType.PROTECTION_DATA.getConfig().isSet("buysigns"))
						if(sign.getLine(0).equalsIgnoreCase(LineRegex.PROTECTION_PREFIX_SHOP_UP)) {
							
							String protectionName = ChatColor.stripColor(sign.getLine(2));
							
							for(ProtectionZoneType type : ProtectionZoneType.values())
								if(type.getShortname().equalsIgnoreCase(protectionName)) {
									
									if(SurvivalBankBase.getMoney(e.getPlayer()) < type.getPrice()) {
										
										e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &7No tienes suficiente dinero para comprar esta piedra de protección."));
										return;
										
									}
									
									if(!InventoryUtils.hasSpaceForStack(e.getPlayer().getInventory(), type.getProtectionItem().clone())) {
										
										e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &7No tienes suficiente espacio en tu inventario para guardar esta piedra de protección."));
										return;
										
									}
									
									SurvivalBankBase.removeMoney(e.getPlayer(), type.getPrice());
									
									e.getPlayer().getInventory().addItem(new ItemBuilder(type.getProtectionItem().clone()).amount(1).build());
									e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
									
									e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &7Has comprado una &b'" + type.getName() + "' &7al precio de &a$" + type.getPrice() + "!"));
									return;
									
								}
									
							return;
							
						}
					
				}
				
			}
			
		}
		
		@EventHandler
		public void onCreate(SignChangeEvent e) {
			
			
			if(e.getLine(0).equalsIgnoreCase(LineRegex.CREATE_PROTECTION_TILE_UP)){
				
				Sign protectionSign = (Sign) e.getBlock().getState();
				
					org.bukkit.material.Sign signMaterial = (org.bukkit.material.Sign) protectionSign.getData();
					BlockFace attachedFace = signMaterial.getAttachedFace();
				
				BlockState tileEntity= protectionSign.getBlock().getRelative(attachedFace).getState();
				
				for(ProtectedTile protectedTile : registeredTiles){
					
					if(protectedTile.getStructure().contains(tileEntity.getBlock())){
						
						e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &7No puedes proteger algo que ya esta protegido :)."));
						
						e.setCancelled(true);
						e.getBlock().breakNaturally();
						return;
						
					}
				}
				
				TileType tileType = TileType.getTileType(tileEntity);
				if(tileType == null){
					e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &7Debes colocar el cartel sobre algo que se pueda proteger!"));
					e.setCancelled(true);
					e.getBlock().breakNaturally();
					return;
				}

				String uniqueID = UUID.randomUUID().toString().substring(0, 10);
				String protectorID = Resolver.getNetworkIDByName(e.getPlayer().getName());
				
				ProtectedTile tile = new ProtectedTile(
						uniqueID,
						protectorID,
						protectionSign,
						tileType,
						tileEntity);
				
				tile.load();
				registeredTiles.add(tile);
				e.setLine(0, LineRegex.PROTECTION_PREFIX_TILE_UP);
				e.setLine(2, TextUtil.format("&c&l"+e.getPlayer().getName()));
				e.getPlayer().sendMessage(TextUtil.format("&lPROTECCIÓN &b&l» &7Protegido correctamente!"));
				return;
			}
			
					
			if(e.getLines().length < 2)
				return;
			
			if(!e.getPlayer().hasPermission("protection.shop.admin"))
				return;
			
			if(e.getLine(0).equalsIgnoreCase(LineRegex.CREATE_PROTECTION_SHOP_UP)){
				
				for(ProtectionZoneType type : ProtectionZoneType.values())
					if(e.getLine(1).equalsIgnoreCase("[" + type.getShortname() + "]")) {
						
						if(ConfigType.PROTECTION_DATA.getConfig().isSet("buysigns"))
							if(!ConfigType.PROTECTION_DATA.getConfig().getList("buysigns").contains(LocationUtils.serializeLocation(e.getBlock().getLocation()))) {
								
								List<String> list = ConfigType.PROTECTION_DATA.getConfig().getStringList("buysigns");
								list.add(LocationUtils.serializeLocation(e.getBlock().getLocation()));
								
								ConfigType.PROTECTION_DATA.getConfig().set("buysigns", list);
								ConfigType.PROTECTION_DATA.getConfigObject().save();
								
							}
					
						if(!ConfigType.PROTECTION_DATA.getConfig().isSet("buysigns")) {
							
							List<String> list = Lists.newArrayList();
							list.add(LocationUtils.serializeLocation(e.getBlock().getLocation()));
							
							ConfigType.PROTECTION_DATA.getConfig().set("buysigns", list);
							ConfigType.PROTECTION_DATA.getConfigObject().save();
							
						}
						
						e.setLine(0, LineRegex.PROTECTION_PREFIX_SHOP_UP);
						e.setLine(1, TextUtil.format("&l" + type.getRadius() + "x" + type.getRadius() + "x" + type.getRadius()));
						e.setLine(2, TextUtil.format("&n" + type.getShortname()));
						e.setLine(3, TextUtil.format("&l$&r " + type.getPrice()));
						
						e.getPlayer().sendMessage(TextUtil.format("&8&lP&8rotecciones &b&l» &7Has creado una tienda para la protección &6'" + type.getShortname() + "'."));
						return;
						
					}
				
			}
			
		}
		
		
		@EventHandler
		public void onPistonExtend(BlockPistonExtendEvent e){
			
			//
			//Revisar cada bloque modificado por el piston
			//
			for(Block block : e.getBlocks()){
				for(ProtectedTile protectedTile : registeredTiles)
					if(protectedTile.getStructure().contains(block)){
						e.setCancelled(true);
						return;
					}
			}
				
			
			
			return;
		}
		
		
		@EventHandler
		public void onPistonRetract(BlockPistonRetractEvent e){
			
			//
			//Revisar cada bloque modificado por el piston
			//
			for(Block block : e.getBlocks())
				for(ProtectedTile protectedTile : registeredTiles)
					if(protectedTile.getStructure().contains(block)){
						e.setCancelled(true);
						return;
					}
			
			return;
		}
		
	}
	
}
