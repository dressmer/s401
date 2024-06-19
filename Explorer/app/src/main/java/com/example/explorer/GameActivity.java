package com.example.explorer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

public class GameActivity extends AppCompatActivity {
    private JSONObject data;
    private String location = "-1";
    private HashMap<String, GameObject> inventory = new HashMap<>(); // itemId -> GameObject
    private String currentEncounterId = null;
    private boolean isYourTurnEncounter = true;
    private int[] encounterHP = {-1, -1};
    private FightPlayer[] fightPlayers = new FightPlayer[2];
    private final String SERVER_URL = "http://164.132.59.66/s401-assets/";
    private boolean win = false;

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
        String startLocation = "0";
        InputStream inputStream = getResources().openRawResource(sourcefile);
        Scanner scanner = new Scanner(inputStream);
        String jsonString = scanner.useDelimiter("\\A").next();
        try {
            data = new JSONObject(jsonString);
            worldtitle = data.getString("title");
            startLocation = data.getString("start");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String newtitle = String.format(getString(R.string.app_title_name), getString(R.string.app_name), worldtitle);
        setTitle(newtitle);

        Log.d("APP", "CREATE");
        if (filename != null) {
            Log.d("APP", "filename: " + filename);
        }

        setLocation(startLocation);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("APP","RESTORE");
        this.setLocation(savedInstanceState.getString("location"));
        this.currentEncounterId = savedInstanceState.getString("encounterId");
        if (this.currentEncounterId != null) {
            this.isYourTurnEncounter = savedInstanceState.getBoolean("isYourTurnEncounter");
            this.encounterHP = savedInstanceState.getIntArray("encounterHP");
            this.setEncounter(this.currentEncounterId);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("APP","SAVE");
        outState.putString("location", location);
        outState.putString("encounterId", currentEncounterId);
        if (currentEncounterId != null) {
            outState.putBoolean("isYourTurnEncounter", isYourTurnEncounter);
            encounterHP[0] = fightPlayers[0].getHealth();
            encounterHP[1] = fightPlayers[1].getHealth();
            outState.putIntArray("encounterHP", encounterHP);
        }
    }

    protected void setLocation(String newLoc) {
        location = newLoc;
        try {
            JSONObject locationObject = data.getJSONObject("places").getJSONObject(location);
            TextView locationTitleTextView = findViewById(R.id.locationName);
            locationTitleTextView.setText(locationObject.getString("name"));
            TextView locationDescTextView = findViewById(R.id.locationDesc);
            locationDescTextView.setText(locationObject.getString("desc"));
            ImageView locationImage = findViewById(R.id.locationImage);
            locationImage.setVisibility(View.GONE);
            locationImage.invalidate();



            if (locationObject.has("image")) {
                locationImage.setVisibility(View.VISIBLE);
                // TODO: Temp disable images and force place0.jpg
                //int resourceId = getResources().getIdentifier(locationObject.getString("image"), "drawable", getPackageName());
                //int resourceId = getResources().getIdentifier("place0", "drawable", getPackageName());
                //locationImage.setImageResource(resourceId);

                Glide.with(this)
                        .load(SERVER_URL + locationObject.getString("image"))
                        .into(locationImage);

            } else {
                locationImage.setVisibility(View.GONE);
            }

            // Create actions
            LinearLayout buttonsContainer = findViewById(R.id.buttons_container);
            JSONArray actions = locationObject.getJSONArray("actions");
            buttonsContainer.removeAllViews();
            LayoutInflater inflater = getLayoutInflater();
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                String actionName = action.getString("action_name");

                // Add button
                MaterialCardView buttonCard = (MaterialCardView) inflater.inflate(R.layout.button_card, buttonsContainer, false);
                TextView buttonText = buttonCard.findViewById(R.id.buttonText);
                buttonText.setText(actionName);

                // Check if the action requires anything
                LinearLayout additionalInfosLayout = buttonCard.findViewById(R.id.additionalInfosLayout);
                String[] requirementItemIds;
                if (action.has("require")) {
                    additionalInfosLayout.setVisibility(View.VISIBLE);
                    LinearLayout requirementsLayout = additionalInfosLayout.findViewById(R.id.requirementsLayout);
                    requirementsLayout.removeAllViews();

                    // Get all the requirements for said action
                    JSONArray requireJSON = action.getJSONArray("require");
                    requirementItemIds = new String[requireJSON.length()];

                    addRequirementsToButton(requireJSON, requirementItemIds, inflater, buttonCard, requirementsLayout);
                }
                else {
                    requirementItemIds = null;
                    additionalInfosLayout.setVisibility(View.GONE);
                }

                // Get drops
                String[] dropItemIds;
                if (action.has("drop")) {
                    // Get all the drops for said action
                    JSONArray dropJSON = action.getJSONArray("drop");
                    dropItemIds = new String[dropJSON.length()];
                    for (int j = 0; j < dropJSON.length(); j++) {
                        String dropItemId = dropJSON.getString(j);
                        dropItemIds[j] = dropItemId;

                        // Make sure the item exists, and if not, create it
                        if (!itemExists(dropItemId)) {
                            createItem(dropItemId);
                            GameObject dropItem = getItem(dropItemId);
                            dropItem.setAmount(0); // Force amount to 0
                        }
                    }
                }
                else {
                    dropItemIds = null;
                }

                // Get "next"
                String next;
                if (action.has("next")) {
                    next = action.getString("next");
                }
                else {
                    next = null;
                }
                // Get "encounter_id"
                String encounter_id;
                if (action.has("encounter_id")) {
                    encounter_id = action.getString("encounter_id");
                }
                else {
                    encounter_id = null;
                }

                // Set event for when the button is clicked
                buttonCard.setOnClickListener(view -> {
                    // Remove items
                    if (requirementItemIds != null) {
                        for (int j = 0; j < requirementItemIds.length; j++) {
                            GameObject itemObj = getItem(requirementItemIds[j]);
                            if (itemObj.getAmount() < 1) {
                                // The player doesn't have this object! Deny!
                                Toast.makeText(this, getResources().getString(R.string.missing_required_items), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            itemObj.setAmount(itemObj.getAmount() - 1);
                            Log.d("GameActivity", "Removed 1x itemId " + requirementItemIds[j] + " (left: " + itemObj.getAmount() + ")");
                        }
                    }

                    // Give drops
                    if (dropItemIds != null) {
                        for (int j = 0; j < dropItemIds.length; j++) {
                            GameObject itemObj = getItem(dropItemIds[j]);
                            itemObj.setAmount(itemObj.getAmount() + 1);
                            Log.d("GameActivity", "Added 1x itemId " + dropItemIds[j] + " (left: " + itemObj.getAmount() + ")");
                        }
                    }

                    if (next != null) {
                        setLocation(next);
                    }
                    else if (encounter_id != null) {
                        // TODO: Start encounter
                        Log.d("GameActivity", "Starting encounter " + encounter_id);
                        setEncounter(encounter_id);
                    }
                    updateItemsContainer();
                });

                buttonsContainer.addView(buttonCard);
            }

            // Check if win status is specified
            if (locationObject.has("win")) {
                win = locationObject.getBoolean("win");
            }

            // If final location, display a button to end the game!
            if (locationObject.has("final") && locationObject.getBoolean("final")) {
                MaterialCardView buttonCard = (MaterialCardView) inflater.inflate(R.layout.button_card, buttonsContainer, false);
                TextView buttonText = buttonCard.findViewById(R.id.buttonText);
                buttonText.setText(getResources().getString(R.string.won_game));
                LinearLayout additionalInfosLayout = buttonCard.findViewById(R.id.additionalInfosLayout);
                additionalInfosLayout.setVisibility(View.GONE);
                buttonCard.setOnClickListener(view -> {
                    Intent intent = new Intent();
                    intent.putExtra("win", win);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                });
                buttonsContainer.addView(buttonCard);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateItemsContainer();
    }

    private void setEncounter(String encounterId) {
        this.currentEncounterId = encounterId;
        setContentView(R.layout.activity_game_fight);

        try {
            JSONObject encounterJSON = data.getJSONObject("encounter").getJSONObject(encounterId);

            TextView locationTitleTextView = findViewById(R.id.locationName);
            locationTitleTextView.setText(encounterJSON.getString("text"));
            //TextView locationDescTextView = findViewById(R.id.locationDesc);
            //locationDescTextView.setText(locationObject.getString("desc"));
            ImageView locationImage = findViewById(R.id.locationImage);
            locationImage.setVisibility(View.GONE);
            locationImage.invalidate();

            if (encounterJSON.has("image")) {
                locationImage.setVisibility(View.VISIBLE);
                // TODO: Temp disable images and force place0.jpg
                //int resourceId = getResources().getIdentifier(locationObject.getString("image"), "drawable", getPackageName());
                int resourceId = getResources().getIdentifier("place0", "drawable", getPackageName());
                locationImage.setImageResource(resourceId);
            } else {
                locationImage.setVisibility(View.GONE);
            }

            // Create players
            JSONObject playerJSON = encounterJSON.getJSONObject("player");
            JSONObject opponentJSON = encounterJSON.getJSONObject("opponent");
            if (encounterHP[0] == -1) {
                // Get the HP if not already set
                encounterHP[0] = playerJSON.getInt("life");
                encounterHP[1] = opponentJSON.getInt("life");
                isYourTurnEncounter = true;
            }
            fightPlayers[0] = new FightPlayer(encounterHP[0], playerJSON.getString("attack"), playerJSON.getInt("defense"));
            fightPlayers[1] = new FightPlayer(encounterHP[1], opponentJSON.getString("attack"), opponentJSON.getInt("defense"));
            fightPlayers[0].setOpponent(fightPlayers[1]);
            fightPlayers[1].setOpponent(fightPlayers[0]);




            // Create actions
            LinearLayout buttonsContainer = findViewById(R.id.buttons_container);
            JSONArray actions = encounterJSON.getJSONArray("options");
            buttonsContainer.removeAllViews();
            LayoutInflater inflater = getLayoutInflater();
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                String actionName = action.getString("choice");

                // Add button
                MaterialCardView buttonCard = (MaterialCardView) inflater.inflate(R.layout.button_card, buttonsContainer, false);
                TextView buttonText = buttonCard.findViewById(R.id.buttonText);
                buttonText.setText(actionName);

                // Check if the action requires anything
                LinearLayout additionalInfosLayout = buttonCard.findViewById(R.id.additionalInfosLayout);
                String[] requirementItemIds;
                if (action.has("require")) {
                    additionalInfosLayout.setVisibility(View.VISIBLE);
                    LinearLayout requirementsLayout = additionalInfosLayout.findViewById(R.id.requirementsLayout);
                    requirementsLayout.removeAllViews();

                    // Get all the requirements for said action
                    JSONArray requireJSON = action.getJSONArray("require");
                    requirementItemIds = new String[requireJSON.length()];

                    addRequirementsToButton(requireJSON, requirementItemIds, inflater, buttonCard, requirementsLayout);
                }
                else {
                    requirementItemIds = null;
                    additionalInfosLayout.setVisibility(View.GONE);
                }

                // Get effects
                int attack_bonus;
                if (action.has("effect")) {
                    JSONObject effectJSON = action.getJSONObject("effect");
                    if (effectJSON.has("attack_bonus")) {
                        attack_bonus = effectJSON.getInt("attack_bonus");
                    } else {
                        attack_bonus = 0;
                    }
                } else {
                    attack_bonus = 0;
                }

                // Set event for when the button is clicked
                buttonCard.setOnClickListener(view -> {
                    if (!isYourTurnEncounter) {
                        Toast.makeText(this, getResources().getString(R.string.fight_wait_for_turn), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Remove items
                    if (requirementItemIds != null) {
                        for (int j = 0; j < requirementItemIds.length; j++) {
                            GameObject itemObj = getItem(requirementItemIds[j]);
                            if (itemObj.getAmount() < 1) {
                                // The player doesn't have this object! Deny!
                                Toast.makeText(this, getResources().getString(R.string.missing_required_items), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            itemObj.setAmount(itemObj.getAmount() - 1);
                            Log.d("GameActivity", "Removed 1x itemId " + requirementItemIds[j] + " (left: " + itemObj.getAmount() + ")");
                        }
                    }

                    processFight(attack_bonus);
                });

                buttonsContainer.addView(buttonCard);
            }

            updateFightDescription();
            updateItemsContainer();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addRequirementsToButton(JSONArray requireJSON, String[] requirementItemIds, LayoutInflater inflater, MaterialCardView buttonCard, LinearLayout requirementsLayout) throws JSONException {
        for (int j = 0; j < requireJSON.length(); j++) {
            String requiredItemId = requireJSON.getString(j);
            requirementItemIds[j] = requiredItemId;

            // Make sure the item exists, and if not, create it
            if (!itemExists(requiredItemId)) {
                createItem(requiredItemId);
                GameObject requiredItem = getItem(requiredItemId);
                requiredItem.setAmount(0); // Force amount to 0
            }

            GameObject requiredItem = getItem(requiredItemId);

            TextView buttonCardRequirementtext = (TextView) inflater.inflate(R.layout.button_card_requirementtext, buttonCard, false);

            buttonCardRequirementtext.setText(requiredItem.getName());
            if (requiredItem.getAmount() >= 1) {
                // Player has enough of this item
                buttonCardRequirementtext.setTextColor(getColor(R.color.green));
            } else {
                // Player doesn't have the required item
                buttonCardRequirementtext.setTextColor(getColor(R.color.red));
            }

            requirementsLayout.addView(buttonCardRequirementtext);
        }
    }

    private void processFight(int attack_bonus) {
        isYourTurnEncounter = false;

        int[] attack_dice = fightPlayers[0].getAttackDice();
        int totalDamage = 0;
        for (int i = 0; i < attack_dice[0]; i++) {
            int randomInt = fightPlayers[0].getRandomDieDamage();
            totalDamage += randomInt;
            Log.d("GameActivity", "Die rolled: " + randomInt);
        }

        totalDamage += attack_dice[1];
        totalDamage += attack_bonus;
        Log.d("GameActivity", "Total damage inflected by You: " + totalDamage);
        fightPlayers[0].attack(totalDamage);

        if (fightPlayers[1].isDead()) {
            // Opponent is dead!
            try {
                String win = data.getJSONObject("encounter").getJSONObject(currentEncounterId).getString("win");
                endFightAndGoto(win);
                return;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        attack_dice = fightPlayers[1].getAttackDice();
        totalDamage = 0;
        for (int i = 0; i < attack_dice[0]; i++) {
            int randomInt = fightPlayers[1].getRandomDieDamage();
            totalDamage += randomInt;
            Log.d("GameActivity", "Die rolled: " + randomInt);
        }

        totalDamage += attack_dice[1];
        Log.d("GameActivity", "Total damage inflected by You: " + totalDamage);
        fightPlayers[1].attack(totalDamage);

        if (fightPlayers[0].isDead()) {
            // Opponent is dead!
            try {
                String lose = data.getJSONObject("encounter").getJSONObject(currentEncounterId).getString("lose");
                endFightAndGoto(lose);
                return;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        updateFightDescription();
        updateItemsContainer();
        isYourTurnEncounter = true;
        // TODO
    }

    private void updateFightDescription() {
        TextView description = findViewById(R.id.locationDesc);

        try {
            JSONObject encounterJSON = data.getJSONObject("encounter").getJSONObject(this.currentEncounterId);

            //String newDesc = String.format(getString(R.string.app_title_name), getString(R.string.app_name), worldtitle);
            // Fill player infos
            TextView playerName = findViewById(R.id.playerName);
            playerName.setText(getString(R.string.fight_you));
            TextView playerHP = findViewById(R.id.playerHP);
            playerHP.setText(String.format(getString(R.string.fight_hp), fightPlayers[0].getHealth()));
            TextView playerAttack = findViewById(R.id.playerAttack);
            playerAttack.setText(String.format(getString(R.string.fight_attack), fightPlayers[0].getAttack()));
            TextView playerDefense = findViewById(R.id.playerDefense);
            playerDefense.setText(String.format(getString(R.string.fight_defense), fightPlayers[0].getDefense()));

            // Fill opponent infos
            TextView opponentName = findViewById(R.id.opponentName);
            opponentName.setText(encounterJSON.getJSONObject("opponent").getString("name"));
            TextView opponentHP = findViewById(R.id.opponentHP);
            opponentHP.setText(String.format(getString(R.string.fight_hp), fightPlayers[1].getHealth()));
            TextView opponentAttack = findViewById(R.id.opponentAttack);
            opponentAttack.setText(String.format(getString(R.string.fight_attack), fightPlayers[1].getAttack()));
            TextView opponentDefense = findViewById(R.id.opponentDefense);
            opponentDefense.setText(String.format(getString(R.string.fight_defense), fightPlayers[1].getDefense()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void endFightAndGoto(String location) {
        currentEncounterId = null;
        encounterHP[0] = -1;
        encounterHP[1] = -1;
        setContentView(R.layout.activity_game);
        setLocation(location);
    }

    private void updateItemsContainer() {
        LinearLayout items_container = findViewById(R.id.items_container);
        items_container.removeAllViews();

        Set<String> itemIds = inventory.keySet();
        LayoutInflater inflater = getLayoutInflater();
        for (String itemId : itemIds) {
            GameObject gameObj = getItem(itemId);
            if (gameObj.getAmount() < 1) { // Ignore if the player doesn't have this item
                continue;
            }
            ImageView itemImage = (ImageView) inflater.inflate(R.layout.item_image, items_container, false);

            Log.d("GameActivity", "Loading image " + gameObj.getImageName());
            Glide.with(this)
                    .load(SERVER_URL + gameObj.getImageName())
                    .into(itemImage);

            items_container.addView(itemImage);
        }
        Log.d("GameActivity", "items_container updated! (set length: " + itemIds.size() + ")");
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

    /* Creates the item in the inventory. If the item already exists (even if amount=0), returns false.
     * If the item is created, an amount of 1 is set by default (by GameObject).
     */
    private boolean createItem(GameObject obj) {
        String itemId = obj.getId();

        // Check if the item doesn't exist, and if so, create it
        if (!itemExists(itemId)) {
            inventory.put(itemId, obj);
            return true;
        }
        return false;
    }

    /* Creates the item in the inventory. If the item already exists (even if amount=0), returns false.
     * If the item is created, an amount of 1 is set by default (by GameObject).
     */
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
            String imageName = objectJSON.getString("image");
            GameObject item = new GameObject(itemId, name, imageName);
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