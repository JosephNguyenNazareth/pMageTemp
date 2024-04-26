package com.pmsconnect.mage.utils;

public class Artifact {
    private String name;
    private boolean available;

    public Artifact(String name) {
        this.name = name;
        this.available = false;
    }

    public Artifact(String name, boolean available) {
        this.name = name;
        this.available = available;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "Artifact " + name + ": " + available;
    }
}
