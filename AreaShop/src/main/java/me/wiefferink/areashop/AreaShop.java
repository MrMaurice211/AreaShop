package me.wiefferink.areashop;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import me.wiefferink.areashop.handlers.BukkitHandler1_13;
import me.wiefferink.areashop.handlers.WorldEditHandler7;
import me.wiefferink.areashop.handlers.WorldGuardHandler7;
import me.wiefferink.areashop.interfaces.AreaShopInterface;
import me.wiefferink.areashop.interfaces.BukkitInterface;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.listeners.PlayerLoginLogoutListener;
import me.wiefferink.areashop.managers.*;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.bukkitdo.Do;
import me.wiefferink.interactivemessenger.processing.Message;
import me.wiefferink.interactivemessenger.source.LanguageManager;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main class for the AreaShop plugin.
 * Contains methods to get parts of the plugins functionality and definitions for constants.
 */
public final class AreaShop extends JavaPlugin implements AreaShopInterface {
    // Statically available instance
    private static AreaShop instance = null;

    // General variables
    private WorldGuardPlugin worldGuard = null;
    private WorldGuardInterface worldGuardInterface = null;
    private WorldEditPlugin worldEdit = null;
    private WorldEditInterface worldEditInterface = null;
    private BukkitInterface bukkitInterface = null;
    private FileManager fileManager = null;
    private LanguageManager languageManager = null;
    private CommandManager commandManager = null;
    private SignLinkerManager signLinkerManager = null;
    private FeatureManager featureManager = null;
    private Set<Manager> managers = null;
    private boolean debug = false;
    private List<String> chatprefix = null;
    private boolean ready = false;

    // Folders and file names
    public static final String languageFolder = "lang";
    public static final String schematicFolder = "schem";
    public static final String regionsFolder = "regions";
    public static final String groupsFile = "groups.yml";
    public static final String defaultFile = "default.yml";
    public static final String configFile = "config.yml";
    public static final String configFileHidden = "hiddenConfig.yml";
    public static final String versionFile = "versions";

    // Euro tag for in the config
    public static final String currencyEuro = "%euro%";

    // Constants for handling file versions
    public static final String versionFiles = "files";
    public static final int versionFilesCurrent = 3;

    // Keys for replacing parts of flags, commands, strings
    public static final String tagPlayerName = "player";
    public static final String tagPlayerUUID = "uuid";
    public static final String tagWorldName = "world";
    public static final String tagRegionName = "region";
    public static final String tagRegionType = "type";
    public static final String tagPrice = "price";
    public static final String tagRawPrice = "rawprice";
    public static final String tagDuration = "duration";
    public static final String tagRentedUntil = "until";
    public static final String tagRentedUntilShort = "untilshort";
    public static final String tagWidth = "width"; // x-axis
    public static final String tagHeight = "height"; // y-axis
    public static final String tagDepth = "depth"; // z-axis
    public static final String tagVolume = "volume"; // Number of blocks in the region (accounting for polygon regions)
    public static final String tagTimeLeft = "timeleft";
    public static final String tagClicker = "clicker";
    public static final String tagResellPrice = "resellprice";
    public static final String tagRawResellPrice = "rawresellprice";
    public static final String tagFriends = "friends";
    public static final String tagFriendsUUID = "friendsuuid";
    public static final String tagMoneyBackPercentage = "moneybackpercent";
    public static final String tagMoneyBackAmount = "moneyback";
    public static final String tagRawMoneyBackAmount = "rawmoneyback";
    public static final String tagTimesExtended = "timesExtended";
    public static final String tagMaxExtends = "maxextends";
    public static final String tagExtendsLeft = "extendsleft";
    public static final String tagMaxRentTime = "maxrenttime";
    public static final String tagMaxInactiveTime = "inactivetime";
    public static final String tagLandlord = "landlord";
    public static final String tagLandlordUUID = "landlorduuid";
    public static final String tagDateTime = "datetime";
    public static final String tagDateTimeShort = "datetimeshort";
    public static final String tagYear = "year";
    public static final String tagMonth = "month";
    public static final String tagDay = "day";
    public static final String tagHour = "hour";
    public static final String tagMinute = "minute";
    public static final String tagSecond = "second";
    public static final String tagMillisecond = "millisecond";
    public static final String tagEpoch = "epoch";
    public static final String tagTeleportX = "tpx";
    public static final String tagTeleportY = "tpy";
    public static final String tagTeleportZ = "tpz";
    public static final String tagTeleportBlockX = "tpblockx";
    public static final String tagTeleportBlockY = "tpblocky";
    public static final String tagTeleportBlockZ = "tpblockz";
    public static final String tagTeleportPitch = "tppitch";
    public static final String tagTeleportYaw = "tpyaw";
    public static final String tagTeleportPitchRound = "tppitchround";
    public static final String tagTeleportYawRound = "tpyawround";
    public static final String tagTeleportWorld = "tpworld";

