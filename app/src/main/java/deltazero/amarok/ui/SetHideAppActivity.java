package deltazero.amarok.ui;

import static deltazero.amarok.utils.AppInfoUtil.AppInfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;

public class SetHideAppActivity extends AmarokActivity {

    private RecyclerView rvAppList;
    private AppListAdapter adapter;
    private MaterialToolbar tbToolBar;
    private SwipeRefreshLayout srLayout;
    private AppListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hideapp);

        initViews();
        setupViewModel();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFilterMenu();
        observeViewModel();
    }

    private void initViews() {
        rvAppList = findViewById(R.id.hideapp_rv_applist);
        tbToolBar = findViewById(R.id.hideapp_tb_toolbar);
        srLayout = findViewById(R.id.hideapp_sr_layout);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AppListViewModel.class);
    }

    private void setupToolbar() {
        setSupportActionBar(tbToolBar);
        tbToolBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AppListAdapter(
                app -> viewModel.toggleAppHidden(app),
                this::showAppConfigDialog
        );
        rvAppList.setAdapter(adapter);
        rvAppList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSwipeRefresh() {
        srLayout.setOnRefreshListener(() -> viewModel.refreshApps());
    }

    private void observeViewModel() {
        viewModel.getAppList().observe(this, apps -> {
            adapter.submitList(apps);
            srLayout.setRefreshing(false);
        });

        viewModel.isLoading().observe(this, isLoading -> srLayout.setRefreshing(isLoading));
    }

    private void showAppConfigDialog(AppInfo app, int position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_config, null);
        MaterialSwitch swKeepDisabled = dialogView.findViewById(R.id.dialog_appconfig_sw_keepdisabled);
        MaterialSwitch swKeepPreviousStatus = dialogView.findViewById(R.id.dialog_appconfig_sw_keeppreviousstatus);

        boolean isKeepDisabled = PrefMgr.getKeepDisabledApps().contains(app.packageName());
        boolean isKeepPreviousStatus = PrefMgr.getKeepPreviousStatusApps().contains(app.packageName());
        swKeepDisabled.setChecked(isKeepDisabled);
        swKeepPreviousStatus.setChecked(isKeepPreviousStatus);

        new MaterialAlertDialogBuilder(this)
                .setTitle(app.label())
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    boolean newKeepDisabled = swKeepDisabled.isChecked();
                    boolean newKeepPreviousStatus = swKeepPreviousStatus.isChecked();
                    boolean changed = false;
                    if (newKeepDisabled != isKeepDisabled) {
                        viewModel.toggleAppKeepDisabled(app);
                        changed = true;
                    }
                    if (newKeepPreviousStatus != isKeepPreviousStatus) {
                        viewModel.toggleAppKeepPreviousStatus(app);
                        changed = true;
                    }
                    if (changed) {
                        adapter.notifyItemChanged(position);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupFilterMenu() {
        addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_hideapp, menu);

                MenuItem systemAppsItem = menu.findItem(R.id.display_system_apps);
                MenuItem rootAppsItem = menu.findItem(R.id.display_root_apps);
                viewModel.getShowSystemApps().observe(SetHideAppActivity.this, systemAppsItem::setChecked);
                viewModel.getShowRootApps().observe(SetHideAppActivity.this, rootAppsItem::setChecked);

                SearchView searchView = (SearchView) menu.findItem(R.id.action_search_app).getActionView();
                assert searchView != null;
                setupSearchView(searchView);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.display_system_apps) {
                    if (Boolean.FALSE.equals(viewModel.getShowSystemApps().getValue())) {
                        new MaterialAlertDialogBuilder(SetHideAppActivity.this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.warning_system_apps)
                            .setPositiveButton(R.string.confirm, (dialog, which) -> viewModel.toggleSystemApps())
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    } else {
                        viewModel.toggleSystemApps();
                    }
                    return true;
                }
                if (menuItem.getItemId() == R.id.display_root_apps) {
                    if (Boolean.FALSE.equals(viewModel.getShowRootApps().getValue())) {
                        new MaterialAlertDialogBuilder(SetHideAppActivity.this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.warning_root_apps)
                            .setPositiveButton(R.string.confirm, (dialog, which) -> viewModel.toggleRootApps())
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    } else {
                        viewModel.toggleRootApps();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
    }
}
