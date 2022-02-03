/**
 * 
 */
package pmak.tools.tess_convert;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.Header;

/**
 * @author max
 *
 */
public class TessFITSreader {
	
	public static final double INVALID_MAG = 99.99; 

	public enum TessType {
		UNKNOWN,
		KEPLER,
		TESS,
		QLP
	}
	
	private TessType tessType = TessType.UNKNOWN;
	private double keplerOrTessMag = INVALID_MAG; 
	private Fits fits = null;
	private BasicHDU<?>[] hdus = null;
	
	private List<Observation> observations = new ArrayList<Observation>();
	
	public TessFITSreader() {
	};
	
	public void initialize(InputStream stream) throws FitsException {
		observations.clear();
		
		tessType = TessType.UNKNOWN;
		fits = null;
		hdus = null;
		keplerOrTessMag = INVALID_MAG;
		
		fits = new Fits(stream);
		hdus = fits.read();
		if (hdus.length > 1 && hdus[0] instanceof ImageHDU && hdus[1] instanceof BinaryTableHDU) {
			ImageHDU imageHDU = (ImageHDU) hdus[0];
			BinaryTableHDU tableHDU = (BinaryTableHDU) hdus[1];
			
			// Check for QLP fields
			if ("TIME".equals(tableHDU.getColumnName(0)) &&
				"SAP_FLUX".equals(tableHDU.getColumnName(2)) &&
				"KSPSAP_FLUX".equals(tableHDU.getColumnName(3)) &&
				"KSPSAP_FLUX_ERR".equals(tableHDU.getColumnName(4)) &&
				"QUALITY".equals(tableHDU.getColumnName(5))) {
				tessType = TessType.QLP;
			} else {
				// TESS or KEPLER?
				if (!"TIME".equals(tableHDU.getColumnName(0)) ||
					!"SAP_FLUX".equals(tableHDU.getColumnName(3)) ||
					!"SAP_FLUX_ERR".equals(tableHDU.getColumnName(4)) ||
					!"PDCSAP_FLUX".equals(tableHDU.getColumnName(7)) ||
					!"PDCSAP_FLUX_ERR".equals(tableHDU.getColumnName(8))) {
					tessType = TessType.UNKNOWN;
					return;
				}

				String telescope = imageHDU.getTelescope();

				if (telescope == null) {
					tessType = TessType.UNKNOWN;
					return;
				}
			
				if ("TESS".equals(telescope)) {
					tessType = TessType.TESS;
				} else if ("Kepler".equals(telescope)) {
					tessType = TessType.KEPLER;
				} else {
					tessType = TessType.UNKNOWN;
					return;
				}
			}
			
			if (tessType == TessType.KEPLER) {
				keplerOrTessMag = imageHDU.getHeader().getDoubleValue("KEPMAG", INVALID_MAG);
			} else if (tessType == TessType.TESS) {
				keplerOrTessMag = imageHDU.getHeader().getDoubleValue("TESSMAG", INVALID_MAG);
			} else if (tessType == TessType.QLP) {
				keplerOrTessMag = imageHDU.getHeader().getDoubleValue("TESSMAG", INVALID_MAG);
			}
			
		}
	}
	
	public void readObservations(boolean loadRaw) throws ObservationReadError, FitsException {
		observations.clear();
		if (tessType == TessType.UNKNOWN) {
			throw new ObservationReadError("Not a valid FITS file");
		}

		double timei;
		double timef;
		int timeColumn;
		int fluxColumn;
		int errorColumn;
		
		BinaryTableHDU tableHDU = (BinaryTableHDU) hdus[1];		
		
		Header tableHeader = tableHDU.getHeader();
		if (tessType == TessType.QLP) {
			if (tableHeader.containsKey("BJDREFI") && tableHeader.containsKey("BJDREFR")) {
				timei = tableHDU.getHeader().getDoubleValue("BJDREFI");
				timef = tableHDU.getHeader().getDoubleValue("BJDREFR");
			} else {
				throw new ObservationReadError("QLP FITS: Cannot find BJDREFI and/or BJDREFR keywords");
			}
			timeColumn = 0;
			if (loadRaw) {
				fluxColumn = 2;
				errorColumn = -1;
			} else {
				fluxColumn = 3;
				errorColumn = 4;
			}
		} else {
			if (tableHeader.containsKey("BJDREFI") && tableHeader.containsKey("BJDREFF")) {
				timei = tableHDU.getHeader().getDoubleValue("BJDREFI");
				timef = tableHDU.getHeader().getDoubleValue("BJDREFF");
			} else {
				throw new ObservationReadError("Kepler/TESS FITS: Cannot find BJDREFI and/or BJDREFF keywords");
			}
			timeColumn = 0;
			if (loadRaw) {
				fluxColumn = 3;
				errorColumn = 4;
			} else {
				fluxColumn = 7;
				errorColumn = 8;
			}
		}
			
		int rows = tableHDU.getNRows();
				
		for (int row = 0; row < rows; row++) {
			double barytime = ((double[]) tableHDU.getElement(row, timeColumn))[0];
			float flux = ((float[]) tableHDU.getElement(row, fluxColumn))[0];
			float flux_err = errorColumn >= 0 ? ((float[]) tableHDU.getElement(row, errorColumn))[0] : 0;
			// Include only valid magnitude fluxes.
			if (!Float.isInfinite(flux)	&& 
				!Float.isInfinite(flux_err)	&& 
				!Float.isNaN(flux) &&
				!Float.isNaN(flux_err) && 
				(flux > 0)) {
					Observation obs = new Observation();
					obs.time = barytime + timei + timef;
					obs.value = flux;
					obs.error = flux_err;
					observations.add(obs);
			}
		}
				
		// Calculating magShift (median of all points)
		double magShift = 15.0; // arbitrary value
		if (keplerOrTessMag != INVALID_MAG) {
			double[] flux = new double[observations.size()];
			for (int i = 0; i < flux.length; i++) {
				flux[i] = observations.get(i).value;
			}
			Median median = new Median();
			double median_flux = median.evaluate(flux);
			double median_inst_mag = -2.5 * Math.log10(median_flux);
			magShift = keplerOrTessMag - median_inst_mag;
		}
				
		for (Observation obs : observations) {
			double mag = magShift - 2.5 * Math.log10(obs.value);
			double magErr = 1.086 * obs.error / obs.value;
			obs.value = mag;
			obs.error = magErr;
		}
	}
	
	public int getObservationCount() {
		return observations.size();
	}
	
	public Observation getObservation(int i) {
		return observations.get(i);
	}
	
	public TessType getTessType() {
		return tessType;
	}
	
	public double getKeplerOrTessMag() {
		return keplerOrTessMag;
	}
}