    public static AreaShop getInstance() {
        return AreaShop.instance;
    }

    /**
     * Called on start or reload of the server.
     */
    @Override
    public void onEnable() {
        AreaShop.instance = this;
        Do.init(this);
        managers = new HashSet<>();

        worldEdit = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");

        // Load interfaces
        worldEditInterface = new WorldEditHandler7(this);
        worldGuardInterface = new WorldGuardHandler7(this);
        bukkitInterface = new BukkitHandler1_13(this);

        // Load all data from files and check versions
        fileManager = new FileManager();
        managers.add(fileManager);
        boolean loadFilesResult = fileManager.loadFiles(false);
        boolean error = !loadFilesResult;

        setupLanguageManager();

        if (error) {
            error("The plugin has not started, fix the errors listed above");
        } else {
            featureManager = new FeatureManager();
            managers.add(featureManager);

            // Register the event listeners
            getServer().getPluginManager().registerEvents(new PlayerLoginLogoutListener(this), this);

            setupTasks();

            // Startup the CommandManager (registers itself for the command)
            commandManager = new CommandManager();
            managers.add(commandManager);

            // Create a signLinkerManager
            signLinkerManager = new SignLinkerManager();
            managers.add(signLinkerManager);

            // Register dynamic permission (things declared in config)
            registerDynamicPermissions();
        }
    }

    /**
     * Called on shutdown or reload of the server.
     */
    @Override
    public void onDisable() {

        Bukkit.getServer().getScheduler().cancelTasks(this);

        // Cleanup managers
        for (Manager manager : managers) {
            manager.shutdown();
        }
        managers = null;
        fileManager = null;
        languageManager = null;
        commandManager = null;
        signLinkerManager = null;
        featureManager = null;

        // Cleanup plugins
        worldGuard = null;
        worldGuardInterface = null;
        worldEdit = null;
        worldEditInterface = null;

        // Cleanup other stuff
        chatprefix = null;
        debug = false;
        ready = false;

        HandlerList.unregisterAll(this);
    }

    /**
     * Indicates if the plugin is ready to be used.
     *
     * @return true if the plugin is ready, false otherwise
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Set if the plugin is ready to be used or not (not to be used from another plugin!).
     *
     * @param ready Indicate if the plugin is ready to be used
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    /**
     * Set if the plugin should output debug messages (loaded from config normally).
     *
     * @param debug Indicates if the plugin should output debug messages or not
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Setup a new LanguageManager.
     */
    private void setupLanguageManager() {
        languageManager = new LanguageManager(
                this,
                languageFolder,
                getConfig().getString("language"),
                "EN",
                chatprefix
        );
    }

    /**
     * Set the chatprefix to use in the chat (loaded from config normally).
     *
     * @param chatprefix The string to use in front of chat messages (supports formatting codes)
     */
    public void setChatprefix(List<String> chatprefix) {
        this.chatprefix = chatprefix;
    }

    /**
     * Function to get the WorldGuard plugin.
     *
     * @return WorldGuardPlugin
     */
    @Override
    public WorldGuardPlugin getWorldGuard() {
        return worldGuard;
    }

    /**
     * Function to get WorldGuardInterface for version dependent things.
     *
     * @return WorldGuardInterface
     */
    public WorldGuardInterface getWorldGuardHandler() {
        return this.worldGuardInterface;
    }

    /**
     * Get the RegionManager.
     *
     * @param world World to get the RegionManager for
     * @return RegionManager for the given world, if there is one, otherwise null
     */
    public RegionManager getRegionManager(World world) {
        return this.worldGuardInterface.getRegionManager(world);
    }

    /**
     * Function to get the WorldEdit plugin.
     *
     * @return WorldEditPlugin
     */
    @Override
    public WorldEditPlugin getWorldEdit() {
        return worldEdit;
    }

    /**
     * Function to get WorldGuardInterface for version dependent things.
     *
     * @return WorldGuardInterface
     */
    public WorldEditInterface getWorldEditHandler() {
        return this.worldEditInterface;
    }

