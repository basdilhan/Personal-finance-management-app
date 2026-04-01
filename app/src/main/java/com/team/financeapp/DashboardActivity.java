package com.team.financeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private MaterialButton btnLogout;
    private View actionAddExpense, actionAddIncome, actionAddBill, actionAddGoal;
    private View btnNotifications;
    private View notificationBadge;
    private TextView buttonViewAllBills;
    private TextView buttonViewAllGoals;
    private TextView dashboardWelcome;
    private TextView textTotalBalance;
    private TextView textIncomeAmount;
    private TextView textExpensesAmount;
    private TextView textAlertMessage;
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
    private long lastBackPressedAt;
    private AuthManager authManager;
    private BillRepository billRepository;
    private ExpenseRepository expenseRepository;
    private FirebaseFirestore firestore;

    private List<Bill> latestBills = new ArrayList<>();
    private List<Expense> latestExpenses = new ArrayList<>();
    private List<GoalSummary> latestGoals = new ArrayList<>();
    private double currentMonthIncome = 0.0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        authManager = new AuthManager();
        billRepository = new BillRepository(this);
        expenseRepository = new ExpenseRepository(this);
        firestore = FirebaseFirestore.getInstance();
        initializeViews();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_home);
        setupClickListeners();
        setupBackPressedCallback();
        setupPieChart();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationFragment.attach(this, R.id.bottom_navigation_container, R.id.nav_home);
        loadDashboardData();
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
        dashboardWelcome = findViewById(R.id.dashboard_welcome);
        textTotalBalance = findViewById(R.id.text_total_balance);
        textIncomeAmount = findViewById(R.id.text_income_amount);
        textExpensesAmount = findViewById(R.id.text_expenses_amount);
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

        // Initialize Quick Action buttons
        actionAddExpense = findViewById(R.id.action_add_expense);
        actionAddIncome = findViewById(R.id.action_add_income);
        actionAddBill = findViewById(R.id.action_add_bill);
        actionAddGoal = findViewById(R.id.action_add_goal);
        btnNotifications = findViewById(R.id.btn_notifications);
        notificationBadge = findViewById(R.id.notification_badge);
    }

    private int getColorCompat(@ColorRes int colorResId) {
        return ContextCompat.getColor(this, colorResId);
    }

    /**
     * Setup the expense categories pie chart
     */
    private void setupPieChart() {
        if (pieChartExpenses == null) return;

        pieChartExpenses.setUsePercentValues(true);
        pieChartExpenses.getDescription().setEnabled(false);
        pieChartExpenses.setExtraOffsets(12f, 8f, 12f, 8f);
        pieChartExpenses.setDragDecelerationFrictionCoef(0.95f);
        pieChartExpenses.setDrawHoleEnabled(true);
        pieChartExpenses.setHoleColor(getColorCompat(R.color.dashboard_chart_hole));
        pieChartExpenses.setTransparentCircleColor(getColorCompat(R.color.dashboard_chart_hole));
        pieChartExpenses.setTransparentCircleAlpha(24);
        pieChartExpenses.setHoleRadius(58f);
        pieChartExpenses.setTransparentCircleRadius(63f);
        pieChartExpenses.setDrawCenterText(true);
        pieChartExpenses.setCenterText("Monthly\nExpenses");
        pieChartExpenses.setCenterTextSize(15f);
        pieChartExpenses.setCenterTextColor(getColorCompat(R.color.dashboard_chart_center));
        pieChartExpenses.setRotationEnabled(false);
        pieChartExpenses.setHighlightPerTapEnabled(true);
        pieChartExpenses.setDrawEntryLabels(false);

        Legend legend = pieChartExpenses.getLegend();
        legend.setEnabled(false);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(45f));
        entries.add(new PieEntry(25f));
        entries.add(new PieEntry(15f));
        entries.add(new PieEntry(10f));
        entries.add(new PieEntry(5f));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(
                getColorCompat(R.color.primary),
                getColorCompat(R.color.success),
                getColorCompat(R.color.accent),
                getColorCompat(R.color.info),
                getColorCompat(R.color.dashboard_chart_other)
        );

        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getColorCompat(R.color.white));
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartExpenses));
        data.setValueTextSize(12f);
        data.setValueTextColor(getColorCompat(R.color.white));

        pieChartExpenses.setData(data);
        pieChartExpenses.highlightValues(null);
        pieChartExpenses.invalidate();
        pieChartExpenses.animateY(900);
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
        firestore.collection("incomes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double total = 0.0d;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        long date = getLong(doc, "date", 0L);
                        if (isInCurrentMonth(date)) {
                            total += getDouble(doc, "amount", 0.0d);
                        }
                    }
                    currentMonthIncome = total;
                    updateDashboardTotalsAndInsight();
                })
                .addOnFailureListener(e -> {
                    currentMonthIncome = 0.0d;
                    updateDashboardTotalsAndInsight();
                });
    }

    private void updateDashboardTotalsAndInsight() {
        double monthlyExpenses = sumCurrentMonthExpenses();
        double totalUpcomingBills = sumUpcomingUnpaidBills();
        double totalGoalSaved = sumGoalCurrentAmounts();

        if (textIncomeAmount != null) {
            textIncomeAmount.setText(formatMoney(currentMonthIncome));
        }
        if (textExpensesAmount != null) {
            textExpensesAmount.setText(formatMoney(monthlyExpenses));
        }

        double totalBalance = currentMonthIncome + totalGoalSaved - monthlyExpenses - totalUpcomingBills;
        if (textTotalBalance != null) {
            textTotalBalance.setText(formatMoney(totalBalance));
        }

        if (textAlertMessage != null) {
            if (monthlyExpenses <= 0.0d && totalUpcomingBills <= 0.0d) {
                textAlertMessage.setText("No expenses or bills yet for this account. Add entries to unlock personalized insights.");
            } else if (currentMonthIncome <= 0.0d) {
                textAlertMessage.setText("You've recorded spending, but no income entries yet. Add income to track net monthly balance.");
            } else {
                double net = currentMonthIncome - monthlyExpenses;
                if (net >= 0) {
                    textAlertMessage.setText(String.format(Locale.getDefault(), "Great job. Your net for this month is %s after expenses.", formatMoney(net)));
                } else {
                    textAlertMessage.setText(String.format(Locale.getDefault(), "Heads up: this month is %s over income. Consider reducing upcoming spending.", formatMoney(Math.abs(net))));
                }
            }
        }
    }

    private void updateExpenseChartFromData() {
        if (pieChartExpenses == null) {
            return;
        }

        Map<String, Double> grouped = new HashMap<>();
        for (Expense expense : latestExpenses) {
            if (!isInCurrentMonth(expense.getDate())) {
                continue;
            }
            String key = normalizeExpenseCategory(expense.getCategory());
            grouped.put(key, grouped.getOrDefault(key, 0.0d) + expense.getAmount());
        }

        if (grouped.isEmpty()) {
            pieChartExpenses.clear();
            pieChartExpenses.setCenterText("No\nExpenses");
            pieChartExpenses.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int colorIndex = 0;
        int[] palette = new int[]{
                getColorCompat(R.color.primary),
                getColorCompat(R.color.success),
                getColorCompat(R.color.accent),
                getColorCompat(R.color.info),
                getColorCompat(R.color.dashboard_chart_other)
        };

        for (Map.Entry<String, Double> entry : grouped.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            colors.add(palette[colorIndex % palette.length]);
            colorIndex++;
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

        pieChartExpenses.setCenterText("Monthly\nExpenses");
        pieChartExpenses.setData(data);
        pieChartExpenses.highlightValues(null);
        pieChartExpenses.invalidate();
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
            if (bill.getDueDate() >= now - (24L * 60 * 60 * 1000)) {
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
            iconView.setImageResource(resolveBillIcon(bill));
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
        long diff = dueDate - System.currentTimeMillis();
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
        double sum = 0.0d;
        for (Expense expense : latestExpenses) {
            if (isInCurrentMonth(expense.getDate())) {
                sum += expense.getAmount();
            }
        }
        return sum;
    }

    private double sumUpcomingUnpaidBills() {
        double sum = 0.0d;
        for (Bill bill : latestBills) {
            if (!"paid".equalsIgnoreCase(bill.getStatus())) {
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
        if (millis <= 0) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && now.get(Calendar.MONTH) == target.get(Calendar.MONTH);
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
