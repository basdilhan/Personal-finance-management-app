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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity for adding new recurring bills
 */
public class AddBillActivity extends AppCompatActivity {

    private TextInputEditText etAmount, etBillName, etDueDate;
    private AutoCompleteTextView spinnerBillType;
    private MaterialButton btnSave, btnCancel;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;

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

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Bill");
        }

        initializeViews();
        setupBillTypeDropdown();
        setupClickListeners();
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
    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddBillActivity.this,
                android.R.style.Theme_Material_Light_Dialog_Alert,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        calendar.set(Calendar.YEAR, selectedYear);
                        calendar.set(Calendar.MONTH, selectedMonth);
                        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                        String formattedDate = dateFormat.format(calendar.getTime());
                        etDueDate.setText(formattedDate);
                    }
                },
                year,
                month,
                day
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
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

        // TODO: Save to database
        Toast.makeText(this, "Bill added: " + billName + " - LKR " + amount, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
