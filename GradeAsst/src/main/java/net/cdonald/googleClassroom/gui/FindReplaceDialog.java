package net.cdonald.googleClassroom.gui;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.cdonald.googleClassroom.utils.HighlightText;
import net.cdonald.googleClassroom.utils.SimpleUtils;
  
@SuppressWarnings("serial")
public class FindReplaceDialog extends JDialog implements ActionListener 
{  

    //private JPanel findPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));  
 
	private JComboBox<String> what;
	private JComboBox<String> with;
	private JCheckBox word;
	private JCheckBox matchCase;
	
    private JButton find = new JButton("Find");
    private JButton findAll = new JButton("Find All");
    private JButton replace = new JButton("Replace");
    private JButton replaceAll = new JButton("Replace All");
    private JButton close = new JButton("Close");
    private ArrayList<String> findHistory;
    private ArrayList<String> replaceHistory;
    
    
    private JFrame owner;
    private JTextArea pane;
    private int currentLocation = 0;
       
    public FindReplaceDialog(JFrame owner)  
    {
        super(owner, "Find & Replace", false);       
        this.owner = owner;        
        initComponents();
        findHistory = new ArrayList<String>();
        replaceHistory = new ArrayList<String>();

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
        		String whatText = getWhat();
        		HighlightText.moveCursorAndHighlight(pane, index, index + whatText.length(), true);
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
        			String whatText = getWhat();
        			HighlightText.moveCursorAndHighlight(pane, index, index + whatText.length(), false);
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
    
    private String getComboSel(JComboBox<String> combo, ArrayList<String> history) {
    	String text = (String)combo.getSelectedItem();
    	combo.removeAllItems();
    	history.add(0, text);
    	
    	for (int i = history.size() - 1; i > 0; i--) {
    		if (history.get(i).equals(text)) {
    			history.remove(i);
    		}
    	}
    	for (String hist : history) {
    		combo.addItem(hist);
    	}
    	return text;
    }
    
    private String getWhat() {
    	return getComboSel(what, findHistory);
    	
    }
    private String getWith() {
    	return getComboSel(with, replaceHistory);
    }
    
    private void replaceText(int index) {
    	String whatText = getWhat();
    	int whatLen = whatText.length();
        String withText = getWith();
        String replacement = withText;
    	pane.setSelectionStart(index);
    	pane.setSelectionEnd(index + whatLen);
    	pane.replaceSelection(replacement);
		currentLocation = index + replacement.length();    	
    }
    private int findNext(int startLocation) {
    	if (startLocation == -1) {
    		startLocation = 0;
    	}
        String toFind = getWhat();
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
        			int charAt = searchText.charAt(index - 1);
        			found = isWordDelimeter(charAt);         			
        		}
        		if (found && index + toFind.length() < searchText.length()) {
        			found = isWordDelimeter(searchText.charAt(index + toFind.length()));
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
    
    private boolean isWordDelimeter(int charToCheck) {
    	if (Character.isAlphabetic(charToCheck) || Character.isDigit(charToCheck) || charToCheck == '_') {
    		return false;
    	}
    	return true;
    }
     
    private void initComponents()  
    {
        Container c = getContentPane();  
        c.setLayout(new BorderLayout());  

        initFindReplace(c);
        initOptions(c);
        initButtons(c);
 
    }
    private void initFindReplace(Container c) {
    	JPanel findReplacePanel = new JPanel(new GridBagLayout());
    	findReplacePanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));    	
        JLabel findWhat = new JLabel("Find:");
        JLabel replaceWith = new JLabel("Replace with:");  
        what = new JComboBox<String>();        
        with = new JComboBox<String>();
        what.setEditable(true);
        with.setEditable(true);
        what.addActionListener(this);
        with.addActionListener(this);
        SimpleUtils.addLabelAndComponent(findReplacePanel, findWhat,  what, 0);        
        SimpleUtils.addLabelAndComponent(findReplacePanel, replaceWith,  with, 2);
        c.add(findReplacePanel, BorderLayout.NORTH);    	
    }
    
    private void initOptions(Container c) {
    	JPanel wordPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));  
        JPanel casePanel = new JPanel(new FlowLayout(FlowLayout.LEADING)); 
        JPanel inputs = new JPanel(new GridLayout(2, 1));    
        inputs.setBorder(BorderFactory.createTitledBorder("Options"));
        word = new JCheckBox();
        matchCase = new JCheckBox();          
        JLabel wordLabel = new JLabel("Match whole word only");  

        JLabel matchCaseLabel = new JLabel("Match case");
        //findPanel.add(findWhat);  
        //findPanel.add(what);

        wordPanel.add(word);  
        wordPanel.add(wordLabel);  
        casePanel.add(matchCase);  
        casePanel.add(matchCaseLabel);  
        inputs.add(wordPanel);  
        inputs.add(casePanel);
        c.add(inputs, "Center");    	
    }
    
    private void initButtons(Container c) {
    	GridLayout buttonLayout = new GridLayout(2, 2);
    	buttonLayout.setVgap(6);
    	buttonLayout.setHgap(12);

    	JPanel findGrid = new JPanel(buttonLayout);
    	findGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    	findGrid.add(find);
        findGrid.add(findAll);
        findGrid.add(replace);
        findGrid.add(replaceAll);
        JPanel findButtons = new JPanel(new BorderLayout());
        findButtons.add(findGrid);
        JPanel closeButton = new JPanel();
        closeButton.setLayout(new BorderLayout());
        closeButton.add(close, BorderLayout.EAST);
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(findButtons, BorderLayout.EAST);
        buttons.add(closeButton, BorderLayout.SOUTH);
        
        JButton [] buttonArray = {find, findAll, replace, replaceAll, close};
        for (JButton button : buttonArray) {
        	button.setPreferredSize(replaceAll.getPreferredSize());
        	button.setSize(replaceAll.getPreferredSize());
        	button.setMaximumSize(replaceAll.getPreferredSize());
        }
        
        //buttons.add(close); 
        find.addActionListener(this);
        findAll.addActionListener(this);
        replace.addActionListener(this);
        replaceAll.addActionListener(this);
        close.addActionListener(this);      
        
        
         
        c.add(buttons, BorderLayout.SOUTH);  
    }   
} 