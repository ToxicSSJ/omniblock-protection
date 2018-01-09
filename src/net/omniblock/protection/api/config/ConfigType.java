package net.omniblock.protection.api.config;

import org.bukkit.configuration.file.FileConfiguration;

import net.omniblock.protection.ProtectionPlugin;
import net.omniblock.network.library.addons.configaddon.object.Config;

public enum ConfigType {

	PROTECTION_DATA(new Config(ProtectionPlugin.getInstance(), "data/protectiondata.yml")),

	;

	private Config config;

	ConfigType(Config config) {
		this.config = config;
	}

	public Config getConfigObject() {
		return config;
	}

	public FileConfiguration getConfig() {
		return config.getConfigFile();
	}

	public void setConfig(Config config) {
		this.config = config;
	}

}
