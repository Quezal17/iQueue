package com.source.iqueue.manager;

public class Manager {
    private String shopName;
    private String shopCity;
    private String shopAddress;
    private double latitudine;
    private double longitudine;
    private int queueIterator;
    private String shopImage;

    public Manager() {
    }

    public Manager(String shopName, String shopCity, String shopAddress, double latitudine, double longitudine, int queueIterator, String shopImage) {
        this.shopName = shopName;
        this.shopCity = shopCity;
        this.shopAddress = shopAddress;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.queueIterator = queueIterator;
        this.shopImage = shopImage;
    }

    public String getShopImage() {
        return shopImage;
    }

    public void setShopImage(String shopImage) {
        this.shopImage = shopImage;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopCity() {
        return shopCity;
    }

    public void setShopCity(String shopCity) {
        this.shopCity = shopCity;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }

    public double getLatitudine() {
        return latitudine;
    }

    public void setLatitudine(double latitudine) {
        this.latitudine = latitudine;
    }

    public double getLongitudine() {
        return longitudine;
    }

    public void setLongitudine(double longitudine) {
        this.longitudine = longitudine;
    }

    public int getQueueIterator() {
        return queueIterator;
    }

    public void setQueueIterator(int queueIterator) {
        this.queueIterator = queueIterator;
    }
}
