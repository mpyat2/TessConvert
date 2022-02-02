/**
 * 
 */
package pmak.tools.tess_convert;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.FitsException;

/**
 * @author max
 *
 */
public class TessConvert {

	private static boolean loadRaw = false;
	private static String inputName = null;
	private static String outputName = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		processCmdLineArgs(args);
		try {
			processFile();
		} catch (Exception ex) {
			System.err.println(ex);
		}
	}
	
	private static void processFile() throws FileNotFoundException, IOException, FitsException, ObservationReadError {
		TessFITSreader tess_reader = new TessFITSreader();
		try(FileInputStream stream = new FileInputStream(inputName);
			BufferedInputStream bufferedStream = new BufferedInputStream(stream);
		) 
		{
			List<String> list = null;
			if (outputName != null) {
				list = new ArrayList<String>();
			}
			tess_reader.readObservations(bufferedStream, loadRaw);
			for (int i = 0; i < tess_reader.getObservationCount(); i++) {
				Observation obs = tess_reader.getObservation(i);
				String s = obs.time + "\t" + obs.value + "\t" + obs.error;
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
	
	private static void printHelpAndExit() {
		System.err.println("usage: TessConvert inputFile [outputFile] [--raw]");
		System.exit(0);
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
