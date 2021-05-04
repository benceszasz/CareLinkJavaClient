package info.nightscout.medtronic.carelink.cli;

import com.google.gson.GsonBuilder;
import info.nightscout.medtronic.carelink.client.CareLinkClient;
import info.nightscout.medtronic.carelink.message.*;
import org.apache.commons.cli.*;


import java.io.FileWriter;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CareLinkClientCLI {

    private static final String OPTION_COUNTRY = "c";
    private static final String OPTION_USERNAME = "u";
    private static final String OPTION_PASSWORD = "p";
    private static final String OPTION_OUTPUT = "o";
    private static final String OPTION_REPEAT = "r";
    private static final String OPTION_DATA = "d";
    private static final String OPTION_SESSION = "s";
    private static final String OPTION_WAIT = "w";
    private static final String OPTION_ANONYM = "a";
    private static final String OPTION_VERBOSE = "v";
    private static final String OPTION_JSON_EXCEPTION = "j";


    private static Options generateOptions() {

        final Options options = new Options();

        //v - Verbose mode
        options.addOption(
                Option.builder(OPTION_VERBOSE)
                        .required(false)
                        .longOpt("verbose")
                        .hasArg(false)
                        .desc("Verbose mode.")
                        .build());

        //c - CareLink country
        options.addOption(
                Option.builder(OPTION_COUNTRY)
                .required(true)
                .hasArg(true)
                .longOpt("country")
                .desc("CareLink two letter country code.")
                .build());

        //u - CareLink username
        options.addOption(
                Option.builder(OPTION_USERNAME)
                .required(true)
                .longOpt("username")
                .hasArg(true)
                .desc("CareLink username.")
                .build());

        //p - CareLink password
        options.addOption(
                Option.builder(OPTION_PASSWORD)
                .required(true)
                .longOpt("password")
                .hasArg(true)
                .desc("CareLink password.")
                .build());

        //o - Output folder
        options.addOption(
                Option.builder(OPTION_OUTPUT)
                .required(false)
                .longOpt("output")
                .hasArg(true)
                .desc("Output folder.")
                .build());

        //s - Session
        options.addOption(
                Option.builder(OPTION_SESSION)
                        .required(false)
                        .longOpt("session")
                        .hasArg(false)
                        .desc("Save session info.")
                        .build());

        //d - Data
        options.addOption(
                Option.builder(OPTION_DATA)
                .required(false)
                .longOpt("data")
                .hasArg(false)
                .desc("Save recent data.")
                .build());

        //a - Anonymize data
        options.addOption(
                Option.builder(OPTION_ANONYM)
                        .required(false)
                        .longOpt("anonym")
                        .hasArg(false)
                        .desc("Anonymize data")
                        .build());

        //r - Repeat times
        options.addOption(
                Option.builder(OPTION_REPEAT)
                        .required(false)
                        .longOpt("repeat")
                        .hasArg(true)
                        .desc("Repeat request times.")
                        .build());

        //w - Wait minutes
        options.addOption(
                Option.builder(OPTION_WAIT)
                        .required(false)
                        .longOpt("wait")
                        .hasArg(true)
                        .desc("Wait minutes between repeated calls.")
                        .build());

        //j - Json exception
        options.addOption(
                Option.builder(OPTION_JSON_EXCEPTION)
                        .required(false)
                        .longOpt("jsonex")
                        .hasArg(false)
                        .desc("Dump response for data Json exception.")
                        .build());

        return options;

    }


    public static void main(String[] args) throws ParseException {


        String folder;
        int repeat;
        int wait;
        boolean downloadSession;
        boolean downloadRecentData;
        boolean verbose;
        boolean anonymize;
        boolean dumpJsonException;


        Options options = generateOptions();

        //print help if started wo params
        if(args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("CareLinkClientCLI", options);

        //call client with params
        }else {
            try {
                CommandLineParser parser = new DefaultParser();
                CommandLine cmd = parser.parse(options, args);
                //Set params
                verbose = cmd.hasOption(OPTION_VERBOSE);
                downloadSession = cmd.hasOption(OPTION_SESSION);
                downloadRecentData = cmd.hasOption(OPTION_DATA);
                anonymize = cmd.hasOption(OPTION_ANONYM);
                dumpJsonException = cmd.hasOption(OPTION_JSON_EXCEPTION);
                folder = (cmd.hasOption(OPTION_OUTPUT)) ? cmd.getOptionValue(OPTION_OUTPUT) : null;
                repeat = (cmd.hasOption(OPTION_REPEAT)) ? Integer.parseInt(cmd.getOptionValue(OPTION_REPEAT)) : 1;
                wait = (cmd.hasOption(OPTION_WAIT)) ? Integer.parseInt(cmd.getOptionValue(OPTION_WAIT)) : 1;
                //Execute client
                callCareLinkClient(
                        verbose,
                        cmd.getOptionValue(OPTION_USERNAME), cmd.getOptionValue(OPTION_PASSWORD), cmd.getOptionValue(OPTION_COUNTRY),
                        downloadSession, downloadRecentData,
                        anonymize,
                        folder,
                        repeat, wait,
                        dumpJsonException);
            } catch (MissingOptionException| UnrecognizedOptionException  exOption) {
                System.out.println(exOption.getMessage());
                System.out.println("Run without options to get usage info!");
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }


    }

    private static void callCareLinkClient(boolean verbose, String username, String password, String country, Boolean downloadSessionInfo, Boolean downloadData, boolean anonymize, String folder, int repeat, int wait, boolean dumpJsonException){

        CareLinkClient client = null;
        RecentData recentData = null;

        client = new CareLinkClient(username, password, country);
        if(verbose)printLog("Client created!");

        if(client.login()) {

            for(int i = 0; i < repeat; i++) {
                if (verbose) printLog("Starting download, count:  " + String.valueOf(i + 1));
                //Session info is requested
                if (downloadSessionInfo) {
                    writeJson(client.getSessionUser(), folder, "user", anonymize, verbose);
                    writeJson(client.getSessionProfile(), folder, "profile", anonymize, verbose);
                    writeJson(client.getSessionCountrySettings(), folder, "country", anonymize, verbose);
                    writeJson(client.getSessionMonitorData(), folder, "monitor", anonymize, verbose);
                }
                //Recent data is requested
                if(downloadData) {
                    try {
                        for(int j = 0; j < 2; j++) {
                            recentData = client.getRecentData();
                            //Auth error
                            if(client.getLastResponseCode() == 401) {
                                printLog("GetRecentData login error (response code 401). Trying again in 1 sec!");
                                Thread.sleep(1000);
                            }
                            //Get success
                            else if(client.getLastResponseCode() == 200) {
                                //Data OK
                                if(client.getLastDataSuccess()) {
                                    writeJson(recentData, folder, "data", anonymize, verbose);
                                //Data error
                                } else {
                                    printLog("Data exception: " + (client.getLastErrorMessage() == null ? "no details available" : client.getLastErrorMessage()));
                                    if(dumpJsonException){
                                        writeFile(client.getLastResponseBody(), folder, "dataex", verbose);
                                    }
                                }
                                //STOP!!!
                                break;
                            } else  {
                                printLog("Error, response code: " + String.valueOf(client.getLastResponseCode()) + " Trying again in 1 sec!");
                                Thread.sleep(1000);
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
                try {
                    if(i < repeat - 1) {
                        if (verbose) printLog("Waiting " + String.valueOf(wait) + " minutes before next download!");
                        Thread.sleep(wait * 60000);
                    }
                } catch (Exception ex){ }
            }
        } else {
            printLog("Client login error! Response code: " + String.valueOf(client.getLastResponseCode()) + " Error message: " + client.getLastErrorMessage());
        }


    }

    protected static void writeJson(Object object, String folder, String name, boolean anonymize, boolean verbose){

        String content;

        //Anonymize data
        if(anonymize) {
            anonymizeData(object);
        }

        //Convert JSON to string and write to file
        try {
            content = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create().toJson(object);
            writeFile(content, folder, name, verbose);
        } catch (Exception ex) {
            printLog("Error during save of " + name + " . Details: " + ex.getClass().getName() + " - " + ex.getMessage());
        }

    }


    protected static void writeFile(String content, String folder, String name, boolean verbose){

        FileWriter writer = null;
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String filename = name + "-" + sdfDate.format(Calendar.getInstance().getTime()) + ".json";

        try {
            if(folder == null)
                writer = new FileWriter(filename);
            else
                writer = new FileWriter(Paths.get(folder, filename).toAbsolutePath().toString());
            writer.write(content);
            writer.flush();
            writer.close();
            if (verbose) printLog(name + " saved!");
        } catch (Exception ex) {
            printLog("Error during save of " + name + " . Details: " + ex.getClass().getName() + " - " + ex.getMessage());
        }

    }


    protected static void anonymizeData(Object object){

        User user;
        Profile profile;
        RecentData recentData;

        if(object != null){
            if(object instanceof User){
                user = (User) object;
                user.accountId = 99999999;
                user.id = String.valueOf(user.accountId);
                user.lastName = "LastName";
                user.firstName = "FirstName";
            } else if(object instanceof Profile){
                profile = (Profile) object;
                profile.address = "Address";
                profile.firstName = "FirstName";
                profile.lastName = "LastName";
                profile.middleName = "MiddleName";
                profile.dateOfBirth  = "1900-01-01";
                profile.city = "City";
                profile.email = "email@email.email";
                profile.parentFirstName = "ParentFirstName";
                profile.parentLastName = "ParentLastName";
                profile.phone = "+00-00-000000";
                profile.phoneLegacy = "+00-00-000000";
                profile.postalCode = "9999";
                profile.patientNickname = "Nickname";
                profile.stateProvince = "State";
                profile.username = "Username";
            } else if(object instanceof RecentData){
                recentData = (RecentData) object;
                recentData.firstName = "FirstName";
                recentData.lastName = "LastName";
                recentData.medicalDeviceSerialNumber = "SN9999999X";
                recentData.conduitSerialNumber = "XXXXXX-XXXX-XXXX-XXXX-9999-9999-9999-9999";
            }
        }


    }

    protected static void printLog(String logText){
        System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Calendar.getInstance().getTime()) + " " + logText);
    }


}
