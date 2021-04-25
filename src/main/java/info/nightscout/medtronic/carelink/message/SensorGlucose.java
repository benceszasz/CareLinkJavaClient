package info.nightscout.medtronic.carelink.message;

import java.util.Date;


public class SensorGlucose {

    public Integer sg;
    public Date datetime;
    public boolean timeChange;
    public String kind;
    public int version;
    public String sensorState;
    public int relativeOffset;

    public String toS() {
        String dt;
        if (datetime == null) { dt = ""; } else{ dt = datetime.toString(); }
        return dt + " "  + String.valueOf(sg);
    }

}
