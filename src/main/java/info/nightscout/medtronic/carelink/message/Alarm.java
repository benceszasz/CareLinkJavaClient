package info.nightscout.medtronic.carelink.message;

import java.util.Date;

public class Alarm {

    public int code;
    public Date datetime;
    public String type;
    public boolean flash;
    public String kind;
    public long version;
    public Integer instanceId;
    public String messageId;
    public Integer sg;
    public Boolean pumpDeliverySuspendState;
    public String referenceGUID;

}
