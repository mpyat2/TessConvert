/**
 * 
 */
package pmak.tools.tess_convert;

/**
 * @author max
 *
 */
public class Observation {
	
	public double time;
	public double value;
	public Double error;
	
	@Override
	public String toString() {
        return time + "\t" + value + (error != null ? "\t" + error : "");
    }
	
}
