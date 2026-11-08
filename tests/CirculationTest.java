import library.dao.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public class CirculationTest {
    interface Action { void run() throws Exception; }
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    static void rejected(Action a) throws Exception {try{a.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Invalid operation accepted");}
    public static void main(String[] args) throws Exception {
        LibraryRepository repo=new LibraryRepository(); repo.initialize();
        String tag="GUI-TEST-"+UUID.randomUUID(); int book=0,member=0,second=0;
        try {
            repo.saveBook(null,tag,"Test title","Test author","Test",2020,1);
            repo.saveMember(null,tag,"test@example.com","",1,true);
            repo.saveMember(null,tag+"-second","","",3,true);
            LibraryRepository.Snapshot snap=repo.load();
            book=(Integer)snap.books.stream().filter(r->tag.equals(r[1])).findFirst().get()[0];
            member=(Integer)snap.members.stream().filter(r->tag.equals(r[2])).findFirst().get()[0];
            second=(Integer)snap.members.stream().filter(r->(tag+"-second").equals(r[2])).findFirst().get()[0];
            final int b=book,m=member,m2=second;
            repo.issueBook(b,m,LocalDate.now().plusDays(14));
            check((Integer)repo.load().books.stream().filter(r->r[0].equals(b)).findFirst().get()[7]==0,"Issue did not decrement availability");
            rejected(()->repo.issueBook(b,m,LocalDate.now().plusDays(1)));
            rejected(()->repo.issueBook(b,m2,LocalDate.now().plusDays(1)));
            rejected(()->repo.saveBook(b,tag,"Title","Author","Test",2020,0));
            int loan=(Integer)repo.load().loans.stream().filter(r->tag.equals(r[2])&&r[5]==null).findFirst().get()[0];
            try(Connection c=DatabaseConnection.openConnection(); PreparedStatement s=c.prepareStatement("UPDATE loans SET issued_date=CURRENT_DATE-20,due_date=CURRENT_DATE-1 WHERE loan_id=?")){s.setInt(1,loan);s.executeUpdate();}
            check(repo.load().loans.stream().anyMatch(r->r[0].equals(loan)&&"Overdue".equals(r[6])),"Overdue not detected");
            repo.returnBook(loan);rejected(()->repo.returnBook(loan));
            repo.saveMember(m,tag,"test@example.com","",1,false);
            rejected(()->repo.issueBook(b,m,LocalDate.now().plusDays(1)));
            repo.saveMember(m,tag,"test@example.com","",1,true);
            // Two librarians racing for the last copy must create only one loan.
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                List<Future<Boolean>> results=pool.invokeAll(Arrays.asList(
                    ()->{try{repo.issueBook(b,m,LocalDate.now().plusDays(1));return true;}catch(IllegalArgumentException e){return false;}},
                    ()->{try{repo.issueBook(b,m2,LocalDate.now().plusDays(1));return true;}catch(IllegalArgumentException e){return false;}}));
                int wins=0;for(Future<Boolean> r:results)if(r.get())wins++;check(wins==1,"Concurrent issue oversold last copy");
            } finally {pool.shutdownNow();}
            for(Object[] r:repo.load().loans)if(r[5]==null&&(tag.equals(r[2])||(tag+"-second").equals(r[2])))repo.returnBook((Integer)r[0]);
            repo.saveBook(b,tag,"Edited title","Author","Fiction",2021,3);
            check((Integer)repo.load().books.stream().filter(r->r[0].equals(b)).findFirst().get()[7]==3,"Edit did not preserve availability");
            System.out.println("PASS: GUI repository, edits, issue/return, overdue detection, inactive member, borrowing limit, copy constraints and concurrent checkout.");
        } finally {
            try(Connection c=DatabaseConnection.openConnection()) {
                c.setAutoCommit(false);
                try(PreparedStatement s=c.prepareStatement("DELETE FROM loans WHERE book_id IN (SELECT book_id FROM books WHERE isbn=?)")){s.setString(1,tag);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("DELETE FROM books WHERE isbn=?")){s.setString(1,tag);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("DELETE FROM members WHERE name=? OR name=?")){s.setString(1,tag);s.setString(2,tag+"-second");s.executeUpdate();}
                c.commit();
            }
        }
    }
}
