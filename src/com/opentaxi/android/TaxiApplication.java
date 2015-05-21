package com.opentaxi.android;

import android.app.Application;
import com.opentaxi.android.utils.CrashReportSender;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

/**
 * Created with IntelliJ IDEA.
 * User: stanimir
 * Date: 12/27/12
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
//@ReportsCrashes(formKey = "dF8wOUJYbFhCeDVlMG1JT3FkN2xXM0E6MQ", logcatFilterByPid = true)
@ReportsCrashes(logcatFilterByPid = true) //formKey = "",
public class TaxiApplication extends Application {

    public static String gcmId = "";
    //private static boolean havePlayService = true;
    private static boolean requestsVisible = false;
    private static boolean requestsHistory = false;
    private static boolean requestsDetailsVisible = false;
    private static boolean userPassVisible = false;
    private static boolean mapVisible = false;
    private static Integer lastRequestId;

    /*public static void setHavePlayService(boolean havePlayService) {
        TaxiApplication.havePlayService = havePlayService;
    }

    public static boolean isHavePlayService() {
        return havePlayService;
    }*/

    public static boolean isRequestsVisible() {
        return requestsVisible;
    }

    public static void requestsResumed() {
        requestsVisible = true;
    }

    public static void requestsPaused() {
        requestsVisible = false;
    }

    public static boolean isRequestsHistory() {
        return requestsHistory;
    }

    public static void requestsHistory(boolean history) {
        requestsHistory = history;
    }

    public static boolean isRequestsDetailsVisible() {
        return requestsDetailsVisible;
    }

    public static void requestsDetailsResumed() {
        requestsDetailsVisible = true;
    }

    public static void requestsDetailsPaused() {
        requestsDetailsVisible = false;
    }

    public static boolean isUserPassVisible() {
        return userPassVisible;
    }

    public static void userPassResumed() {
        userPassVisible = true;
    }

    public static void userPassPaused() {
        userPassVisible = false;
    }

    public static boolean isMapVisible() {
        return mapVisible;
    }

    public static void mapResumed() {
        mapVisible = true;
    }

    public static void mapPaused() {
        mapVisible = false;
    }

    public static Integer getLastRequestId() {
        return lastRequestId;
    }

    public static void setLastRequestId(Integer lastRequestId) {
        TaxiApplication.lastRequestId = lastRequestId;
    }

    @Override
    public void onCreate() {

        try {
            Class.forName("android.os.AsyncTask");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        super.onCreate();

        //RestClient.getInstance().clearCache();

        AndroidGraphicFactory.createInstance(this);

        //Log.d("TaxiApplication", "onCreate()");

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        CrashReportSender mySender = new CrashReportSender();
        ACRA.getErrorReporter().setReportSender(mySender);

        /*// output debug to LogCat, with tag LittleFluffyLocationLibrary
        //LocationLibrary.showDebugOutput(true);

        // in most cases the following initialising code using defaults is probably sufficient:
        //
        // LocationLibrary.initialiseLibrary(getBaseContext(), "com.opentaxi.android.service");
        //
        // however for the purposes of the test app, we will request unrealistically frequent location broadcasts
        // every 1 minute, and force a location update if there hasn't been one for 2 minutes.
        LocationLibrary.initialiseLibrary(getBaseContext(), 30 * 1000, 60 * 1000, "com.opentaxi.android");
        LocationLibrary.useFineAccuracyForRequests(true);
        LocationLibrary.showDebugOutput(true);
        //LocationLibrary.forceLocationUpdate(getBaseContext());
        LocationLibrary.startAlarmAndListener(getBaseContext());*/
    }
}