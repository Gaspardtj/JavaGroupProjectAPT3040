package library.dao;

import library.model.Member;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {
    private final Connection connection;

    public MemberDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // CREATE - Add new member
    public boolean addMember(Member member) {
        String sql = "INSERT INTO members (member_code, name, email, phone, membership_date, max_books_allowed) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, member.getMemberCode());
            stmt.setString(2, member.getName());
            stmt.setString(3, member.getEmail());
            stmt.setString(4, member.getPhone());
            stmt.setDate(5, Date.valueOf(member.getMembershipDate()));
            stmt.setInt(6, member.getMaxBooksAllowed());

            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("❌ Error adding member: " + e.getMessage());
            return false;
        }
    }

    // READ - Get all members
    public List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Member member = resultSetToMember(rs);
                members.add(member);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error fetching members: " + e.getMessage());
        }
        return members;
    }

    // Helper method to convert ResultSet to Member object
    private Member resultSetToMember(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setMemberId(rs.getInt("member_id"));
        member.setMemberCode(rs.getString("member_code"));
        member.setName(rs.getString("name"));
        member.setEmail(rs.getString("email"));
        member.setPhone(rs.getString("phone"));
        member.setMembershipDate(rs.getDate("membership_date").toLocalDate());
        member.setMaxBooksAllowed(rs.getInt("max_books_allowed"));
        member.setActiveStatus(rs.getBoolean("active_status"));
        return member;
    }
}