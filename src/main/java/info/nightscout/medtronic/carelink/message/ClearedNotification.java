package info.nightscout.medtronic.carelink.message;

import java.util.Date;

public class ClearedNotification {

    public String GUID;
    public String referenceGUID;
    public Date dateTime;
    public String type;
    public int faultId;
    public int instanceId;
    public String messageId;
    public String pumpDeliverySuspendState;
    public String pnpId;
    public int relativeOffset;
    public Date triggeredDateTime;
    public Boolean alertSilenced;

}
