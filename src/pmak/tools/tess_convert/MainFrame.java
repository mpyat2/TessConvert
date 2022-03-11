/**
 * 
 */
package pmak.tools.tess_convert;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author max
 *
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
	
	private JButton buttonOpen; 
	private JButton buttonSave;
	private JButton buttonProcess;
	private JTextField inputFileName;
	private JTextField outputFileName;
	private TessFITSreader tess_reader;
	private JFileChooser openChooser;
	private JFileChooser saveChooser;
	
	private boolean saveDialogFirstTime = true; 
	
	public MainFrame() {
		super("TESS Converter");
		
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container contentPane = getContentPane();
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        
        mainPanel.add(createOpenPane());
        mainPanel.add(createSavePane());
        mainPanel.add(createControlPane());        
        
        contentPane.add(mainPanel);
        
        buttonOpen.addActionListener(
        		new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						selectInputFile();
					}
        		}
        		);

        buttonSave.addActionListener(
        		new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						selectOutputFile();
					}
        		}
        		);

        buttonProcess.addActionListener(
        		new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						safeProcessFile();
					}
        		}
        		);
        
        inputFileName.getDocument().addDocumentListener(new DocListener());
        outputFileName.getDocument().addDocumentListener(new DocListener());
        
        tess_reader = new TessFITSreader();
        
        openChooser = new JFileChooser();
        openChooser.setDialogTitle("Select input FITS file");
        
        saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Select output text file or enter a name of the new one");
        
        //Display the window.
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
	}
	
	private void selectInputFile() {
		inputFileName.setText("");
		while (true) {
			int returnVal = openChooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = openChooser.getSelectedFile();
				if (file != null) {
					if (file.exists()) {
						inputFileName.setText(file.getPath());
						break;
					} else {
						showMessage("You must select an existing file!");
					}
				}
			} else {
				break;
			}
		}
	}

	private void selectOutputFile() {
		outputFileName.setText("");
		if (saveDialogFirstTime) {
			saveDialogFirstTime = false;
			saveChooser.setCurrentDirectory(openChooser.getCurrentDirectory());
		}
		int returnVal = saveChooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = saveChooser.getSelectedFile();
			outputFileName.setText(file.getPath());
		}
	}

	private void safeProcessFile() {
		String inputName = getInputFileName();
		if (inputName == null || "".equals(inputName)) {
			showError("Input file name must be specified");
			return;
		}
		String outputName = getOutputFileName();
		if (outputName == null || "".equals(outputName)) {
			showError("Output file name must be specified");
			return;
		}
		
		{
			File file = new File(outputName);
			if (file.exists()) {
				int result = JOptionPane.showConfirmDialog(this, "Overwrite existing output file?", "Confirm", JOptionPane.YES_NO_OPTION);
				if (result != JOptionPane.YES_OPTION) {
					return;
				}
			}
		}
		
		try {
			try(FileInputStream stream = new FileInputStream(inputName);
				BufferedInputStream bufferedStream = new BufferedInputStream(stream);
			) 
			{
				List<Object> options = new ArrayList<Object>();
				Object defaultOption;
				options.add(UIManager.getString("OptionPane.yesButtonText"));
		        options.add(UIManager.getString("OptionPane.noButtonText"));
			
		        Cursor defaultCursor = getCursor();
		        Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
		        setCursor(waitCursor);
		        try {
		        	tess_reader.initialize(bufferedStream);
		        } finally {
		        	setCursor(defaultCursor);
		        }
		        
				String selectRawMessage;			
				switch (tess_reader.getTessType()) {
					case QLP:
						selectRawMessage = "This is a QLP file. It is recommended to answer YES (use raw magnitudes).";
						defaultOption = UIManager.getString("OptionPane.yesButtonText");
						break;
					case KEPLER:
						selectRawMessage = "This is a Kepler file. It is recommended to answer NO (use corrected magnitudes).";
						defaultOption = UIManager.getString("OptionPane.noButtonText");
						break;
					case TESS:
						selectRawMessage = "This is a TESS file. It is recommended to answer NO (use corrected magnitudes).";
						defaultOption = UIManager.getString("OptionPane.noButtonText");
						break;
					default:
						selectRawMessage = "Unknown File.";
						showError("Invalid FITS file");
						return;
				}
				
				boolean loadRaw = false;
				
				int result = JOptionPane.showOptionDialog(this, 
						(tess_reader.getTessType() == TessFITSreader.TessType.KEPLER ? "Kepler" : "TESS") +
						" magnitude = " + tess_reader.getKeplerOrTessMag() +
						"\n\n" +
						"Use raw (SAP) data?\n\n" + selectRawMessage, 
						"Options", 
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null, options.toArray(), defaultOption);
				if (result == JOptionPane.YES_OPTION) {
					loadRaw = true;
				} else if (result == JOptionPane.NO_OPTION) {
					loadRaw = false;
				} else {
					return;
				}
				
				setCursor(waitCursor);
				try {
					List<String> list = null;
					list = new ArrayList<String>();
					tess_reader.readObservations(loadRaw);
					for (int i = 0; i < tess_reader.getObservationCount(); i++) {
						Observation obs = tess_reader.getObservation(i);
						list.add(obs.toString());
					}
					Files.write(Paths.get(outputName), list);
				} finally {
					setCursor(defaultCursor);
				}
				showMessage(tess_reader.getObservationCount() + " observations converted.");
			}
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
		catch (Throwable t) {
			showFatalError(t.getMessage());
		}
	}
	
	private JPanel createOpenPane() {
		JPanel pane = new JPanel();
		FlowLayout layout = new FlowLayout();
		pane.setLayout(layout);
		buttonOpen = new JButton("Input File");
		pane.add(buttonOpen);
		inputFileName = new JTextField(50);
		inputFileName.setEditable(false);
		pane.add(inputFileName);
		return pane;
	}
	
	private JPanel createSavePane() {
		JPanel pane = new JPanel();
		FlowLayout layout = new FlowLayout();
		pane.setLayout(layout);
		buttonSave = new JButton("Output File");
		pane.add(buttonSave);
		outputFileName = new JTextField(50);
		outputFileName.setEditable(false);
		pane.add(outputFileName);
		return pane;
	}

	private JPanel createControlPane() {
		JPanel pane = new JPanel();
		FlowLayout layout = new FlowLayout();
		pane.setLayout(layout);
		buttonProcess = new JButton("Process");
		buttonProcess.setEnabled(false);
		pane.add(buttonProcess);
		return pane;
	}
	
	private String getInputFileName() {
		return inputFileName.getText();
	}
	
	private String getOutputFileName() {
		return outputFileName.getText();
	}
	
	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	private void showFatalError(String message) {
		JOptionPane.showMessageDialog(this, "FATAL ERROR: The program will be terminated\n\n" + message, "Fatal Error", JOptionPane.ERROR_MESSAGE);
		System.exit(1);
	}

	private void showMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Message", JOptionPane.PLAIN_MESSAGE);
	}

	private void updateProcessButtonState() {
		String inputName = getInputFileName();
		String outputName = getOutputFileName();		
		buttonProcess.setEnabled(inputName != null && !"".equals(inputName) && outputName != null && !"".equals(outputName));
	}
	
	private class DocListener implements DocumentListener {
		public void changedUpdate(DocumentEvent e) {
			updateProcessButtonState();
		}
		public void removeUpdate(DocumentEvent e) {
			updateProcessButtonState();
		}
		public void insertUpdate(DocumentEvent e) {
			updateProcessButtonState();
		}
	}
	
}
