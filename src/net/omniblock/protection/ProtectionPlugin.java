package net.omniblock.protection;

import org.bukkit.plugin.java.JavaPlugin;

import net.omniblock.network.handlers.Handlers;
import net.omniblock.network.handlers.network.NetworkManager;
import net.omniblock.packets.object.external.ServerType;
import net.omniblock.protection.api.ProtectionManager;

public class ProtectionPlugin extends JavaPlugin {

	private static ProtectionPlugin instance;
	
	@Override
	public void onEnable() {

		instance = this;

		if (NetworkManager.getServertype() != ServerType.SURVIVAL) {

			Handlers.LOGGER.sendModuleInfo("&7Se ha registrado Protection v" + this.getDescription().getVersion() + "!");
			Handlers.LOGGER.sendModuleMessage("Survival", "Se ha inicializado Protection en modo API!");
			return;

		}

		ProtectionManager.setup();
		
		Handlers.LOGGER.sendModuleInfo("&7Se ha Protection Protection v" + this.getDescription().getVersion() + "!");
		Handlers.LOGGER.sendModuleMessage("Survival", "Se ha inicializado Protection correctamente!");
		
	}

	public static ProtectionPlugin getInstance() {
		return instance;
	}
	
}
