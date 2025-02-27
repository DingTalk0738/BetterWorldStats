package me.xGinko.betterworldstats;

import me.xGinko.betterworldstats.commands.BetterWorldStatsCommand;
import me.xGinko.betterworldstats.config.Config;
import me.xGinko.betterworldstats.config.LanguageCache;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class BetterWorldStats extends JavaPlugin {

    private static BetterWorldStats instance;
    private static HashMap<String, LanguageCache> languageCacheMap;
    private static Config config;
    private static WorldStats statistics;
    private static PAPIExpansion papiExpansion;
    private static Logger logger;
    public static boolean foundPlaceholderAPI;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        logger.info("                                                                                ");
        logger.info("  ___      _   _         __      __       _    _ ___ _        _                 ");
        logger.info(" | _ ) ___| |_| |_ ___ _ \\ \\    / /__ _ _| |__| / __| |_ __ _| |_ ___         ");
        logger.info(" | _ \\/ -_)  _|  _/ -_) '_\\ \\/\\/ / _ \\ '_| / _` \\__ \\  _/ _` |  _(_-<    ");
        logger.info(" |___/\\___|\\__|\\__\\___|_|  \\_/\\_/\\___/_| |_\\__,_|___/\\__\\__,_|\\__/__/");
        logger.info("                                                                                ");
        logger.info("Loading languages");
        reloadLang();
        logger.info("Loading config");
        reloadConfiguration();
        logger.info("Registering commands");
        BetterWorldStatsCommand.reloadCommands();
        logger.info("Loading Metrics");
        new Metrics(this, 17204);
        logger.info("Done.");
    }

    public static BetterWorldStats getInstance()  {
        return instance;
    }
    public static WorldStats getStatistics() {
        return statistics;
    }
    public static Config getConfiguration() {
        return config;
    }
    public static Logger getLog() {
        return logger;
    }
    public static LanguageCache getLang(CommandSender commandSender) {
        return commandSender instanceof Player ? getLang(((Player) commandSender).getLocale()) : getLang(config.default_lang);
    }
    public static LanguageCache getLang(String lang) {
        if (!config.auto_lang) return languageCacheMap.get(config.default_lang);
        return languageCacheMap.getOrDefault(lang.replace("-", "_"), languageCacheMap.get(config.default_lang));
    }

    public void reloadPlugin() {
        reloadLang();
        reloadConfiguration();
        BetterWorldStatsCommand.reloadCommands();
    }

    private void reloadConfiguration() {
        try {
            config = new Config();
            HandlerList.unregisterAll(this);
            statistics = new WorldStats();
            if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                foundPlaceholderAPI = true;
                if (papiExpansion != null) papiExpansion.unregister();
                papiExpansion = new PAPIExpansion();
            } else {
                foundPlaceholderAPI = false;
            }
            config.saveConfig();
        } catch (Exception e) {
            logger.severe("Failed while loading config! - " + e.getLocalizedMessage());
        }
    }

    public void reloadLang() {
        languageCacheMap = new HashMap<>();
        try {
            File langDirectory = new File(getDataFolder()  + File.separator + "lang");
            Files.createDirectories(langDirectory.toPath());
            for (String fileName : getDefaultLanguageFiles()) {
                final String localeString = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.lastIndexOf('.'));
                logger.info("Found language file for " + localeString);
                languageCacheMap.put(localeString, new LanguageCache(localeString));
            }
            final Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            for (File langFile : langDirectory.listFiles()) {
                final Matcher langMatcher = langPattern.matcher(langFile.getName());
                if (langMatcher.find()) {
                    final String localeString = langMatcher.group(1).toLowerCase();
                    if (!languageCacheMap.containsKey(localeString)) { // make sure it wasn't a default file that we already loaded
                        logger.info("Found language file for " + localeString);
                        languageCacheMap.put(localeString, new LanguageCache(localeString));
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error loading language files! Language files will not reload to avoid errors, make sure to correct this before restarting the server!");
            e.printStackTrace();
        }
    }

    private Set<String> getDefaultLanguageFiles() {
        try (final JarFile pluginJarFile = new JarFile(this.getFile())) {
            return pluginJarFile.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("lang" + File.separator) && name.endsWith(".yml"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            logger.severe("Failed getting default lang files! - "+e.getLocalizedMessage());
            return Collections.emptySet();
        }
    }
}