package com.example.backend.controller;

import com.example.backend.entity.ExpenseEntity;
import com.example.backend.entity.IncomeEntity;
import com.example.backend.repository.BudgetLimitRepository;
import com.example.backend.repository.ExpenseRepository;
import com.example.backend.repository.IncomeRepository;
import com.example.backend.service.MLServiceClient;
import com.example.backend.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final MLServiceClient mlServiceClient;
    private final GeminiService geminiService;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetLimitRepository budgetLimitRepository;

    public ChatController(MLServiceClient mlServiceClient,
                          GeminiService geminiService,
                          ExpenseRepository expenseRepository,
                          IncomeRepository incomeRepository,
                          BudgetLimitRepository budgetLimitRepository) {
        this.mlServiceClient = mlServiceClient;
        this.geminiService = geminiService;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.budgetLimitRepository = budgetLimitRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        // Build real financial context from database
        String context = buildFinancialContext(userId);

        // Build Gemini prompt
        String prompt = "You are DreamSaver AI, a personal finance chatbot built into the DreamSaver mobile app. " +
                        "Your ONLY job is to analyze the user's ACTUAL financial data shown in the context below and answer their question. " +
                        "RULES:\n" +
                        "- ONLY discuss the user's income, expenses, bills, goals, and budget data from the context.\n" +
                        "- DO NOT recommend external financial products like fixed deposits, stocks, mutual funds, or investment platforms.\n" +
                        "- DO NOT make up numbers. Only use the exact figures from the context.\n" +
                        "- If the ML forecast shows LKR 0.00, explain that the app needs at least 2 months of expense data to generate accurate predictions.\n" +
                        "- Keep responses concise (under 150 words).\n" +
                        "- Use LKR currency and Sri Lankan context.\n\n" +
                        "Context:\n" + context + "\n\n" +
                        "User Message: " + message;

        // Get response from Gemini
        String reply = geminiService.generateChatResponse(prompt);

        return ResponseEntity.ok(Map.of(
                "reply", reply,
                "status", "success"
        ));
    }

    private String buildFinancialContext(String userId) {
        try {
            // Get current month boundaries
            YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Colombo"));
            long monthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();
            long monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli();

            List<ExpenseEntity> allExpenses = expenseRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);
            List<IncomeEntity> allIncomes = incomeRepository.findByUserIdAndIsDeletedFalseOrderByDateDesc(userId);

            // Fetch ML Forecast
            // Get the last 6 months of historical expenses (aggregated by month)
            Map<YearMonth, BigDecimal> monthlyAggregates = allExpenses.stream()
                .collect(Collectors.groupingBy(
                    e -> YearMonth.from(java.time.Instant.ofEpochMilli(e.getDate()).atZone(ZoneId.of("Asia/Colombo")).toLocalDate()),
                    Collectors.reducing(BigDecimal.ZERO, ExpenseEntity::getAmount, BigDecimal::add)
                ));

            // Generate historical data array for Chronos ML (last 6 months)
            List<Double> historicalData = new java.util.ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = currentMonth.minusMonths(i);
                historicalData.add(monthlyAggregates.getOrDefault(ym, BigDecimal.ZERO).doubleValue());
            }

            Double predictedNextMonthExpense = mlServiceClient.generateForecast(userId, historicalData);

            // Monthly totals for current month
            BigDecimal monthlyExpenses = allExpenses.stream()
                    .filter(e -> e.getDate() >= monthStart && e.getDate() <= monthEnd)
                    .map(ExpenseEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthlyIncome = allIncomes.stream()
                    .filter(i -> i.getDate() >= monthStart && i.getDate() <= monthEnd)
                    .map(IncomeEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Top spending categories this month
            Map<String, BigDecimal> byCategory = allExpenses.stream()
                    .filter(e -> e.getDate() >= monthStart && e.getDate() <= monthEnd)
                    .collect(Collectors.groupingBy(
                            ExpenseEntity::getCategory,
                            Collectors.reducing(BigDecimal.ZERO, ExpenseEntity::getAmount, BigDecimal::add)
                    ));

            String topCategory = byCategory.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (LKR " + e.getValue().toPlainString() + ")")
                    .orElse("None");

            return String.format(
                "Total income this month: LKR %s.\n" +
                "Total expenses this month: LKR %s.\n" +
                "Remaining balance: LKR %s.\n" +
                "Highest spending category: %s.\n" +
                "Machine Learning Forecast for Next Month's Expenses: LKR %.2f.",
                monthlyIncome.toPlainString(),
                monthlyExpenses.toPlainString(),
                monthlyIncome.subtract(monthlyExpenses).toPlainString(),
                topCategory,
                predictedNextMonthExpense
            );
        } catch (Exception e) {
            return "User is tracking their personal finances in Sri Lankan Rupees (LKR). Help them manage their budget wisely.";
        }
    }
}
