package com.team.financeapp;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.BillRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity for adding or editing recurring bills
 */
public class AddBillActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_ID = "extra_bill_id";
    public static final String EXTRA_BILL_NAME = "extra_bill_name";
    public static final String EXTRA_BILL_DESCRIPTION = "extra_bill_description";
    public static final String EXTRA_BILL_AMOUNT = "extra_bill_amount";
    public static final String EXTRA_BILL_DUE_DATE = "extra_bill_due_date";
    public static final String EXTRA_BILL_TYPE = "extra_bill_type";
    public static final String EXTRA_BILL_STATUS = "extra_bill_status";

    private TextInputEditText etAmount, etBillName, etDueDate;
    private AutoCompleteTextView spinnerBillType;
    private MaterialCheckBox cbRecurring;
    private View editActionsContainer;
    private MaterialButton btnSave, btnCancel, btnMarkPaid, btnDelete;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private AuthManager authManager;
    private BillRepository billRepository;
    private boolean isEditMode;
    private int editingBillId;
    private String billStatus;

    // Common bill types in Sri Lanka
    private String[] billTypes = {
            "Electricity (CEB)",
            "Water (NWSDB)",
            "Internet (SLT/Dialog/Mobitel)",
            "Mobile Phone",
            "Gas/LP Gas",
            "Rent",
            "Insurance",
            "Credit Card",
            "Loan Payment",
            "Subscription Service",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bill);

        isEditMode = getIntent().hasExtra(EXTRA_BILL_ID);
        editingBillId = getIntent().getIntExtra(EXTRA_BILL_ID, -1);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Bill" : "Add Bill");
        }

        initializeViews();
        authManager = new AuthManager();
        billRepository = new BillRepository(this);
        setupBillTypeDropdown();
        setupClickListeners();
        populateIfEditing();
    }

    private void initializeViews() {
        etAmount = findViewById(R.id.et_amount);
        etBillName = findViewById(R.id.et_bill_name);
        etDueDate = findViewById(R.id.et_due_date);
        spinnerBillType = findViewById(R.id.spinner_bill_type);
        cbRecurring = findViewById(R.id.cb_recurring);
        editActionsContainer = findViewById(R.id.edit_actions_container);
        btnMarkPaid = findViewById(R.id.btn_mark_paid);
        btnDelete = findViewById(R.id.btn_delete);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // Initialize calendar and date format
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    private void setupBillTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                billTypes
        );
        spinnerBillType.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Date picker for bill due date
        etDueDate.setOnClickListener(v -> {
            if (!"paid".equalsIgnoreCase(billStatus)) {
                showDatePickerDialog();
            }
        });

        // Make date field non-editable (only click to open calendar)
        etDueDate.setFocusable(false);
        etDueDate.setClickable(true);

        btnSave.setOnClickListener(v -> saveBill());
        btnCancel.setOnClickListener(v -> finish());

        if (btnMarkPaid != null) {
            btnMarkPaid.setOnClickListener(v -> {
                if ("paid".equalsIgnoreCase(billStatus)) {
                    Toast.makeText(AddBillActivity.this, "This bill is already marked as paid.", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmMarkAsPaid();
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDeleteBill());
        }
    }

    private android.widget.DatePicker billDatePicker;

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        AlertDialog.Builder builder = new AlertDialog.Builder(AddBillActivity.this);
        builder.setTitle("Select Due Date");

        billDatePicker = new android.widget.DatePicker(AddBillActivity.this);
        billDatePicker.init(year, month, day, null);
        billDatePicker.setMinDate(System.currentTimeMillis());

        builder.setView(billDatePicker);

        builder.setPositiveButton("Select Date", (dialog, which) -> {
            int selectedYear = billDatePicker.getYear();
            int selectedMonth = billDatePicker.getMonth();
            int selectedDay = billDatePicker.getDayOfMonth();

            calendar.set(Calendar.YEAR, selectedYear);
            calendar.set(Calendar.MONTH, selectedMonth);
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

            String formattedDate = dateFormat.format(calendar.getTime());
            etDueDate.setText(formattedDate);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void saveBill() {
        if ("paid".equalsIgnoreCase(billStatus)) {
            Toast.makeText(this, "Paid bills cannot be edited.", Toast.LENGTH_SHORT).show();
            return;
        }

        String amount = etAmount.getText().toString().trim();
        String billName = etBillName.getText().toString().trim();
        String billType = spinnerBillType.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();

        if (amount.isEmpty()) {
            etAmount.setError("Please enter amount");
            etAmount.requestFocus();
            return;
        }

        if (billName.isEmpty()) {
            etBillName.setError("Please enter bill name");
            etBillName.requestFocus();
            return;
        }

        if (billType.isEmpty()) {
            Toast.makeText(this, "Please select a bill type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate.isEmpty()) {
            etDueDate.setError("Please select due date");
            etDueDate.requestFocus();
            return;
        }

        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        double amountValue;
        try {
            amountValue = Double.parseDouble(amount);
        } catch (NumberFormatException ex) {
            etAmount.setError("Please enter a valid amount");
            etAmount.requestFocus();
            return;
        }

        long dueDateMillis = calendar.getTimeInMillis();
        String status = resolveStatus(dueDateMillis);
        int indicator = resolveIndicator(status);

        Bill bill = new Bill(
                isEditMode ? editingBillId : 0,
                billName,
                billType,
                amountValue,
                dueDateMillis,
                billType,
                resolveCategoryIcon(billType),
                status,
                indicator
        );

        if (isEditMode) {
            billRepository.updateBill(userId, bill, new BillRepository.ModifyBillCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AddBillActivity.this, "Bill updated", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AddBillActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        billRepository.saveBill(userId, bill, new BillRepository.SaveBillCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddBillActivity.this, "Bill added: " + billName + " - LKR " + amount, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddBillActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmMarkAsPaid() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Paid")
                .setMessage("Mark this bill as paid?")
                .setPositiveButton("Mark Paid", (dialog, which) -> markBillAsPaid())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void markBillAsPaid() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        String billName = etBillName.getText().toString().trim();
        String billType = spinnerBillType.getText().toString().trim();
        double amountValue = 0.0d;
        try {
            amountValue = Double.parseDouble(etAmount.getText().toString().trim());
        } catch (Exception ignored) {}

        Bill bill = new Bill(
                editingBillId,
                billName,
                billType,
                amountValue,
                calendar.getTimeInMillis(),
                billType,
                resolveCategoryIcon(billType),
                "paid",
                R.drawable.circle_success_light
        );

        billRepository.updateBill(userId, bill, new BillRepository.ModifyBillCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddBillActivity.this, "Bill marked as paid!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddBillActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteBill() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Bill")
                .setMessage("Are you sure you want to delete this bill?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBill())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteBill() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        billRepository.deleteBill(userId, editingBillId, new BillRepository.ModifyBillCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddBillActivity.this, "Bill deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddBillActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateIfEditing() {
        if (!isEditMode) {
            return;
        }

        if (editActionsContainer != null) {
            editActionsContainer.setVisibility(View.VISIBLE);
        }

        if (btnSave != null) {
            btnSave.setText("Save Changes");
        }

        etBillName.setText(getIntent().getStringExtra(EXTRA_BILL_NAME));
        spinnerBillType.setText(getIntent().getStringExtra(EXTRA_BILL_TYPE), false);
        etAmount.setText(String.valueOf(getIntent().getDoubleExtra(EXTRA_BILL_AMOUNT, 0.0d)));

        long dueDate = getIntent().getLongExtra(EXTRA_BILL_DUE_DATE, System.currentTimeMillis());
        calendar.setTimeInMillis(dueDate);
        etDueDate.setText(dateFormat.format(calendar.getTime()));

        billStatus = getIntent().getStringExtra(EXTRA_BILL_STATUS);
        if ("paid".equalsIgnoreCase(billStatus)) {
            // Disable input fields for paid bills
            etBillName.setEnabled(false);
            spinnerBillType.setEnabled(false);
            etAmount.setEnabled(false);
            etDueDate.setEnabled(false);
            etDueDate.setClickable(false);
            if (cbRecurring != null) {
                cbRecurring.setEnabled(false);
            }

            btnSave.setEnabled(false);
            btnSave.setAlpha(0.5f);
            btnSave.setText("Paid Bill (Locked)");

            if (btnMarkPaid != null) {
                btnMarkPaid.setText("Paid ✓");
                btnMarkPaid.setAlpha(0.7f);
            }

            Toast.makeText(this, "Paid bills cannot be edited.", Toast.LENGTH_SHORT).show();
        }
    }

    private String resolveStatus(long dueDateMillis) {
        long now = System.currentTimeMillis();
        long days = (dueDateMillis - now) / (24L * 60L * 60L * 1000L);
        if (days <= 3) {
            return "urgent";
        }
        if (days <= 7) {
            return "due_soon";
        }
        return "pending";
    }

    private int resolveIndicator(String status) {
        if ("urgent".equals(status)) {
            return R.drawable.circle_urgent;
        }
        if ("due_soon".equals(status)) {
            return R.drawable.circle_warning;
        }
        return R.drawable.circle_blue_light;
    }

    private int resolveCategoryIcon(String billType) {
        String normalized = billType.toLowerCase(Locale.ROOT);
        if (normalized.contains("electric")) {
            return R.drawable.ic_electricity;
        }
        if (normalized.contains("water")) {
            return R.drawable.ic_water;
        }
        if (normalized.contains("internet") || normalized.contains("mobile")) {
            return R.drawable.ic_wifi;
        }
        return R.drawable.ic_receipt;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
