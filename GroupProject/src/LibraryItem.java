// ABSTRACTION + will be used for INHERITANCE
public abstract class LibraryItem {
    protected String itemId;
    protected String title;
    protected boolean isAvailable;

    public LibraryItem(String itemId, String title) {
        this.itemId = itemId;
        this.title = title;
        this.isAvailable = true;
    }

    // Abstract method - must be implemented by subclasses
    public abstract String getItemType();

    // Concrete methods
    public String getItemId() { return itemId; }
    public String getTitle() { return title; }
    public boolean isAvailable() { return isAvailable; }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    public void displayBasicInfo() {
        System.out.println("Type: " + getItemType());
        System.out.println("ID: " + itemId + " | Title: " + title);
        System.out.println("Status: " + (isAvailable ? "Available" : "Not Available"));
    }
}