package deltazero.amarok.ui;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class BackupActivity extends AmarokActivity {

    private static final String TAG = "BackupActivity";

    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        MaterialToolbar toolbar = findViewById(R.id.backup_tb_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri != null) performExport(uri);
                }
        );

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) confirmImport(uri);
                }
        );

        findViewById(R.id.backup_ll_export).setOnClickListener(v -> {
            String filename = "amarok_backup_"
                    + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())
                    + ".json";
            exportLauncher.launch(filename);
        });

        findViewById(R.id.backup_ll_import).setOnClickListener(v ->
                importLauncher.launch(new String[]{"application/json", "*/*"})
        );
    }

    private void performExport(Uri uri) {
        try {
            JSONObject json = new JSONObject();
            json.put("version", 1);

            JSONArray appsArray = new JSONArray();
            for (String pkg : PrefMgr.getHideApps()) appsArray.put(pkg);
            json.put("hiddenApps", appsArray);

            JSONArray filesArray = new JSONArray();
            for (String path : PrefMgr.getHideFilePath()) filesArray.put(path);
            json.put("hiddenFiles", filesArray);

            JSONArray keepDisabledArray = new JSONArray();
            for (String pkg : PrefMgr.getKeepDisabledApps()) keepDisabledArray.put(pkg);
            json.put("keepDisabledApps", keepDisabledArray);

            JSONArray keepPreviousStatusArray = new JSONArray();
            for (String pkg : PrefMgr.getKeepPreviousStatusApps()) keepPreviousStatusArray.put(pkg);
            json.put("keepPreviousStatusApps", keepPreviousStatusArray);

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                Objects.requireNonNull(os).write(json.toString(2).getBytes(StandardCharsets.UTF_8));
            }

            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmImport(Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.import_confirm_title)
                .setMessage(R.string.import_confirm_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> performImport(uri))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performImport(Uri uri) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(getContentResolver().openInputStream(uri)),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            JSONObject json = new JSONObject(sb.toString());

            JSONArray appsArray = json.optJSONArray("hiddenApps");
            if (appsArray != null) {
                Set<String> apps = new HashSet<>();
                for (int i = 0; i < appsArray.length(); i++) apps.add(appsArray.getString(i));
                PrefMgr.setHideApps(apps);
            }

            JSONArray filesArray = json.optJSONArray("hiddenFiles");
            if (filesArray != null) {
                Set<String> files = new HashSet<>();
                for (int i = 0; i < filesArray.length(); i++) files.add(filesArray.getString(i));
                PrefMgr.setHideFilePath(files);
            }

            JSONArray keepDisabledArray = json.optJSONArray("keepDisabledApps");
            if (keepDisabledArray != null) {
                Set<String> keepDisabled = new HashSet<>();
                for (int i = 0; i < keepDisabledArray.length(); i++) keepDisabled.add(keepDisabledArray.getString(i));
                PrefMgr.setKeepDisabledApps(keepDisabled);
            }

            JSONArray keepPreviousStatusArray = json.optJSONArray("keepPreviousStatusApps");
            if (keepPreviousStatusArray != null) {
                Set<String> keepPreviousStatus = new HashSet<>();
                for (int i = 0; i < keepPreviousStatusArray.length(); i++) keepPreviousStatus.add(keepPreviousStatusArray.getString(i));
                PrefMgr.setKeepPreviousStatusApps(keepPreviousStatus);
            }

            Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
