package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;

import net.cdonald.googleClassroom.control.DataController;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetAdditionalConsoleListener;
import net.cdonald.googleClassroom.model.MyPreferences;

public class GuidedSetupDialog extends JDialog {
	private static final long serialVersionUID = -7680441177775501992L;
	private MyPreferences prefs;
	private DataController dataController;
	private Frame parent;
	private JButton next;
	private JButton back;
	private JPanel buttonsPanel;
	private JTextField userNameField;
	private JTextField dirNameField;
	private enum Step {CHOOSE_NAME, CHOOSE_DIR, DO_AUTH, FINAL};
	private Step currentStep;
	private JPanel[] steps;
	private JButton browseButton;
	private JTextArea openingPath;
	private JProgressBar progressBar;
	private volatile SwingWorker<Void, Void> authorizing;
	public GuidedSetupDialog(Frame parent, DataController dataController) {
		super(parent, "One Time Guided Setup", true);
		this.parent = parent;
		this.prefs = dataController.getPrefs();
		this.dataController = dataController;
		steps = new JPanel[Step.values().length];
		browseButton = new JButton("Browse");
		next = new JButton("Next");
		back = new JButton("Back");
		next.setPreferredSize(browseButton.getPreferredSize());
		back.setPreferredSize(browseButton.getPreferredSize());
		buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BorderLayout());
		buttonsPanel.add(back, BorderLayout.WEST);
		buttonsPanel.add(next, BorderLayout.EAST);
		next.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = currentStep.ordinal();
				if (currentStep == Step.CHOOSE_DIR) {
					if (validateChooseDir() == false) {
						return;
					}
				}
				if (currentStep == Step.CHOOSE_NAME) {
					prefs.setUserName(userNameField.getText());
				}
				
