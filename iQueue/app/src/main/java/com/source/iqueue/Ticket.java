package com.source.iqueue;

public class Ticket {
    private long code;
    private int number;
    private String qrCodeName;

    public Ticket() {
    }

    public Ticket(long code, int number, String qrCodeName) {
        this.code = code;
        this.number = number;
        this.qrCodeName = qrCodeName;
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getQrCodeName() {
        return qrCodeName;
    }

    public void setQrCodeName(String qrCodeName) {
        this.qrCodeName = qrCodeName;
    }
}
