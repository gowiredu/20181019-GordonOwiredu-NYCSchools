package com.gordonkwame.downloadtest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    protected final String urlSchoolList = "https://data.cityofnewyork.us/api/views/s3k6-pzi2/rows.xml?accessType=DOWNLOAD"; // URL FOR LIST OF SCHOOLS
    protected final String urlSchoolSAT = "https://data.cityofnewyork.us/api/views/f9bf-2cp4/rows.xml?accessType=DOWNLOAD"; // URL FOR SAT SCORES
    protected ArrayList<String> parsedSchoolArrayList; // ARRAYLIST FOR NAMES OF SCHOOLS
    protected ArrayList<String> dialogDataArrayList; // ARRAYLIST FOR SAT SCORE DIALOG BOX INFORMATION
    protected ArrayAdapter<String> schoolListViewAdapter; // SCHOOL LIST LISTVIEW ADAPTER
    protected String touchedSchool; // SCHOOL THE USER SELECTED
    protected ListView schoolsListView; // LISTVIEW TO BE POPULATED BY SCHOOL LIST ADAPTER (schoolListViewAdapter)
    protected ProgressDialog downloadProgressDialog; // LOADING DIALOG SPINNER TO BE SHOWN AT STARTUP (WHEN LIST OF SCHOOLS ARE DOWNLOADING)



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialogDataArrayList = new ArrayList<>();
        parsedSchoolArrayList = new ArrayList<>();
        downloadProgressDialog = new ProgressDialog(MainActivity.this);

        schoolsListView = findViewById(R.id.schoolsListView);


        schoolsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                touchedSchool = schoolsListView.getItemAtPosition(position).toString(); // GET NAME OF SELECTED SCHOOL AND STORE IN VARIABLE "touchedSchool"
                Log.i("SELECTED_SCHOOL", touchedSchool); // DISPLAY SELECTED SCHOOL IN LOG
                new AsyncTaskGetSAT().execute(); // ASYNCTASK FOR GETTING SAT SCORES OF SELECTED SCHOOL (TO AVOID RUNNING ON MAIN THREAD AND AVOID "Application not Responding)
            }
        });

        schoolListViewAdapter = new ArrayAdapter<>(this,
                R.layout.list_schools, parsedSchoolArrayList);

        // CHECK IF AN INTERNET CONNECTION IS AVAILABLE
        if (isNetworkAvailable())
        {
            new AsyncTaskGetSchoolList().execute(); // ASYNCTASK FOR GETTING LIST OF SCHOOLS  (TO AVOID RUNNING ON MAIN THREAD AND AVOID "Application not Responding)
            Log.i("NETWORK", "Internet connection available");
        }

        else
        {
            noInternetConnectionDialog();
            dismissInitLoadingSpinner();
            Log.i("NETWORK", "No internet connection available");
        }
    }



    protected void noInternetConnectionDialog()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);


        alertDialogBuilder.setMessage("No internet connection available. Try again?");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Reload",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                new AsyncTaskGetSchoolList().execute();
            }
        });

        alertDialogBuilder.setNegativeButton("Exit",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
                System.exit(0);
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }



    // LOAD DIALOG SPINNER AT STARTUP
    protected void initLoadingSpinner()
    {
        downloadProgressDialog.setTitle("Downloading");
        downloadProgressDialog.setMessage("Please wait...");
        downloadProgressDialog.setCancelable(false);
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        downloadProgressDialog.show();
    }





    // DISMISS DIALOG SPINNER
    protected void dismissInitLoadingSpinner()
    {
        if (downloadProgressDialog.isShowing())
        {
            downloadProgressDialog.dismiss();
        }
    }




    // OPEN CONNECTION TO APPROPRIATE URL
    protected InputStream getInputStream(URL url) {
        try {
            return url.openConnection().getInputStream();
        } catch (IOException e) {
            return null;
        }
    }



    // ASYNCTASK FOR GETTING SCHOOL LIST
    private class AsyncTaskGetSchoolList extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // LOAD DIALOG SPINNER AT STARTUP (MAIN THREAD WILL HANDLE SPINNER)
            MainActivity.this.runOnUiThread(new Runnable(){

                @Override
                public void run(){
                    initLoadingSpinner();
                }
            });
        }


        @Override
        protected String doInBackground(String... strings) {

            URL url;

            try {
                url = new URL(urlSchoolList);

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(getInputStream(url), "UTF_8"); // SET INPUT TO SCHOOL LIST URL
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    String tagName = xpp.getName();

                    // IF AT THE BEGINNING OF THE XML FILE
                    if (eventType == XmlPullParser.START_DOCUMENT)
                    {
                        System.out.println("Start SCHOOL LIST document");
                    }

                    else if (eventType == XmlPullParser.START_TAG) // IF THE TAG IS A START TAG
                    {
                        // IF THE TAGNAME IS "school_name"
                        if (tagName.equalsIgnoreCase("school_name")) {
                            parsedSchoolArrayList.add(xpp.nextText()); // ADD SCHOOL NAME IN THAT TAG TO THE SCHOOL LIST ARRAYLIST (parsedSchoolArrayList)
                        }
                    }
                    eventType = xpp.next();

                }
                System.out.println("End SCHOOL LIST document");
                System.out.println("SCHOOLS: " + parsedSchoolArrayList);

            } catch (Exception e) {
                e.printStackTrace();

                // ASK USER IF THEY WOULD LIKE TO TRY RELOADING LIST OF SCHOOLS OR EXIT APPLICATION
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run(){
                        refreshOrExitDialog();
                    }
                });
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            schoolsListView.setAdapter(schoolListViewAdapter);

            // DISMISS SPINNER (HAS TO BE DISMISSED ON UI THREAD BECAUSE IT WAS STARTED THERE)
            MainActivity.this.runOnUiThread(new Runnable(){

                @Override
                public void run(){
                    dismissInitLoadingSpinner();
                }
            });

        }
    }


    // ASYNCTASK FOR GETTING SAT SCORES WHEN A SCHOOL IS SELECTED
    private class AsyncTaskGetSAT extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {

            URL url;

            try {
                url = new URL(urlSchoolSAT); //  URL WITH SAT SCORES

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(getInputStream(url), "UTF_8");
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    String tagName = xpp.getName();

                    // IF TAG IS A START TAG AND THE TAG EQUALS "school_name"
                    if (xpp.getEventType() == XmlPullParser.START_TAG && tagName.equalsIgnoreCase("school_name"))
                    {
                        xpp.next(); // MOVE TO NEXT ELEMENT

                        // IF NEXT ELEMENT'S TEXT EQUALS THE SELECTED SCHOOL
                        if (xpp.getText().equalsIgnoreCase(touchedSchool))
                        {
                            System.out.println("FOUND SCHOOL:  " + xpp.getText()); // THE RIGHT CHILD TAG HAS BEEN FOUND

                            // WHILE THE CURRENT TAG IS NOT AN END TAG AND THAT END TAG IS NOT "row"
                            while (xpp.getEventType() != XmlPullParser.END_TAG && !"row".equalsIgnoreCase(xpp.getName()))
                            {
                                // IT THE CURRENT ELEMENT IS TEXT
                                if ((xpp.getEventType() == XmlPullParser.TEXT))
                                {
                                    dialogDataArrayList.add(xpp.getText()); // ADD IT TO THE ARRAYLIST TO BE USED FOR THE SAT SCORE DIALOG BOX LATER
                                    xpp.next(); // MOVE TO THE NEXT ELEMENT
                                }
                                xpp.next(); // MOVE TO THE NEXT ELEMENT
                            }
                        }
                    }
                    eventType = xpp.next();
                }

                System.out.println("End SAT document");
                System.out.println("SAT_SCORES: " + dialogDataArrayList.toString());

                // CREATE SAT INFO DIALOG BOX
                MainActivity.this.runOnUiThread(new Runnable(){

                    @Override
                    public void run(){
                        SATDialog();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                // IF INTERNET CONNECTION IS UNAVAILABLE WHEN A SCHOOL IS SELECTED
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isNetworkAvailable())
                        {
                            Toast.makeText(getApplicationContext(), "Currently unable to display scores", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            unableSATScoresSchoolDialog(); // DIALOG BOX FOR ERROR MESSAGE AND ASKING USER TO PLEASE CHECK INTERNET CONNECTION
                        }
                    }
                });
            }
            return null;
        }
    }


    // DIALOG BOX FOR DISPLAYING SAT SCORES WHEN A SCHOOL IS SELECTED
    protected void SATDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // IF NO SAT DATA IS AVAILABLE FOR THE SELECTED SCHOOL
        if (dialogDataArrayList.isEmpty()) {
            alertDialogBuilder.setMessage("No SAT scores available for this school");
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setPositiveButton("Done",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            dialogDataArrayList.clear();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        // ELSE IF SAT DATA IS AVAILABLE, DISPLAY IN DIALOG BOX
        else {
            alertDialogBuilder.setTitle(dialogDataArrayList.get(0));
            alertDialogBuilder.setMessage(
                    "Number of Test Takers: " + dialogDataArrayList.get(1) + "\n" +
                            "SAT Reading: " + dialogDataArrayList.get(2) + "\n" +
                            "SAT Math: " + dialogDataArrayList.get(3) + "\n" +
                            "SAT Writing: " + dialogDataArrayList.get(4));

            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setPositiveButton("Done",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            dialogDataArrayList.clear();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }




    // IF NO INTERNET CONNECTION IS AVAILABLE WHEN TRYING TO DISPLAY SCORES FOR A SELECTED SCHOOL, DISPLAY AN ERROR MESSAGE IN A DIALOG BOX
    protected void unableSATScoresSchoolDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setMessage("Unable to display SAT scores for this school. Please check your internet connection.");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Done",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface theDialog, int arg1) {
                theDialog.dismiss();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }




    // IF SCHOOL DATA CANNOT BE LOADED TO THE ARRAYLIST, LET USER DECIDE IF THEY WANT TO TRY AGAIN
    protected void refreshOrExitDialog()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        if (dialogDataArrayList.isEmpty()) {
            alertDialogBuilder.setMessage("Unable to load NYC school data. Try again?");
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setPositiveButton("Reload",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            new AsyncTaskGetSchoolList().execute();
                        }
                    });

            alertDialogBuilder.setNegativeButton("Exit",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            finish();
                            System.exit(0);
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }


    // CHECK IF ANY FORM OF INTERNET CONNECTION IS AVAILABLE
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;

        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}