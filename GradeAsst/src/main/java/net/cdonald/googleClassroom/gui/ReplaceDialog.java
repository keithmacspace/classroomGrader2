package net.cdonald.googleClassroom.gui;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import net.cdonald.googleClassroom.utils.HighlightText;  
   
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
    private int currentLocation = 0;
       
    public ReplaceDialog(JFrame owner)  
    {
        super(owner, "Find & Replace", true);       
        this.owner = owner;        
        initComponents();  
        //setSize(360, 135);  
        Container c = getContentPane();  
        c.setLayout(new BorderLayout());  
        c.add(inputs, "Center");  
        c.add(buttons, "East");  
        pack();  
    }
    
    public void showDialog(JTextArea pane) {
        setLocationRelativeTo(owner);
        this.pane = pane;
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
        	int index = findNext(currentLocation);
        	if (index != -1) {
        		HighlightText.moveCursorAndHighlight(pane, index, index + what.getText().length(), true);
        	}
        }
        if(e.getSource() == findAll)  
        {
        	int index = -1;
        	pane.getHighlighter().removeAllHighlights();
        	currentLocation = 0;

        	do {
        		index = findNext(currentLocation);
        		if (index != -1) {
        			HighlightText.moveCursorAndHighlight(pane, index, index + what.getText().length(), false);
        		}
        	}while (index != -1);
        }
        else if(e.getSource() == replace)  
        {
        	int index = findNext(currentLocation);
        	if (index != -1) {
        		replaceText(index);
        	}      
        }  
        else if(e.getSource() == replaceAll)  
        {  
        	int index = -1;
        	currentLocation = 0;
        	do {
        		index = findNext(currentLocation);
        		if (index != -1) {
        			replaceText(index);
        		}
        	}while (index != -1);
        }  
        else if(e.getSource() == close)  
        {  
        	pane.getHighlighter().removeAllHighlights();
            setVisible(false);  
        }  
    }
    private void replaceText(int index) {
    	int whatLen = what.getText().length();
        String text = pane.getText();
        String replacement = with.getText();
    	pane.setSelectionStart(index);
    	pane.setSelectionEnd(index + whatLen);
    	pane.replaceSelection(replacement);
		currentLocation = index + replacement.length();    	
    }
    private int findNext(int startLocation) {
    	if (startLocation == -1) {
    		startLocation = 0;
    	}
        String toFind = what.getText();
        String text = pane.getText();
        if (startLocation >= text.length()) {
        	return -1;
        }
        String searchText = text;
        if (!matchCase.isSelected()) {
        	toFind = toFind.toLowerCase();
        	searchText = text.toLowerCase();
        }
        int index = searchText.indexOf(toFind, startLocation);
    	if (word.isSelected()) {
    		while (index != -1) {
    			boolean found = true;
        		if (index != 0) {
        			found = Character.isWhitespace(searchText.charAt(index - 1));        			
        		}
        		if (found && index + toFind.length() < searchText.length()) {
        			found = Character.isWhitespace(searchText.charAt(index + toFind.length()));
        		}
        		if (found == false) {
        			index = searchText.indexOf(toFind, index + 1);
        		}
        		else {
        			break;
        		}
        	}
        }
    	if (index != -1) {
    		currentLocation = index + 1;
    	}
    	else {
    		currentLocation = 0;
    	}
    	return index;
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