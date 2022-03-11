/**
 * 
 */
package pmak.tools.tess_convert;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import nom.tam.fits.FitsException;

/**
 * @author max
 *
 */
public class TessConvert {

	private static boolean loadRaw = false;
	private static boolean overwrite = false;
	private static String inputName = null;
	private static String outputName = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			processCmdLineArgs(args);
			try {
				processFile();
			} catch (Exception ex) {
				System.err.println(ex.getMessage());
				System.exit(1);
			}
		} else {
			// Schedule a job for the event-dispatching thread:
			// creating and showing this application's GUI.
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					createAndShowGUI();				
				}
			});
		}
	}
	
	public static void processFile() throws IOException, FitsException, ObservationReadError, GeneralError {
		TessFITSreader tess_reader = new TessFITSreader();
		try(FileInputStream stream = new FileInputStream(inputName);
			BufferedInputStream bufferedStream = new BufferedInputStream(stream);
		) 
		{
			if (!overwrite && outputName != null && (new File(outputName).exists())) {
				throw new GeneralError("Output file already exists. Use --force-overwrite switch to overwrite.");
			}
			
			tess_reader.initialize(bufferedStream);			
			List<String> list = null;
			if (outputName != null) {
				list = new ArrayList<String>();
			}
			tess_reader.readObservations(loadRaw);
			for (int i = 0; i < tess_reader.getObservationCount(); i++) {
				Observation obs = tess_reader.getObservation(i);
				String s = obs.toString();
				if (outputName != null) {
					list.add(s);
				} else {
					System.out.println(s);
				}
			}
			if (outputName != null) {
				Files.write(Paths.get(outputName), list);
			}
		}
	}
	
	/**
	 * Create and display the main window.
	 */
    private static void createAndShowGUI() {
		// Set the Look & Feel of the application to be native.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Unable to set native look & feel. Exiting.");
			//System.exit(1);
		}
        //Create and set up the window.
        new MainFrame();
    }
    
	private static void printHelpAndExit() {
		System.err.println("usage: TessConvert inputFile [outputFile] [--raw] [--force-overwrite]");
		System.exit(1);
	}

	private static void invalidCommandLineError() {
		System.err.println("Invalid command-line arguments");
		System.err.println();
		printHelpAndExit();
	}
	
	private static void processCmdLineArgs(String[] args) {
		for (String arg : args) {
			if (arg != null && arg.startsWith("--")) {
				if ("--help".equalsIgnoreCase(arg)) {
					printHelpAndExit();
				} else if ("--raw".equalsIgnoreCase(arg)) {
					loadRaw = true;
				} else if ("--force-overwrite".equalsIgnoreCase(arg)) {
					overwrite = true;
				} else {
					invalidCommandLineError();
				}
			} else {
				if (inputName == null) {
					inputName = arg;
				} else if (outputName == null) {
					outputName = arg;
				} else {
					invalidCommandLineError();
				}
			}
		}
		if (inputName == null) {
			invalidCommandLineError();
		}
	}

}
