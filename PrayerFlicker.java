package Scurrius;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.dreambot.api.Client;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.interactive.Projectiles;
import org.dreambot.api.methods.prayer.Prayer;
import org.dreambot.api.methods.prayer.Prayers;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.graphics.Projectile;

import java.util.List;

import static org.dreambot.api.utilities.Logger.log;

public class PrayerFlicker {
    private int lastGameTick;
    private Client client;
    private Players players;
    private Prayers prayers;
    private static final int RANGED_PROJECTILE_ID = 2642;
    private static final int MAGIC_PROJECTILE_ID = 2640;
    private Prayer currentPrayer = null;


    public PrayerFlicker(Prayers prayers) {
        this.prayers = prayers;
        this.lastGameTick = Client.getGameTick();
    }


    public void onGameTick() {
        handlePrayerFlicking();
    }

    private void handlePrayerFlicking() {
        Prayer prayerToUse = getCurrentPrayer();

        // Flick the specified prayer
        Prayers.toggle(true, prayerToUse);  // Turn the prayer on
        log("Activated " + prayerToUse);

        Sleep.sleepUntil(() -> Client.getGameTick() != lastGameTick, 1000);  // Wait for the next tick

        Prayers.toggle(false, prayerToUse); // Turn the prayer off
        log("Deactivated " + prayerToUse);

        lastGameTick = Client.getGameTick(); // Update lastGameTick
    }

    private Prayer getCurrentPrayer() {
        Projectile rangedProjectile = handleProjectile(RANGED_PROJECTILE_ID);
        Projectile magicProjectile = handleProjectile(MAGIC_PROJECTILE_ID);

        if (rangedProjectile != null) {
            return Prayer.PROTECT_FROM_MISSILES;
        } else if (magicProjectile != null) {
            return Prayer.PROTECT_FROM_MAGIC;
        } else {
            return Prayer.PROTECT_FROM_MELEE;
        }
    }

    private Projectile handleProjectile(int projectileId) {
        Projectile projectile = Projectiles.closest(p -> p.getID() == projectileId);
        if (projectile != null) {
            log("Handling projectile attack with ID: " + projectileId);
        }
        return projectile;
    }

    private void log(String message) {
        Logger.log(message);
    }
}