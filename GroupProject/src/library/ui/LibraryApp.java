package library.ui;

import library.dao.LibraryRepository;
import library.dao.LibraryRepository.Snapshot;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/** Native Java desktop workspace. All JDBC work runs off the event dispatch thread. */
public class LibraryApp extends JFrame {
    private static final Color BG = new Color(245,246,251), INK = new Color(30,35,57), MUTED = new Color(119,126,148),
        PURPLE = new Color(104,83,230), SIDE = new Color(29,33,54), LINE = new Color(232,234,243),
        GREEN = new Color(26,153,123), RED = new Color(215,81,101), AMBER = new Color(207,143,36);
    private final LibraryRepository repository = new LibraryRepository();
    private Snapshot data;
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel nav = new JPanel();
    private final JLabel status = new JLabel("Connecting to your library...");
    private final Map<String,JButton> navButtons = new LinkedHashMap<>();
    private String page = "Overview";
    private boolean busy;
    private JTable currentTable;
    private JLabel resultCount;
    private String searchText = "";
    private String loanFilter = "Active loans";
    private final JLabel title = label("Overview", 27, INK, true);
    private final JLabel subtitle = label("A clear view of your library, all in one place.", 13, MUTED, false);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            for (Object key : Collections.list(UIManager.getDefaults().keys())) {
                if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource)
                    UIManager.put(key, new javax.swing.plaf.FontUIResource("Segoe UI",Font.PLAIN,13));
            }
            UIManager.put("OptionPane.background", Color.WHITE);
            UIManager.put("Panel.background", Color.WHITE);
            LibraryApp app = new LibraryApp();
            app.setVisible(true);
            app.load(true);
        });
    }
    public LibraryApp() {
        super("Folio | Library Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050,720));
        setSize(1320,850);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG); setContentPane(root);
        root.add(sidebar(),BorderLayout.WEST);
        JPanel workspace = new JPanel(new BorderLayout()); workspace.setBackground(BG);
        JPanel top = new JPanel(new BorderLayout()); top.setBackground(Color.WHITE); top.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,LINE),new EmptyBorder(18,30,18,30)));
        top.add(label("WORKSPACE   /   LIBRARY MANAGEMENT",11,MUTED,true),BorderLayout.WEST);
        top.add(label("Librarian workspace",13,INK,true),BorderLayout.EAST);
        workspace.add(top,BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0,23)); center.setOpaque(false); center.setBorder(new EmptyBorder(28,30,18,30));
        JPanel heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        JPanel titles = stack(); titles.add(title); titles.add(Box.createVerticalStrut(6)); titles.add(subtitle); heading.add(titles,BorderLayout.WEST);
        JButton refresh = button("Refresh",false,() -> load(false));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0)); right.setOpaque(false);
        right.add(label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),12,MUTED,false)); right.add(refresh);
        heading.add(right,BorderLayout.EAST); center.add(heading,BorderLayout.NORTH);
        body.setOpaque(false); center.add(body,BorderLayout.CENTER);
        status.setFont(new Font("Segoe UI",Font.PLAIN,12)); status.setForeground(MUTED); center.add(status,BorderLayout.SOUTH);
        workspace.add(center,BorderLayout.CENTER); root.add(workspace,BorderLayout.CENTER);
        showLoading();
    }
    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout()); side.setBackground(SIDE); side.setPreferredSize(new Dimension(222,0));
        JPanel brand = new JPanel(); brand.setOpaque(false); brand.setLayout(new BoxLayout(brand,BoxLayout.Y_AXIS)); brand.setBorder(new EmptyBorder(30,26,32,20));
        brand.add(label("folio.",35,Color.WHITE,true)); brand.add(Box.createVerticalStrut(4)); brand.add(label("LIBRARY MANAGER",10,new Color(153,161,190),true)); side.add(brand,BorderLayout.NORTH);
        nav.setOpaque(false); nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS)); nav.setBorder(new EmptyBorder(0,14,0,14));
        JLabel caption = label("  YOUR LIBRARY",10,new Color(139,148,178),true); caption.setAlignmentX(Component.LEFT_ALIGNMENT); nav.add(caption); nav.add(Box.createVerticalStrut(16));
        String[][] items = {{"Overview","01"},{"Books","02"},{"Members","03"},{"Circulation","04"},{"Overdue","05"}};
        for (String[] item : items) {
            JButton b = new JButton(item[1] + "    " + item[0]); b.setHorizontalAlignment(SwingConstants.LEFT); b.setFont(new Font("Segoe UI",Font.BOLD,13));
            b.setBorder(new EmptyBorder(14,16,14,10)); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setMaximumSize(new Dimension(200,48)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.addActionListener(e -> { if (!busy) { page=item[0]; searchText=""; render(); } }); navButtons.put(item[0],b); nav.add(b); nav.add(Box.createVerticalStrut(7));
        }
        side.add(nav,BorderLayout.CENTER);
        JPanel bottom = new JPanel(); bottom.setOpaque(false); bottom.setLayout(new BoxLayout(bottom,BoxLayout.Y_AXIS)); bottom.setBorder(new EmptyBorder(22,26,26,20));
        bottom.add(label("A place for every story.",12,new Color(183,189,211),false)); bottom.add(Box.createVerticalStrut(7)); bottom.add(label("Folio / Desktop edition",10,new Color(125,136,168),false)); side.add(bottom,BorderLayout.SOUTH);
        return side;
    }
    private void showLoading() { body.removeAll(); body.add(label("Opening your library...",18,MUTED,false)); }
    @FunctionalInterface private interface Task { void run() throws Exception; }
    private void load(boolean initialize) { work(() -> { if (initialize) repository.initialize(); },"Library is up to date.",null); }
    private void work(Task task, String success, JDialog dialog) {
        if (busy) return;
        busy=true; status.setForeground(MUTED); status.setText("Working...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (dialog != null) dialog.setEnabled(false);
        new SwingWorker<Snapshot,Void>() {
            boolean actionCompleted;
            protected Snapshot doInBackground() throws Exception { task.run(); actionCompleted=true; return repository.load(); }
            protected void done() {
                busy=false; setCursor(Cursor.getDefaultCursor());
                if (dialog != null) dialog.setEnabled(true);
                try {
                    data=get(); if (dialog != null) dialog.dispose(); render(); status.setForeground(GREEN); status.setText(success);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) {
                    Throwable cause=e.getCause(); String message=cause.getMessage();
                    if (cause instanceof java.sql.SQLException) {
                        String state=((java.sql.SQLException)cause).getSQLState();
                        if ("23505".equals(state)) message="This ISBN or member code already exists. Please use a unique value.";
                        else if (state != null && state.startsWith("08")) message="The database connection was interrupted. Check PostgreSQL and try Refresh.";
                    }
                    if (actionCompleted && dialog != null) { dialog.dispose(); message="The change was saved, but refreshing failed. Click Refresh before trying again.\n" + message; }
                    status.setForeground(RED); status.setText("Unable to complete the request. You can try again.");
                    if (data == null) {
                        body.removeAll(); JPanel error=stack(); error.add(label("Your library could not be opened",22,INK,true)); error.add(Box.createVerticalStrut(12));
                        error.add(label("Check your connection settings and that PostgreSQL is running.",13,MUTED,false)); error.add(Box.createVerticalStrut(18)); error.add(button("Try again",true,() -> load(true))); body.add(error); body.revalidate(); body.repaint();
                    }
                    JOptionPane.showMessageDialog(dialog == null ? LibraryApp.this : dialog,message,"Please check",JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    private void render() {
        for (Map.Entry<String,JButton> e : navButtons.entrySet()) {
            e.getValue().setBackground(e.getKey().equals(page) ? PURPLE : SIDE);
            e.getValue().setForeground(e.getKey().equals(page) ? Color.WHITE : new Color(182,190,212));
        }
        title.setText(page.equals("Overview") ? "Library overview" : page.equals("Books") ? "Book collection" : page.equals("Overdue") ? "Overdue books" : page);
        String desc=page.equals("Overview") ? "A clear view of your library, all in one place." : page.equals("Books") ? "Organize your collection and keep every copy accounted for." : page.equals("Members") ? "Your reading community, thoughtfully organized." : page.equals("Overdue") ? "Follow up with members and bring books back to the shelves." : "Issue books, manage returns and explore borrowing history.";
        subtitle.setText(desc); body.removeAll(); currentTable=null;
        if (data != null) body.add(page.equals("Overview") ? dashboard() : records(),BorderLayout.CENTER);
        body.revalidate(); body.repaint();
    }
    private JPanel dashboard() {
        JPanel dashboard = new JPanel(new BorderLayout(0,22)); dashboard.setOpaque(false);
        int available=data.books.stream().mapToInt(r -> (Integer)r[7]).sum();
        int total=data.books.stream().mapToInt(r -> (Integer)r[6]).sum();
        long active=data.loans.stream().filter(r -> r[5]==null).count();
        long overdue=data.loans.stream().filter(r -> "Overdue".equals(r[6])).count();
        JPanel metrics=new JPanel(new GridLayout(1,4,16,0)); metrics.setOpaque(false);
        metrics.add(metric("BOOK TITLES",String.valueOf(data.books.size()),total+" copies in your collection",PURPLE));
        metrics.add(metric("MEMBERS",String.valueOf(data.members.size()),data.members.stream().filter(r -> Boolean.TRUE.equals(r[7])).count()+" active memberships",GREEN));
        metrics.add(metric("ON LOAN",String.valueOf(active),"Books currently with readers",AMBER));
        metrics.add(metric("OVERDUE",String.valueOf(overdue),overdue==0 ? "You're all caught up" : "Ready for a friendly reminder",RED)); dashboard.add(metrics,BorderLayout.NORTH);
        JPanel lower=new JPanel(new BorderLayout(20,20)); lower.setOpaque(false);
        JPanel left=new JPanel(new BorderLayout(0,20)); left.setOpaque(false);
        JPanel quick=card(new BorderLayout(0,15)); JPanel intro=stack(); intro.add(label("Keep your library moving",20,INK,true)); intro.add(Box.createVerticalStrut(7)); intro.add(label("Add a new arrival or help a reader find their next chapter.",12,MUTED,false)); quick.add(intro,BorderLayout.NORTH);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); actions.setOpaque(false); actions.add(button("+ Add book",true,() -> bookForm(null))); actions.add(button("+ Add member",false,() -> memberForm(null))); actions.add(button("Issue a book",false,this::issueForm)); quick.add(actions,BorderLayout.SOUTH); left.add(quick,BorderLayout.NORTH);
        JPanel recent=card(new BorderLayout(0,16)); JPanel head=new JPanel(new BorderLayout()); head.setOpaque(false); head.add(label("Recent borrowing",17,INK,true),BorderLayout.WEST); head.add(button("View all",false,() -> { page="Circulation"; searchText=""; loanFilter="All history"; render(); }),BorderLayout.EAST); recent.add(head,BorderLayout.NORTH);
        List<Object[]> rows=new ArrayList<>(); for(Object[] r:data.loans.subList(0,Math.min(8,data.loans.size()))) rows.add(new Object[]{r[1],r[2],r[4],r[6]});
        recent.add(tablePanel(table(new String[]{"Book","Member","Due date","Status"},rows),rows.isEmpty(),"No borrowing activity yet","Issue your first book to see activity here."),BorderLayout.CENTER); left.add(recent,BorderLayout.CENTER); lower.add(left,BorderLayout.CENTER);
        JPanel right=card(new BorderLayout(0,8)); right.setPreferredSize(new Dimension(280,0)); right.add(label("Collection availability",17,INK,true),BorderLayout.NORTH);
        JPanel chartArea=new JPanel(); chartArea.setOpaque(false); chartArea.setLayout(new BoxLayout(chartArea,BoxLayout.Y_AXIS));
        Donut chart=new Donut(available,total); chart.setPreferredSize(new Dimension(230,240)); chart.setMaximumSize(new Dimension(280,270)); chartArea.add(chart);
        chartArea.add(legend("Available",available,GREEN)); chartArea.add(Box.createVerticalStrut(15)); chartArea.add(legend("Checked out",total-available,PURPLE)); chartArea.add(Box.createVerticalStrut(24));
        JLabel hint=label(total==0 ? "Add books to build your collection." : "Live counts across all book titles.",11,MUTED,false); hint.setAlignmentX(Component.CENTER_ALIGNMENT); chartArea.add(hint); right.add(chartArea,BorderLayout.CENTER);
        lower.add(right,BorderLayout.EAST); dashboard.add(lower,BorderLayout.CENTER); return dashboard;
    }
    private JPanel metric(String heading,String value,String foot,Color accent) {
        JPanel p=card(new BorderLayout(0,10)); p.setBorder(new CompoundBorder(new MatteBorder(3,0,0,0,accent),new EmptyBorder(18,20,18,16)));
        p.add(label(heading,10,MUTED,true),BorderLayout.NORTH); p.add(label(value,33,INK,true),BorderLayout.CENTER); p.add(label(foot,11,MUTED,false),BorderLayout.SOUTH); return p;
    }
    private JPanel legend(String text,int value,Color color) {
        JPanel p=new JPanel(new BorderLayout()); p.setOpaque(false); p.setMaximumSize(new Dimension(230,25)); p.add(label("  ●  "+text,13,color,true),BorderLayout.WEST); p.add(label(String.valueOf(value),14,INK,true),BorderLayout.EAST); return p;
    }
    private JPanel records() {
        JPanel panel=card(new BorderLayout(0,18));
        JPanel tools=new JPanel(new BorderLayout(12,12)); tools.setOpaque(false);
        JTextField search=new JTextField(searchText); search.setToolTipText("Search the table"); search.setPreferredSize(new Dimension(250,37)); search.setBorder(new CompoundBorder(new LineBorder(LINE),new EmptyBorder(8,11,8,11)));
        JPanel searchArea=new JPanel(new BorderLayout(10,0)); searchArea.setOpaque(false); searchArea.add(label("Search",12,MUTED,true),BorderLayout.WEST); searchArea.add(search); tools.add(searchArea,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actions.setOpaque(false);
        List<Object[]> rows=new ArrayList<>(); String[] headers;
        if (page.equals("Books")) {
            headers=new String[]{"ID","ISBN","Title","Author","Genre","Year","Copies","Available"}; rows.addAll(data.books);
            actions.add(button("Edit selected",false,() -> { Object[] r=selected(data.books); if(r!=null) bookForm(r); })); actions.add(button("+ Add book",true,() -> bookForm(null)));
        } else if (page.equals("Members")) {
            headers=new String[]{"ID","Member code","Name","Email","Phone","Joined","Limit","Status"};
            for(Object[] r:data.members) { Object[] copy=r.clone(); copy[7]=Boolean.TRUE.equals(r[7]) ? "Active" : "Inactive"; rows.add(copy); }
            actions.add(button("Edit selected",false,() -> { Object[] r=selected(data.members); if(r!=null) memberForm(r); })); actions.add(button("+ Add member",true,() -> memberForm(null)));
        } else {
            headers=new String[]{"Loan","Book","Member","Issued","Due date","Returned","Status"};
            for(Object[] r:data.loans) if(page.equals("Overdue") ? "Overdue".equals(r[6]) : loanFilter.equals("All history") || (loanFilter.equals("Active loans") ? r[5]==null : r[5]!=null)) rows.add(r);
            if (!page.equals("Overdue")) {
                JComboBox<String> filter=new JComboBox<>(new String[]{"Active loans","All history","Returned"}); filter.setSelectedItem(loanFilter); filter.addActionListener(e -> { loanFilter=(String)filter.getSelectedItem(); render(); }); actions.add(filter);
            }
            actions.add(button("Return selected",false,this::returnSelected)); actions.add(button("Issue book",true,this::issueForm));
        }
        tools.add(actions,BorderLayout.EAST); panel.add(tools,BorderLayout.NORTH);
        currentTable=table(headers,rows); JTable table=currentTable;
        TableRowSorter<TableModel> sorter=new TableRowSorter<>(table.getModel()); table.setRowSorter(sorter);
        resultCount=label("",12,MUTED,false);
        Runnable filterRows=() -> { sorter.setRowFilter(search.getText().isBlank() ? null : RowFilter.regexFilter("(?iu)"+Pattern.quote(search.getText().trim()))); resultCount.setText(table.getRowCount()+" records  ·  Select a row to manage it"); };
        search.getDocument().addDocumentListener(new DocumentListener(){ public void insertUpdate(DocumentEvent e){changed();} public void removeUpdate(DocumentEvent e){changed();} public void changedUpdate(DocumentEvent e){changed();} void changed(){searchText=search.getText();filterRows.run();} }); filterRows.run();
        String empty=page.equals("Books") ? "Your shelves are ready" : page.equals("Members") ? "Meet your reading community" : page.equals("Overdue") ? "Nothing overdue" : "No loans to display";
        String detail=page.equals("Books") ? "Add your first book to begin building the collection." : page.equals("Members") ? "Add a member to start lending books." : page.equals("Overdue") ? "Books returned on time make room for more stories." : "Issue a book or choose another history filter.";
        panel.add(tablePanel(table,rows.isEmpty(),empty,detail),BorderLayout.CENTER); panel.add(resultCount,BorderLayout.SOUTH);
        return panel;
    }
    private Object[] selected(List<Object[]> source) {
        if (currentTable==null || currentTable.getSelectedRow()<0) { JOptionPane.showMessageDialog(this,"Select a row in the table first."); return null; }
        Object id=currentTable.getValueAt(currentTable.getSelectedRow(),0);
        return source.stream().filter(r -> r[0].equals(id)).findFirst().orElse(null);
    }
    private void returnSelected() {
        Object[] r=selected(data.loans); if(r==null) return;
        if(r[5]!=null) { JOptionPane.showMessageDialog(this,"This book has already been returned."); return; }
        if(JOptionPane.showConfirmDialog(this,"Mark \""+r[1]+"\" as returned by "+r[2]+"?","Return book",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)
            work(() -> repository.returnBook((Integer)r[0]),"Book returned. The copy is available again.",null);
    }
    private void bookForm(Object[] row) {
        if(busy) return;
        Form f=new Form(row==null ? "Add a book" : "Edit book", "Give every title a home in your collection.");
        JTextField isbn=f.text("ISBN *",row==null ? "" : row[1]); JTextField name=f.text("Title *",row==null ? "" : row[2]);
        JTextField author=f.text("Author *",row==null ? "" : row[3]); JTextField genre=f.text("Genre",row==null ? "" : row[4]);
        JSpinner year=f.number("Published year *",row==null ? LocalDate.now().getYear() : (Integer)row[5],1,9999);
        JSpinner copies=f.number("Total copies *",row==null ? 1 : (Integer)row[6],0,1000000);
        f.finish("Save book",() -> {
            try {
                year.commitEdit(); copies.commitEdit();
                String i=isbn.getText().trim(), n=name.getText().trim(), a=author.getText().trim(), g=genre.getText().trim();
                if(i.isEmpty() || n.isEmpty() || a.isEmpty()) throw new IllegalArgumentException("ISBN, title and author are required.");
                int y=(Integer)year.getValue(), count=(Integer)copies.getValue();
                work(() -> repository.saveBook(row==null ? null : (Integer)row[0],i,n,a,g,y,count),"Book saved successfully.",f);
            } catch(Exception e) { f.error(e.getMessage()); }
        });
    }
    private void memberForm(Object[] row) {
        if(busy) return;
        Form f=new Form(row==null ? "Add a member" : "Edit member","Make room for another reader.");
        JTextField name=f.text("Full name *",row==null ? "" : row[2]); JTextField email=f.text("Email",row==null ? "" : row[3]); JTextField phone=f.text("Phone",row==null ? "" : row[4]);
        JSpinner limit=f.number("Borrowing limit *",row==null ? 3 : (Integer)row[6],1,1000);
        JCheckBox active=new JCheckBox("Active membership",row==null || Boolean.TRUE.equals(row[7])); active.setOpaque(false); f.field("Membership",active);
        f.finish("Save member",() -> {
            try {
                limit.commitEdit(); String n=name.getText().trim(),e=email.getText().trim(),p=phone.getText().trim(); int l=(Integer)limit.getValue(); boolean a=active.isSelected();
                if(n.isEmpty()) throw new IllegalArgumentException("Please enter a member name.");
                work(() -> repository.saveMember(row==null ? null : (Integer)row[0],n,e,p,l,a),"Member saved successfully.",f);
            } catch(Exception e) { f.error(e.getMessage()); }
        });
    }
    private static class Choice {
        final int id; final String name;
        Choice(int id,String name){this.id=id;this.name=name;}
        public String toString(){return name;}
    }
    private void issueForm() {
        if(busy) return;
        List<Choice> books=new ArrayList<>(),members=new ArrayList<>();
        for(Object[] r:data.books) if((Integer)r[7]>0) books.add(new Choice((Integer)r[0],r[2]+"  |  ISBN "+r[1]+"  ("+r[7]+" available)"));
        for(Object[] r:data.members) if(Boolean.TRUE.equals(r[7])) members.add(new Choice((Integer)r[0],r[2]+"  |  "+r[1]));
        if(books.isEmpty() || members.isEmpty()) { JOptionPane.showMessageDialog(this,"You need an available book and an active member before issuing a loan.","Ready to lend?",JOptionPane.INFORMATION_MESSAGE); return; }
        Form f=new Form("Issue a book","Connect a reader with their next great read.");
        JComboBox<Choice> book=new JComboBox<>(books.toArray(new Choice[0])), member=new JComboBox<>(members.toArray(new Choice[0])); f.field("Available book *",book); f.field("Member *",member);
        JTextField due=f.text("Due date (YYYY-MM-DD) *",LocalDate.now().plusDays(14));
        f.finish("Issue book",() -> {
            try {
                LocalDate date=LocalDate.parse(due.getText().trim()); if(date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Choose today or a future due date.");
                int b=((Choice)book.getSelectedItem()).id, m=((Choice)member.getSelectedItem()).id;
                work(() -> repository.issueBook(b,m,date),"Book issued successfully.",f);
            } catch(Exception e) { f.error("Check the due date. Use YYYY-MM-DD and choose today or a future date."); }
        });
    }
    private class Form extends JDialog {
        private final JPanel fields=new JPanel(new GridBagLayout()); private int row;
        private final JLabel error=label(" ",12,RED,false);
        Form(String heading,String description) {
            super(LibraryApp.this,heading,true); setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            JPanel content=card(new BorderLayout(0,22)); content.setBorder(new EmptyBorder(28,30,24,30)); setContentPane(content);
            JPanel intro=stack(); intro.add(label(heading,25,INK,true)); intro.add(Box.createVerticalStrut(8)); intro.add(label(description,12,MUTED,false)); content.add(intro,BorderLayout.NORTH);
            fields.setOpaque(false); content.add(fields,BorderLayout.CENTER);
        }
        JTextField text(String label,Object value) { JTextField field=new JTextField(String.valueOf(value)); field(fieldLabel(label),field); return field; }
        String fieldLabel(String label){return label;}
        JSpinner number(String label,int value,int min,int max){JSpinner s=new JSpinner(new SpinnerNumberModel(value,min,Math.max(max,value),1)); s.setEditor(new JSpinner.NumberEditor(s,"#")); field(label,s);return s;}
        void field(String name,JComponent component) {
            GridBagConstraints g=new GridBagConstraints(); g.gridx=0;g.gridy=row++;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(0,0,7,0); fields.add(label(name,12,INK,true),g);
            g.gridy=row++;g.insets=new Insets(0,0,16,0);component.setPreferredSize(new Dimension(440,36)); fields.add(component,g);
        }
        void error(String message){error.setText(message==null ? "Please check the entered values." : message);}
        void finish(String action,Runnable save){
            JPanel foot=stack(); foot.add(error);foot.add(Box.createVerticalStrut(12)); JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));buttons.setOpaque(false);
            buttons.add(button("Cancel",false,() -> {if(!busy) dispose();}));JButton submit=button(action,true,save);buttons.add(submit);foot.add(buttons);getContentPane().add(foot,BorderLayout.SOUTH);getRootPane().setDefaultButton(submit);
            pack();setMinimumSize(new Dimension(530,getHeight()));setLocationRelativeTo(LibraryApp.this);setVisible(true);
        }
    }
    private static JPanel stack(){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p;}
    private static JPanel card(LayoutManager layout){JPanel p=new JPanel(layout);p.setBackground(Color.WHITE);p.setBorder(new CompoundBorder(new LineBorder(LINE),new EmptyBorder(22,22,22,22)));return p;}
    private static JLabel label(String s,int size,Color color,boolean bold){JLabel l=new JLabel(s);l.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size));l.setForeground(color);return l;}
    private JButton button(String text,boolean primary,Runnable action){
        JButton b=new JButton(text);b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setBackground(primary?PURPLE:Color.WHITE);b.setForeground(primary?Color.WHITE:INK);b.setOpaque(true);b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(primary?PURPLE:LINE),new EmptyBorder(10,15,10,15)));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(e -> {if(!busy) action.run();});return b;
    }
    private JTable table(String[] headers,List<Object[]> rows){
        DefaultTableModel model=new DefaultTableModel(headers,0){ public boolean isCellEditable(int r,int c){return false;} public Class<?> getColumnClass(int c){for(int r=0;r<getRowCount();r++){Object v=getValueAt(r,c);if(v!=null)return v.getClass();}return Object.class;}};
        for(Object[] row:rows) model.addRow(row);
        JTable t=new JTable(model);t.setRowHeight(46);t.setFont(new Font("Segoe UI",Font.PLAIN,12));t.setForeground(INK);t.setSelectionBackground(new Color(237,233,255));t.setSelectionForeground(INK);t.setShowGrid(false);t.setIntercellSpacing(new Dimension(0,0));t.setFillsViewportHeight(true);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setAutoCreateRowSorter(true);
        t.getTableHeader().setPreferredSize(new Dimension(0,40));t.getTableHeader().setReorderingAllowed(false);t.getTableHeader().setBackground(new Color(247,248,252));t.getTableHeader().setForeground(MUTED);t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,11));
        DefaultTableCellRenderer renderer=new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col){
            JLabel l=(JLabel)super.getTableCellRendererComponent(table,value,selected,focus,row,col);l.setBorder(new EmptyBorder(0,10,0,10));l.setBackground(selected?new Color(237,233,255):row%2==0?Color.WHITE:new Color(250,251,254));l.setForeground(INK);
            if("Overdue".equals(value)||"Inactive".equals(value))l.setForeground(RED);else if("Returned".equals(value)||"Active".equals(value))l.setForeground(GREEN);else if("On loan".equals(value))l.setForeground(PURPLE);
            l.setToolTipText(value==null?null:value.toString());return l;}};
        for(int i=0;i<t.getColumnCount();i++){t.getColumnModel().getColumn(i).setCellRenderer(renderer);String h=headers[i];t.getColumnModel().getColumn(i).setPreferredWidth(h.equals("Title")||h.equals("Book")||h.equals("Email")?210:h.equals("ID")||h.equals("Loan")?45:110);}
        return t;
    }
    private JComponent tablePanel(JTable table,boolean empty,String heading,String detail){
        if(empty){JPanel p=new JPanel(new GridBagLayout());p.setBackground(Color.WHITE);JPanel text=stack();JLabel icon=label("[  ]",36,new Color(200,193,244),false);icon.setAlignmentX(Component.CENTER_ALIGNMENT);text.add(icon);text.add(Box.createVerticalStrut(18));JLabel h=label(heading,18,INK,true);h.setAlignmentX(Component.CENTER_ALIGNMENT);text.add(h);text.add(Box.createVerticalStrut(10));JLabel d=label(detail,12,MUTED,false);d.setAlignmentX(Component.CENTER_ALIGNMENT);text.add(d);p.add(text);return p;}
        JScrollPane scroll=new JScrollPane(table);scroll.setBorder(new MatteBorder(1,0,1,0,LINE));scroll.getViewport().setBackground(Color.WHITE);return scroll;
    }
    private static class Donut extends JComponent {
        final int available,total;
        Donut(int a,int t){available=a;total=t;}
        protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int d=Math.min(getWidth()-40,getHeight()-50),x=(getWidth()-d)/2,y=(getHeight()-d)/2;g.setStroke(new BasicStroke(20,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));g.setColor(LINE);g.drawOval(x+12,y+12,d-24,d-24);
            if(total>0){g.setColor(PURPLE);g.drawArc(x+12,y+12,d-24,d-24,90,360);g.setColor(GREEN);g.drawArc(x+12,y+12,d-24,d-24,90,-(int)Math.round(360.0*available/total));}
            String count=total==0?"0":Math.round(100.0*available/total)+"%";g.setFont(new Font("Segoe UI",Font.BOLD,33));g.setColor(INK);g.drawString(count,(getWidth()-g.getFontMetrics().stringWidth(count))/2,getHeight()/2+6);
            String sub=total==0?"no books yet":"available";g.setFont(new Font("Segoe UI",Font.PLAIN,12));g.setColor(MUTED);g.drawString(sub,(getWidth()-g.getFontMetrics().stringWidth(sub))/2,getHeight()/2+30);g.dispose();
        }
    }
}
