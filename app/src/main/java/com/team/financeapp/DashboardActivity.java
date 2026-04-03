package com.team.financeapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.button.MaterialButton;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.BillRepository;
import com.team.financeapp.data.repository.ExpenseRepository;
import com.team.financeapp.data.repository.IncomeRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dashboard activity displaying user's financial overview.
 * Shows summary cards and provides quick actions for managing finances.
 */
public class DashboardActivity extends AppCompatActivity {

    private static final long BACK_PRESS_EXIT_INTERVAL_MS = 2000;
    private static final String PREF_DASHBOARD = "dashboard_preferences";
    private static final String KEY_BALANCE_VISIBLE = "balance_visible";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 2001;
    private static final String PREFS_NAME = "finance_preferences";
    private static final String PREF_NOTIFICATION_PERMISSION_PROMPTED = "notification_permission_prompted";
    private static final String PREF_NOTIFICATION_SETTINGS_HINT_SHOWN = "notification_settings_hint_shown";

    private MaterialButton btnLogout;
    private MaterialButton buttonToggleBalanceVisibility;
    private View actionAddExpense, actionAddIncome, actionAddBill, actionAddGoal;
    private View btnNotifications;
    private View notificationBadge;
    private TextView buttonViewAllBills;
    private TextView buttonViewAllGoals;
    private TextView dashboardWelcome;
    private TextView textTotalBalance;
    private TextView textIncomeAmount;
    private TextView textExpensesAmount;
    private TextView textBalanceTrend;
    private TextView textBalanceTrendCaption;
    private TextView textAlertMessage;
    private TextView textLegendHousingPercent;
    private TextView textLegendFoodPercent;
    private TextView textLegendTransportPercent;
    private TextView textLegendEntertainmentPercent;
    private TextView textLegendOtherPercent;
    private TextView textGoalName;
    private TextView textGoalDeadline;
    private TextView textGoalPercentage;
    private TextView textGoalCurrent;
    private TextView textGoalTarget;
    private View progressGoalView;
    private View cardGoal;
    private View cardBill1;
    private View cardBill2;
    private View cardBill3;
    private ImageView imageBill1Icon;
    private ImageView imageBill2Icon;
    private ImageView imageBill3Icon;
    private TextView textBill1Name;
    private TextView textBill1Due;
    private TextView textBill1Amount;
    private TextView textBill2Name;
    private TextView textBill2Due;
    private TextView textBill2Amount;
    private TextView textBill3Name;
    private TextView textBill3Due;
    private TextView textBill3Amount;
    private View profileAvatar;
    private PieChart pieChartExpenses;
    private PieChart pieChartIncome;
    private PieChart pieChartBills;
    private long lastBackPressedAt;
    private AuthManager authManager;
    private BillRepository billRepository;
    private ExpenseRepository expenseRepository;
    private IncomeRepository incomeRepository;
    private FirebaseFirestore firestore;

