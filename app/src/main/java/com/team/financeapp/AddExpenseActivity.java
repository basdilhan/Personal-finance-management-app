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
import com.team.financeapp.data.repository.ExpenseRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * Activity for adding new expense transactions
 */
public class AddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_EXPENSE_ID = "extra_expense_id";
    public static final String EXTRA_EXPENSE_CATEGORY = "extra_expense_category";
    public static final String EXTRA_EXPENSE_AMOUNT = "extra_expense_amount";
    public static final String EXTRA_EXPENSE_DESCRIPTION = "extra_expense_description";
    public static final String EXTRA_EXPENSE_DATE = "extra_expense_date";
    public static final String EXTRA_EXPENSE_TIME = "extra_expense_time";
    public static final String EXTRA_EXPENSE_ICON = "extra_expense_icon";

    private TextInputEditText etAmount, etDescription, etDate;
    private AutoCompleteTextView spinnerCategory;
    private MaterialButton btnSave, btnCancel, btnScanReceipt;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private AuthManager authManager;
    private ExpenseRepository expenseRepository;
    private boolean isEditMode;
    private int editingExpenseId;

    // Sri Lankan expense categories
    private String[] expenseCategories = {
            "Food & Dining",
            "Transportation",
            "Mobile & Internet",
            "Healthcare",
            "Education",
            "Entertainment",
            "Shopping",
            "Groceries",
            "Fuel",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        isEditMode = getIntent().hasExtra(EXTRA_EXPENSE_ID);
        editingExpenseId = getIntent().getIntExtra(EXTRA_EXPENSE_ID, -1);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Expense" : "Add Expense");
        }

        initializeViews();
        setupCameraLaunchers();
        authManager = new AuthManager();
        expenseRepository = new ExpenseRepository(this);
        setupCategoryDropdown();
        setupClickListeners();
        populateIfEditing();
    }

    private void initializeViews() {
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        etDate = findViewById(R.id.et_date);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnScanReceipt = findViewById(R.id.btn_scan_receipt);

        // Initialize calendar and date format
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                expenseCategories
        );
        spinnerCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Date picker for expense date
        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Make date field non-editable (only click to open calendar)
        etDate.setFocusable(false);
        etDate.setClickable(true);

        // Magical real-time AI auto-categorization when user stops typing
        final android.os.Handler typingHandler = new android.os.Handler();
        final Runnable typingRunnable = new Runnable() {
            @Override
            public void run() {
                autoCategorizeWithAI();
            }
        };

        etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                typingHandler.removeCallbacks(typingRunnable);
                String text = s.toString().trim();
                if (text.length() >= 3) {
                    typingHandler.postDelayed(typingRunnable, 1500); // 1.5 seconds after typing stops
                }
            }
        });



        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (btnScanReceipt != null) {
            btnScanReceipt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(AddExpenseActivity.this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        launchCamera();
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    }
                }
            });
        }
    }

    /**
     * Show date picker dialog for selecting expense date
     */
    private android.widget.DatePicker expenseDatePicker;

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create AlertDialog with DatePicker and buttons
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(AddExpenseActivity.this);
        builder.setTitle("Select Date");

        // Create a DatePicker
        expenseDatePicker = new android.widget.DatePicker(AddExpenseActivity.this);
        expenseDatePicker.init(year, month, day, null);
        expenseDatePicker.setMaxDate(System.currentTimeMillis());

        builder.setView(expenseDatePicker);

        // Add Select Date button
        builder.setPositiveButton("Select Date", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                // Get selected date from DatePicker
                int selectedYear = expenseDatePicker.getYear();
                int selectedMonth = expenseDatePicker.getMonth();
                int selectedDay = expenseDatePicker.getDayOfMonth();

                // Update calendar with selected date
                calendar.set(Calendar.YEAR, selectedYear);
                calendar.set(Calendar.MONTH, selectedMonth);
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                // Format and display the date
                String formattedDate = dateFormat.format(calendar.getTime());
                etDate.setText(formattedDate);
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

    private void autoCategorizeWithAI() {
        String description = etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            return; // Don't show toast for auto-typing
        }

        // Show loading state in spinner instead of button
        String originalText = spinnerCategory.getText().toString();
        spinnerCategory.setText("Auto-detecting...", false);

        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("description", description);

        com.team.financeapp.data.remote.ApiClient.getClient().create(com.team.financeapp.data.remote.ExpenseApiService.class).categorizeExpense(request)
            .enqueue(new retrofit2.Callback<java.util.Map<String, String>>() {
                @Override
                public void onResponse(retrofit2.Call<java.util.Map<String, String>> call, retrofit2.Response<java.util.Map<String, String>> response) {

                    if (response.isSuccessful() && response.body() != null) {
                        String category = response.body().get("category");
                        if (category != null && !category.isEmpty()) {
                            // Clean AI response (remove quotes, asterisks, "Category:" prefixes)
                            String cleanCategory = category.replaceAll("[\"'\\*]", "").replace("Category:", "").trim();
                            
                            // Find matching category in our list (case insensitive)
                            for (String c : expenseCategories) {
                                if (c.equalsIgnoreCase(cleanCategory) || cleanCategory.toLowerCase().contains(c.toLowerCase())) {
                                    spinnerCategory.setText(c, false);
                                    return; // Silently update, it's magical
                                }
                            }
                            // If exact match not found but we got one, try assigning the closest or just setting text
                            spinnerCategory.setText(originalText, false); // Revert to what it was
                        }
                    } else {
                        spinnerCategory.setText(originalText, false);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<java.util.Map<String, String>> call, Throwable t) {
                    spinnerCategory.setText(originalText, false);
                }
            });
    }

    private void saveExpense() {
        String amount = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = spinnerCategory.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        // Validation
        if (amount.isEmpty()) {
            etAmount.setError("Please enter amount");
            etAmount.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty()) {
            etDate.setError("Please select expense date");
            etDate.requestFocus();
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

        long expenseDateMillis = calendar.getTimeInMillis();
        Expense expense = new Expense(
                isEditMode ? editingExpenseId : 0,
                category,
                amountValue,
                description,
                expenseDateMillis,
                timeFormat.format(calendar.getTime()),
                resolveCategoryIcon(category)
        );

        if (isEditMode) {
            expenseRepository.updateExpense(userId, expense, new ExpenseRepository.ModifyExpenseCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AddExpenseActivity.this, "Expense updated", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AddExpenseActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        expenseRepository.saveExpense(userId, expense, new ExpenseRepository.SaveExpenseCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddExpenseActivity.this, "Expense added: LKR " + amount, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddExpenseActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Uri currentPhotoUri;

    private void setupCameraLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && currentPhotoUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentPhotoUri);
                            // Scale down to prevent OOM / large payload, but keep readable for AI
                            int maxDim = 1024;
                            float scale = Math.min(((float)maxDim) / bitmap.getWidth(), ((float)maxDim) / bitmap.getHeight());
                            if (scale < 1) {
                                bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() * scale), Math.round(bitmap.getHeight() * scale), true);
                            }
                            processReceiptImage(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, "Camera permission required to scan receipts", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                java.io.File photoFile = java.io.File.createTempFile(
                        "receipt_" + System.currentTimeMillis(),
                        ".jpg",
                        getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                );
                currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                        this, 
                        getPackageName() + ".fileprovider", 
                        photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                cameraLauncher.launch(takePictureIntent);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error creating temp file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void processReceiptImage(Bitmap bitmap) {
        Toast.makeText(this, "Scanning receipt with AI...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);
                
                java.util.Map<String, String> request = new java.util.HashMap<>();
                request.put("image", base64Image);

                com.team.financeapp.data.remote.AIApiService aiService = 
                        com.team.financeapp.data.remote.ApiClient.getClient().create(com.team.financeapp.data.remote.AIApiService.class);
                
                aiService.scanReceipt(request).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(AddExpenseActivity.this, "AI Scan Complete!", Toast.LENGTH_SHORT).show();
                            
                            java.util.Map<String, Object> respMap = response.body();
                            if (respMap.containsKey("error")) {
                                Toast.makeText(AddExpenseActivity.this, String.valueOf(respMap.get("error")), Toast.LENGTH_LONG).show();
                            } else if (respMap.containsKey("amount")) {
                                try {
                                    double amt = Double.parseDouble(String.valueOf(respMap.get("amount")));
                                    etAmount.setText(String.valueOf(amt));
                                    etDescription.setText("AI Scanned Receipt");
                                    
                                    if (respMap.containsKey("category")) {
                                        String cat = String.valueOf(respMap.get("category"));
                                        spinnerCategory.setText(cat, false);
                                    }
                                    
                                    if (respMap.containsKey("date")) {
                                        String dateStr = String.valueOf(respMap.get("date"));
                                        if (!dateStr.isEmpty() && !dateStr.equals("null")) {
                                            etDate.setText(dateStr);
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    Toast.makeText(AddExpenseActivity.this, "Invalid amount format from AI.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(AddExpenseActivity.this, "AI response missing amount data.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AddExpenseActivity.this, "AI Scan Failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                        Toast.makeText(AddExpenseActivity.this, "Error connecting to AI: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void populateIfEditing() {
        if (!isEditMode) {
            return;
        }

        spinnerCategory.setText(getIntent().getStringExtra(EXTRA_EXPENSE_CATEGORY), false);
        etAmount.setText(String.valueOf(getIntent().getDoubleExtra(EXTRA_EXPENSE_AMOUNT, 0.0d)));
        etDescription.setText(getIntent().getStringExtra(EXTRA_EXPENSE_DESCRIPTION));

        long expenseDate = getIntent().getLongExtra(EXTRA_EXPENSE_DATE, System.currentTimeMillis());
        calendar.setTimeInMillis(expenseDate);
        etDate.setText(dateFormat.format(calendar.getTime()));
    }

    private int resolveCategoryIcon(String category) {
        String normalized = category.toLowerCase(Locale.ROOT);
        if (normalized.contains("utilities") || normalized.contains("electric") || normalized.contains("water")) {
            return R.drawable.ic_electricity;
        }
        if (normalized.contains("internet") || normalized.contains("mobile")) {
            return R.drawable.ic_wifi;
        }
        if (normalized.contains("transport") || normalized.contains("fuel")) {
            return R.drawable.ic_water;
        }
        return R.drawable.ic_receipt;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

