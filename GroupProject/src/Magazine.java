// INHERITANCE: Magazine IS-A LibraryItem
public class Magazine extends LibraryItem {
    private final String issueNumber;
    private final String publisher;
    private final String publicationDate;

    public Magazine(String itemId, String title, String issueNumber, String publisher, String publicationDate) {
        super(itemId, title);
        this.issueNumber = issueNumber;
        this.publisher = publisher;
        this.publicationDate = publicationDate;
    }

    // Implement abstract method - POLYMORPHISM
    @Override
    public String getItemType() {
        return "Magazine";
    }

    @Override
    public void displayBasicInfo() {
        super.displayBasicInfo();
        System.out.println("Issue: " + issueNumber + " | Publisher: " + publisher);
        System.out.println("Publication Date: " + publicationDate);
    }

    // Magazine-specific methods
    public String getIssueNumber() { return issueNumber; }
    public String getPublisher() { return publisher; }
    public String getPublicationDate() { return publicationDate; }
}