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

	private EbeanServer database;

	public void onEnable() {
		server = this.getServer();
		startLogging();
		loadConfiguration();
		instantiateReputationGraph();
		initializeDatabase();
		loadReputationData();
		initializeCommandHandler();
	}

	private void startLogging() {
		log = Logger.getLogger("Minecraft");
		log.info("Reputation Web enabled."); // TODO Confirm log message
	}

	private void loadConfiguration() {
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			// If it doesn't exist, we need to generate it for next time.
			writeDefaultConfigFile();
		}
		config = getConfig();
	}

	private void writeDefaultConfigFile() {
		// TODO: message here to indicate default config is being written.
		// configFileInData is the config.yml file that is in the plugin's data
		// folder inside the plugin directory (not in .jar)
		File configFileInData = new File(getDataFolder(), "config.yml");
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}
		if (!configFileInData.exists()) {
			try {
				configFileInData.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
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
			while ((length = embeddedConfigFile.read(buffer)) > 0) {
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
			// TODO: installing DB message
			installDDL();
			database = getDatabase(); // not sure if necessary
		}
	}

	private void handleEbeanDotProperties() {
		File ebeanDotProperties = new File("ebean.properties");
		try {
			if (ebeanDotProperties.createNewFile()) {
				// TODO: Logging message that ebean.properties was created by
				// this plugin.
			}
		} catch (IOException e) {
			// TODO: message that ebean.properties could not be created and to
			// ignore the warning message. (? or not. Decide).
			e.printStackTrace();
		}

	}

	// Overridden to return the correct list of persistence classes
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> persistentClassList = new ArrayList<Class<?>>();
		persistentClassList.add(Trust.class);
		return persistentClassList;
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

	EbeanServer getPluginDatabase() {
		return getDatabase();
	}

	private void initializeCommandHandler() {
		reputationCommandExecutor = new ReputationCommandExecutor(this,
				reputationGraph, server);
		getCommand("reputation").setExecutor(reputationCommandExecutor);
		getCommand("rep").setExecutor(reputationCommandExecutor);
		getCommand("trust").setExecutor(reputationCommandExecutor);
		getCommand("untrust").setExecutor(reputationCommandExecutor);
	}

	public void onDisable() {
		log.info("Reputation Web disabled."); // TODO
	}
}