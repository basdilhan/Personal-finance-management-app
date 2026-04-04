package com.team.financeapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.team.financeapp.auth.AuthManager;
import com.team.financeapp.data.repository.BillRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity for adding new recurring bills
 */
public class AddBillActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_ID = "extra_bill_id";
    public static final String EXTRA_BILL_NAME = "extra_bill_name";
    public static final String EXTRA_BILL_DESCRIPTION = "extra_bill_description";
    public static final String EXTRA_BILL_AMOUNT = "extra_bill_amount";
    public static final String EXTRA_BILL_DUE_DATE = "extra_bill_due_date";
    public static final String EXTRA_BILL_TYPE = "extra_bill_type";

    private TextInputEditText etAmount, etBillName, etDueDate;
    private AutoCompleteTextView spinnerBillType;
    private MaterialButton btnSave, btnCancel;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private AuthManager authManager;
    private BillRepository billRepository;
    private boolean isEditMode;
    private int editingBillId;

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
        etDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Make date field non-editable (only click to open calendar)
        etDueDate.setFocusable(false);
        etDueDate.setClickable(true);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBill();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Show date picker dialog for selecting bill due date
     */
    private android.widget.DatePicker billDatePicker;

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create AlertDialog with DatePicker and buttons
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(AddBillActivity.this);
        builder.setTitle("Select Due Date");

        // Create a DatePicker
        billDatePicker = new android.widget.DatePicker(AddBillActivity.this);
        billDatePicker.init(year, month, day, null);
        billDatePicker.setMinDate(System.currentTimeMillis());

        builder.setView(billDatePicker);

        // Add Select Date button
        builder.setPositiveButton("Select Date", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                // Get selected date from DatePicker
                int selectedYear = billDatePicker.getYear();
                int selectedMonth = billDatePicker.getMonth();
                int selectedDay = billDatePicker.getDayOfMonth();

                // Update calendar with selected date
                calendar.set(Calendar.YEAR, selectedYear);
                calendar.set(Calendar.MONTH, selectedMonth);
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                // Format and display the date
                String formattedDate = dateFormat.format(calendar.getTime());
                etDueDate.setText(formattedDate);
            }
        });

        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void saveBill() {
        String amount = etAmount.getText().toString().trim();
        String billName = etBillName.getText().toString().trim();
        String billType = spinnerBillType.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();

        // Validation
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

    private void populateIfEditing() {
        if (!isEditMode) {
            return;
        }

        etBillName.setText(getIntent().getStringExtra(EXTRA_BILL_NAME));
        spinnerBillType.setText(getIntent().getStringExtra(EXTRA_BILL_TYPE), false);
        etAmount.setText(String.valueOf(getIntent().getDoubleExtra(EXTRA_BILL_AMOUNT, 0.0d)));

        long dueDate = getIntent().getLongExtra(EXTRA_BILL_DUE_DATE, System.currentTimeMillis());
        calendar.setTimeInMillis(dueDate);
        etDueDate.setText(dateFormat.format(calendar.getTime()));
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
