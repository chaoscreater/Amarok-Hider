package deltazero.amarok.ui.settings;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import deltazero.amarok.R;
import deltazero.amarok.ui.BackupActivity;

public class BackupCategory extends BaseCategory {

    public BackupCategory(@NonNull FragmentActivity activity, @NonNull PreferenceScreen screen) {
        super(activity, screen);
        setTitle(R.string.backup_restore);

        Preference backupPref = new Preference(activity);
        backupPref.setTitle(R.string.backup_restore);
        backupPref.setSummary(R.string.backup_restore_description);
        backupPref.setIcon(R.drawable.settings_backup_restore_black_24dp);
        backupPref.setIntent(new Intent(activity, BackupActivity.class));
        addPreference(backupPref);
    }
}
