package info.nightscout.medtronic.carelink.message;

import java.util.Date;

public class Marker {

    public static final String MARKER_TYPE_MEAL = "MEAL";
    public static final String MARKER_TYPE_CALIBRATION = "CALIBRATION";
    public static final String MARKER_TYPE_BG_READING = "BG_READING";
    public static final String MARKER_TYPE_INSULIN = "INSULIN";
    public static final String MARKER_TYPE_AUTO_BASAL = "AUTO_BASAL_DELIVERY";
    public static final String MARKER_TYPE_AUTO_MODE_STATUS = "AUTO_MODE_STATUS";


    public String type;
    public int index;
    public Integer value;
    public String kind;
    public int version;
    public Date dateTime;
    public Integer relativeOffset;
    public Boolean calibrationSuccess;
    public Integer amount;
    public Float programmedExtendedAmount;
    public String activationType;
    public Float deliveredExtendedAmount;
    public Float programmedFastAmount;
    public Integer programmedDuration;
    public Float deliveredFastAmount;
    public Integer effectiveDuration;
    public Boolean completed;
    public String bolusType;
    public Boolean autoModeOn;
    public Float bolusAmount;

}
