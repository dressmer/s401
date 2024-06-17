package com.example.explorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {


    private int playCount = 0;
    private int winCount = 0;

    private TextView playCountText;
    private TextView wonCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent intent = result.getData();
                            // Handle the Intent
                            boolean win = false;
                            if (intent != null) {
                                win = intent.getBooleanExtra("win", false);
                                if (win) {
                                    winCount++;
                                }
                                playCount++;
                                updateCounters();
                            }
                        }
                    }
                }
        );
        Button mButtonStartGame = findViewById(R.id.button);
        mButtonStartGame.setOnClickListener(view -> startGameActivity(mStartForResult));

        playCountText = findViewById(R.id.countGamesPlayed);
        wonCountText = findViewById(R.id.countGamesWon);
        updateCounters();
    }

    private void startGameActivity(ActivityResultLauncher<Intent> mStartForResult) {
        Intent intent;
        intent = new Intent(this, GameActivity.class);
        intent.putExtra("filename", "data");
        //startActivity(intent);


        mStartForResult.launch(intent);

    }

    private void updateCounters() {
        playCountText.setText(Integer.toString(playCount));
        wonCountText.setText(Integer.toString(winCount));
    }



}