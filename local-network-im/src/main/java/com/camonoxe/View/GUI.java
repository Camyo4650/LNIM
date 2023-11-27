package com.camonoxe.View;
//Generated by GuiGenie - Copyright (c) 2004 Mario Awad.
//Home Page http://guigenie.cjb.net - Check often for new versions!

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.camonoxe.Model.MessageLogs;
import com.camonoxe.Model.SendMessageDel;
import com.camonoxe.Model.SyncDel;
import com.camonoxe.Model.UpdateMessagesDel;
import com.camonoxe.Model.UserTable.User;
import com.camonoxe.Model.UsersChangedDel;

public class GUI extends JFrame implements UpdateMessagesDel, WindowStateListener, UsersChangedDel {
    private JList<UserEnvelope> uxParticipants;
    private DefaultListModel<UserEnvelope> participantsList;
    private JTextPane uxMessages;
    private JTextArea uxText;
    private KeyAdapter sharedKeyAdapter;

    private SendMessageDel sendMessageDel;
    private SyncDel syncDel;

    private UUID userOnDisplay;
    private SimpleAttributeSet readFont;    //            _________________
    private SimpleAttributeSet unReadFont;  //           |                 |
    private SimpleAttributeSet usFont;      // us..      |        /\       |
    private SimpleAttributeSet themFont;    // and them..|   __--/  \==__  |
                                            //           |_--   /____\  ==_|
                                            //           |                 |                          
                                            //           |                 |
                                            //           |_________________|

    private boolean isAlive;
    private StyledDocument document;
    private Style usStyle;
    private Style centerStyle;
    private Style themStyle;
    
    private static final int CHAT_ROW_LIMIT = 4;

