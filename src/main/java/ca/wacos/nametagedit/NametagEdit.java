package ca.wacos.nametagedit;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import ca.wacos.nametagedit.core.NametagHandler;
import ca.wacos.nametagedit.core.NametagManager;
import ca.wacos.nametagedit.events.AsyncPlayerChat;
import ca.wacos.nametagedit.events.PlayerJoin;
import ca.wacos.nametagedit.tasks.SQLDataTask;
import ca.wacos.nametagedit.tasks.TableCreatorTask;
import ca.wacos.nametagedit.utils.FileManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * This is the main class for the NametagEdit plugin.
 * 
 * @author sgtcaze
 * 
 */
public class NametagEdit extends JavaPlugin {

    private static NametagEdit instance;

    private FileManager fileUtils;
    private NametagHandler nteHandler;
    private NametagManager nametagManager;

    private HikariDataSource connectionPool;

    @Override
    public void onEnable() {
        instance = this;

        fileUtils = new FileManager();
        nteHandler = new NametagHandler();
        nametagManager = new NametagManager();
        
        getCommand("ne").setExecutor(new NametagCommand());

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerJoin(), this);

        FileConfiguration config = getConfig();
        
        if (config.getBoolean("Chat.Enabled")) {
            pm.registerEvents(new AsyncPlayerChat(), this);
        }
        
        if (config.getBoolean("MetricsEnabled")) {
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException e) {
                // Failed to submit the stats :-(
            }
        }

        saveDefaultConfig();
        fileUtils.loadFiles();

        NametagManager.load();

        if (nteHandler.usingDatabase()) {
            setupHikari();
            new TableCreatorTask().runTask(this);
            new SQLDataTask().runTask(this);
        } else {
            nteHandler.loadFromFile(fileUtils.getPlayersFile(), fileUtils.getGroupsFile());
        }

        nteHandler.applyTags();
    }

    @Override
    public void onDisable() {
        NametagManager.reset();

        nteHandler.saveFileData(fileUtils.getPlayersFile(), fileUtils.getGroupsFile());

        if (connectionPool != null) {
            connectionPool.shutdown();
        }
    }

    public static NametagEdit getInstance() {
        return instance;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public NametagHandler getNTEHandler() {
        return nteHandler;
    }

    public FileManager getFileUtils() {
        return fileUtils;
    }
    
    public HikariDataSource getConnectionPool() {
        return connectionPool;
    }
    
    private void setupHikari() {
        FileConfiguration config = getConfig();

        String address = config.getString("MySQL.Hostname");
        String name = config.getString("MySQL.Database");
        String username = config.getString("MySQL.Username");
        String password = config.getString("MySQL.Password");

        connectionPool = new HikariDataSource();
        connectionPool.setMaximumPoolSize(5);
        connectionPool.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        connectionPool.addDataSourceProperty("serverName", address);
        connectionPool.addDataSourceProperty("port", "3306");
        connectionPool.addDataSourceProperty("databaseName", name);
        connectionPool.addDataSourceProperty("user", username);
        connectionPool.addDataSourceProperty("password", password);   
    }
}