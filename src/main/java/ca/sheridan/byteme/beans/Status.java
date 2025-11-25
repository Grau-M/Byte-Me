package ca.sheridan.byteme.beans;

public enum Status {
    Pending("Pending"),
    Confirmed("Confirmed"),
    Baking("Baking"),
    Ready_for_Shipment("Ready for Shipment"),
    Shipped("Shipped"),
    Delivered("Delivered"),
    Canceled("Canceled");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
