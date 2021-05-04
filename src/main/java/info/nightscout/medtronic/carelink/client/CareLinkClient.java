package info.nightscout.medtronic.carelink.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import info.nightscout.medtronic.carelink.message.*;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CareLinkClient {

    protected static final String CARELINK_CONNECT_SERVER_EU = "carelink.minimed.eu";
    protected static final String CARELINK_CONNECT_SERVER_US = "carelink.minimed.com";
    protected static final String CARELINK_LANGUAGE_EN = "en";
    protected static final String CARELINK_LOCALE_EN = "en";
    protected static final String CARELINK_AUTH_TOKEN_COOKIE_NAME = "auth_tmp_token";
    protected static final String CARELINK_TOKEN_VALIDTO_COOKIE_NAME = "c_token_valid_to";
    protected static final int AUTH_EXPIRE_DEADLINE_MINUTES = 1;

    //Authentication data
    protected String carelinkUsername;
    protected String carelinkPassword;
    protected String carelinkCountry;

    //Session info
    protected boolean loggedIn;
    public boolean isLoggedIn(){
        return loggedIn;
    }
    protected User sessionUser;
    public User getSessionUser() {
        return getClone(sessionUser, User.class);
    }
    protected Profile sessionProfile;
    public Profile getSessionProfile() {
        return getClone(sessionProfile, Profile.class);
    }
    protected CountrySettings sessionCountrySettings;
    public CountrySettings getSessionCountrySettings() {
        return getClone(sessionCountrySettings, CountrySettings.class);
    }
    protected MonitorData sessionMonitorData;
    public MonitorData getSessionMonitorData() {
        return getClone(sessionMonitorData, MonitorData.class);
    }

    protected <T> T getClone(Object origObject, Class<T> origClass){

        Object clone = null;

        try{
            clone = new GsonBuilder().create().fromJson(new GsonBuilder().create().toJson(origObject), origClass);
        }catch (Exception ex){ }

        return clone == null ? null : origClass.cast(clone);

    }

    //Communication info
    protected OkHttpClient httpClient = null;
    protected boolean loginInProcess = false;
    protected String lastResponseBody;
    protected void setLastResponseBody(Response response){
        try{
            this.lastResponseBody = response.body().string();
        } catch (Exception ex){}
    }
    protected void setLastResponseBody(String responseBody){
            this.lastResponseBody = responseBody;
    }
    public String getLastResponseBody(){
        return lastResponseBody;
    }
    protected int lastResponseCode;
    public int getLastResponseCode() {
        return lastResponseCode;
    }
    protected boolean lastLoginSuccess;
    public boolean getLastLoginSuccess() {
        return lastLoginSuccess;
    }
    protected boolean lastDataSuccess;
    public boolean getLastDataSuccess() {
        return lastDataSuccess;
    }
    protected String lastErrorMessage;
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    protected String lastStackTraceString;
    public String getLastStackTraceString() {
        return lastStackTraceString;
    }

    protected enum RequestType {
        HtmlGet(), HtmlPost(), Json()
    }


    public CareLinkClient(String carelinkUsername, String carelinkPassword, String carelinkCountry) {

        this.carelinkUsername = carelinkUsername;
        this.carelinkPassword = carelinkPassword;
        this.carelinkCountry = carelinkCountry;

        // Create main http client with CookieJar
        this.httpClient = new OkHttpClient.Builder()
                .cookieJar(new SimpleOkHttpCookieJar())
                .connectionPool(new ConnectionPool(5, 10, TimeUnit.MINUTES))
                .build();
    }

    /*
     *  WRAPPER DATA RETRIEVAL METHODS
     */
    public RecentData getRecentData() {

        // Force login to get basic info
        if(getAuthorizationToken() != null) {
            if (CountryUtils.isUS(carelinkCountry) || sessionMonitorData.isBle())
                return this.getConnectDisplayMessage(this.sessionProfile.username, this.sessionUser.getUserRole(),
                        sessionCountrySettings.blePereodicDataEndpoint);
            else
                return this.getLast24Hours();
        }
        else {
            return null;
        }


    }

    // Get server URL
    protected String careLinkServer() {
       return this.carelinkCountry.equals("us") ? CARELINK_CONNECT_SERVER_US : CARELINK_CONNECT_SERVER_EU;
    }


    // Authentication methods
    public boolean login(){
        if(!this.loggedIn)
            this.executeLoginProcedure();
        return this.loggedIn;
    }

    protected boolean executeLoginProcedure() {

        Response loginSessionResponse = null;
        Response doLoginResponse = null;
        Response consentResponse = null;

        lastLoginSuccess = false;
        loginInProcess = true;
        lastErrorMessage = null;

        try {
            // Clear cookies
            ((SimpleOkHttpCookieJar) this.httpClient.cookieJar()).deleteAllCookies();

            // Clear basic infos
            this.sessionUser = null;
            this.sessionProfile = null;
            this.sessionCountrySettings = null;
            this.sessionMonitorData = null;

            // Open login (get SessionId and SessionData)
            loginSessionResponse = this.getLoginSession();
            this.lastResponseCode = loginSessionResponse.code();

            // Login
            doLoginResponse = this.doLogin(loginSessionResponse);
            this.lastResponseCode = doLoginResponse.code();
            setLastResponseBody(loginSessionResponse);
            loginSessionResponse.close();

            // Consent
            consentResponse = this.doConsent(doLoginResponse);
            setLastResponseBody(doLoginResponse);
            doLoginResponse.close();
            this.lastResponseCode = consentResponse.code();
            setLastResponseBody(consentResponse);
            consentResponse.close();

            // Get sessions infos if required
            if(this.sessionUser == null)
                this.sessionUser = this.getMyUser();
            if(this.sessionProfile == null)
                this.sessionProfile = this.getMyProfile();
            if(this.sessionCountrySettings == null)
                this.sessionCountrySettings = this.getCountrySettings(this.carelinkCountry, CARELINK_LANGUAGE_EN);
            if(this.sessionMonitorData == null)
                this.sessionMonitorData = this.getMonitorData();
            // Set login success if everything was ok:
            if(this.sessionUser != null && this.sessionProfile != null && this.sessionCountrySettings != null && this.sessionMonitorData != null)
                lastLoginSuccess = true;

        } catch (Exception e) {
            lastErrorMessage = e.getMessage();
            // lastStackTraceString = Log.getStackTraceString(e);
        }

        loginInProcess = false;
        loggedIn = lastLoginSuccess;

        return lastLoginSuccess;

    }

    protected Response getLoginSession() throws IOException {

        HttpUrl url = null;
        Request.Builder requestBuilder = null;

        url = new HttpUrl.Builder().scheme("https").host(this.careLinkServer()).addPathSegments("patient/sso/login")
                .addQueryParameter("country", this.carelinkCountry).addQueryParameter("lang", CARELINK_LANGUAGE_EN)
                .build();

        requestBuilder = new Request.Builder().url(url);

        this.addHttpHeaders(requestBuilder, RequestType.HtmlGet);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    protected Response doLogin(Response loginSessionResponse) throws IOException {

        HttpUrl url = null;
        Request.Builder requestBuilder = null;
        RequestBody form = null;

        form = new FormBody.Builder()
                .add("sessionID", loginSessionResponse.request().url().queryParameter("sessionID"))
                .add("sessionData", loginSessionResponse.request().url().queryParameter("sessionData"))
                .add("locale", CARELINK_LOCALE_EN)
                .add("action", "login")
                .add("username", this.carelinkUsername)
                .add("password", this.carelinkPassword)
                .add("actionButton", "Log in")
                .build();

        url = new HttpUrl.Builder().scheme("https").host("mdtlogin.medtronic.com")
                .addPathSegments("mmcl/auth/oauth/v2/authorize/login").addQueryParameter("locale", CARELINK_LOCALE_EN)
                .addQueryParameter("country", this.carelinkCountry).build();

        requestBuilder = new Request.Builder().url(url).post(form);

        this.addHttpHeaders(requestBuilder, RequestType.HtmlGet);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    protected Response doConsent(Response doLoginResponse) throws IOException {

        Request.Builder requestBuilder = null;
        RequestBody form = null;
        String doLoginRespBody = null;

        doLoginRespBody = doLoginResponse.body().string();

        // Extract data for consent
        String consentUrl = this.extractResponseData(doLoginRespBody, "(form action=\")(.*)(\" method=\"POST\")", 2);
        String consentSessionData = this.extractResponseData(doLoginRespBody,
                "(<input type=\"hidden\" name=\"sessionData\" value=\")(.*)(\">)", 2);
        String consentSessionId = this.extractResponseData(doLoginRespBody,
                "(<input type=\"hidden\" name=\"sessionID\" value=\")(.*)(\">)", 2);

        // Send consent
        form = new FormBody.Builder().add("action", "consent").add("sessionID", consentSessionId)
                .add("sessionData", consentSessionData).add("response_type", "code").add("response_mode", "query")
                .build();

        requestBuilder = new Request.Builder().url(consentUrl).post(form);

        this.addHttpHeaders(requestBuilder, RequestType.HtmlPost);

        return this.httpClient.newCall(requestBuilder.build()).execute();

    }

    protected String getAuthorizationToken() {

        // New token is needed:
        // a) no token or about to expire => execute authentication
        // b) last response 401
        if (!((SimpleOkHttpCookieJar) httpClient.cookieJar()).contains(CARELINK_AUTH_TOKEN_COOKIE_NAME)
                || !((SimpleOkHttpCookieJar) httpClient.cookieJar()).contains(CARELINK_TOKEN_VALIDTO_COOKIE_NAME)
                || !((new Date(Date.parse(((SimpleOkHttpCookieJar) httpClient.cookieJar())
                .getCookies(CARELINK_TOKEN_VALIDTO_COOKIE_NAME).get(0).value())))
                .after(new Date(new Date(System.currentTimeMillis()).getTime()
                        + AUTH_EXPIRE_DEADLINE_MINUTES * 60000)))
                || this.lastResponseCode == 401
        ) {
            //execute new login process | null, if error OR already doing login
            if(this.loginInProcess || !this.executeLoginProcedure())
                return null;

        }

        // there can be only one
        return "Bearer" + " " + ((SimpleOkHttpCookieJar) httpClient.cookieJar()).getCookies(CARELINK_AUTH_TOKEN_COOKIE_NAME).get(0).value();

    }

    /*
     * CareLink APIs
     */

    // My user
    public User getMyUser() {
        return this.getData(this.careLinkServer(), "patient/users/me", null, null, User.class);
    }

    // My profile
    public Profile getMyProfile() {
        return this.getData(this.careLinkServer(), "patient/users/me/profile", null, null, Profile.class);
    }

    // Monitoring data
    public MonitorData getMonitorData() {
        return this.getData(this.careLinkServer(), "patient/monitor/data", null, null, MonitorData.class);
    }

    // Country settings
    public CountrySettings getCountrySettings(String country, String language) {

        Map<String, String> queryParams = null;

        queryParams = new HashMap<String, String>();
        queryParams.put("countryCode", country);
        queryParams.put("language", language);

        return this.getData(this.careLinkServer(), "patient/countries/settings", queryParams, null,
                CountrySettings.class);

    }

    // Old last24hours webapp data
    public RecentData getLast24Hours() { //Last24Hour getLast24Hours() {

        Map<String, String> queryParams = null;

        queryParams = new HashMap<String, String>();
        queryParams.put("cpSerialNumber", "NONE");
        queryParams.put("msgType", "last24hours");
        queryParams.put("requestTime", String.valueOf(System.currentTimeMillis()));

        return this.getData(this.careLinkServer(), "patient/connect/data", queryParams, null, RecentData.class);

    }

    // Periodic data from CareLink Cloud
    public RecentData getConnectDisplayMessage(String username, String role, String endpointUrl) {

        RequestBody requestBody = null;
        Gson gson = null;
        JsonObject userJson = null;

        // Build user json for request
        userJson = new JsonObject();
        userJson.addProperty("username", username);
        userJson.addProperty("role", role);

        gson = new GsonBuilder().create();

        requestBody = RequestBody.create(gson.toJson(userJson), MediaType.get("application/json; charset=utf-8"));

        RecentData recentData = this.getData(HttpUrl.parse(endpointUrl), requestBody, RecentData.class);
        if (recentData != null)
            correctTimeInRecentData(recentData);
        return recentData;

    }


    // Helper methods
    // Response parsing
    protected String extractResponseData(String respBody, String groupRegex, int groupIndex) {

        String responseData = null;

        Matcher responseDataMatcher = Pattern.compile(groupRegex).matcher(respBody);
        if (responseDataMatcher.find())
            responseData = responseDataMatcher.group(groupIndex);

        return responseData;

    }

    // Http header builder for requests
    protected void addHttpHeaders(Request.Builder requestBuilder, RequestType type) {

        //Add common browser headers
        requestBuilder.addHeader("Accept-Language", "en;q=0.9, *;q=0.8").addHeader("Connection", "keep-alive")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"87\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"87\"")
                .addHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36")
        //.addHeader("Connection", "keep-alive");
        ;

        //Set media type based on request type
        switch (type) {
            case Json:
                requestBuilder.addHeader("Accept", "application/json, text/plain, */*");
                requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
                break;
            case HtmlGet:
                requestBuilder.addHeader("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                break;
            case HtmlPost:
                requestBuilder.addHeader("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded");
                break;
        }

    }

    // Data request for API calls
    protected <T> T getData(HttpUrl url, RequestBody requestBody, Class<T> dataClass) {

        Object data = null;

        this.lastDataSuccess = false;
        this.lastErrorMessage = null;

        // Get auth token
        String authToken = this.getAuthorizationToken();

        if (authToken != null) {

            // Create request for URL with authToken
            Request.Builder requestBuilder = new Request.Builder().url(url).addHeader("Authorization", authToken);

            // Add header
            if (requestBody == null) {
                this.addHttpHeaders(requestBuilder, RequestType.Json);
            } else {
                requestBuilder.post(requestBody);
                this.addHttpHeaders(requestBuilder, RequestType.HtmlPost);
            }

            String responseBody = null;

            // Send request
            try {
                Response response = this.httpClient.newCall(requestBuilder.build()).execute();
                this.lastResponseCode = response.code();
                if (response.isSuccessful()) {
                    responseBody = response.body().string();
                    setLastResponseBody(responseBody);
                    data = new GsonBuilder().create().fromJson(responseBody, dataClass);
                    this.lastDataSuccess = true;
                }
                response.close();
            } catch (Exception e) {
                lastErrorMessage = e.getMessage();
            }

        }

        //Return result
        if(data != null)
            return dataClass.cast(data);
        else
            return null;

    }

    protected <T> T getData(String host, String path, Map<String, String> queryParams, RequestBody requestBody,
                            Class<T> dataClass) {

        HttpUrl.Builder urlBuilder = null;
        HttpUrl url = null;

        // Build url
        urlBuilder = new HttpUrl.Builder().scheme("https").host(host).addPathSegments(path);
        if (queryParams != null) {
            for (Map.Entry<String, String> param : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(param.getKey(), param.getValue());
            }
        }

        url = urlBuilder.build();

        return this.getData(url, requestBody, dataClass);

    }

    protected void correctTimeInRecentData(RecentData recentData){

        if(recentData.sMedicalDeviceTime != null &&  recentData.lastMedicalDeviceDataUpdateServerTime > 1) {

            //Calc time diff between event time and actual local time (timezone is wrong in CareLink data)
            int diffInHour = (int) Math.round(((recentData.lastMedicalDeviceDataUpdateServerTime - recentData.sMedicalDeviceTime.getTime()) / 3600000D));

            //Correct times if server <> device time differs in hours
            if (diffInHour != 0 && diffInHour < 26) {


                recentData.medicalDeviceTimeAsString = shiftDateByHours(recentData.medicalDeviceTimeAsString, diffInHour);
                recentData.sMedicalDeviceTime = shiftDateByHours(recentData.sMedicalDeviceTime, diffInHour);
                recentData.lastConduitDateTime = shiftDateByHours(recentData.lastConduitDateTime, diffInHour);
                recentData.lastSensorTSAsString =  shiftDateByHours(recentData.lastSensorTSAsString, diffInHour);
                recentData.sLastSensorTime = shiftDateByHours(recentData.sLastSensorTime, diffInHour);
                //Sensor
                if(recentData.sgs != null){
                    for (SensorGlucose sg : recentData.sgs) {
                        sg.datetime = shiftDateByHours(sg.datetime, diffInHour);
                    }
                }
                //Markers
                if(recentData.markers != null){
                    for (Marker marker : recentData.markers) {
                        marker.dateTime = shiftDateByHours(marker.dateTime, diffInHour);
                    }
                }
                //Notifications
                if(recentData.notificationHistory != null){
                    if(recentData.notificationHistory.clearedNotifications != null) {
                        for (ClearedNotification notification : recentData.notificationHistory.clearedNotifications) {
                            notification.dateTime = shiftDateByHours(notification.dateTime, diffInHour);
                            notification.triggeredDateTime= shiftDateByHours(notification.triggeredDateTime, diffInHour);
                        }
                    }
                    if(recentData.notificationHistory.activeNotifications != null) {
                        for(ActiveNotification notification : recentData.notificationHistory.activeNotifications) {
                            notification.dateTime = shiftDateByHours(notification.dateTime, diffInHour);
                        }
                    }
                }
            }
        }

    }

    protected Date shiftDateByHours(Date date, int hours){
        if(date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.HOUR_OF_DAY, hours);
            return calendar.getTime();
        } else {
            return  null;
        }
    }

}
