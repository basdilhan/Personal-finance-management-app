package com.team.financeapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Reusable bottom navigation fragment shared across top-level screens.
 */
public class BottomNavigationFragment extends Fragment {

    private static final String ARG_SELECTED_ITEM_ID = "arg_selected_item_id";

    private int selectedItemId = R.id.nav_home;

    public BottomNavigationFragment() {
        super(R.layout.fragment_bottom_navigation);
    }

    public static BottomNavigationFragment newInstance(@IdRes int selectedItemId) {
        BottomNavigationFragment fragment = new BottomNavigationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SELECTED_ITEM_ID, selectedItemId);
        fragment.setArguments(args);
        return fragment;
    }

    public static void attach(
            @NonNull AppCompatActivity activity,
            @IdRes int containerId,
            @IdRes int selectedItemId
    ) {
        androidx.fragment.app.Fragment existing = activity.getSupportFragmentManager()
                .findFragmentById(containerId);

        if (existing instanceof BottomNavigationFragment) {
            ((BottomNavigationFragment) existing).setSelectedItemId(selectedItemId);
            return;
        }

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, BottomNavigationFragment.newInstance(selectedItemId))
                .commit();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            selectedItemId = args.getInt(ARG_SELECTED_ITEM_ID, R.id.nav_home);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> handleNavigation(item.getItemId()));
        bottomNavigationView.setSelectedItemId(selectedItemId);
    }

    private void setSelectedItemId(@IdRes int itemId) {
        selectedItemId = itemId;

        View view = getView();
        if (view == null) {
            return;
        }

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(itemId);
    }

    private boolean handleNavigation(@IdRes int itemId) {
        if (itemId == R.id.nav_profile) {
            Toast.makeText(requireContext(), "Profile coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (itemId == selectedItemId) {
            return true;
        }

        Class<? extends Activity> destination = resolveDestination(itemId);
        if (destination == null) {
            return false;
        }

        Activity hostActivity = requireActivity();
        hostActivity.startActivity(new Intent(hostActivity, destination));
        hostActivity.finish();
        return true;
    }

    @Nullable
    private Class<? extends Activity> resolveDestination(@IdRes int itemId) {
        if (itemId == R.id.nav_home) {
            return DashboardActivity.class;
        }
        if (itemId == R.id.nav_expenses) {
            return ExpensesActivity.class;
        }
        if (itemId == R.id.nav_bills) {
            return BillsActivity.class;
        }
        if (itemId == R.id.nav_goals) {
            return GoalsActivity.class;
        }
        return null;
    }
}
