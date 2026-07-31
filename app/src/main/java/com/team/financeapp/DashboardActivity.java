package com.team.financeapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;
import com.team.financeapp.auth.AuthManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.team.financeapp.notifications.FcmTokenUploader;
import com.team.financeapp.data.repository.GoalRepository;
import com.team.financeapp.data.repository.BillRepository;
import com.team.financeapp.data.repository.ExpenseRepository;
import com.team.financeapp.data.repository.IncomeRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.team.financeapp.IncomeEntry;

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
    private ImageView btnSyncStatus;
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
    private TextView textInsightAmount;
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
    private PieChart chartExpense;
    private PieChart chartIncome;
    private long lastBackPressedAt;
    private AuthManager authManager;
    private BillRepository billRepository;
    private ExpenseRepository expenseRepository;
    private IncomeRepository incomeRepository;
    private GoalRepository goalRepository;

    private View containerBills;
    private View emptyStateExpenses;
    private View emptyStateIncomes;
    private View emptyStateGoals;
    private View emptyStateBills;

    private List<Bill> latestBills = new ArrayList<>();
    private List<Expense> latestExpenses = new ArrayList<>();
    private List<GoalSummary> latestGoals = new ArrayList<>();
    private List<IncomeEntry> latestIncomes = new ArrayList<>();
    private double currentMonthIncome = 0.0d;
    private double currentTotalBalance = 0.0d;
    private double currentTotalExpenses = 0.0d;
    private boolean isBalanceVisible = true;

    // Month switcher state
    private int selectedYear;
    private int selectedMonth; // 0-based (Calendar.MONTH)
    private TextView textSelectedMonth;
    private View btnPrevMonth;
    private View btnNextMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        authManager = new AuthManager();
        billRepository = new BillRepository(this);
        expenseRepository = new ExpenseRepository(this);
        incomeRepository = new IncomeRepository(this);
        goalRepository = new GoalRepository(this);
        initializeViews();
        loadPrivacyPreference();
        ensureNotificationPermission();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_home);
        setupClickListeners();
        setupBackPressedCallback();
        loadDashboardData();
        
        // Refresh FCM Token on every app launch to ensure it's up to date
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> FcmTokenUploader.uploadToken(this, token));

        LocalBroadcastManager.getInstance(this).registerReceiver(logoutReceiver,
                new IntentFilter(com.team.financeapp.data.remote.ApiClient.ACTION_LOGOUT));
    }

    private final BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(DashboardActivity.this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            handleLogout();
        }
    };

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
        chartExpense = findViewById(R.id.chart_expense);
        chartIncome = findViewById(R.id.chart_income);
        dashboardWelcome = findViewById(R.id.dashboard_welcome);
        textTotalBalance = findViewById(R.id.text_total_balance);
        textIncomeAmount = findViewById(R.id.text_income_amount);
        textExpensesAmount = findViewById(R.id.text_expenses_amount);
        buttonToggleBalanceVisibility = findViewById(R.id.button_toggle_balance_visibility);
        textBalanceTrend = findViewById(R.id.text_balance_trend);
        textBalanceTrendCaption = findViewById(R.id.text_balance_trend_caption);
        textInsightAmount = findViewById(R.id.text_insight_amount);
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

        containerBills = findViewById(R.id.container_bills);
        emptyStateExpenses = findViewById(R.id.empty_state_expenses);
        emptyStateIncomes = findViewById(R.id.empty_state_incomes);
        emptyStateGoals = findViewById(R.id.empty_state_goals);
        emptyStateBills = findViewById(R.id.empty_state_bills);
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
        textBill3Amount = findViewById(R.id.text_bill_3_amount);

        // Month switcher
        Calendar now = Calendar.getInstance();
        selectedYear = now.get(Calendar.YEAR);
        selectedMonth = now.get(Calendar.MONTH);
        textSelectedMonth = findViewById(R.id.text_selected_month);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        updateMonthLabel();
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> navigateMonth(-1));
        }
        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> navigateMonth(1));
        }

        // Quick Action buttons
        actionAddExpense = findViewById(R.id.action_add_expense);
        actionAddIncome = findViewById(R.id.action_add_income);
        actionAddBill = findViewById(R.id.action_add_bill);
        actionAddGoal = findViewById(R.id.action_add_goal);
        
        View actionBudget = findViewById(R.id.action_budget);
        if (actionBudget != null) {
            actionBudget.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, BudgetActivity.class));
            });
        }
        
        View actionForecast = findViewById(R.id.action_forecast);
        if (actionForecast != null) {
            actionForecast.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, ForecastActivity.class));
            });
        }

        View fabChatbot = findViewById(R.id.fab_chatbot);
        if (fabChatbot != null) {
            fabChatbot.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, com.team.financeapp.chatbot.ChatbotActivity.class));
            });
        }
        
        View cardEducation = findViewById(R.id.card_education);
        if (cardEducation != null) {
            cardEducation.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, EducationActivity.class));
            });
        }

        btnNotifications = findViewById(R.id.btn_notifications);
        btnSyncStatus = findViewById(R.id.btn_sync_status);
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


    private void loadDashboardData() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindUserHeader();
        
        // Reset counter just in case
        pendingNetworkRequests = 0;
        
        loadBills(userId);
        loadExpenses(userId);
        loadGoals(userId);
        loadIncome(userId);
    }

    private int pendingNetworkRequests = 0;

    private void setNetworkLoading(boolean isLoading) {
        runOnUiThread(() -> {
            if (btnSyncStatus == null) return;
            if (isLoading) {
                pendingNetworkRequests++;
                if (pendingNetworkRequests == 1) {
                    btnSyncStatus.setImageResource(R.drawable.ic_sync);
                    btnSyncStatus.setColorFilter(getColorCompat(R.color.accent));
                    ObjectAnimator rotation = ObjectAnimator.ofFloat(btnSyncStatus, "rotation", 0f, 360f);
                    rotation.setDuration(1000);
                    rotation.setRepeatCount(ObjectAnimator.INFINITE);
                    rotation.setInterpolator(new android.view.animation.LinearInterpolator());
                    rotation.start();
                    btnSyncStatus.setTag(rotation);
                }
            } else {
                pendingNetworkRequests--;
                if (pendingNetworkRequests <= 0) {
                    pendingNetworkRequests = 0;
                    ObjectAnimator rotation = (ObjectAnimator) btnSyncStatus.getTag();
                    if (rotation != null) {
                        rotation.cancel();
                        btnSyncStatus.setTag(null);
                    }
                    btnSyncStatus.setRotation(0f);
                    btnSyncStatus.setImageResource(R.drawable.ic_check);
                    btnSyncStatus.setColorFilter(getColorCompat(R.color.success));
                }
            }
        });
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
        setNetworkLoading(true);
        billRepository.loadBills(userId, new BillRepository.LoadBillsCallback() {
            @Override
            public void onBillsLoaded(List<Bill> bills) {
                setNetworkLoading(false);
                latestBills = new ArrayList<>(bills);
                updateUpcomingBills();
                updateBillsChartFromData();
                updateDashboardTotalsAndInsight();
            }

            @Override
            public void onError(String message) {
                setNetworkLoading(false);
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExpenses(String userId) {
        setNetworkLoading(true);
        expenseRepository.loadExpenses(userId, new ExpenseRepository.LoadExpensesCallback() {
            @Override
            public void onExpensesLoaded(List<Expense> expenses) {
                setNetworkLoading(false);
                latestExpenses = new ArrayList<>(expenses);
                updateExpenseChartFromData();
                updateDashboardTotalsAndInsight();
            }

            @Override
            public void onError(String message) {
                setNetworkLoading(false);
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGoals(String userId) {
        setNetworkLoading(true);
        goalRepository.loadGoals(userId, new GoalRepository.LoadGoalsCallback() {
            @Override
            public void onGoalsLoaded(List<Goal> loadedGoals) {
                setNetworkLoading(false);
                List<GoalSummary> goals = new ArrayList<>();
                for (Goal g : loadedGoals) {
                    goals.add(new GoalSummary(
                            g.getName(),
                            g.getTargetAmount(),
                            g.getCurrentAmount(),
                            g.getAddedSavingsAmount(),
                            g.getTargetDate(),
                            System.currentTimeMillis() // Or a real updated_at if Goal had it
                    ));
                }
                latestGoals = goals;
                updateGoalCard();
                updateDashboardTotalsAndInsight();
            }

            @Override
            public void onError(String message) {
                setNetworkLoading(false);
                latestGoals = Collections.emptyList();
                updateGoalCard();
                updateDashboardTotalsAndInsight();
            }
        });
    }

    private void loadIncome(String userId) {
        setNetworkLoading(true);
        incomeRepository.loadIncome(userId, new IncomeRepository.LoadIncomeCallback() {
            @Override
            public void onIncomeLoaded(List<IncomeEntry> incomes) {
                setNetworkLoading(false);
                latestIncomes = new ArrayList<>(incomes);
                updateIncomeChartFromData();
                updateDashboardTotalsAndInsight();
            }

            @Override
            public void onError(String message) {
                setNetworkLoading(false);
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateMonth(int delta) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth);
        cal.add(Calendar.MONTH, delta);
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH);
        updateMonthLabel();
        updateDashboardTotalsAndInsight();
    }

    private void updateMonthLabel() {
        if (textSelectedMonth == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selectedYear);
        cal.set(Calendar.MONTH, selectedMonth);
        String label = new java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
        textSelectedMonth.setText(label);
        // Disable next-month button if we're already at the current month
        Calendar now = Calendar.getInstance();
        boolean isCurrentOrFuture = (selectedYear > now.get(Calendar.YEAR)) ||
                (selectedYear == now.get(Calendar.YEAR) && selectedMonth >= now.get(Calendar.MONTH));
        if (btnNextMonth != null) btnNextMonth.setAlpha(isCurrentOrFuture ? 0.3f : 1.0f);
        if (btnNextMonth != null) btnNextMonth.setEnabled(!isCurrentOrFuture);
    }

    private void updateDashboardTotalsAndInsight() {
        double monthlyIncome = sumIncomeForMonth(selectedYear, selectedMonth);
        double monthlyExpenses = sumExpensesForMonth(selectedYear, selectedMonth);

        // Filter paid bills for the selected month
        double monthlyPaidBills = 0.0d;
        for (Bill bill : latestBills) {
            if ("paid".equalsIgnoreCase(bill.getStatus())) {
                long normalizedDate = normalizeEpochMillis(bill.getDueDate());
                if (normalizedDate > 0L) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(normalizedDate);
                    if (cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth) {
                        monthlyPaidBills += bill.getAmount();
                    }
                }
            }
        }

        double totalCashOut = monthlyExpenses + monthlyPaidBills;
        currentTotalExpenses = totalCashOut;
        currentMonthIncome = monthlyIncome;

        // Balance = income - (expenses + paid bills)
        double totalBalance = currentMonthIncome - currentTotalExpenses;
        currentTotalBalance = totalBalance;
        applyBalancePrivacyState();

        // Balance trend is always vs the month before the selected month
        updateBalanceTrend(totalBalance);

        // Insight uses due (unpaid) bills only for the current month
        Calendar now = Calendar.getInstance();
        boolean viewingCurrentMonth = (selectedYear == now.get(Calendar.YEAR) && selectedMonth == now.get(Calendar.MONTH));
        double dueBillsTotal = viewingCurrentMonth ? sumDueBills() : 0.0d;
        double insightBalance = currentMonthIncome - totalCashOut - dueBillsTotal;
        if (textInsightAmount != null) {
            if (isBalanceVisible) {
                textInsightAmount.setVisibility(View.VISIBLE);
                textInsightAmount.setText(formatMoney(insightBalance));
                textInsightAmount.setTextColor(getColorCompat(insightBalance >= 0 ? R.color.success : R.color.error));
            } else {
                textInsightAmount.setVisibility(View.GONE);
            }
        }

        if (textAlertMessage != null) {
            if (totalCashOut <= 0.0d && dueBillsTotal <= 0.0d) {
                textAlertMessage.setText("No cash-out activity or due bills yet. Add expenses or pay bills to see your insight.");
            } else if (currentMonthIncome <= 0.0d) {
                textAlertMessage.setText("You've recorded spending and bills, but no income entries yet. Add income to see your remaining balance.");
            } else {
                if (insightBalance >= 0) {
                    textAlertMessage.setText(String.format(Locale.getDefault(),
                            "After cash out and due bills, you still have %s available. Nice buffer.",
                            formatMoney(insightBalance)));
                } else {
                    textAlertMessage.setText(String.format(Locale.getDefault(),
                            "After cash out and due bills, you're short by %s. Review bills or spending to close the gap.",
                            formatMoney(Math.abs(insightBalance))));
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

        // Compare vs the month before the currently selected month
        Calendar previous = Calendar.getInstance();
        previous.set(Calendar.YEAR, selectedYear);
        previous.set(Calendar.MONTH, selectedMonth);
        previous.add(Calendar.MONTH, -1);
        int prevYear = previous.get(Calendar.YEAR);
        int prevMonth = previous.get(Calendar.MONTH);

        double previousIncome = sumIncomeForMonth(prevYear, prevMonth);
        double previousExpenses = sumExpensesForMonth(prevYear, prevMonth);
        
        // Also subtract paid bills for previous month
        double previousPaidBills = 0.0d;
        for (Bill bill : latestBills) {
            if ("paid".equalsIgnoreCase(bill.getStatus())) {
                long normalizedDate = normalizeEpochMillis(bill.getDueDate());
                if (normalizedDate > 0L) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(normalizedDate);
                    if (cal.get(Calendar.YEAR) == prevYear && cal.get(Calendar.MONTH) == prevMonth) {
                        previousPaidBills += bill.getAmount();
                    }
                }
            }
        }

        double previousNet = previousIncome - (previousExpenses + previousPaidBills);

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
        if (chartExpense == null) return;

        Map<String, Double> grouped = new HashMap<>();
        double total = 0.0d;
        for (Expense expense : latestExpenses) {
            String key = normalizeExpenseCategory(expense.getCategory());
            double amount = expense.getAmount();
            grouped.put(key, grouped.getOrDefault(key, 0.0d) + amount);
            total += amount;
        }

        if (total <= 0.0d) {
            chartExpense.clear();
            findViewById(R.id.card_expense_chart).setVisibility(View.GONE);
            if (emptyStateExpenses != null) {
                emptyStateExpenses.setVisibility(View.VISIBLE);
                ((TextView) emptyStateExpenses.findViewById(R.id.text_empty_title)).setText("No Expenses Yet");
                ((TextView) emptyStateExpenses.findViewById(R.id.text_empty_message)).setText("Your expenses will appear here once you start spending.");
                ((ImageView) emptyStateExpenses.findViewById(R.id.image_empty_state)).setImageResource(R.drawable.ic_expenses);
            }
            return;
        } else {
            findViewById(R.id.card_expense_chart).setVisibility(View.VISIBLE);
            if (emptyStateExpenses != null) emptyStateExpenses.setVisibility(View.GONE);
        }

        String[] order = new String[]{"Housing", "Food", "Transport", "Entertainment", "Other"};
        int[] icons = new int[]{R.drawable.ic_home, R.drawable.ic_receipt, R.drawable.ic_car, R.drawable.ic_expenses, R.drawable.ic_wallet};
        int[] palette = new int[]{
                getColorCompat(R.color.primary),
                getColorCompat(R.color.success),
                getColorCompat(R.color.accent),
                getColorCompat(R.color.info),
                getColorCompat(R.color.dashboard_chart_other)
        };

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (int i = 0; i < order.length; i++) {
            double amount = grouped.getOrDefault(order[i], 0.0d);
            if (amount <= 0.0d) continue;
            
            entries.add(new PieEntry((float) amount, order[i], ContextCompat.getDrawable(this, icons[i])));
            colors.add(palette[i]);
        }

        setupPieChart(chartExpense, entries, colors, "Expenses");

        chartExpense.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof PieEntry) {
                    String category = ((PieEntry) e).getLabel();
                    showCategoryTransactionsDialog(category);
                }
            }
            @Override
            public void onNothingSelected() {}
        });
    }

    private void showCategoryTransactionsDialog(String category) {
        List<String> items = new ArrayList<>();
        double total = 0.0;
        for (Expense e : latestExpenses) {
            if (normalizeExpenseCategory(e.getCategory()).equals(category)) {
                String dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date((long)e.getDate()));
                String desc = (e.getDescription() != null && !e.getDescription().trim().isEmpty()) ? " - " + e.getDescription() : "";
                items.add(String.format(Locale.getDefault(), "%s%s: %s", dateStr, desc, formatMoney(e.getAmount())));
                total += e.getAmount();
            }
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(category + " Expenses (" + formatMoney(total) + ")")
            .setItems(items.toArray(new String[0]), null)
            .setPositiveButton("Close", null)
            .show();
    }

    private void updateIncomeChartFromData() {
        if (chartIncome == null) return;

        Map<String, Double> grouped = new HashMap<>();
        double total = 0.0d;
        for (IncomeEntry entry : latestIncomes) {
            long normalizedDate = com.team.financeapp.utils.DateUtils.normalizeEpochMillis(entry.getDate());
            if (normalizedDate <= 0L) continue;
            
            String key = normalizeIncomeSource(entry.getSource());
            double amount = entry.getAmount();
            grouped.put(key, grouped.getOrDefault(key, 0.0d) + amount);
            total += amount;
        }

        if (total <= 0.0d) {
            chartIncome.clear();
            findViewById(R.id.card_income_chart).setVisibility(View.GONE);
            if (emptyStateIncomes != null) {
                emptyStateIncomes.setVisibility(View.VISIBLE);
                ((TextView) emptyStateIncomes.findViewById(R.id.text_empty_title)).setText("No Income Yet");
                ((TextView) emptyStateIncomes.findViewById(R.id.text_empty_message)).setText("Your incomes will be visualized here.");
                ((ImageView) emptyStateIncomes.findViewById(R.id.image_empty_state)).setImageResource(R.drawable.ic_wallet);
            }
            return;
        } else {
            findViewById(R.id.card_income_chart).setVisibility(View.VISIBLE);
            if (emptyStateIncomes != null) emptyStateIncomes.setVisibility(View.GONE);
        }

        String[] order = new String[]{"Salary", "Business", "Freelance", "Other"};
        int[] icons = new int[]{R.drawable.ic_wallet, R.drawable.ic_savings, R.drawable.ic_laptop, R.drawable.ic_receipt};
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
            if (amount <= 0.0d) continue;

            entries.add(new PieEntry((float) amount, order[i], ContextCompat.getDrawable(this, icons[i])));
            colors.add(palette[i]);
        }

        setupPieChart(chartIncome, entries, colors, "Income");

        chartIncome.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof PieEntry) {
                    String category = ((PieEntry) e).getLabel();
                    showIncomeCategoryDialog(category);
                }
            }
            @Override
            public void onNothingSelected() {}
        });
    }

    private void showIncomeCategoryDialog(String category) {
        List<String> items = new ArrayList<>();
        double total = 0.0;
        for (IncomeEntry i : latestIncomes) {
            if (i.getSource().equals(category)) {
                String dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date((long)i.getDate()));
                items.add(String.format(Locale.getDefault(), "%s: %s", dateStr, formatMoney(i.getAmount())));
                total += i.getAmount();
            }
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(category + " Income (" + formatMoney(total) + ")")
            .setItems(items.toArray(new String[0]), null)
            .setPositiveButton("Close", null)
            .show();
    }

    private void setupPieChart(PieChart chart, List<PieEntry> entries, List<Integer> colors, String label) {
        com.team.financeapp.utils.ChartHelper.setupPieChart(chart, entries, colors, label, getColorCompat(R.color.text_primary));
    }


    private void updateGoalsChartFromData() {
        // Implementation for goals chart update (omitted for brevity)
    }


    private void updateBillsChartFromData() {
        // Bills chart was removed
    }

    private void updateChartLegendPercentages(Map<String, Double> grouped) {
        // Obsolete
    }

    private void setLegendPercent(TextView view, double amount, double total) {
        // Obsolete
    }

    private void updateGoalCard() {
        if (cardGoal == null) {
            return;
        }

        if (latestGoals.isEmpty()) {
            cardGoal.setVisibility(View.GONE);
            if (emptyStateGoals != null) {
                emptyStateGoals.setVisibility(View.VISIBLE);
                ((TextView) emptyStateGoals.findViewById(R.id.text_empty_title)).setText("No Savings Goals");
                ((TextView) emptyStateGoals.findViewById(R.id.text_empty_message)).setText("Create a goal to start tracking your savings.");
                ((ImageView) emptyStateGoals.findViewById(R.id.image_empty_state)).setImageResource(R.drawable.ic_target);
            }
            return;
        } else {
            cardGoal.setVisibility(View.VISIBLE);
            if (emptyStateGoals != null) emptyStateGoals.setVisibility(View.GONE);
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
            long normalizedDueDate = com.team.financeapp.utils.DateUtils.normalizeEpochMillis(bill.getDueDate());
            if (normalizedDueDate >= now - (24L * 60 * 60 * 1000)) {
                sortedUpcoming.add(bill);
            }
        }
        sortedUpcoming.sort(Comparator.comparingLong(Bill::getDueDate));

        if (sortedUpcoming.isEmpty()) {
            if (containerBills != null) containerBills.setVisibility(View.GONE);
            if (emptyStateBills != null) {
                emptyStateBills.setVisibility(View.VISIBLE);
                ((TextView) emptyStateBills.findViewById(R.id.text_empty_title)).setText("No Upcoming Bills");
                ((TextView) emptyStateBills.findViewById(R.id.text_empty_message)).setText("You are all caught up! Enjoy your peace of mind.");
                ((ImageView) emptyStateBills.findViewById(R.id.image_empty_state)).setImageResource(R.drawable.ic_electricity);
            }
        } else {
            if (containerBills != null) containerBills.setVisibility(View.VISIBLE);
            if (emptyStateBills != null) emptyStateBills.setVisibility(View.GONE);
            bindBillCard(cardBill1, imageBill1Icon, textBill1Name, textBill1Due, textBill1Amount, sortedUpcoming, 0);
            bindBillCard(cardBill2, imageBill2Icon, textBill2Name, textBill2Due, textBill2Amount, sortedUpcoming, 1);
            bindBillCard(cardBill3, imageBill3Icon, textBill3Name, textBill3Due, textBill3Amount, sortedUpcoming, 2);
        }
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
        long days = daysFromToday(normalizedDueDate);
        if (days == 0) {
            return "Due today";
        }
        if (days == 1) {
            return "Due tomorrow";
        }
        if (days == -1) {
            return "Overdue by 1 day";
        }
        if (days < 0) {
            return String.format(Locale.getDefault(), "Overdue by %d days", Math.abs(days));
        }
        return String.format(Locale.getDefault(), "Due in %d days", days);
    }

    private long daysFromToday(long targetMillis) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(targetMillis);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long diff = target.getTimeInMillis() - today.getTimeInMillis();
        return diff / (24L * 60L * 60L * 1000L);
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

    private double sumPaidBills() {
        double sum = 0.0d;
        for (Bill bill : latestBills) {
            if ("paid".equalsIgnoreCase(bill.getStatus())) {
                sum += bill.getAmount();
            }
        }
        return sum;
    }

    private double sumDueBills() {
        double sum = 0.0d;
        for (Bill bill : latestBills) {
            if (!"paid".equalsIgnoreCase(bill.getStatus())) {
                sum += bill.getAmount();
            }
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

    private double sumGoalAddedSavings() {
        double sum = 0.0d;
        if (latestGoals != null) {
            for (GoalSummary goal : latestGoals) {
                sum += goal.currentAmount;
            }
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

    private static class GoalSummary {
        final String name;
        final double targetAmount;
        final double currentAmount;
        final double addedSavingsAmount;
        final long targetDate;
        final long updatedAt;

        GoalSummary(String name, double targetAmount, double currentAmount, double addedSavingsAmount, long targetDate, long updatedAt) {
            this.name = name;
            this.targetAmount = targetAmount;
            this.currentAmount = currentAmount;
            this.addedSavingsAmount = addedSavingsAmount;
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
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(logoutReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
