package com.team.financeapp.data.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.BillDao;
import com.team.financeapp.data.local.dao.ExpenseDao;
import com.team.financeapp.data.local.dao.IncomeDao;
import com.team.financeapp.data.local.entity.BillEntity;
import com.team.financeapp.data.local.entity.ExpenseEntity;
import com.team.financeapp.data.local.entity.IncomeEntity;
import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.BillApiService;
import com.team.financeapp.data.remote.ExpenseApiService;
import com.team.financeapp.data.remote.IncomeApiService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

public class PendingSyncWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "pending_finance_sync";

    private static final String TAG = "PendingSyncWorker";

    public PendingSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Log.d(TAG, "Skipping sync because there is no signed-in user");
            return Result.success();
        }

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        BillDao billDao = db.billDao();
        ExpenseDao expenseDao = db.expenseDao();
        IncomeDao incomeDao = db.incomeDao();
        
        BillApiService billApiService = ApiClient.getClient().create(BillApiService.class);
        ExpenseApiService expenseApiService = ApiClient.getClient().create(ExpenseApiService.class);
        IncomeApiService incomeApiService = ApiClient.getClient().create(IncomeApiService.class);

        boolean hasFailures = false;
        int syncedCount = 0;

        List<BillEntity> pendingBills = billDao.getPendingSync(userId);
        for (BillEntity bill : pendingBills) {
            if (syncBill(billApiService, bill)) {
                syncedCount++;
                continue;
            }
            hasFailures = true;
            bill.syncState = SyncState.FAILED;
            bill.updatedAt = System.currentTimeMillis();
            billDao.update(bill);
        }

        List<ExpenseEntity> pendingExpenses = expenseDao.getPendingSync(userId);
        for (ExpenseEntity expense : pendingExpenses) {
            if (syncExpense(expenseApiService, expense)) {
                syncedCount++;
                continue;
            }
            hasFailures = true;
            expense.syncState = SyncState.FAILED;
            expense.updatedAt = System.currentTimeMillis();
            expenseDao.update(expense);
        }

        List<IncomeEntity> pendingIncomes = incomeDao.getPendingSync(userId);
        for (IncomeEntity income : pendingIncomes) {
            if (syncIncome(incomeApiService, income)) {
                syncedCount++;
                continue;
            }
            hasFailures = true;
            income.syncState = SyncState.FAILED;
            income.updatedAt = System.currentTimeMillis();
            incomeDao.update(income);
        }

        Log.d(TAG, "Sync run finished. Synced records=" + syncedCount + ", hadFailures=" + hasFailures);
        return hasFailures ? Result.retry() : Result.success();
    }

    private static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return null;
        }
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private boolean syncBill(BillApiService apiService, BillEntity bill) {
        try {
            int backendId = 0;
            try {
                backendId = Integer.parseInt(bill.remoteId);
            } catch (NumberFormatException ignored) {}

            Response<BillEntity> response;
            if (backendId > 0 && !bill.deleted) {
                response = apiService.updateBill(backendId, bill).execute();
            } else if (bill.deleted) {
                Response<Void> deleteResponse = apiService.deleteBill(backendId).execute();
                if (deleteResponse.isSuccessful()) {
                    bill.syncState = SyncState.SYNCED;
                    bill.updatedAt = System.currentTimeMillis();
                    AppDatabase.getInstance(getApplicationContext()).billDao().update(bill);
                    return true;
                }
                return false;
            } else {
                response = apiService.createBill(bill).execute();
            }

            if (response.isSuccessful() && response.body() != null) {
                bill.syncState = SyncState.SYNCED;
                bill.updatedAt = System.currentTimeMillis();
                bill.remoteId = String.valueOf(response.body().localId);
                AppDatabase.getInstance(getApplicationContext()).billDao().update(bill);
                return true;
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Bill sync failed for remoteId=" + bill.remoteId, e);
            return false;
        }
    }

    private boolean syncExpense(ExpenseApiService apiService, ExpenseEntity expense) {
        try {
            int backendId = 0;
            try {
                backendId = Integer.parseInt(expense.remoteId);
            } catch (NumberFormatException ignored) {}

            Response<ExpenseEntity> response;
            if (backendId > 0 && !expense.deleted) {
                response = apiService.updateExpense(backendId, expense).execute();
            } else if (expense.deleted) {
                Response<Void> deleteResponse = apiService.deleteExpense(backendId).execute();
                if (deleteResponse.isSuccessful()) {
                    expense.syncState = SyncState.SYNCED;
                    expense.updatedAt = System.currentTimeMillis();
                    AppDatabase.getInstance(getApplicationContext()).expenseDao().update(expense);
                    return true;
                }
                return false;
            } else {
                response = apiService.createExpense(expense).execute();
            }

            if (response.isSuccessful() && response.body() != null) {
                expense.syncState = SyncState.SYNCED;
                expense.updatedAt = System.currentTimeMillis();
                expense.remoteId = String.valueOf(response.body().localId);
                AppDatabase.getInstance(getApplicationContext()).expenseDao().update(expense);
                return true;
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Expense sync failed for remoteId=" + expense.remoteId, e);
            return false;
        }
    }

    private boolean syncIncome(IncomeApiService apiService, IncomeEntity income) {
        try {
            int backendId = 0;
            try {
                backendId = Integer.parseInt(income.remoteId);
            } catch (NumberFormatException ignored) {}

            Response<IncomeEntity> response;
            if (backendId > 0 && !income.deleted) {
                response = apiService.updateIncome(backendId, income).execute();
            } else if (income.deleted) {
                Response<Void> deleteResponse = apiService.deleteIncome(backendId).execute();
                if (deleteResponse.isSuccessful()) {
                    income.syncState = SyncState.SYNCED;
                    income.updatedAt = System.currentTimeMillis();
                    AppDatabase.getInstance(getApplicationContext()).incomeDao().update(income);
                    return true;
                }
                return false;
            } else {
                response = apiService.createIncome(income).execute();
            }

            if (response.isSuccessful() && response.body() != null) {
                income.syncState = SyncState.SYNCED;
                income.updatedAt = System.currentTimeMillis();
                income.remoteId = String.valueOf(response.body().localId);
                AppDatabase.getInstance(getApplicationContext()).incomeDao().update(income);
                return true;
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Income sync failed for remoteId=" + income.remoteId, e);
            return false;
        }
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                PendingSyncWorker.class,
                15,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
