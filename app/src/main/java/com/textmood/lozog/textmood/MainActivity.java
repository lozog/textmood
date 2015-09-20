package com.textmood.lozog.textmood;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.database.Cursor;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.lang.String;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

import io.indico.Indico;
import io.indico.network.IndicoCallback;
import io.indico.results.BatchIndicoResult;
import io.indico.results.IndicoResult;
import io.indico.utils.IndicoException;


public class MainActivity extends ActionBarActivity {

    // inbox SMS data
    protected List<Double> sentimentInbox = new ArrayList<>();
    protected List<Double> sentimentdeltaInbox = new ArrayList<>();
    protected List<String> bodyInbox = new ArrayList<>();
    protected List<String> numberInbox = new ArrayList<>();
    protected HashMap<String, String> contactInbox = new HashMap<>();
    protected List<String> dateInbox = new ArrayList<>();

    // sent SMS data
    protected List<Double> sentimentSent = new ArrayList<>();
    protected List<Double> sentimentdeltaSent = new ArrayList<>();
    protected List<String> bodySent = new ArrayList<>();
    protected List<String> numberSent = new ArrayList<>();
    protected List<String> dateSent = new ArrayList<>();

    Button button;
    LineChart chart;
    Spinner spinner;
    TextView graphTitle;
    TextView warnText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.goButton);
        chart = (LineChart) findViewById(R.id.chart);
        chart.setNoDataTextDescription("Choose a target and hit Go!");
        spinner = (Spinner) findViewById(R.id.contactList);
        graphTitle = (TextView) findViewById(R.id.graphTitle);
        warnText = (TextView) findViewById(R.id.warnText);

        Indico.init(this, getString(R.string.indico_api_key), null);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

