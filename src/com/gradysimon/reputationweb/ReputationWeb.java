package com.gradysimon.reputationweb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ReputationWeb extends JavaPlugin {

	private Logger log;
	private ReputationCommandExecutor reputationCommandExecutor;
	protected FileConfiguration config;

	public void onEnable() {

		// -- Start logging --
		log = Logger.getLogger("Minecraft");
		log.info("Reputation Web enabled."); // TODO Confirm log message

		// -- Load configuration --
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			// If it doesn't exist, we need to generate it for next time.
			writeDefaultConfigFile();
		}
		config = getConfig();

		// Instantiate ReputationGraph?

		// Register command handler
		reputationCommandExecutor = new ReputationCommandExecutor(this);
		getCommand("reputation").setExecutor(reputationCommandExecutor);
		getCommand("rep").setExecutor(reputationCommandExecutor);

		// Load reputation data from database

	}

	private void writeDefaultConfigFile() {
		// configFileInData is the config.yml file that is in the plugin's data
		// folder
		File configFileInData = new File(getDataFolder(), "config.yml");
		// embeddedConfigFile is a default config.yml that is inside the .jar
		InputStream embeddedConfigFile = getResource("config.yml");
		OutputStream configFileOutput;
		try {
			configFileOutput = new FileOutputStream(configFileInData);
		} catch (FileNotFoundException e) {
			// TODO Add logging message here
			e.printStackTrace();
			return;
		}
		byte[] buffer = new byte[1024]; // input buffer
		int length; // length actually read
		try {
			while ((length = embeddedConfigFile.read(buffer)) < 0) {
				try {
					configFileOutput.write(buffer, 0, length);
				} catch (IOException e) {
					// TODO Add logging message here
					e.printStackTrace();
					return;
				}
			}
		} catch (IOException e) {
			// TODO Add logging message here
			e.printStackTrace();
			return;
		}
		try {
			embeddedConfigFile.close();
		} catch (IOException e) {
			// TODO Add logging message here
			e.printStackTrace();
			return;
		}
		try {
			configFileOutput.close();
		} catch (IOException e) {
			// TODO Add logging message here
			e.printStackTrace();
			return;
		}
	}

	public void onDisable() {
		log.info("Reputation Web disabled."); // TODO
	}
}
