package info.nightscout.medtronic.carelink.message;

public class MonitorData {

    public String deviceFamily;

    public boolean isBle() {
        return deviceFamily.contains("BLE");
    }

}
