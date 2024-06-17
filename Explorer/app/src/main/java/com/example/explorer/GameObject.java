package com.example.explorer;

public class GameObject {
    private String id;
    private String name;
    private String imageName;
    private int amount;

    public GameObject(String id, String name, String imageName, int amount) {
        super();
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.imageName = imageName;
    }

    public GameObject(String id, String name, String imageName) {
        this(id, name, imageName, 1);
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getImageName() {
        return this.imageName;
    }

    public int getAmount() {
        return this.amount;
    }

    // get image

    public void setAmount(int newAmount) {
        this.amount = newAmount;
    }
}
