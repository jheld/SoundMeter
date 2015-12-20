package com.yl.soundmeter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Admin on 13-9-10.
 */
public class Recorder{

    private MediaRecorder mRecorder = null;
    private Context mContext;
    String postUrl = "http://192.168.0.121:8000";

    public Recorder(Context applicationContext) {

        mContext = applicationContext;
    }

    private void RecorderErr()
    {
        mRecorder = null;
        Toast.makeText(mContext, mContext.getString(R.string.msg_mic_error), Toast.LENGTH_SHORT).show();
    }

    public void RecorderInit()
    {
        float bak = new CalAvg().Cal(3.0f);
        Log.d("SoundMeter", String.valueOf(bak));


        if (mRecorder != null)
            return;

        try
        {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(1);
            mRecorder.setOutputFormat(1);
            mRecorder.setAudioEncoder(1);
            mRecorder.setOutputFile("/dev/null");
            mRecorder.prepare();
            mRecorder.start();


        }
        catch (IllegalStateException e) {
            e.printStackTrace();
            RecorderErr();
        }
        catch (IOException e) {
            e.printStackTrace();
            RecorderErr();
        }
        catch (Exception e) {
            e.printStackTrace();
            RecorderErr();
        }

        return;
    }

    public void RecorderRel() {

        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // Create a JSON Object from the supplied data
    public JSONObject dataToJson(String value) {

        // Make a new JSON objects
        JSONObject obj = new JSONObject();

        // Put the data in it
        try {
            obj.put("value", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }


//    public static String getResponseBody(HttpResponse response) {
//
//        String response_text = null;
//
//        HttpEntity entity = null;
//
//        try {
//
//            entity = response.getEntity();
//
//            response_text = _getResponseBody(entity);
//
//        } catch (ParseException e) {
//
//            e.printStackTrace();
//
//        } catch (IOException e) {
//
//            if (entity != null) {
//
//                try {
//
//                    entity.consumeContent();
//
//                } catch (IOException e1) {
//
//                }
//
//            }
//
//        }
//
//        return response_text;
//
//    }

    public String _getResponseBody(final HttpEntity entity) throws IOException, ParseException {

        if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }

        InputStream instream = entity.getContent();

        if (instream == null) { return ""; }

        if (entity.getContentLength() > Integer.MAX_VALUE) { throw new IllegalArgumentException(

                "HTTP entity too large to be buffered in memory"); }

        String charset = getContentCharSet(entity);

        if (charset == null) {

            charset = HTTP.DEFAULT_CONTENT_CHARSET;

        }

        Reader reader = new InputStreamReader(instream, charset);

        StringBuilder buffer = new StringBuilder();

        try {

            char[] tmp = new char[1024];

            int l;

            while ((l = reader.read(tmp)) != -1) {

                buffer.append(tmp, 0, l);

            }

        } finally {

            reader.close();

        }

        return buffer.toString();

    }

    public String getContentCharSet(final HttpEntity entity) throws ParseException {

        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }

        String charset = null;

        if (entity.getContentType() != null) {

            HeaderElement values[] = entity.getContentType().getElements();

            if (values.length > 0) {

                NameValuePair param = values[0].getParameterByName("charset");

                if (param != null) {

                    charset = param.getValue();

                }

            }

        }

        return charset;
    }

    private static JSONObject getJsonObjectFromMap(Map params) throws JSONException {

        //all the passed parameters from the post request
        //iterator used to loop through all the parameters
        //passed in the post request
        Iterator iter = params.entrySet().iterator();

        //Stores JSON
        JSONObject holder = new JSONObject();

        //using the earlier example your first entry would get email
        //and the inner while would get the value which would be 'foo@bar.com'
        //{ fan: { email : 'foo@bar.com' } }

        //While there is another entry
        while (iter.hasNext())
        {
            //gets an entry in the params
            Map.Entry pairs = (Map.Entry)iter.next();

            //creates a key for Map
            String key = (String)pairs.getKey();

            //Create a new map
            Map m = (Map)pairs.getValue();

            //object for storing Json
            JSONObject data = new JSONObject();

            //gets the value
            Iterator iter2 = m.entrySet().iterator();
            while (iter2.hasNext())
            {
                Map.Entry pairs2 = (Map.Entry)iter2.next();
                data.put((String)pairs2.getKey(), (String)pairs2.getValue());
            }

            //puts email and 'foo@bar.com'  together in map
            holder.put(key, data);
        }
        return holder;
    }

    public static Object makeRequest(String path, Map params) throws Exception
    {
        //instantiates httpclient to make request
        DefaultHttpClient httpclient = new DefaultHttpClient();

        //url with the post data
        HttpPost httpost = new HttpPost(path);

        //convert parameters into JSON object
        JSONObject holder = getJsonObjectFromMap(params);

        //passes the results to a string builder/entity
        StringEntity se = new StringEntity(holder.toString());

        //sets the post request as the resulting string
        httpost.setEntity(se);
        //sets a request header so the page receving the request
        //will know what to do with it
        httpost.setHeader("Accept", "application/json");
        httpost.setHeader("Content-type", "application/json");

        //Handles what is returned from the page
        ResponseHandler responseHandler = new BasicResponseHandler();
        return httpclient.execute(httpost, responseHandler);
    }


    public void SoundDB()
    {
        float f1 = mRecorder.getMaxAmplitude();
        float f2 = 0;
        TextView localTextView;
        StringBuilder localStringBuilder;

        if (f1 > 0.0F)
        {
            f2 = (float)(20.0D * Math.log10(f1));
            Log.d("SoundMeter", "SoundDB: " + f2);
        }
        String ip_addr = SoundMeter.mPref.getString("server_addr", "0.0.0.0:8000");

        if (SoundMeter.mSend_db != null &&  SoundMeter.mSend_db.isChecked()) {
            Log.d("ip addr", "is checked...");

            if (!SoundMeter.mServerIP.getText().toString().equalsIgnoreCase(ip_addr)) {
                Log.d("ip addr", "not equal...");
                SharedPreferences.Editor editor = SoundMeter.mPref.edit();
                editor.putString("server_addr", SoundMeter.mServerIP.getText().toString());
                editor.commit();
                ip_addr = SoundMeter.mPref.getString("server_addr", "0.0.0.0.:8000");
                SoundMeter.mServerIP.setText(ip_addr);
            }
        }
        Log.d("ip addr", ip_addr);
        localTextView = SoundMeter.mSoundDB;
        localTextView.setText(String.format("%d DB", Math.round(f2)));
        if (SoundMeter.mSend_db != null) {
            Log.d("is checked", "" + SoundMeter.mSend_db.isChecked());
        }
        // Create a default HTTP client
        //String ip_addr = SoundMeter.mServerIP.getText().toString();
        if (SoundMeter.mSend_db != null && !ip_addr.equalsIgnoreCase("") && SoundMeter.mSend_db.isChecked()) {
            Log.d(ip_addr, "set as.");
            Integer db_integer = Math.round(f2);
            String db_string = db_integer.toString();
            JSONObject json_object = this.dataToJson(db_string);

            try {

                // Create a default HTTP client
                HttpClient client = new DefaultHttpClient();

                // Create HTTP post object
                HttpPost poster = new HttpPost("http://" + ip_addr + "/adjust/");

                // Get a string from the JSON Object
                String jsonString = json_object.toString();
                Log.e("json string: ", jsonString);

                // Set the HTTP entity
                StringEntity entity = new StringEntity(jsonString);
                poster.setEntity(entity);

                // Set the header
                poster.setHeader("Accept", "application/json");
                poster.setHeader("Content-type", "application/json");

                // Execute the post
                HttpResponse response = null;
                try {
                    response = client.execute((HttpUriRequest) poster);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Get entity from the response
                HttpEntity entityHttp = response.getEntity();

                // Log the response (should be JSON string data)
                if (entity != null) {
                    Log.e("result: ", EntityUtils.toString(entityHttp));
                }

            } catch (Exception e) {
                Log.e("post error: ", "Unable to post to database");
                e.printStackTrace();
            }
        }

//        if (f1 > 0.0F)
//        {
//            f2 = (float)(20.0D * Math.log10(f1));
//            if ((SmartSound.b > 0) && (SmartSound.c != 0.0F))
//                f2 += (f2 - SmartSound.b) * SmartSound.c;
//            if ((SmartSound.g) && (DialogSound.a != null))
//            {
//                DialogSound.a.setText(a((int)f1));
//                localTextView = DialogSound.b;
//                localStringBuilder = new StringBuilder(String.valueOf(this.c.getString(2131427370))).append(" = (").append(Integer.toString(Math.round(f2)));
//                if (SmartSound.a < 0)
//                    break label178;
//            }
//        }
//        label178: for (String str = " +"; ; str = " ")
//        {
//            localTextView.setText(str + Integer.toString(SmartSound.a) + ") dB");
//            float f3 = f2 + SmartSound.a;
//            this.d.a(f3);
//            this.d.postInvalidate();
//            return;
//        }
    }

    class CalAvg {

        final int a = 4;
        float[] b = new float[a];
        int c = 0;

        private float Cal(float paramFloat)
        {
            float f3;
            if (paramFloat == 0.0F)
            {
                f3 = 0.0F;
                return f3;
            }
            c = (1 + c);
            if (c > -1 + b.length)
                c = 0;
            b[c] = paramFloat;
            float[] arrayOfFloat = b;
            int i = arrayOfFloat.length;
            int j = 0;
            float f1 = 0.0F;
            while (true)
            {
                if (j >= i)
                {
                    f3 = f1 / b.length;
                    break;
                }
                float f2 = arrayOfFloat[j];
                if (f2 == 0.0F)
                    f2 = paramFloat;
                f1 += f2;
                j++;
            }

            return f3;
        }
    }

}
