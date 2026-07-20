package com.team.financeapp.chatbot;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.team.financeapp.R;
import com.team.financeapp.data.remote.ChatApiService;
import com.team.financeapp.data.remote.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private ChatAdapter chatAdapter;
    private ChatApiService chatApiService;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Financial Assistant");
        }

        initViews();
        initRetrofit();

        // Add a welcome message
        chatAdapter.addMessage(new ChatMessage("Hello! I am your AI Financial Assistant. I can help you analyze your budget, predict future expenses, and categorize your spending. How can I help you today?", false));

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {
        recyclerChat = findViewById(R.id.recycler_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);
    }

    private void initRetrofit() {
        chatApiService = ApiClient.getClient().create(ChatApiService.class);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        // Display user message
        chatAdapter.addMessage(new ChatMessage(text, true));
        etMessage.setText("");
        recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Show typing indicator
        chatAdapter.setTyping(true);
        recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);
        btnSend.setEnabled(false);

        Map<String, String> request = new HashMap<>();
        request.put("message", text);

        chatApiService.sendMessage(userId, request).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                chatAdapter.setTyping(false);
                btnSend.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().get("reply");
                    if (reply != null) {
                        chatAdapter.addMessage(new ChatMessage(reply, false));
                    } else {
                        chatAdapter.addMessage(new ChatMessage("Received an empty response from the AI.", false));
                    }
                } else {
                    chatAdapter.addMessage(new ChatMessage("Sorry, there was a problem talking to the server. (Code: " + response.code() + ")", false));
                }
                recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                chatAdapter.setTyping(false);
                btnSend.setEnabled(true);
                chatAdapter.addMessage(new ChatMessage("Network error: " + t.getMessage(), false));
                recyclerChat.scrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