    /**
     * Function to get the LanguageManager.
     *
     * @return the LanguageManager
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * Get the BukkitHandler, for sign interactions.
     *
     * @return BukkitHandler
     */
    public BukkitInterface getBukkitHandler() {
        return this.bukkitInterface;
    }

    /**
     * Function to get the CommandManager.
     *
     * @return the CommandManager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Get the SignLinkerManager.
     * Handles sign linking mode.
     *
     * @return The SignLinkerManager
     */
    public SignLinkerManager getSignlinkerManager() {
        return signLinkerManager;
    }

    /**
     * Get the FeatureManager.
     * Manages region specific features.
     *
     * @return The FeatureManager
     */
    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    /**
     * Function to get the Vault plugin.
     *
     * @return Economy
     */
    public Economy getEconomy() {
        RegisteredServiceProvider<Economy> economy = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economy == null) {
            error("There is no economy provider to support Vault, make sure you installed an economy plugin");
            return null;
        } else {
            economy.getProvider();
        }
        return economy.getProvider();
    }

    /**
     * Get the Vault permissions provider.
     *
     * @return Vault permissions provider
     */
    public net.milkbowl.vault.permission.Permission getPermissionProvider() {
        RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider == null) {
            return null;
        } else {
            permissionProvider.getProvider();
        }
        return permissionProvider.getProvider();
    }

    /**
     * Check for a permission of a (possibly offline) player.
     *
     * @param offlinePlayer OfflinePlayer to check
     * @param permission    Permission to check
     * @return true if the player has the permission, false if the player does not have permission or, is offline and there is not Vault-compatible permission plugin
     */
    public boolean hasPermission(OfflinePlayer offlinePlayer, String permission) {
        // Online, return through Bukkit
        if (offlinePlayer.getPlayer() != null) {
            return offlinePlayer.getPlayer().hasPermission(permission);
        }

        // Resolve while offline if possible
        net.milkbowl.vault.permission.Permission permissionProvider = getPermissionProvider();
        if (permissionProvider != null) {
            // TODO: Should we provide a world here?
            return permissionProvider.playerHas(null, offlinePlayer, permission);
        }

        // Player offline and no offline permission provider available, safely say that there is no permission
        return false;
    }

    /**
     * Method to get the FileManager (loads/save regions and can be used to get regions).
     *
     * @return The fileManager
     */
    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * Register dynamic permissions controlled by config settings.
     */
    private void registerDynamicPermissions() {
        // Register limit groups of amount of regions a player can have
        ConfigurationSection section = getConfig().getConfigurationSection("limitGroups");
        if (section == null) {
            return;
        }
        for (String group : section.getKeys(false)) {
            if (!"default".equals(group)) {
                Permission perm = new Permission("areashop.limits." + group);
                try {
                    Bukkit.getPluginManager().addPermission(perm);
                } catch (IllegalArgumentException e) {
                    warn("Could not add the following permission to be used as limit: " + perm.getName());
                }
            }
        }
        Bukkit.getPluginManager().recalculatePermissionDefaults(Bukkit.getPluginManager().getPermission("playerwarps.limits"));
    }

    /**
     * Register all required tasks.
     */
    private void setupTasks() {
        // Rent expiration timer
        long expirationCheck = Utils.millisToTicks(Utils.getDurationFromSecondsOrString("expiration.delay"));
        final AreaShop finalPlugin = this;
        if (expirationCheck > 0) {
            Do.syncTimer(expirationCheck, () -> {
                if (isReady()) {
                    finalPlugin.getFileManager().checkRents();
                    AreaShop.debugTask("Checking rent expirations...");
                } else {
                    AreaShop.debugTask("Skipped checking rent expirations, plugin not ready");
                }
            });
        }

        // Inactive unrenting/selling timer
        long inactiveCheck = Utils.millisToTicks(Utils.getDurationFromMinutesOrString("inactive.delay"));
        if (inactiveCheck > 0) {
            Do.syncTimer(inactiveCheck, () -> {
                if (isReady()) {
                    finalPlugin.getFileManager().checkForInactiveRegions();
                    AreaShop.debugTask("Checking for regions with players that are inactive too long...");
                } else {
                    AreaShop.debugTask("Skipped checking for regions of inactive players, plugin not ready");
                }
            });
        }

        // Periodic updating of signs for timeleft tags
        long periodicUpdate = Utils.millisToTicks(Utils.getDurationFromSecondsOrString("signs.delay"));
        if (periodicUpdate > 0) {
            Do.syncTimer(periodicUpdate, () -> {
                if (isReady()) {
                    finalPlugin.getFileManager().performPeriodicSignUpdate();
                    AreaShop.debugTask("Performing periodic sign update...");
                } else {
                    AreaShop.debugTask("Skipped performing periodic sign update, plugin not ready");
                }
            });
        }

        // Saving regions and group settings
        long saveFiles = Utils.millisToTicks(Utils.getDurationFromMinutesOrString("saving.delay"));
        if (saveFiles > 0) {
            Do.syncTimer(saveFiles, () -> {
                if (isReady()) {
                    finalPlugin.getFileManager().saveRequiredFiles();
                    AreaShop.debugTask("Saving required files...");
                } else {
                    AreaShop.debugTask("Skipped saving required files, plugin not ready");
                }
            });
        }

        // Sending warnings about rent regions to online players
        long expireWarning = Utils.millisToTicks(Utils.getDurationFromMinutesOrString("expireWarning.delay"));
        if (expireWarning > 0) {
            Do.syncTimer(expireWarning, () -> {
                if (isReady()) {
                    finalPlugin.getFileManager().sendRentExpireWarnings();
                    AreaShop.debugTask("Sending rent expire warnings...");
                } else {
                    AreaShop.debugTask("Skipped sending rent expire warnings, plugin not ready");
                }
            });
        }

        // Update all regions on startup
        if (getConfig().getBoolean("updateRegionsOnStartup")) {
            Do.syncLater(20, () -> {
                finalPlugin.getFileManager().updateAllRegions();
                AreaShop.debugTask("Updating all regions at startup...");
            });
        }
    }

    /**
     * Send a message to a target without a prefix.
     *
     * @param target       The target to send the message to
     * @param key          The key of the language string
     * @param replacements The replacements to insert in the message
     */
    public void messageNoPrefix(Object target, String key, Object... replacements) {
        Message.fromKey(key).replacements(replacements).send(target);
    }

    /**
     * Send a message to a target, prefixed by the default chat prefix.
     *
     * @param target       The target to send the message to
     * @param key          The key of the language string
     * @param replacements The replacements to insert in the message
     */
    public void message(Object target, String key, Object... replacements) {
        Message.fromKey(key).prefix().replacements(replacements).send(target);
    }


    /**
     * Return the config.
     */
    @Override
    public YamlConfiguration getConfig() {
        return fileManager.getConfig();
    }

    /**
     * Sends an debug message to the console.
     *
     * @param message The message that should be printed to the console
     */
    public static void debug(Object... message) {
        if (AreaShop.getInstance().debug) {
            info("Debug: " + StringUtils.join(message, " "));
        }
    }

    /**
     * Non-static debug to use as implementation of the interface.
     *
     * @param message Object parts of the message that should be logged, toString() will be used
     */
    @Override
    public void debugI(Object... message) {
        AreaShop.debug(StringUtils.join(message, " "));
    }

    /**
     * Print an information message to the console.
     *
     * @param message The message to print
     */
    public static void info(Object... message) {
        AreaShop.getInstance().getLogger().info(StringUtils.join(message, " "));
    }

    /**
     * Print a warning to the console.
     *
     * @param message The message to print
     */
    public static void warn(Object... message) {
        AreaShop.getInstance().getLogger().warning(StringUtils.join(message, " "));
    }

    /**
     * Print an error to the console.
     *
     * @param message The message to print
     */
    public static void error(Object... message) {
        AreaShop.getInstance().getLogger().severe(StringUtils.join(message, " "));
    }

    /**
     * Print debug message for periodic task.
     *
     * @param message The message to print
     */
    public static void debugTask(Object... message) {
        if (AreaShop.getInstance().getConfig().getBoolean("debugTask")) {
            AreaShop.debug(StringUtils.join(message, " "));
        }
    }

    /**
     * Reload all files of the plugin and update all regions.
     *
     * @param confirmationReceiver The CommandSender which should be notified when complete, null for nobody
     */
    public void reload(final CommandSender confirmationReceiver) {
        setReady(false);
        fileManager.saveRequiredFilesAtOnce();
        fileManager.loadFiles(true);
        setupLanguageManager();
        message(confirmationReceiver, "reload-reloading");
        fileManager.checkRents();
        fileManager.updateAllRegions(confirmationReceiver);
    }

    /**
     * Reload all files of the plugin and update all regions.
     */
    public void reload() {
        reload(null);
    }

}




