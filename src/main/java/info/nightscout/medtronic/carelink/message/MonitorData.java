package info.nightscout.medtronic.carelink.message;

public class MonitorData {

    public String deviceFamily;

    public boolean isBleX() {
        return deviceFamily.contains("BLE_X");
    }

}
