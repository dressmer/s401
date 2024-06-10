package com.example.explorer;

public class GameObject {
    private String id;
    private String name;
    private int amount;
    private String image; // ?

    public GameObject(String id, String name, int amount) {
        super();
        this.id = id;
        this.name = name;
        this.amount = amount;
    }

    public GameObject(String id, String name) {
        this(id, name, 1);
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getAmount() {
        return this.amount;
    }

    // get image

    public void setAmount(int newAmount) {
        this.amount = newAmount;
    }
}
