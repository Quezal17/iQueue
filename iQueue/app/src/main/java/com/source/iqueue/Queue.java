package com.source.iqueue;

public class Queue {
    private String name;
    private String managerId;
    private int currentNumber;
    private int totalNumber;
    private String state;

    public Queue() {}
    public Queue(String name, String managerId, int currentNumber, int totalNumber, String state) {
        this.name = name;
        this.managerId = managerId;
        this.currentNumber = currentNumber;
        this.totalNumber = totalNumber;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public int getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(int currentNumber) {
        this.currentNumber = currentNumber;
    }

    public int getTotalNumber() {
        return totalNumber;
    }

    public void setTotalNumber(int totalNumber) {
        this.totalNumber = totalNumber;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
