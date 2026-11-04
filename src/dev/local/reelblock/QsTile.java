package dev.local.reelblock;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.text.format.DateFormat;
import java.util.Date;

// Quick Settings tile. Resuming is one tap. Pausing opens the app to ask "for how long?", so a pause is a
// choice and not a slip of the thumb. Needs no permission: BIND_QUICK_SETTINGS_TILE is held by the system.
public class QsTile extends TileService {
    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        if (State.enabled(this) && State.paused(this)) {
            State.resume(this);
            update();
            return;
        }
        Runnable open = () -> openApp(State.enabled(this) ? MainActivity.EXTRA_PAUSE : null);
        if (isLocked()) unlockAndRun(open);
        else open.run();
    }

    @SuppressWarnings("deprecation")
    private void openApp(String extra) {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (extra != null) i.putExtra(extra, true);
        if (Build.VERSION.SDK_INT >= 34)
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        else startActivityAndCollapse(i);
    }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean enabled = State.enabled(this), paused = State.paused(this);
        long until = State.pausedUntil(this);
        String sub = !enabled ? getString(R.string.tile_off)
            : !paused ? getString(R.string.tile_on)
            : until > 0 ? getString(R.string.tile_until, DateFormat.getTimeFormat(this).format(new Date(until)))
            : getString(R.string.tile_paused);
        t.setState(enabled && !paused ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_brand));
        t.setLabel(getString(R.string.tile_label));
        if (Build.VERSION.SDK_INT >= 29) t.setSubtitle(sub);
        if (Build.VERSION.SDK_INT >= 30) t.setStateDescription(sub);
        t.setContentDescription(getString(R.string.tile_label));
        t.updateTile();
    }
}