    private List<Bill> latestBills = new ArrayList<>();
    private List<Expense> latestExpenses = new ArrayList<>();
    private List<GoalSummary> latestGoals = new ArrayList<>();
    private List<IncomeEntry> latestIncomes = new ArrayList<>();
    private double currentMonthIncome = 0.0d;
    private double currentTotalBalance = 0.0d;
    private double currentTotalExpenses = 0.0d;
    private boolean isBalanceVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        authManager = new AuthManager();
        billRepository = new BillRepository(this);
        expenseRepository = new ExpenseRepository(this);
        incomeRepository = new IncomeRepository(this);
        firestore = FirebaseFirestore.getInstance();
        initializeViews();
        loadPrivacyPreference();
        ensureNotificationPermission();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_home);
        setupClickListeners();
        setupBackPressedCallback();
        setupPieChart();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always start hidden when user returns to the app for better privacy.
        isBalanceVisible = false;
        savePrivacyPreference();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_home);
        loadDashboardData();
    }

    @Override
    protected void onPause() {
        // Hide sensitive wallet figures whenever dashboard leaves foreground.
        isBalanceVisible = false;
        savePrivacyPreference();
        super.onPause();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        btnLogout = findViewById(R.id.button_logout);
        profileAvatar = findViewById(R.id.profile_avatar);
        buttonViewAllBills = findViewById(R.id.button_view_all_bills);
        buttonViewAllGoals = findViewById(R.id.btn_view_all_goals);
        pieChartExpenses = findViewById(R.id.pie_chart_expenses);
        pieChartIncome = findViewById(R.id.pie_chart_income);
        pieChartBills = null;
        dashboardWelcome = findViewById(R.id.dashboard_welcome);
        textTotalBalance = findViewById(R.id.text_total_balance);
        textIncomeAmount = findViewById(R.id.text_income_amount);
        textExpensesAmount = findViewById(R.id.text_expenses_amount);
        buttonToggleBalanceVisibility = findViewById(R.id.button_toggle_balance_visibility);
        textBalanceTrend = findViewById(R.id.text_balance_trend);
        textBalanceTrendCaption = findViewById(R.id.text_balance_trend_caption);
        textAlertMessage = findViewById(R.id.text_alert_message);
        textGoalName = findViewById(R.id.text_goal_name);
        textGoalDeadline = findViewById(R.id.text_goal_deadline);
        textGoalPercentage = findViewById(R.id.text_goal_percentage);
        textGoalCurrent = findViewById(R.id.text_goal_current);
        textGoalTarget = findViewById(R.id.text_goal_target);
        progressGoalView = findViewById(R.id.progress_goal_view);
        cardGoal = findViewById(R.id.card_goal);
        cardBill1 = findViewById(R.id.card_bill_electricity);
        cardBill2 = findViewById(R.id.card_bill_water);
        cardBill3 = findViewById(R.id.card_bill_internet);
        imageBill1Icon = findViewById(R.id.image_bill_1_icon);
        imageBill2Icon = findViewById(R.id.image_bill_2_icon);
        imageBill3Icon = findViewById(R.id.image_bill_3_icon);
        textBill1Name = findViewById(R.id.text_bill_1_name);
        textBill1Due = findViewById(R.id.text_bill_1_due);
        textBill1Amount = findViewById(R.id.text_bill_1_amount);
        textBill2Name = findViewById(R.id.text_bill_2_name);
        textBill2Due = findViewById(R.id.text_bill_2_due);
        textBill2Amount = findViewById(R.id.text_bill_2_amount);
        textBill3Name = findViewById(R.id.text_bill_3_name);
        textBill3Due = findViewById(R.id.text_bill_3_due);
        textBill3Amount = findViewById(R.id.text_bill_3_amount);
        textLegendHousingPercent = findViewById(R.id.text_legend_housing_percent);
        textLegendFoodPercent = findViewById(R.id.text_legend_food_percent);
        textLegendTransportPercent = findViewById(R.id.text_legend_transport_percent);
        textLegendEntertainmentPercent = findViewById(R.id.text_legend_entertainment_percent);
        textLegendOtherPercent = findViewById(R.id.text_legend_other_percent);

        // Initialize Quick Action buttons
        actionAddExpense = findViewById(R.id.action_add_expense);
        actionAddIncome = findViewById(R.id.action_add_income);
        actionAddBill = findViewById(R.id.action_add_bill);
        actionAddGoal = findViewById(R.id.action_add_goal);
        btnNotifications = findViewById(R.id.btn_notifications);
        notificationBadge = findViewById(R.id.notification_badge);
    }

    private void loadPrivacyPreference() {
        SharedPreferences preferences = getSharedPreferences(PREF_DASHBOARD, MODE_PRIVATE);
        isBalanceVisible = preferences.getBoolean(KEY_BALANCE_VISIBLE, false);
    }

    private void savePrivacyPreference() {
        getSharedPreferences(PREF_DASHBOARD, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BALANCE_VISIBLE, isBalanceVisible)
                .apply();
    }

    private void applyBalancePrivacyState() {
        if (textTotalBalance == null || textIncomeAmount == null || textExpensesAmount == null) {
            return;
        }

        if (isBalanceVisible) {
            textTotalBalance.setText(formatMoney(currentTotalBalance));
            textIncomeAmount.setText(formatMoney(currentMonthIncome));
            textExpensesAmount.setText(formatMoney(currentTotalExpenses));
        } else {
            textTotalBalance.setText("••••••");
            textIncomeAmount.setText("••••••");
            textExpensesAmount.setText("••••••");
        }

        if (buttonToggleBalanceVisibility != null) {
            buttonToggleBalanceVisibility.setIconResource(isBalanceVisible
                ? R.drawable.ic_eye_open
                : R.drawable.ic_eye_closed);
            buttonToggleBalanceVisibility.setContentDescription(getString(isBalanceVisible
                ? R.string.dashboard_hide_amounts
                : R.string.dashboard_show_amounts));
        }
    }

    private int getColorCompat(@ColorRes int colorResId) {
        return ContextCompat.getColor(this, colorResId);
    }

    /**
     * Setup the expense categories pie chart
     */
    private void setupPieChart() {
        configurePieChart(pieChartExpenses, "No\nExpenses");
        configurePieChart(pieChartIncome, "No\nIncome");
        configurePieChart(pieChartBills, "No\nBills");
        updateChartLegendPercentages(new HashMap<>());
    }

    private void configurePieChart(PieChart chart, String emptyText) {
        if (chart == null) {
            return;
        }
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(12f, 8f, 12f, 8f);
        chart.setDragDecelerationFrictionCoef(0.95f);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(getColorCompat(R.color.dashboard_chart_hole));
        chart.setTransparentCircleColor(getColorCompat(R.color.dashboard_chart_hole));
        chart.setTransparentCircleAlpha(24);
        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(63f);
        chart.setDrawCenterText(true);
        chart.setCenterTextSize(15f);
        chart.setCenterTextColor(getColorCompat(R.color.dashboard_chart_center));
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setDrawEntryLabels(false);
        Legend legend = chart.getLegend();
        legend.setEnabled(false);
        chart.clear();
        chart.setCenterText(emptyText);
        chart.invalidate();
    }

    private void loadDashboardData() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindUserHeader();
        loadBills(userId);
        loadExpenses(userId);
        loadGoals(userId);
        loadIncome(userId);
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean prompted = preferences.getBoolean(PREF_NOTIFICATION_PERMISSION_PROMPTED, false);
        boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        );

        if (!prompted || shouldShowRationale) {
            preferences.edit().putBoolean(PREF_NOTIFICATION_PERMISSION_PROMPTED, true).apply();
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION
            );
            return;
        }

        if (!preferences.getBoolean(PREF_NOTIFICATION_SETTINGS_HINT_SHOWN, false)) {
            preferences.edit().putBoolean(PREF_NOTIFICATION_SETTINGS_HINT_SHOWN, true).apply();
            showNotificationSettingsDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Notifications are off. Goal and bill reminders may not appear.", Toast.LENGTH_LONG).show();
    }

    private void showNotificationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable notifications")
                .setMessage("Goal and bill reminders are blocked. Enable notifications in app settings.")
                .setPositiveButton("Open settings", (dialog, which) -> openAppNotificationSettings())
                .setNegativeButton("Not now", null)
                .show();
    }

    private void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            return;
        }

        Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + getPackageName()));
        startActivity(fallback);
    }

    private void bindUserHeader() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (dashboardWelcome == null) {
            return;
        }
        if (user == null) {
            dashboardWelcome.setText("User");
            return;
        }
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.trim().isEmpty()) {
            dashboardWelcome.setText(displayName.trim());
            return;
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            dashboardWelcome.setText(email.substring(0, email.indexOf('@')));
            return;
        }
        dashboardWelcome.setText("User");
    }

    private void loadBills(String userId) {
        billRepository.loadBills(userId, new BillRepository.LoadBillsCallback() {
            @Override
            public void onBillsLoaded(List<Bill> bills) {
                latestBills = new ArrayList<>(bills);
                updateUpcomingBills();
                updateBillsChartFromData();
                updateDashboardTotalsAndInsight();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExpenses(String userId) {
        expenseRepository.loadExpenses(userId, new ExpenseRepository.LoadExpensesCallback() {
            @Override
            public void onExpensesLoaded(List<Expense> expenses) {
                latestExpenses = new ArrayList<>(expenses);
                updateExpenseChartFromData();
                updateDashboardTotalsAndInsight();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGoals(String userId) {
        firestore.collection("goals")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<GoalSummary> goals = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        goals.add(new GoalSummary(
                                getString(document, "name", "Savings Goal"),
                                getDouble(document, "targetAmount", 0.0d),
                                getDouble(document, "currentAmount", 0.0d),
                                 getLong(document, "targetDate", 0L),
                                getLong(document, "updatedAt", 0L) // Get updatedAt timestamp
                        ));
                    }
                    // Sort by latest updated first (descending order)
                    goals.sort((goal1, goal2) -> Long.compare(goal2.updatedAt, goal1.updatedAt));
                    latestGoals = goals;
                    updateGoalCard();
                    updateDashboardTotalsAndInsight();
                })
                .addOnFailureListener(e -> {
                    latestGoals = Collections.emptyList();
                    updateGoalCard();
                });
    }

    private void loadIncome(String userId) {
        incomeRepository.loadIncome(userId, new IncomeRepository.LoadIncomeCallback() {
            @Override
            public void onIncomeLoaded(List<IncomeEntry> incomes) {
                latestIncomes = new ArrayList<>(incomes);
                currentMonthIncome = sumAllIncome();
                updateIncomeChartFromData();
                updateDashboardTotalsAndInsight();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDashboardTotalsAndInsight() {
        double monthlyExpenses = sumAllExpenses();
        double totalUpcomingBills = sumUpcomingUnpaidBills();
        currentTotalExpenses = monthlyExpenses;

        // Balance represents monthly cash position after known upcoming unpaid bills.
        double totalBalance = currentMonthIncome - monthlyExpenses - totalUpcomingBills;
        currentTotalBalance = totalBalance;
        applyBalancePrivacyState();

        updateBalanceTrend(totalBalance);

        if (textAlertMessage != null) {
            if (monthlyExpenses <= 0.0d && totalUpcomingBills <= 0.0d) {
                textAlertMessage.setText("No expenses or bills yet for this account. Add entries to unlock personalized insights.");
            } else if (currentMonthIncome <= 0.0d) {
                textAlertMessage.setText("You've recorded spending, but no income entries yet. Add income to track your balance.");
            } else {
                if (totalBalance >= 0) {
                    textAlertMessage.setText(String.format(Locale.getDefault(), "Great job. You still have %s available after expenses and upcoming bills.", formatMoney(totalBalance)));
                } else {
                    textAlertMessage.setText(String.format(Locale.getDefault(), "Heads up: you're short by %s after expenses and upcoming bills.", formatMoney(Math.abs(totalBalance))));
                }
            }
        }
    }

    private void updateBalanceTrend(double currentNet) {
        if (textBalanceTrend == null || textBalanceTrendCaption == null) {
            return;
        }

        if (!isBalanceVisible) {
            textBalanceTrend.setText("••••");
            textBalanceTrend.setTextColor(getColorCompat(R.color.text_secondary));
            textBalanceTrendCaption.setText(R.string.dashboard_amounts_hidden);
            return;
        }

        Calendar previous = Calendar.getInstance();
        previous.add(Calendar.MONTH, -1);
        int prevYear = previous.get(Calendar.YEAR);
        int prevMonth = previous.get(Calendar.MONTH);

        double previousIncome = sumIncomeForMonth(prevYear, prevMonth);
        double previousExpenses = sumExpensesForMonth(prevYear, prevMonth);
        double previousNet = previousIncome - previousExpenses;

        if (Math.abs(previousNet) < 0.01d) {
            textBalanceTrend.setText("--");
            textBalanceTrend.setTextColor(getColorCompat(R.color.text_secondary));
            textBalanceTrendCaption.setText("No previous month data");
            return;
        }

        double delta = currentNet - previousNet;
        double percentChange = (delta / Math.abs(previousNet)) * 100.0d;
        textBalanceTrend.setText(String.format(Locale.getDefault(), "%+.1f%%", percentChange));
        textBalanceTrend.setTextColor(getColorCompat(percentChange >= 0 ? R.color.success : R.color.error));
        textBalanceTrendCaption.setText("vs last month");
    }

    private void updateExpenseChartFromData() {
        if (pieChartExpenses == null) {
            return;
        }

        Map<String, Double> grouped = new HashMap<>();
        for (Expense expense : latestExpenses) {
            String key = normalizeExpenseCategory(expense.getCategory());
            grouped.put(key, grouped.getOrDefault(key, 0.0d) + expense.getAmount());
        }

        updateChartLegendPercentages(grouped);

        if (grouped.isEmpty()) {
            pieChartExpenses.clear();
            pieChartExpenses.setCenterText("No\nExpenses");
            pieChartExpenses.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        String[] order = new String[]{"Housing", "Food", "Transport", "Entertainment", "Other"};
        int[] palette = new int[]{
                getColorCompat(R.color.primary),
                getColorCompat(R.color.success),
                getColorCompat(R.color.accent),
                getColorCompat(R.color.info),
                getColorCompat(R.color.dashboard_chart_other)
        };

        for (int i = 0; i < order.length; i++) {
            double amount = grouped.getOrDefault(order[i], 0.0d);
            if (amount <= 0.0d) {
                continue;
            }
            entries.add(new PieEntry((float) amount, order[i]));
            colors.add(palette[i]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(getColorCompat(R.color.white));
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartExpenses));
        data.setValueTextSize(11f);
        data.setValueTextColor(getColorCompat(R.color.white));

        pieChartExpenses.setCenterText("All Time\nExpenses");
        pieChartExpenses.setData(data);
        pieChartExpenses.highlightValues(null);
        pieChartExpenses.invalidate();
    }

    private void updateIncomeChartFromData() {
        if (pieChartIncome == null) {
            return;
        }

        pieChartIncome.setDrawEntryLabels(true);
        pieChartIncome.setEntryLabelColor(getColorCompat(R.color.text_primary));
        pieChartIncome.setEntryLabelTextSize(12f);

        Map<String, Double> grouped = new HashMap<>();
        for (IncomeEntry entry : latestIncomes) {
            long normalizedDate = normalizeEpochMillis(entry.getDate());
            if (normalizedDate <= 0L) {
                continue;
            }
            String key = normalizeIncomeSource(entry.getSource());
            grouped.put(key, grouped.getOrDefault(key, 0.0d) + entry.getAmount());
        }

        if (grouped.isEmpty()) {
            pieChartIncome.clear();
            pieChartIncome.setCenterText("No\nIncome");
            pieChartIncome.invalidate();
            return;
        }

        String[] order = new String[]{"Salary", "Business", "Freelance", "Other"};
        int[] palette = new int[]{
                getColorCompat(R.color.success),
                getColorCompat(R.color.primary),
                getColorCompat(R.color.accent),
                getColorCompat(R.color.dashboard_chart_other)
        };

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < order.length; i++) {
            double amount = grouped.getOrDefault(order[i], 0.0d);
            if (amount <= 0.0d) {
                continue;
            }
            entries.add(new PieEntry((float) amount, order[i]));
            colors.add(palette[i]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(getColorCompat(R.color.white));
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartIncome));
        data.setValueTextSize(11f);
        data.setValueTextColor(getColorCompat(R.color.white));

        pieChartIncome.setCenterText("All Time\nIncome");
        pieChartIncome.setData(data);
        pieChartIncome.highlightValues(null);
        pieChartIncome.invalidate();
    }

    private void updateBillsChartFromData() {
        if (pieChartBills == null) {
            return;
        }

        double paid = 0.0d;
        double unpaid = 0.0d;
        double overdue = 0.0d;
        long now = System.currentTimeMillis();

        for (Bill bill : latestBills) {
            long normalizedDueDate = normalizeEpochMillis(bill.getDueDate());
            if ("paid".equalsIgnoreCase(bill.getStatus())) {
                paid += bill.getAmount();
            } else if (normalizedDueDate > 0L && normalizedDueDate < now) {
                overdue += bill.getAmount();
            } else {
                unpaid += bill.getAmount();
            }
        }

        if (paid <= 0.0d && unpaid <= 0.0d && overdue <= 0.0d) {
            pieChartBills.clear();
            pieChartBills.setCenterText("No\nBills");
            pieChartBills.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (paid > 0.0d) {
            entries.add(new PieEntry((float) paid, "Paid"));
            colors.add(getColorCompat(R.color.success));
        }
        if (unpaid > 0.0d) {
            entries.add(new PieEntry((float) unpaid, "Upcoming"));
            colors.add(getColorCompat(R.color.info));
        }
        if (overdue > 0.0d) {
            entries.add(new PieEntry((float) overdue, "Overdue"));
            colors.add(getColorCompat(R.color.error));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(getColorCompat(R.color.white));
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartBills));
        data.setValueTextSize(11f);
        data.setValueTextColor(getColorCompat(R.color.white));

        pieChartBills.setCenterText("Bills\nStatus");
        pieChartBills.setData(data);
        pieChartBills.highlightValues(null);
        pieChartBills.invalidate();
    }

    private void updateChartLegendPercentages(Map<String, Double> grouped) {
        double total = 0.0d;
        for (double value : grouped.values()) {
            total += value;
        }

        setLegendPercent(textLegendHousingPercent, grouped.getOrDefault("Housing", 0.0d), total);
        setLegendPercent(textLegendFoodPercent, grouped.getOrDefault("Food", 0.0d), total);
        setLegendPercent(textLegendTransportPercent, grouped.getOrDefault("Transport", 0.0d), total);
        setLegendPercent(textLegendEntertainmentPercent, grouped.getOrDefault("Entertainment", 0.0d), total);
        setLegendPercent(textLegendOtherPercent, grouped.getOrDefault("Other", 0.0d), total);
    }

    private void setLegendPercent(TextView view, double amount, double total) {
        if (view == null) {
            return;
        }
        if (total <= 0.0d) {
            view.setText("0%");
            return;
        }
        int percent = (int) Math.round((amount / total) * 100.0d);
        view.setText(String.format(Locale.getDefault(), "%d%%", percent));
    }

    private void updateGoalCard() {
        if (cardGoal == null) {
            return;
        }

        if (latestGoals.isEmpty()) {
            textGoalName.setText("No savings goals yet");
            textGoalDeadline.setText("Create a goal to track progress");
            textGoalPercentage.setText("0%");
            textGoalCurrent.setText(formatMoney(0.0d));
            textGoalTarget.setText(formatMoney(0.0d));
            setGoalProgressWidth(0);
            return;
        }

        GoalSummary topGoal = latestGoals.get(0);
        int percent = topGoal.targetAmount <= 0 ? 0 : (int) Math.min(100, Math.round((topGoal.currentAmount / topGoal.targetAmount) * 100.0d));

        textGoalName.setText(topGoal.name);
        textGoalDeadline.setText(topGoal.targetDate > 0
                ? String.format(Locale.getDefault(), "Target: %1$tB %1$tY", topGoal.targetDate)
                : "Target date not set");
        textGoalPercentage.setText(String.format(Locale.getDefault(), "%d%%", percent));
        textGoalCurrent.setText(formatMoney(topGoal.currentAmount));
        textGoalTarget.setText(formatMoney(topGoal.targetAmount));
        setGoalProgressWidth(percent);
    }

    private void setGoalProgressWidth(int percent) {
        if (progressGoalView == null || progressGoalView.getParent() == null) {
            return;
        }
        View parent = (View) progressGoalView.getParent();
        parent.post(() -> {
            int trackWidth = parent.getWidth();
            int newWidth = Math.max(0, Math.min(trackWidth, (int) (trackWidth * (percent / 100f))));
            ViewGroup.LayoutParams params = progressGoalView.getLayoutParams();
            params.width = newWidth;
            progressGoalView.setLayoutParams(params);
        });
    }

    private void updateUpcomingBills() {
        List<Bill> sortedUpcoming = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Bill bill : latestBills) {
            if ("paid".equalsIgnoreCase(bill.getStatus())) {
                continue;
            }
            long normalizedDueDate = normalizeEpochMillis(bill.getDueDate());
            if (normalizedDueDate >= now - (24L * 60 * 60 * 1000)) {
                sortedUpcoming.add(bill);
            }
        }
        sortedUpcoming.sort(Comparator.comparingLong(Bill::getDueDate));

        bindBillCard(cardBill1, imageBill1Icon, textBill1Name, textBill1Due, textBill1Amount, sortedUpcoming, 0);
        bindBillCard(cardBill2, imageBill2Icon, textBill2Name, textBill2Due, textBill2Amount, sortedUpcoming, 1);
        bindBillCard(cardBill3, imageBill3Icon, textBill3Name, textBill3Due, textBill3Amount, sortedUpcoming, 2);
    }

    private void bindBillCard(View card, ImageView iconView, TextView nameView, TextView dueView, TextView amountView,
                              List<Bill> bills, int index) {
        if (card == null || nameView == null || dueView == null || amountView == null) {
            return;
        }

        if (index >= bills.size()) {
            card.setVisibility(View.GONE);
            return;
        }

        Bill bill = bills.get(index);
        card.setVisibility(View.VISIBLE);
        nameView.setText(bill.getName());
        dueView.setText(formatDueLabel(bill.getDueDate()));
        amountView.setText(formatMoney(bill.getAmount()));
        if (iconView != null) {
            DrawableUtils.safeSetImageResource(iconView, resolveBillIcon(bill), R.drawable.ic_receipt);
        }
    }

    private int resolveBillIcon(Bill bill) {
        if (bill.getCategoryIcon() != 0) {
            return bill.getCategoryIcon();
        }
        String normalized = bill.getCategory() == null ? "" : bill.getCategory().toLowerCase(Locale.ROOT);
        if (normalized.contains("electric")) {
            return R.drawable.ic_electricity;
        }
        if (normalized.contains("water")) {
            return R.drawable.ic_water;
        }
        if (normalized.contains("internet") || normalized.contains("mobile") || normalized.contains("wifi")) {
            return R.drawable.ic_wifi;
        }
        return R.drawable.ic_receipt;
    }

    private String formatDueLabel(long dueDate) {
        long normalizedDueDate = normalizeEpochMillis(dueDate);
        long diff = normalizedDueDate - System.currentTimeMillis();
        long days = diff / (24L * 60L * 60L * 1000L);
        if (days <= 0) {
            return "Due today";
        }
        if (days == 1) {
            return "Due tomorrow";
        }
        return String.format(Locale.getDefault(), "Due in %d days", days);
    }

    private double sumCurrentMonthExpenses() {
        Calendar now = Calendar.getInstance();
        return sumExpensesForMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH));
    }

    private double sumAllIncome() {
        double sum = 0.0d;
        for (IncomeEntry entry : latestIncomes) {
            sum += entry.getAmount();
        }
        return sum;
    }

    private double sumAllExpenses() {
        double sum = 0.0d;
        for (Expense expense : latestExpenses) {
            sum += expense.getAmount();
        }
        return sum;
    }

    private double sumIncomeForMonth(int year, int month) {
        double sum = 0.0d;
        for (IncomeEntry entry : latestIncomes) {
            long normalizedDate = normalizeEpochMillis(entry.getDate());
            if (normalizedDate <= 0L) {
                continue;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(normalizedDate);
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                sum += entry.getAmount();
            }
        }
        return sum;
    }

    private double sumExpensesForMonth(int year, int month) {
        double sum = 0.0d;
        for (Expense expense : latestExpenses) {
            long normalizedDate = normalizeEpochMillis(expense.getDate());
            if (normalizedDate <= 0L) {
                continue;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(normalizedDate);
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                sum += expense.getAmount();
            }
        }
        return sum;
    }

    private double sumUpcomingUnpaidBills() {
        double sum = 0.0d;
        long now = System.currentTimeMillis();
        for (Bill bill : latestBills) {
            long normalizedDueDate = normalizeEpochMillis(bill.getDueDate());
            if (!"paid".equalsIgnoreCase(bill.getStatus()) && normalizedDueDate >= now - (24L * 60L * 60L * 1000L)) {
                sum += bill.getAmount();
            }
        }
        return sum;
    }

    private double sumGoalCurrentAmounts() {
        double sum = 0.0d;
        for (GoalSummary goal : latestGoals) {
            sum += goal.currentAmount;
        }
        return sum;
    }

    private boolean isInCurrentMonth(long millis) {
        long normalized = normalizeEpochMillis(millis);
        if (normalized <= 0) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(normalized);
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && now.get(Calendar.MONTH) == target.get(Calendar.MONTH);
    }

    private long normalizeEpochMillis(long raw) {
        if (raw <= 0L) {
            return raw;
        }
        return raw < 1_000_000_000_000L ? raw * 1000L : raw;
    }

    private String normalizeExpenseCategory(String category) {
        if (category == null) {
            return "Other";
        }
        String value = category.toLowerCase(Locale.ROOT);
        if (value.contains("food") || value.contains("grocery")) {
            return "Food";
        }
        if (value.contains("transport") || value.contains("fuel")) {
            return "Transport";
        }
        if (value.contains("rent") || value.contains("housing") || value.contains("utilit")) {
            return "Housing";
        }
        if (value.contains("entertain")) {
            return "Entertainment";
        }
        return "Other";
    }

    private String normalizeIncomeSource(String source) {
        if (source == null) {
            return "Other";
        }
        String value = source.toLowerCase(Locale.ROOT);
        if (value.contains("salary") || value.contains("bonus")) {
            return "Salary";
        }
        if (value.contains("business") || value.contains("rental") || value.contains("investment")) {
            return "Business";
        }
        if (value.contains("freelance") || value.contains("part-time")) {
            return "Freelance";
        }
        return "Other";
    }

    private String formatMoney(double amount) {
        return String.format(Locale.getDefault(), "LKR %,.2f", amount);
    }

    private static String getString(QueryDocumentSnapshot doc, String key, String fallback) {
        String value = doc.getString(key);
        return value == null ? fallback : value;
    }

    private static long getLong(QueryDocumentSnapshot doc, String key, long fallback) {
        Long value = doc.getLong(key);
        return value == null ? fallback : value;
    }

    private static double getDouble(QueryDocumentSnapshot doc, String key, double fallback) {
        Double value = doc.getDouble(key);
        return value == null ? fallback : value;
    }

    private static class GoalSummary {
        final String name;
        final double targetAmount;
        final double currentAmount;
        final long targetDate;
        final long updatedAt;

        GoalSummary(String name, double targetAmount, double currentAmount, long targetDate, long updatedAt) {
            this.name = name;
            this.targetAmount = targetAmount;
            this.currentAmount = currentAmount;
            this.targetDate = targetDate;
            this.updatedAt = updatedAt;
        }
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDropdown(v);
            }
        });

        btnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notificationBadge != null) {
                    notificationBadge.setVisibility(View.GONE);
                }
                startActivity(new Intent(DashboardActivity.this, NotificationsActivity.class));
            }
        });

        // Profile avatar click listener - Navigate to ProfileActivity
        profileAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Expense
        actionAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddExpenseActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Income
        actionAddIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddIncomeActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Bill
        actionAddBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddBillActivity.class);
                startActivity(intent);
            }
        });

        // Quick Action: Add Goal
        actionAddGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddGoalActivity.class);
                startActivity(intent);
            }
        });

        // View All Bills button
        buttonViewAllBills.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, BillsActivity.class);
                startActivity(intent);
            }
        });

        // View All Goals button
        buttonViewAllGoals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, GoalsActivity.class);
                startActivity(intent);
            }
        });

        if (buttonToggleBalanceVisibility != null) {
            buttonToggleBalanceVisibility.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isBalanceVisible = !isBalanceVisible;
                    savePrivacyPreference();
                    applyBalancePrivacyState();
                    updateBalanceTrend(currentTotalBalance);
                }
            });
        }

    }

    /**
     * Show a small dropdown popup menu with logout option
     */
    private void showLogoutDropdown(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_logout, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_logout) {
                    showLogoutConfirmation();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * Show confirmation dialog before logging out
     */
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> handleLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Handle logout action - navigate back to LoginActivity
     */
    private void handleLogout() {
        authManager.signOut(this);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Require a double back press to exit only when Dashboard is the root screen.
     */
    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long now = System.currentTimeMillis();
                if (now - lastBackPressedAt < BACK_PRESS_EXIT_INTERVAL_MS) {
                    finishAffinity();
                    return;
                }

                lastBackPressedAt = now;
                Toast.makeText(DashboardActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