				if (index + 1 < Step.values().length) {
					nextStep(Step.values()[index + 1]);
				}
				else {
					setVisible(false);
				}				
			}			
		});
		back.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = currentStep.ordinal();
				if (index - 1 >= 0 ) {
					nextStep(Step.values()[index  - 1]);
				}
				else {
					setVisible(false);
				}				
			}			
			
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				if (authorizing != null) {
					authorizing.cancel(true);
				}
				dataController.closing();
				dispose();
				System.gc();
			}
		});
		setLayout(new BorderLayout());
		fillChooseNamePanel();
		fillChooseDirPanel();
		fillAuthorizationPanel();
		fillFinalPanel();
	}
	public void runGuidedSetup() {		
		nextStep(Step.CHOOSE_NAME);

	}
	
	private void nextStep(Step step) {		
		ListenerCoordinator.fire(SetAdditionalConsoleListener.class, null);
		JPanel currentPanel = steps[step.ordinal()];
		if (currentPanel != null) {
			if (currentStep != null) {
				remove(steps[currentStep.ordinal()]);
			}
			currentStep = step;
			currentPanel.add(buttonsPanel, BorderLayout.SOUTH);
			add(currentPanel, BorderLayout.CENTER);
			pack();
			switch(currentStep) {
			case CHOOSE_NAME:
				doChooseName();
				break;
			case CHOOSE_DIR:
				doChooseDir();
				break;
			case DO_AUTH:
				doAuth();
				break;
			case FINAL:
				doFinal();
				break;
			default:
				break;				
			}
			if (isVisible() == false) {
				setVisible(true);
			}
		}
	}
	private void fillChooseNamePanel() {
		JPanel chooseName = new JPanel();
		chooseName.setLayout(new BorderLayout());
		final int SPACE = 5;
		chooseName.setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE));
		JLabel prompt = new JLabel("Choose the name that will appear in the gradebook: ");
		userNameField = new JTextField(20);
		String name = prefs.getUserName();
		userNameField.setText(name);
		JPanel promptPanel = new JPanel();
		promptPanel.setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, 0));
		promptPanel.setLayout(new BorderLayout());
		promptPanel.add(prompt, BorderLayout.WEST);
		promptPanel.add(userNameField, BorderLayout.CENTER);
		chooseName.add(promptPanel, BorderLayout.CENTER);		
		enableNext(userNameField.getDocument(), 1);
		steps[Step.CHOOSE_NAME.ordinal()] = chooseName;		
	}
	
	private void enableNext(Document d, int minLen) {
		next.setEnabled(d.getLength() > minLen);
		d.addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				next.setEnabled(d.getLength() > minLen);				
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				next.setEnabled(d.getLength() > minLen);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
			
		});	
	}
	
	private boolean validateChooseDir() {
		String directoryPath = dirNameField.getText();
		new File(directoryPath).mkdir();	
		prefs.setWorkingDir(directoryPath);
		String studentFileName = directoryPath + File.separator + "credentials.json";
		try {
			PrintWriter out = new PrintWriter(studentFileName);
			out.print("{\"installed\":{\"client_id\":\"392984368369-s70mpfm8uu387garftdmm9sut5ue5cdc.apps.googleusercontent.com\",\"project_id\":\"classroom-grader\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_secret\":\"NyLUJ6AlyRnXCDCpljg8qPBa\",\"redirect_uris\":[\"urn:ietf:wg:oauth:2.0:oob\",\"http://localhost\"]}}");
			out.flush();
			out.close();
		} catch (FileNotFoundException ex) {
			JOptionPane.showMessageDialog(null, "We need to be able to write data to the directory.\nAttempted to write:\n" + studentFileName + "\n and failed",  "Bad Working Dir",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	
	private void fillChooseDirPanel() {
		JPanel chooseDir = new JPanel();
		chooseDir.setLayout(new BorderLayout());
		final int SPACE = 5;
		chooseDir.setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE));
		JLabel prompt = new JLabel("Choose the directory this program can use to store data:\n");
		dirNameField = new JTextField(getDefaultWorkingDir());
		JPanel browsePanel = new JPanel();
		browsePanel.setLayout(new BorderLayout());
		browsePanel.setBorder(BorderFactory.createEmptyBorder(0, SPACE, 0, 0));
		browsePanel.add(browseButton, BorderLayout.CENTER);
		JPanel promptPanel = new JPanel();
		promptPanel.setBorder(BorderFactory.createEmptyBorder(SPACE, 0, SPACE, 0));
		promptPanel.setLayout(new BorderLayout());
		promptPanel.add(prompt, BorderLayout.NORTH);
		promptPanel.add(dirNameField, BorderLayout.CENTER);
		promptPanel.add(browsePanel, BorderLayout.EAST);
		chooseDir.add(promptPanel, BorderLayout.CENTER);
		//chooseDir.add(browse, BorderLayout.EAST);		
		steps[Step.CHOOSE_DIR.ordinal()] = chooseDir;	
		
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String dir = launchChooseDirDialog(dirNameField.getText());
				if (dir != null) {
					dirNameField.setText(dir);
				}				
			}			
		});
	}
	
	private void fillAuthorizationPanel() {
		JPanel authorizationPanel = new JPanel();
		authorizationPanel.setLayout(new BorderLayout());
		final int SPACE = 5;
		authorizationPanel.setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE));
		authorizationPanel.setLayout(new BorderLayout());
		openingPath = new JTextArea();		
		openingPath.setEditable(false);		

		JPanel openingPanel = new JPanel();
		openingPanel.setLayout(new BorderLayout());
		openingPanel.setBorder(BorderFactory.createTitledBorder("Authorization Messages"));
		openingPanel.setPreferredSize(new Dimension(400, 150));
		openingPanel.add(new JScrollPane(openingPath), BorderLayout.CENTER);
		progressBar = new JProgressBar();	
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(true);
		progressBar.setString("One Time Authorization In Progress");
		openingPanel.add(progressBar, BorderLayout.SOUTH);
		JPopupMenu popupSource = new JPopupMenu();
		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		popupSource.add(copy);
		openingPath.setComponentPopupMenu(popupSource);
		
		JPanel informationPanel = new JPanel();
		informationPanel.setLayout(new BorderLayout());
		informationPanel.setBorder(BorderFactory.createTitledBorder("General instructions"));
		JLabel informationSouth = new JLabel();
		
		String informationString = "";
		informationString+="<html><br/>";
		informationString+="Whenever you are prompted to log in, use your school gmail account.<br/><br/>";
		informationString+="When the URL opens, you will get a big warning about the program not being verified (I'm not sure out how to fix that).<br/><br/>";
		informationString+="Click the small \"advanced\" link.<br/>";
		informationString+="Click the small \"Go to Classroom Grading Assistant (unsafe)\" link.<br/><br/>";
		informationString+="You will be asked to grant a bunch of permissions.  The program needs those to:<br/>";
		informationString+="\t1. Download the classroom assignments, roster etc.<br/>";
		informationString+="\t2. Access the google drive & sheets to store grades & read the rubrics.<br/>";
		informationString+="So please hit accept.<br/><br/><html/>";
		informationSouth.setText(informationString);
		informationPanel.add(new JScrollPane(informationSouth), BorderLayout.SOUTH);
		authorizationPanel.add(openingPanel, BorderLayout.CENTER);
		authorizationPanel.add(informationPanel, BorderLayout.NORTH);
		steps[Step.DO_AUTH.ordinal()] = authorizationPanel;
	
	}
	
	private void fillFinalPanel() {
		JPanel finalMessagePanel = new JPanel();
		finalMessagePanel.setLayout(new BorderLayout());
		final int SPACE = 5;
		finalMessagePanel.setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE));
		JLabel finalMessageLabel = new JLabel();
		//finalMessageText.setEditable(false);
		String finalMessageText = "";
		finalMessageText += "<html><br/>";
		finalMessageText += "The grader will now close.<br/>";
		finalMessageText += "You will need to restart it manually because I haven't learned how to make a program auto-restart.<br/>";
		finalMessageText += "I promise, the actual program is better designed than this setup. :)<br/> <br/></html>";
		finalMessageLabel.setText(finalMessageText);
		finalMessagePanel.add(finalMessageLabel, BorderLayout.CENTER);
		JPanel someSpace = new JPanel();
		someSpace.setLayout(new BorderLayout());
		someSpace.setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE));
		someSpace.add(finalMessagePanel);
		steps[Step.FINAL.ordinal()] = someSpace;
	}
	
	
	private String launchChooseDirDialog(String dir) {

		JFileChooser workingDirChooser = new JFileChooser(dir);
		workingDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (workingDirChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			File directory = workingDirChooser.getSelectedFile();
			String directoryPath = directory.getAbsolutePath();
			return directoryPath;
		}
		return null;
	}
	private String getDefaultWorkingDir() {
		String workingDirectory;
		//here, we assign the name of the OS, according to Java, to a variable...
		String OS = (System.getProperty("os.name")).toUpperCase();
		//to determine what the workingDirectory is.
		//if it is some version of Windows
		if (OS.contains("WIN"))
		{
		    //it is simply the location of the "AppData" folder
		    workingDirectory = System.getenv("AppData");
		}
		//Otherwise, we assume Linux or Mac
		else
		{
		    //in either case, we would start in the user's home directory
		    workingDirectory = System.getProperty("user.home");		   
		    //if we are on a Mac, we are not done, we look for "Application Support"
		    workingDirectory += File.separator + "Library" + File.separator + "Application Support";
		}
		workingDirectory += File.separator + "Google Classroom Grader";
		return workingDirectory;
	}
	


	private void doChooseName() {
		back.setVisible(false);
		next.setEnabled(true);		
	}
	private void doChooseDir() {
		back.setVisible(true);
		back.setEnabled(true);
		next.setEnabled(true);
		next.setText("Next");
	}
	
	private void doAuth() {
		next.setText("Next");
		next.setEnabled(false);
		authorizing = new SwingWorker<Void,Void>() {


			@Override
			protected Void doInBackground() throws Exception {
				ListenerCoordinator.fire(SetAdditionalConsoleListener.class, openingPath);
				boolean success = false;
				progressBar.setVisible(true);
				try {
					dataController.initGoogle();
					dataController.testCredentials();					
					success = true;
				} catch (Exception e) {
					System.out.println("Authorization failed\n");
					System.out.println(e.getMessage() + "\n");			
				}
				if (success) {
					System.out.println("\n");
					System.out.println("Successfully linked to google classroom!");
				}
				progressBar.setVisible(false);
				next.setEnabled(success);
				authorizing = null;
				return null;
			}				
		};
		authorizing.execute();
	}
	
	private void doFinal() {
		next.setText("Exit");
	
	}


}
