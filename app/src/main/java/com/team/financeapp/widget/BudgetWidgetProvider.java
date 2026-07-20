package com.team.financeapp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.team.financeapp.AddExpenseActivity;
import com.team.financeapp.AddIncomeActivity;
import com.team.financeapp.R;

public class BudgetWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_budget);

        // Open Add Expense
        Intent expenseIntent = new Intent(context, AddExpenseActivity.class);
        PendingIntent expensePendingIntent = PendingIntent.getActivity(context, 0, expenseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_add_expense, expensePendingIntent);

        // Open Add Income
        Intent incomeIntent = new Intent(context, AddIncomeActivity.class);
        PendingIntent incomePendingIntent = PendingIntent.getActivity(context, 1, incomeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_add_income, incomePendingIntent);

        // Calculate and set balance
        // Note: For a real implementation, we would query the local DB asynchronously 
        // or through a Service and update the RemoteViews. For now we just show a static hint.
        views.setTextViewText(R.id.widget_text_balance, "Open App to Sync");

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
