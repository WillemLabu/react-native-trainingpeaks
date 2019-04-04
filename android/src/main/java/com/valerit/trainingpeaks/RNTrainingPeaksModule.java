
package com.valerit.trainingpeaks;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.io.File;

public class RNTrainingPeaksModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public RNTrainingPeaksModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNTrainingPeaks";
    }

    /*
     * */
    @ReactMethod
    public void login(String client_id, String redirect_uri, String response_type, String scope, Boolean isSandbox) {
        String sandboxUrl = "https://oauth.sandbox.trainingpeaks.com/OAuth/Authorize";
        String productionUrl = "https://oauth.trainingpeaks.com/OAuth/Authorize";

        response_type = response_type == null ? "code" : response_type;
        scope = scope == null ? "events:write,events:read,athlete:profile" : scope;

        if (client_id == null) {
            Log.e("rn-tp:", "client_id missing!");
            return;
        }

        if (redirect_uri == null) {
            Log.e("rn-tp:", "redirect_uri missing!");
            return;
        }

        Uri intentUri = Uri.parse(isSandbox ? sandboxUrl : productionUrl)
                .buildUpon()
                .appendQueryParameter("client_id", client_id)
                .appendQueryParameter("redirect_uri", redirect_uri)
                .appendQueryParameter("response_type", response_type)
                .appendQueryParameter("scope", scope)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, intentUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ReactContext context = getReactApplicationContext();
        context.startActivity(intent);
    }


    @ReactMethod
    public void readFitFile(String path, Promise promise) {
        java.io.File file = new java.io.File(path);
        try {
            java.io.InputStream insputStream = new java.io.FileInputStream(file);
            long length = file.length();
            byte[] bytes = new byte[(int) length];
            insputStream.read(bytes);
            insputStream.close();
            byte[] encoded = Base64.encode(bytes, Base64.DEFAULT);
            promise.resolve(new String(encoded));
        } catch (java.io.IOException e) {
            promise.reject("file_not_found");
        }
    }

/*
    @ReactMethod
    public void generateFitFile(ReadableMap session, Promise promise) {
        // Required Parameters, unit
        FileEncoder encode;
        String fileName = UUID.randomUUID().toString();
        java.io.File file;
        try {
            // , reactContext.getExternalMediaDirs()[0]
            file = java.io.File.createTempFile(fileName, ".fit");
        } catch (IOException e) {
            promise.reject("io_exception", "Failed to create a temp file! Please check if you have enough internal storage left.");
            return;
        }

        ReadableArray records = session.getArray("records");

        encode = new FileEncoder(file, Fit.ProtocolVersion.V2_0);

        DateTime timestamp = new DateTime(new Date((long) (session.getDouble("date"))));

        DateTime startTime = new DateTime(timestamp);
        startTime.add(-session.getDouble("usetime"));
        Log.d("react-native-strava", timestamp.getDate().toString());

        // TODO: use the right Manufacturer id, product id, serial number
        //Generate FileIdMessage
        FileIdMesg fileIdMesg = new FileIdMesg(); // Every FIT file MUST contain a 'File ID' message as the first message
        fileIdMesg.setManufacturer(Manufacturer.GARMIN);
        fileIdMesg.setType(File.ACTIVITY);
        fileIdMesg.setProduct(9001);
        fileIdMesg.setSerialNumber(1701L);
        fileIdMesg.setTimeCreated(timestamp);

        encode.write(fileIdMesg); // Encode the FileIDMesg

        EventMesg eventMesgStart = new EventMesg();
        eventMesgStart.setTimestamp(startTime);
        eventMesgStart.setEventType(EventType.START);
        encode.write(eventMesgStart);


        RecordMesg record = new RecordMesg();

        record.setActivityType(ActivityType.RUNNING);

        record.setTimestamp(startTime);
        record.setHeartRate((short) 0);
        record.setDistance(0.f);
        record.setSpeed(0.f);
        record.setCalories(0);

        // TODO: set steps
        encode.write(record);

        if (records != null) {
            Log.d("rn-strava-record:", records.toString());
            Log.d("rn-strava-record-size:", String.valueOf(records.size()));

            for (int i = 0; i < records.size(); i++) {
                ReadableMap r = records.getMap(i);
                DateTime tempTimestamp = new DateTime(startTime);

                // next start time
                tempTimestamp.add(r.getDouble("usetime"));
                Log.d("rn-strava-record:", tempTimestamp.toString());

                record.setTimestamp(tempTimestamp);
                record.setHeartRate((short) r.getInt("pulse"));
                record.setDistance((float) r.getDouble("distance"));
                record.setSpeed((float) r.getDouble("speed"));
                record.setCalories(r.getInt("calories"));

                encode.write(record);
            }
        }

        record.setActivityType(ActivityType.RUNNING);
        record.setTimestamp(timestamp);
        // TODO: extract lat, lng from session
//        record.setPositionLat(degreeToSemicircles(41.726667));
//        record.setPositionLong(degreeToSemicircles(44.883333));
        record.setHeartRate((short) session.getInt("pulse"));
        record.setDistance((float) session.getDouble("distance"));
        record.setSpeed((float) session.getDouble("speed"));
        record.setCalories(session.getInt("calories"));

        // TODO: set steps
        encode.write(record);

        EventMesg eventMesgEnd = new EventMesg();
        eventMesgEnd.setTimestamp(timestamp);
        eventMesgEnd.setEventType(EventType.STOP);
        encode.write(eventMesgEnd);

        LapMesg lapMsg = new LapMesg();
        lapMsg.setTimestamp(timestamp);
        lapMsg.setTotalElapsedTime((float) session.getDouble("usetime"));
        lapMsg.setTotalTimerTime((float) session.getDouble("usetime"));
        lapMsg.setTotalDistance((float) session.getDouble("distance"));

        EventMesg eventMesgDisableAll = new EventMesg();
        eventMesgDisableAll.setTimestamp(timestamp);
        eventMesgDisableAll.setEventType(EventType.STOP_DISABLE_ALL);
        encode.write(eventMesgDisableAll);

        SessionMesg sessionMsg = new SessionMesg();
        sessionMsg.setSport(Sport.RUNNING);
        sessionMsg.setStartTime(startTime);
        sessionMsg.setTotalElapsedTime((float) session.getDouble("usetime"));
        sessionMsg.setTotalTimerTime((float) session.getDouble("usetime"));
        sessionMsg.setTotalDistance((float) session.getDouble("distance"));
        sessionMsg.setTotalAscent(0);
        sessionMsg.setTimestamp(timestamp);
        encode.write(sessionMsg);

        ActivityMesg aMsg = new ActivityMesg();
        aMsg.setNumSessions(1);
        aMsg.setTotalTimerTime((float) session.getDouble("usetime"));
        aMsg.setTimestamp(timestamp);

        encode.write(aMsg);

        try {
            encode.close();
        } catch (FitRuntimeException e) {
            promise.reject("io_exception", "Failed to finalize encoding.");
            return;
        }
        try {
            java.io.InputStream insputStream = new java.io.FileInputStream(file);
            long length = file.length();
            byte[] bytes = new byte[(int) length];
            insputStream.read(bytes);
            insputStream.close();
            byte[] encoded = Base64.encode(bytes, Base64.DEFAULT);
            promise.resolve(new String(encoded));
        } catch (java.io.IOException e) {
            promise.reject("file_not_found");
        }
    }
*/
}
