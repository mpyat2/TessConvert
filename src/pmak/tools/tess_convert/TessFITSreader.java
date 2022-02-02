/**
 * 
 */
package pmak.tools.tess_convert;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;

/**
 * @author max
 *
 */
public class TessFITSreader {
	
	public static final double INVALID_MAG = 99.99; 
	
	private List<Observation> observations = new ArrayList<Observation>();
	
	public TessFITSreader() {
	};
	
	public void readObservations(InputStream stream, boolean loadRaw) throws FitsException, ObservationReadError, IOException {
		observations.clear();
		Fits fits = new Fits(stream);
		try {
		    BasicHDU<?>[] hdus = fits.read();
		    
			if (hdus.length > 1 && hdus[0] instanceof ImageHDU && hdus[1] instanceof BinaryTableHDU) {
				ImageHDU imageHDU = (ImageHDU) hdus[0];
				
				String telescope = imageHDU.getTelescope();
				if (telescope == null) {
					telescope = "MAST";
				}
				
				double keplerOrTessMag = INVALID_MAG;
				
				// TESSMAG/KEPMAG from FITS header
				if ("TESS".equals(telescope)) {
					keplerOrTessMag = imageHDU.getHeader().getDoubleValue("TESSMAG", INVALID_MAG);
				}
				if ("Kepler".equals(telescope)) {
					keplerOrTessMag = imageHDU.getHeader().getDoubleValue("KEPMAG", INVALID_MAG);
				}
				
				BinaryTableHDU tableHDU = (BinaryTableHDU) hdus[1];
				if (!"TIME".equals(tableHDU.getColumnName(0)) ||
					!"SAP_FLUX".equals(tableHDU.getColumnName(3)) ||
					!"SAP_FLUX_ERR".equals(tableHDU.getColumnName(4)) ||
					!"PDCSAP_FLUX".equals(tableHDU.getColumnName(7)) ||
					!"PDCSAP_FLUX_ERR".equals(tableHDU.getColumnName(8))) {
					throw new ObservationReadError("Not a valid FITS file");
				}
				
				double timei = tableHDU.getHeader().getDoubleValue("BJDREFI");
				double timef = tableHDU.getHeader().getDoubleValue("BJDREFF");
				
				int rows = tableHDU.getNRows();
				
				for (int row = 0; row < rows; row++) {
					double barytime = ((double[]) tableHDU.getElement(row, 0))[0];
					float flux;
					float flux_err;
					if (!loadRaw) {
						flux = ((float[]) tableHDU.getElement(row, 7))[0];
						flux_err = ((float[]) tableHDU.getElement(row, 8))[0];
					} else {
						flux = ((float[]) tableHDU.getElement(row, 3))[0];
						flux_err = ((float[]) tableHDU.getElement(row, 4))[0];
					}
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
			} else {
				throw new ObservationReadError("Not a valid FITS file");
			}
		} finally {
			fits.close();
		}
	}
	
	public int getObservationCount() {
		return observations.size();
	}
	
	public Observation getObservation(int i) {
		return observations.get(i);
	}
}
