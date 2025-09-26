
package no.arctic.core;
import java.util.*;


/** 
 * Base class, implementing common methods of ServerConfig. 
 * This may be subclassed by application (e.g. by Main class) 
 */

public abstract class ConfigBase implements ServerConfig {

    private Properties _config =new Properties();
    private Logfile _log = new Logfile(this, null);

        
    /** Get logfile */
    public Logfile log() 
        { return _log; }
        
    /* Set logfile */
    protected void setLogger(Logfile lf) 
        { _log = lf; }
            
    /** Get configuration properties. */
    public Properties config()
        { return _config; }
    
    /* Set configuration properties */
    protected void setConfig(Properties prop)
        { _config = prop; }
        
    /** Set a configuration property */
    public void setProperty(String pname, String dvalue) {
        if (_config!=null)
            _config.setProperty(pname, dvalue);
    }
    
    /** Get a configuration property */
    public String getProperty(String pname, String dvalue) { 
        if (_config==null)
            return null;
        String x = _config.getProperty(pname, dvalue); 
        return (x == null ? x : x.trim()); 
    }

    /** Get boolean property */       
    public boolean getBoolProperty(String pname, boolean dvalue) { 
        if (_config==null)
            return false;
        return _config.getProperty(pname, (dvalue  ? "true" : "false"))
            .trim().matches("TRUE|true|YES|yes"); 
    } 
          
    /** Get integer property */
    public int getIntProperty(String pname, int dvalue) {  
        if (_config==null)
            return 0;
        return Integer.parseInt(_config.getProperty(pname, ""+dvalue).trim());
    }
                 
        
    /** Get position property (lat, long) */
    public double[] getPosProperty(String pname)
    {
        if (_config==null)
            return null;
        String inp = _config.getProperty(pname, "0,0").trim(); 
        if (!inp.matches("[0-9]+(\\.([0-9]+))?\\,([0-9]+)(\\.([0-9]+))?")) {
            return new double[] {0,0};
        }
        String[] scoord = inp.split(",");
        double[] res = new double[] {0,0};
        if (scoord.length != 2)
            return res;
        res[0] = Double.parseDouble(scoord[0].trim());
        res[1] = Double.parseDouble(scoord[1].trim());
        return res;
    }

    
    
    // FIXME
    /** Get plugin properties. */
    public Map<String, Object> properties()
        { return null;}
    
    
    
}
