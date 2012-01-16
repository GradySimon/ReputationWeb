package com.gradysimon.reputationweb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query;
import javax.persistence.PersistenceException;

public class ReputationWeb extends JavaPlugin {

	private Logger log;
	private ReputationCommandExecutor reputationCommandExecutor;
	protected FileConfiguration config;
	private Server server;
	private ReputationGraph reputationGraph;
	private PluginDescriptionFile description;

	private EbeanServer database;

	public void onEnable() {
		loadPluginEnvironment();
		startLogging();
		loadConfiguration();
		instantiateReputationGraph();
		initializeDatabase();
		loadReputationData();
		initializeCommandHandler();
	}

	public void onDisable() {
		log.info(formatLog(description.getFullName() + ", version "
				+ description.getVersion() + " disabled."));
	}

	// Overridden to return the correct list of persistence classes
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> persistentClassList = new ArrayList<Class<?>>();
		persistentClassList.add(Trust.class);
		return persistentClassList;
	}

	EbeanServer getPluginDatabase() {
		return getDatabase();
	}

	private void loadPluginEnvironment() {
		this.server = getServer();
		this.description = getDescription();
	}

	private void startLogging() {
		log = Logger.getLogger("Minecraft");
		log.info(description.getFullName() + " enabled. Author: "
				+ description.getAuthors().get(0) + ".");
	}

	private String formatLog(String message) {
		return description.getName() + ": " + message;
	}

	private void loadConfiguration() {
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			// If it doesn't exist, we need to generate it for next time.
			log.info(formatLog("No " + description.getName()
					+ " config.yml found."));
			writeDefaultConfigFile();
		}
		config = getConfig();
	}

	private void writeDefaultConfigFile() {
		// configFileInData is the config.yml file that is in the plugin's data
		// folder inside the plugin directory (not in .jar)
		log.info(formatLog("Writing default config.yml to plugins/" + description.getName() + "/"));
		File configFileInData = new File(getDataFolder(), "config.yml");
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}
		try {
			if (!configFileInData.exists()) {
				configFileInData.createNewFile();
			}
			// embeddedConfigFile is a default config.yml that is inside the
			// .jar
			InputStream embeddedConfigFile = getResource("config.yml");
			OutputStream configFileOutput;
			try {
				configFileOutput = new FileOutputStream(configFileInData);
			} catch (FileNotFoundException e) {
				log.warning(formatLog("Unable to write config file to resource directory."));
				log.info(formatLog("Forced to rely on default configuration."));
				e.printStackTrace();
				return;
			}
			byte[] buffer = new byte[1024]; // input buffer
			int length; // length actually read
			while ((length = embeddedConfigFile.read(buffer)) > 0) {
				configFileOutput.write(buffer, 0, length);
			}
			embeddedConfigFile.close();
			configFileOutput.close();
		} catch (IOException e) {
			log.warning(formatLog("Unable to write config file to resource directory."));
			log.info(formatLog("Forced to rely on default configuration."));
			e.printStackTrace();
			return;
		}
	}

	private void instantiateReputationGraph() {
		double flowFactor = config.getDouble("parameters.flow_factor");
		int maxChainLength = config.getInt("parameters.max_chain_length");
		reputationGraph = new ReputationGraph(flowFactor, maxChainLength);
	}

	private void initializeDatabase() {
		handleEbeanDotProperties();
		database = getDatabase();
		try {
			database.find(Trust.class).findRowCount();
		} catch (PersistenceException ex) {
			log.info(formatLog("No existing reputation data found."));
			log.info(formatLog("Initializing database."));
			installDDL();
		}
	}

	private void handleEbeanDotProperties() {
		File ebeanDotProperties = new File("ebean.properties");
		try {
			if (ebeanDotProperties.createNewFile()) {
				log.info(formatLog("Creating ebean.properties file. This file will be empty. This is normal."));
			}
		} catch (IOException e) {
			log.warning(formatLog("Unable to write ebean.properties file."));
			e.printStackTrace();
		}

	}

	private void loadReputationData() {
		populateReputationGraph(getTrustsFromDatabase());
	}

	private void populateReputationGraph(List<Trust> trusts) {
		for (Trust trust : trusts) {
			String trusterName = trust.getTrusterName();
			String trusteeName = trust.getTrusteeName();
			OfflinePlayer truster = server.getOfflinePlayer(trusterName);
			OfflinePlayer trustee = server.getOfflinePlayer(trusteeName);
			reputationGraph.addTrustRelation(truster, trustee);
		}
	}

	// Should execute query equivalent to:
	// "SELECT nameOfTruster,nameOfTrustee FROM rw_trust"
	private List<Trust> getTrustsFromDatabase() {
		Query<Trust> trustsQuery = database.find(Trust.class);
		trustsQuery = trustsQuery.select("trusterName,trusteeName");
		List<Trust> allTrusts = trustsQuery.findList();
		return allTrusts;
	}

	private void initializeCommandHandler() {
		reputationCommandExecutor = new ReputationCommandExecutor(this,
				reputationGraph, server);
		getCommand("reputation").setExecutor(reputationCommandExecutor);
		getCommand("rep").setExecutor(reputationCommandExecutor);
		getCommand("trust").setExecutor(reputationCommandExecutor);
		getCommand("untrust").setExecutor(reputationCommandExecutor);
	}
}