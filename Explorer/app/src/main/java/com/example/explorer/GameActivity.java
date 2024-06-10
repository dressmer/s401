package com.example.explorer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class GameActivity extends AppCompatActivity {
    private JSONObject data;
    private int location = -1;
    private HashMap<String, GameObject> inventory = new HashMap<>(); // itemId -> GameObject

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String filename = null;
        if (extras != null) {
            filename = extras.getString("filename");
        }

        Resources res = this.getResources();
        int sourcefile = res.getIdentifier(filename, "raw", this.getPackageName());
        String worldtitle = "";
        int startLocation = 0;
        InputStream inputStream = getResources().openRawResource(sourcefile);
        Scanner scanner = new Scanner(inputStream);
        String jsonString = scanner.useDelimiter("\\A").next();
        try {
            data = new JSONObject(jsonString);
            worldtitle = data.getString("title");
            startLocation = data.getInt("start");

            /*
            JSONObject location = data.getJSONArray("places").getJSONObject(startLocation);
            int imageResourceId = this.getResources().getIdentifier(location.getString("image"), "drawable", this.getPackageName());
            ImageView image = findViewById(R.id.locationImage);
            image.setImageResource(imageResourceId);

            TextView name = findViewById(R.id.locationName);
            name.setText(location.getString("name"));

            TextView desc = findViewById(R.id.locationDesc);
            desc.setText(location.getString("desc"));

             */
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String newtitle = String.format(getString(R.string.app_title_name), getString(R.string.app_name), worldtitle);
        setTitle(newtitle);

        Log.d("APP", "CREATE");
        if (filename != null) {
            Log.d("APP", "filename: " + filename);
        }

        setLocation(0);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("APP","RESTORE");
        this.setLocation(savedInstanceState.getInt("location"));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("APP","SAVE");
        outState.putInt("location", location);
    }

    protected void setLocation(int newLoc) {
        location = newLoc;
        try {
            JSONObject locationObject = data.getJSONArray("places").getJSONObject(location);
            TextView locationTitleTextView = findViewById(R.id.locationName);
            locationTitleTextView.setText(locationObject.getString("name"));
            TextView locationDescTextView = findViewById(R.id.locationDesc);
            locationDescTextView.setText(locationObject.getString("desc"));
            ImageView locationImage = findViewById(R.id.locationImage);
            locationImage.setVisibility(View.GONE);
            locationImage.invalidate();



            if (locationObject.has("image")) {
                locationImage.setVisibility(View.VISIBLE);
                int resourceId = getResources().getIdentifier(locationObject.getString("image"), "drawable", getPackageName());
                locationImage.setImageResource(resourceId);
            } else {
                locationImage.setVisibility(View.GONE);
            }

            // Create actions
            LinearLayout buttonsContainer = findViewById(R.id.buttons_container);
            JSONArray actions = locationObject.getJSONArray("actions");
            buttonsContainer.removeAllViews();
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                String actionName = action.getString("action_name");

                // Check if the action requires anything
                if (action.has("require")) {
                    // TODO
                }

                Button button = new Button(this);
                button.setText(actionName);
                int next = action.getInt("next");
                button.setOnClickListener(view -> {
                    setLocation(next);
                });
                buttonsContainer.addView(button);
            }

            if (locationObject.getBoolean("final")) {
                Button button = new Button(this);
                button.setText(getResources().getString(R.string.won_game));
                button.setOnClickListener(view -> {
                    Intent intent = new Intent();
                    intent.putExtra("win", true);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                });
                buttonsContainer.addView(button);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean itemExists(String itemId) {
        return (inventory.containsKey(itemId));
    }

    private GameObject getItem(String itemId) {
        if (!itemExists(itemId)) {
            return null;
        }
        return inventory.get(itemId);
    }

    private int getItemAmount(String itemId) {
        if (!itemExists(itemId)) {
            return 0;
        }
        return getItem(itemId).getAmount();
    }

    // Create the item in the inventory. If the item already exists (even if amount=0), returns false.
    private boolean createItem(GameObject obj) {
        String itemId = obj.getId();

        // Check if the item doesn't exist, and if so, create it
        if (!itemExists(itemId)) {
            inventory.put(itemId, obj);
            return true;
        }
        return false;
    }

    private boolean createItem(String itemId) {
        // Check if the item doesn't exist, and if so, create it
        if (itemExists(itemId)) {
            return false;
        }

        // Create the item by getting all of its infos
        try {
            JSONObject objectsJSON = data.getJSONObject("items");
            JSONObject objectJSON = objectsJSON.getJSONObject(itemId);
            String name = objectJSON.getString("name");
            // TODO: Add image
            GameObject item = new GameObject(itemId, name);
            createItem(item); // Add the item to the inventory hashmap
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    // Adds an item to the inventory. Creates it if it doesn't exist.
    private void addItem(String itemId, int amount) {
        if (!itemExists(itemId)) {
            createItem(itemId);
        }
        GameObject item = getItem(itemId);
        item.setAmount(item.getAmount() + amount);
    }

    private void addItem(String itemId) {
        addItem(itemId, 1);
    }
}