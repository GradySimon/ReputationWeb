package com.gradysimon.reputationweb;

import java.util.logging.Logger;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ReputationWeb extends JavaPlugin {
	
	Logger log = Logger.getLogger("Minecraft");
	PluginManager pm = this.getServer().getPluginManager();
	
	public void onEnable() {
		log.info("Reputation Web enabled."); //TODO
	}
	
	public void onDisable() {
		log.info("Reputation Web disabled."); //TODO
	}
}
