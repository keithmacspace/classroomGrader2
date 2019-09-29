package net.cdonald.googleClassroom.gui;
import javax.swing.*;  
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import java.util.HashMap;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;  
   
public class ReplaceDialog extends JDialog implements ActionListener 
{  
    //private JPanel findPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));  
    private JPanel wordPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));  
    private JPanel casePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));  
    private JPanel inputs = new JPanel(new GridLayout(6, 1));  
    private JPanel buttons = new JPanel(new GridLayout(5, 1));  
    private JLabel findWhat = new JLabel("Find What");  
    private JTextField what = new JTextField();
    private JLabel replaceWith = new JLabel("Replace With");
    private JTextField with = new JTextField();
    private JCheckBox word = new JCheckBox();  
    private JLabel wordLabel = new JLabel("Match whole word only");  
    private JCheckBox matchCase = new JCheckBox();  
    private JLabel matchCaseLabel = new JLabel("Match case");
    private JButton find = new JButton("Find");
    private JButton findAll = new JButton("Find All");
    private JButton replace = new JButton("Replace");
    private JButton replaceAll = new JButton("Replace All");
    private JButton close = new JButton("Close");  
    private JFrame owner;
    private JTextArea pane;
    private HashMap<Object, Action> actions;
       
    public ReplaceDialog(JFrame owner, JTextArea pane)  
    {
        super(owner, "Find & Replace", true);       
        this.owner = owner;
        this.pane = pane;
        initComponents();  
        //setSize(360, 135);  
        Container c = getContentPane();  
        c.setLayout(new BorderLayout());  
        c.add(inputs, "Center");  
        c.add(buttons, "East");  
        pack();  
        setLocationRelativeTo(owner);  
        setVisible(true);
    }
     
    public void actionPerformed(ActionEvent e)  
    {  
        if(e.getSource() == what)  
        {  
            with.requestFocusInWindow();  
        }  
        if(e.getSource() == find)  
        {
            if(!word.isSelected() && !matchCase.isSelected())  
            {
            }  
            else if(!word.isSelected() && matchCase.isSelected())  
            {
            }  
            else if(word.isSelected() && !matchCase.isSelected())  
            {  
            }  
            else if(word.isSelected() && matchCase.isSelected())  
            { 
            }
        }
        if(e.getSource() == findAll)  
        { 
           if(!word.isSelected() && !matchCase.isSelected())  
            {  
            }  
            else if(!word.isSelected() && matchCase.isSelected())  
            {
                Pattern p = Pattern.compile(what.getText());
                Matcher m = p.matcher(pane.getText());
                while (m.find())
                {
                    //select that portion of the text
                     Action action = getActionByName(DefaultEditorKit.selectionForwardAction);
                     action.actionPerformed(null);
                }
            }  
            else if(word.isSelected() && !matchCase.isSelected())  
            {  
            }  
            else if(word.isSelected() && matchCase.isSelected())  
            { 
            }       
        }
        else if(e.getSource() == replace)  
        {
           if(!word.isSelected() && !matchCase.isSelected())  
            {  
            }  
            else if(!word.isSelected() && matchCase.isSelected())  
            {
            }  
            else if(word.isSelected() && !matchCase.isSelected())  
            {  
            }  
            else if(word.isSelected() && matchCase.isSelected())  
            { 
            }       
        }  
        else if(e.getSource() == replaceAll)  
        {  
            if(!word.isSelected() && !matchCase.isSelected())  
            {  
            }  
            else if(!word.isSelected() && matchCase.isSelected())  
            {  
                String toReplace = what.getText();  
                String replacement = with.getText();  
                String text = pane.getText();  
                text = text.replaceAll(toReplace, replacement);  
                pane.setText(text);  
            }  
            else if(word.isSelected() && !matchCase.isSelected())  
            {  
            }  
            else if(word.isSelected() && matchCase.isSelected())  
            {  
                String toReplace = "\\b" + what.getText() + "\\b";  
                String replacement = with.getText();  
                String text = pane.getText();  
                text = text.replaceAll(toReplace, replacement);  
                pane.setText(text);  
            }  
        }  
        else if(e.getSource() == close)  
        {  
            dispose();  
        }  
    }     
     
    private void initComponents()  
    {  
        //findPanel.add(findWhat);  
        //findPanel.add(what);
        what.addActionListener(this);
        find.addActionListener(this);
        findAll.addActionListener(this);
        replace.addActionListener(this);
        replaceAll.addActionListener(this);
        close.addActionListener(this);      
        wordPanel.add(word);  
        wordPanel.add(wordLabel);  
        casePanel.add(matchCase);  
        casePanel.add(matchCaseLabel);  
        inputs.add(findWhat);
        inputs.add(what);
        inputs.add(replaceWith);
        inputs.add(with);
        inputs.add(wordPanel);  
        inputs.add(casePanel);
        buttons.add(find);
        buttons.add(findAll);
        buttons.add(replace);
        buttons.add(replaceAll);  
        buttons.add(close);  
    }
 
    //The following two methods allow us to find an
    //action provided by the editor kit by its name.
    private HashMap<Object, Action> createActionTable(JTextComponent textComponent)
    {
        HashMap<Object, Action> actions = new HashMap<Object, Action>();
        Action[] actionsArray = textComponent.getActions();
        for (int i = 0; i < actionsArray.length; i++)
        {
            Action a = actionsArray[i];
            actions.put(a.getValue(Action.NAME), a);
        }
    return actions;
    }
 
    private Action getActionByName(String name)
    {
        return actions.get(name);
    }   
} 