//                final String contact = String.valueOf(spinner.getSelectedItem());
//                String contactName = contact;
//                if (!contact.equals("All Contacts")) contactName = getDisplayNameByNumber(getBaseContext(), contact);
//                graphTitle.setText("Positivity Graph for " + contactName);

                final String contact = String.valueOf(spinner.getSelectedItem());
                final String contactName = contactInbox.get(contact);
                graphTitle.setText("Positivity Graph for " + contactName);
                warnText.setText("");

                // Perform Sentimental analysis on the inbox
                try {
                    Indico.sentiment.predict(bodyInbox, new IndicoCallback<BatchIndicoResult>() {
                        @Override
                        public void handle(BatchIndicoResult result) throws IndicoException {
                            Log.i("Indico Sentiment", "sentiment of: " + result.getSentiment());

                            // Move sentiment values to sentiment container
                            sentimentInbox.addAll(result.getSentiment());

                            // Perform Sentimental analysis on the sent messages
                            try {
                                Indico.sentiment.predict(bodySent, new IndicoCallback<BatchIndicoResult>() {
                                    @Override
                                    public void handle(BatchIndicoResult result) throws IndicoException {
                                        Log.i("Indico Sentiment", "sentiment of: " + result.getSentiment());

                                        // Move sentiment values to sentiment container
                                        sentimentSent.addAll(result.getSentiment());

                                        // call the graph drawing function
                                        drawGraph(contact);
                                    }
                                });
                            } catch (IOException | IndicoException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (IOException | IndicoException e) {
                    e.printStackTrace();
                }


//                tv.setText(body[getcurrSMS()] + " is " + "100" + "% positive.");
//                tv.setText(output);

//                incrementcurrSMS();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Read SMS Inbox
        Uri uriInbox = Uri.parse("content://sms/inbox");
        Cursor cInbox = getContentResolver().query(uriInbox, null, null, null, null);

        if(cInbox.moveToLast()){
            for(int i=0;i<cInbox.getCount();i++){

                bodyInbox.add(cInbox.getString(cInbox.getColumnIndexOrThrow("body")));
                numberInbox.add(cInbox.getString(cInbox.getColumnIndexOrThrow("address")));
                dateInbox.add(cInbox.getString(cInbox.getColumnIndexOrThrow("date")));

                cInbox.moveToPrevious();
            }
        }
        cInbox.close();

        // Read SMS Sent
        Uri uriSent = Uri.parse("content://sms/sent");
        Cursor cSent = getContentResolver().query(uriSent, null, null, null, null);

        if(cSent.moveToLast()){
            for(int i=0;i<cSent.getCount();i++){

                bodySent.add(cSent.getString(cSent.getColumnIndexOrThrow("body")));
                numberSent.add(cSent.getString(cSent.getColumnIndexOrThrow("address")));
                dateSent.add(cSent.getString(cSent.getColumnIndexOrThrow("date")));

                cSent.moveToPrevious();
            }
        }
        cSent.close();

        addItemsOnSpinner();
    }

    public void drawGraph(String contact) {
        YAxis leftAxis = chart.getAxisLeft();

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(8f);
        xAxis.setTextColor(Color.RED);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        ArrayList<String> xVals = new ArrayList<String>();

        // set inbox data
        int inboxSize = bodyInbox.size();
        sentimentdeltaInbox.add(50d);
        ArrayList<Entry> valsCompInbox = new ArrayList<Entry>();

        int count = 0;
        for(int i = 0; i < inboxSize; i++) {
            //Log.i("Contact Info", "contact: " + contact + ", numberInbox[i]: "+numberInbox.get(i));

                if(i>0) {
                    sentimentdeltaInbox.add(sentimentdeltaInbox.get(i - 1) + (sentimentInbox.get(i)-0.5));
                    //tv.setText(sentiment.get(i).toString());
                }
                float positivity = sentimentdeltaInbox.get(i).floatValue();
                Date timestamp = new Date(Long.parseLong(dateInbox.get(i)));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String formattedDate = sdf.format(timestamp);
            if (contact.equals("All Contacts") || numberInbox.get(i).equals(contact)) {
                count++;
                Entry c1e1 = new Entry(positivity, i);
                valsCompInbox.add(c1e1);

                xVals.add(formattedDate);
            }
        }
        if (count < 50) warnText.setText("Warning: Insufficient Data");
        Log.i("Text Count", "Text count: "+count);

        LineDataSet setCompInbox = new LineDataSet(valsCompInbox, "Inbox messages");
        setCompInbox.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSets.add(setCompInbox);
        setCompInbox.setDrawCircles(false);
        setCompInbox.setLineWidth(1.4f);

        // set sent data
        sentimentdeltaSent.add(50d);
        int sentSize = bodySent.size();
        ArrayList<Entry> valsCompSent = new ArrayList<Entry>();
        for(int i = 0; i < sentSize; i++) {

                if (i > 0) {
                    sentimentdeltaSent.add(sentimentdeltaSent.get(i - 1) + (sentimentSent.get(i) - 0.5));
                    //tv.setText(sentiment.get(i).toString());
                }
                float positivity = sentimentdeltaSent.get(i).floatValue();
            if (contact.equals("All Contacts") || numberInbox.get(i).equals(contact)) {
                Entry c1e1 = new Entry(positivity, i);
                valsCompSent.add(c1e1);
            }
        }

        LineDataSet setCompSent = new LineDataSet(valsCompSent, "Sent messages");
        setCompSent.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSets.add(setCompSent);
        setCompSent.setColor(Color.RED);
        setCompSent.setDrawCircles(false);
        setCompSent.setLineWidth(1.4f);

        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);

        // modify the viewport
        chart.setMaxVisibleValueCount(5);
        //chart.setVisibleXRangeMaximum(100);

        // Center about Y data
        chart.setVisibleYRangeMaximum(chart.getYMax() - chart.getYMin() + 10f, YAxis.AxisDependency.LEFT);
        chart.moveViewToY(chart.getYMin() + ((chart.getYMax() - chart.getYMin()) / 2), YAxis.AxisDependency.LEFT);

        chart.setDescription(""); // remove the description
//        chart.fitScreen();

        chart.invalidate(); // refresh
    }

    public void addItemsOnSpinner() {
        spinner = (Spinner) findViewById(R.id.contactList);
        List<String> contacts = new ArrayList<String>();
        contacts.add("All Contacts");
        contactInbox.put("0", "All Contacts");
        HashSet<String> hs = new HashSet<>();
        hs.addAll(numberInbox);
        contacts.addAll(hs);

        int contactCount = contacts.size();
        for(int i=0;i<contactCount;i++){
            contactInbox.put(contacts.get(i), getDisplayNameByNumber(this, contacts.get(i)));
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, contacts);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
    }

    public String getDisplayNameByNumber(Context context, String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor contactLookup = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        int indexName = contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);

        try {
            if (contactLookup != null && contactLookup.moveToNext()) {
                number = contactLookup.getString(indexName);
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return number;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
