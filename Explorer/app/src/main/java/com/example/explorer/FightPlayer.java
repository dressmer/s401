package com.example.explorer;

import java.util.Random;

public class FightPlayer {
    private int health;
    private String attack;
    private int defense;
    private int[] attack_dice;
    private int attack_bonus = 0;
    private FightPlayer opponent;

    public FightPlayer(int health, String attack, int defense) {
        super();
        this.health = health;
        this.attack = attack;
        this.defense = defense;

        String[] splitAttack = attack.split("\\+");
        if (splitAttack.length >= 2) {
            attack_bonus = Integer.parseInt(splitAttack[1]);
        }

        String[] splitDice = splitAttack[0].split("d");
        attack_dice = new int[] {Integer.parseInt(splitDice[0]), Integer.parseInt(splitDice[1])};
    }

    public void setOpponent(FightPlayer opponent) {
        this.opponent = opponent;
    }

    public int getHealth() {
        return this.health;
    }

    public void setHealth(int newHealth) {
        this.health = newHealth;
    }

    public String getAttack() {
        return this.attack;
    }

    public int getDefense() {
        return this.defense;
    }

    public int[] getAttackDice() {
        return this.attack_dice;
    }

    public int getAttackBonus() {
        return this.attack_bonus;
    }

    public int getRandomDieDamage() {
        Random random = new Random();

        // Bounds
        int min = 1;
        int max = attack_dice[1];

        return random.nextInt((max - min) + 1) + min;
    }

    public void attack(int damage) {
        if (opponent != null) {
            opponent.setHealth(opponent.getHealth() - damage);
        }
    }

    public boolean isDead() {
        return (this.getHealth() <= 0);
    }
}