    public GUI(String name, SendMessageDel del, SyncDel del2) {
        readFont = new SimpleAttributeSet();
        StyleConstants.setForeground(readFont, new Color(0x000000));

        usFont = new SimpleAttributeSet(readFont);
        StyleConstants.setBold(usFont, true);
        StyleConstants.setAlignment(usFont, StyleConstants.ALIGN_RIGHT);
        themFont = new SimpleAttributeSet(readFont);
        StyleConstants.setBold(themFont, false);
        StyleConstants.setAlignment(themFont, StyleConstants.ALIGN_LEFT);

        unReadFont = new SimpleAttributeSet(readFont);
        StyleConstants.setForeground(unReadFont, new Color(0x00b300));

        sendMessageDel = del;
        syncDel = del2;
        participantsList = new DefaultListModel<>();
        userOnDisplay = null;
        setTitle(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(700, 600));
        addWindowStateListener(this);

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(700, 600));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setLayout(new BorderLayout(5, 5));
        JPanel rightPanel = new JPanel(new BorderLayout(5,5));
        rightPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        uxText = new JTextArea();
        uxText.addKeyListener(new KeyAdapter() {
            Set<Integer> keysHeld = new HashSet<Integer>();

            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiersEx() != KeyEvent.SHIFT_DOWN_MASK)
                {
                    e.consume();
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK)
                {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            uxText.insert("\n", uxText.getCaretPosition());
                        }
                    });
                }
                if (keysHeld.contains(e.getKeyCode())) return;
                keysHeld.add(e.getKeyCode());
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiersEx() != KeyEvent.SHIFT_DOWN_MASK)
                {
                    attemptSendMessage();
                }
            }

            public void keyReleased(KeyEvent e)
            {
                keysHeld.remove(e.getKeyCode());
            }
        });

        uxParticipants = new JList<UserEnvelope>(participantsList);
        uxParticipants.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        uxParticipants.setPreferredSize(new Dimension(200, 0));
        uxParticipants.setMinimumSize(new Dimension(200, 0));
        uxParticipants.setCellRenderer(new ClientCellRenderer());
        uxParticipants.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    if (participantsList.size() == 0 || uxParticipants.getSelectedIndex() == -1) return;
                    UserEnvelope ue = participantsList.get(uxParticipants.getSelectedIndex());
                    userOnDisplay = ue.getUser().getUserId();
                    uxMessages.setText("");
                    GetAllMessages();
                }
            }
            
        });
        JScrollPane scrollParty = new JScrollPane(uxParticipants);

        StyleContext styleContext = new StyleContext();
        document = new DefaultStyledDocument(styleContext);
        Style defaultStyle = styleContext.getStyle(StyleContext.DEFAULT_STYLE);
        usStyle = styleContext.addStyle("us", defaultStyle);
        centerStyle = styleContext.addStyle("center", defaultStyle);
        themStyle = styleContext.addStyle("them", defaultStyle);
        StyleConstants.setAlignment(usStyle, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setAlignment(centerStyle, StyleConstants.ALIGN_CENTER);
        StyleConstants.setAlignment(themStyle, StyleConstants.ALIGN_LEFT);
        uxMessages = new JTextPane(document);
        uxMessages.setEditable(false);
        uxText.setRows(CHAT_ROW_LIMIT);
        JScrollPane scrollMessages = new JScrollPane(uxMessages);
        rightPanel.add(scrollMessages, BorderLayout.CENTER);

        JScrollPane scrollText = new JScrollPane(uxText);
        rightPanel.add(scrollText, BorderLayout.SOUTH);

        JSplitPane jsplitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollParty, rightPanel);

        panel.add(jsplitpane, BorderLayout.CENTER);
        
        sharedKeyAdapter = new KeyAdapter() {
            Set<Integer> keysHeld = new HashSet<Integer>();

            public void keyPressed(KeyEvent e)
            {
                if (keysHeld.contains(e.getKeyCode())) return;
                keysHeld.add(e.getKeyCode());
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    attemptSendMessage();
                }
            }

            public void keyReleased(KeyEvent e)
            {
                keysHeld.remove(e.getKeyCode());
            }
        };
        uxParticipants.addKeyListener(sharedKeyAdapter);
        uxMessages.addKeyListener(sharedKeyAdapter);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu connectMenu = new JMenu("Connect");
        JMenuItem refresh = new JMenuItem("Sync");
        refresh.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                syncDel.refresh();
            }
            
        });
        menuBar.add(fileMenu);
        connectMenu.add(refresh);
        menuBar.add(connectMenu);
        setJMenuBar(menuBar);
        add(panel);
        pack();
        setVisible(true);
        isAlive = true;
    }

    private void attemptSendMessage()
    {
        if (StringUtils.isBlank(uxText.getText())) return;
        if (userOnDisplay == null) {
            uxMessages.setText("");
            document.setLogicalStyle(document.getLength(), centerStyle);
            try {
                document.insertString(document.getLength(), "**NO CONVERSATION SELECTED**\n", null);
            } catch (BadLocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UserEnvelope ue = getUserEnvByUserId(userOnDisplay);
                if (ue == null) {
                    return;
                }
                ue.setMessagesCursor(uxMessages.getDocument().getLength() + uxText.getText().length() + 1);
                sendMessageDel.MessageHandler(userOnDisplay, uxText.getText());
                ue.readMessages();
                uxText.setText("");
                uxParticipants.validate();
                uxParticipants.repaint();
            }
        });
    }

    @Override
    public void windowStateChanged(WindowEvent e) {
        if (e.getNewState() == WindowEvent.WINDOW_CLOSING)
        {
            isAlive = false;
        }
    }

    public boolean isAlive()
    {
        return isAlive;
    }

    private UserEnvelope getUserEnvByUserId(UUID userId)
    {
        for (int i = 0; i < participantsList.size(); i++) {
            UserEnvelope ue = participantsList.get(i);
            if (ue.getUser().getUserId().compareTo(userId) == 0) 
                return ue;           
        }
        return null;
    }

    private void GetAllMessages()
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Iterator<Pair<Boolean, String>> messages = MessageLogs.getMessagesByUserId(userOnDisplay);
                UserEnvelope ue = getUserEnvByUserId(userOnDisplay);
                MutableAttributeSet set = new SimpleAttributeSet();
                try {
                    document.setLogicalStyle(document.getLength(), centerStyle);
                    document.insertString(document.getLength(), "**START OF CONVERSATION**\n\n", set);
                    if (messages == null) return;
                    while (messages.hasNext()) {
                        Pair<Boolean, String> message = messages.next();
                        colorChat(set, message, ue);
                    }
                } catch (BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    private void colorChat(MutableAttributeSet set, Pair<Boolean, String> message, UserEnvelope ue) throws BadLocationException
    {
        int num = 0;
        String[] lines = message.getValue().split("\n");
        for (String line : lines) {
            if (message.getKey())
            {
                document.setLogicalStyle(document.getLength(), usStyle);
            } else {
                document.setLogicalStyle(document.getLength(), themStyle);
            }
            if (document.getLength() > ue.getMessagesCursor())
            {
                StyleConstants.setBold(set, true);
            } else {
                StyleConstants.setBold(set, false);
            }
            if (num == 0) 
            {
                StyleConstants.setForeground(set, new Color(0xaaaaaa));
            } else if (num == 1) {
                if (message.getKey())
                {
                    StyleConstants.setForeground(set, new Color(0xFF00FF));
                } else {
                    StyleConstants.setForeground(set, new Color(0x0000FF));
                }
            } else {
                StyleConstants.setForeground(set, new Color(0x000000));
            }
            num++;
            document.insertString(document.getLength(), line+"\n", set);
        }
        document.insertString(document.getLength(), "\n", set);
    }

    @Override
    public void NewMessageHandler(UUID userId) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Pair<Boolean, String> latestMessage = MessageLogs.getNewestMessageByUserId(userId);
                UserEnvelope ue = getUserEnvByUserId(userId);
                StyledDocument document = uxMessages.getStyledDocument();
                MutableAttributeSet set = new SimpleAttributeSet();
                try {
                    if (latestMessage != null)
                    {
                        if (!latestMessage.getKey()) {
                            refreshMessageBox(userId); // this is to let the user know that another user sent them a message (updates the icon)
                        }
                        if (userOnDisplay == null || (userOnDisplay != null && !userId.equals(userOnDisplay))) return;
                        if (document.getLength() > ue.getMessagesCursor())
                        {
                            StyleConstants.setBold(set, true);
                        } else {
                            StyleConstants.setBold(set, false);
                            document.setCharacterAttributes(0, document.getLength(), set, false);
                        }
                        colorChat(set, latestMessage, ue);
                    } else {
                        document.setLogicalStyle(document.getLength(), centerStyle);
                        document.insertString(document.getLength(), "**START OF CONVERSATION**\n\n", set);
                    }
                } catch (BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    private void refreshMessageBox(UUID userId) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getUserEnvByUserId(userId).newMessages();
                uxParticipants.validate();
                uxParticipants.repaint();
            }
        });
    }

    @Override
    public void addUserDel(User user) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UserEnvelope ue = new UserEnvelope(user);
                participantsList.addElement(ue);
                uxParticipants.validate();
                uxParticipants.repaint();
            }
        });
    }

    @Override
    public void remUserDel(User user) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UserEnvelope ue = new UserEnvelope(user);
                if (user.getUserId().equals(userOnDisplay))
                {
                    document.setLogicalStyle(document.getLength(), centerStyle);
                    try {
                        document.insertString(document.getLength(), "**END OF CONVERSATION**\n", null);
                    } catch (BadLocationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                participantsList.removeElement(ue);
                uxParticipants.validate();
                uxParticipants.repaint();
            }
        });
    }
}

