package library.model;

import java.time.LocalDate;

public class Member {
    private int memberId;
    private String memberCode;
    private String name;
    private String email;
    private String phone;
    private LocalDate membershipDate;
    private int maxBooksAllowed;
    private boolean activeStatus;

    // Constructors
    public Member() {}

    public Member(String memberCode, String name, String email, String phone) {
        this.memberCode = memberCode;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.membershipDate = LocalDate.now();
        this.maxBooksAllowed = 3;
        this.activeStatus = true;
    }

    // Getters and Setters
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getMembershipDate() { return membershipDate; }
    public void setMembershipDate(LocalDate membershipDate) { this.membershipDate = membershipDate; }

    public int getMaxBooksAllowed() { return maxBooksAllowed; }
    public void setMaxBooksAllowed(int maxBooksAllowed) { this.maxBooksAllowed = maxBooksAllowed; }

    public boolean isActiveStatus() { return activeStatus; }
    public void setActiveStatus(boolean activeStatus) { this.activeStatus = activeStatus; }

    // Business methods
    public void displayInfo() {
        System.out.println("👤 " + name + " (" + memberCode + ")");
        System.out.println("   Email: " + email + " | Phone: " + phone);
        System.out.println("   Member since: " + membershipDate);
        System.out.println("   Status: " + (activeStatus ? "ACTIVE" : "INACTIVE"));
    }
}