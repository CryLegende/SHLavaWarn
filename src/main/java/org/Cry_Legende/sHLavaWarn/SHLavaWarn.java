package org.Cry_Legende.sHLavaWarn;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public class SHLavaWarn extends JavaPlugin implements Listener {

    private String warningMessageFormat;
    private String soundName;
    private float soundVolume;
    private float soundPitch;
    private double warningRadius;
    private Sound soundToPlay; // Cache le son pour éviter de le rechercher à chaque fois

    @Override
    public void onEnable() {
        // Sauvegarder la configuration par défaut si elle n'existe pas
        saveDefaultConfig();
        // Charger la configuration
        loadConfiguration();

        // Enregistrer cet écouteur d'événements
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("SHLavaWarn a été activé !");
        getLogger().info("Message: " + warningMessageFormat);
        getLogger().info("Son: " + soundName + ", Volume: " + soundVolume + ", Pitch: " + soundPitch);
        getLogger().info("Rayon d'alerte: " + warningRadius);
    }

    @Override
    public void onDisable() {
        getLogger().info("SHLavaWarn a été désactivé !");
    }

    private void loadConfiguration() {
        FileConfiguration config = getConfig();

        // Lire le message, avec une valeur par défaut au cas où
        warningMessageFormat = config.getString("warning.message",
            "&6Obsidienne créé en &eX:{x} Y:{y} Z:{z}&b. &aUtilisez un seau vide pour récupérer la lave !");

        // Lire les paramètres du son
        soundName = config.getString("warning.sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase();
        soundVolume = (float) config.getDouble("warning.sound.volume", 1.0);
        soundPitch = (float) config.getDouble("warning.sound.pitch", 1.0);

        // Essayer de convertir le nom ou la clé du son en objet Sound
        soundToPlay = sound(soundName);
        if (soundToPlay == null) {
            getLogger().warning("Nom de son invalide dans config.yml: '" + soundName + "'. Utilisation de ENTITY_EXPERIENCE_ORB_PICKUP par défaut.");
            soundToPlay = Sound.ENTITY_EXPERIENCE_ORB_PICKUP; // Son par défaut robuste
            soundName = "ENTITY_EXPERIENCE_ORB_PICKUP"; // Mettre à jour soundName pour le log
        }

        // Lire le rayon
        warningRadius = config.getDouble("warning.radius", 15.0);
        if (warningRadius < 0) {
            getLogger().warning("Le rayon d'alerte ne peut pas être négatif. Utilisation de 15.0 par défaut.");
            warningRadius = 15.0;
        }
    }

    @EventHandler
    public void onObsidianForm(BlockFormEvent event) {
        // Vérifier si le nouveau bloc formé est de l'obsidienne
        if (event.getNewState().getType() == Material.OBSIDIAN) {
            Block obsidianBlock = event.getBlock();
            Location obsidianLocation = obsidianBlock.getLocation();

            // Préparer le message en remplaçant les placeholders
            String message = ChatColor.translateAlternateColorCodes('&', warningMessageFormat)
                .replace("{x}", String.valueOf(obsidianLocation.getBlockX()))
                .replace("{y}", String.valueOf(obsidianLocation.getBlockY()))
                .replace("{z}", String.valueOf(obsidianLocation.getBlockZ()));

            // Rayon carré pour une comparaison plus efficace
            double radiusSquared = warningRadius * warningRadius;

            // Parcourir tous les joueurs dans le monde du bloc
            for (Player player : obsidianBlock.getWorld().getPlayers()) {
                // Vérifier si le joueur est dans le rayon défini
                if (player.getLocation().distanceSquared(obsidianLocation) <= radiusSquared) {
                    // Envoyer le message au joueur
                    player.sendMessage(message);
                    // Jouer le son pour le joueur (à sa propre position pour une meilleure spatialisation)
                    if (soundToPlay != null) {
                        player.playSound(player.getLocation(), soundToPlay, soundVolume, soundPitch);
                    }
                }
            }
        }
    }

    /** Son lu en config : ancien nom (ENTITY_PLAYER_LEVELUP) ou clé (minecraft:entity.player.levelup). */
    static Sound sound(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (s.indexOf(':') >= 0 || s.indexOf('.') >= 0) {
            NamespacedKey key = NamespacedKey.fromString(s.toLowerCase(Locale.ROOT));
            return key == null ? null : Registry.SOUND_EVENT.get(key);
        }
        try {
            return (Sound) Sound.class.getField(s.toUpperCase(Locale.ROOT)).get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

}