class UserEnvelope {
    private User user;
    private boolean newMessages;
    private int messagesCursor; // this is so that new messages can render as a different font

    public UserEnvelope(User user)
    {
        this.user = user;
        newMessages = false;
        messagesCursor = 0;
    }

    public void newMessages() { newMessages = true; }
    public void readMessages() { newMessages = false; }
    public boolean hasNewMessages() { return newMessages; }
    public int getMessagesCursor() { return messagesCursor; }
    public void setMessagesCursor(int cursor) { messagesCursor = cursor; }

    public User getUser() { return user; }

    @Override
    public String toString()
    {
        return user.toString();
    }

    @Override
    public boolean equals(Object other)
    {
        UserEnvelope ue = (UserEnvelope) other;
        if (ue == null) return false;
        if (ue.user == null || this.user == null) return false;
        return user.equals(ue.user);
    }
}

class ClientCellRenderer implements ListCellRenderer<UserEnvelope> {
    ImageIcon unreadIcon;
    ImageIcon readIcon;
    DefaultListCellRenderer renderer;

    public ClientCellRenderer()
    {
        renderer = new DefaultListCellRenderer();
        unreadIcon = new ImageIcon(GUI.class.getClassLoader().getResource("unread.png"));
        readIcon = new ImageIcon(GUI.class.getClassLoader().getResource("read.png"));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends UserEnvelope> list, UserEnvelope value, int index, boolean isSelected, boolean cellHasFocus) {
        renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value.hasNewMessages())
        {
            renderer.setIcon(unreadIcon);
        } else {
            renderer.setIcon(readIcon);
        }
        renderer.setEnabled(list.isEnabled());
        renderer.setOpaque(true);
        return renderer;
    }
}