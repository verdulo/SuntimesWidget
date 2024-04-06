/**
    Copyright (C) 2014-2024 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.getfix;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.forrestguice.suntimeswidget.ExportTask;
import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.calculator.core.Location;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class BuildPlacesTask extends AsyncTask<Object, Object, Integer>
{
    public static final long MIN_WAIT_TIME = 2000;

    private GetFixDatabaseAdapter db;
    private WeakReference<Context> contextRef;

    private boolean isPaused = false;
    public void pauseTask()
    {
        isPaused = true;
        //Log.d("DEBUG", "BuildPlacesTask paused");
    }
    public void resumeTask()
    {
        isPaused = false;
        //Log.d("DEBUG", "BuildPlacesTask resumed");
    }
    public boolean isPaused()
    {
        return isPaused;
    }

    public BuildPlacesTask(Context context)
    {
        this.contextRef = new WeakReference<Context>(context);
        db = new GetFixDatabaseAdapter(context.getApplicationContext());
    }

    private int clearPlaces()
    {
        db.open();
        boolean result = db.clearPlaces();
        db.close();

        Log.i("BuildPlacesTask", "clearPlaces: " + result);
        return (result ? 1 : 0);
    }

    /**
     * @param context context
     * @param locations added to the given ArrayList
     */
    private void addPlacesFromRes(Context context, @NonNull ArrayList<Location> locations)
    {
        for (Locale locale : Locale.getAvailableLocales())
        {
            Location location = null;
            if (Build.VERSION.SDK_INT >= 17 && context != null)
            {
                Configuration config = new Configuration(context.getResources().getConfiguration());
                config.setLocale(locale);

                Resources resources = context.createConfigurationContext(config).getResources();
                String label = resources.getString(R.string.default_location_label);
                String lat = resources.getString(R.string.default_location_latitude);
                String lon = resources.getString(R.string.default_location_longitude);
                String alt = resources.getString(R.string.default_location_altitude);
                location = new Location(label, lat, lon, alt);
            } // else    // TODO: legacy support

            if (location != null && !locations.contains(location))
            {
                locations.add(location);
            }
        }
    }

    private void addPlacesFromGroup(Context context, @NonNull String[] groups, @NonNull ArrayList<Location> locations)
    {
        if (groups.length == 0) {
            addPlacesFromGroup(context, (String) null, locations);
        } else {
            for (String group : groups) {
                addPlacesFromGroup(context, group, locations);
            }
        }
    }

    private void addPlacesFromGroup(Context context, @Nullable String fromGroup, @NonNull ArrayList<Location> locations)
    {
        Resources r = context.getResources();
        int groupID = (fromGroup == null) ? 0
                : r.getIdentifier(fromGroup, "array", context.getPackageName());

        if (fromGroup == null || (fromGroup.startsWith("place_group_") && groupID != 0))
        {
            String[] groups = fromGroup != null
                    ? r.getStringArray(groupID)
                    : r.getStringArray(R.array.place_groups);

            for (String groupItem : groups)
            {
                String[] parts = groupItem.split(",");
                if (parts.length > 0) {
                    addPlacesFromGroup(context, parts[0].trim(), locations);    // recursive call
                }
            }

        } else if (groupID != 0) {
            ArrayList<String> items = new ArrayList<>(Arrays.asList(r.getStringArray(groupID)));    // base case
            if (items.size() > 0)
            {
                for (String item : items)
                {
                    Location location = csvItemToLocation(item);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        }
    }

    private void addPlacesFromUri(Context context, @NonNull Uri uri, @NonNull ArrayList<Location> locations)
    {
        try {
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in != null)
            {
                BufferedInputStream input = new BufferedInputStream(in);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String line = reader.readLine();
                while (line != null)
                {
                    Location location = csvItemToLocation(line);
                    if (location != null && !locations.contains(location)) {
                        locations.add(location);
                    }
                    line = reader.readLine();
                }

            } else {
                Log.e("BuildPlacesTask", "Failed to import from " + uri + " (null)");
            }
        } catch (FileNotFoundException e) {
            Log.e("BuildPlacesTask", "Failed to import from " + uri + ": " + e);
        } catch (IOException e) {
            Log.e("BuildPlacesTask", "Failed to import from " + uri + ": " + e);
        }
    }

    @Nullable
    public static Location csvItemToLocation(String csv_item)
    {
        if (csv_item == null) {
            return null;
        }

        String[] parts = splitCSV(csv_item, ','); // csv_item.split(",");
        if (parts.length < 3) {
            Log.e("BuildPlacesTask", "Ignoring malformed line; " + csv_item);
            return null;
        }

        String label = parts[0];
        if (label.startsWith("\"")) {
            label = label.substring(1);
        }
        if (label.endsWith("\"")) {
            label = label.substring(0, label.length()-1);
        }

        String lat, lon;
        String alt = "0";
        try {
            lat = "" + Double.parseDouble(parts[1]);
            lon = "" + Double.parseDouble(parts[2]);
            if (parts.length >= 4) {
                alt = "" + Double.parseDouble(parts[3]);
            }
        } catch (NumberFormatException e) {
            Log.e("BuildPlacesTask", "Ignoring line " + csv_item + " .. " + e);
            return null;
        }

        return new Location(label, lat, lon, alt);
    }

    public static String[] splitCSV(String value, Character delimiter)
    {
        ArrayList<String> parts = new ArrayList<>();
        boolean quoted = false;
        int j = 0;
        for (int i=0; i<value.length(); i++)
        {
            if (value.charAt(i) == '\"') {
                quoted = !quoted;

            } else if (value.charAt(i) == delimiter) {
                if (!quoted) {
                    parts.add(value.substring(j, i));
                    j = i + 1;
                }
            }
        }
        parts.add(value.substring(j));
        return parts.toArray(new String[0]);
    }

    /**
     * Pass a URI to build from file, groups[] to build from resources, or null for both to build
     * from internal locales.
     * @param uri optional source uri; null to skip
     * @param groups optional group list; null to skip, or pass an empty list to add all
     * @return the number of items added to the database
     */
    private int buildPlaces(@Nullable Uri uri, @Nullable String[] groups)
    {
        int result = 0;
        ArrayList<Location> locations = new ArrayList<>();
        try {
            Context context = contextRef.get();
            db.open();

            if (uri != null) {
                addPlacesFromUri(context, uri, locations);
            } else if (groups != null) {
                addPlacesFromGroup(context, groups, locations);
            } else {
                addPlacesFromRes(context, locations);
            }

            Collections.sort(locations, new Comparator<Location>()
            {
                @Override
                public int compare(Location o1, Location o2)
                {
                    return o2.getLabel().compareTo(o1.getLabel());  // descending
                }
            });

            Cursor cursor = db.getAllPlaces(0, false);
            for (int i=0; i<locations.size(); i++)
            {
                Location location = locations.get(i);
                int p = GetFixDatabaseAdapter.findPlaceByName(location.getLabel(), cursor);
                if (p < 0)    // if not found
                {                 // then add new place
                    db.addPlace(location, PlaceItem.TAG_DEFAULT);
                    result++;
                }
            }

            Log.i("BuildPlacesTask", "buildPlaces: " + result);
            db.close();

        } catch (SQLException e) {
            Log.e("BuildPlacesTask", "Failed to access database: " + e);
            result = -1;
        }
        return result;
    }

    @Override
    protected Integer doInBackground(Object... params)
    {
        long startTime = System.currentTimeMillis();

        boolean param_clearPlaces = false;
        if (params.length > 0) {
            param_clearPlaces = (Boolean)params[0];
        }

        Uri param_source = null;
        if (params.length > 1) {
            param_source = (Uri)params[1];
        }

        String[] param_groups = null;
        if (params.length > 2) {
            param_groups = (String[])params[2];
        }

        int result = param_clearPlaces ? clearPlaces()
                                       : buildPlaces(param_source, param_groups);

        long endTime = System.currentTimeMillis();
        while ((endTime - startTime) < MIN_WAIT_TIME || isPaused)
        {
            endTime = System.currentTimeMillis();
        }
        return result;
    }

    @Override
    protected void onPreExecute()
    {
        signalStarted();
    }

    @Override
    protected void onPostExecute(Integer result)
    {
        signalFinished(result);
    }


    /**
     * Event Listener
     */
    private TaskListener taskListener = null;
    public void setTaskListener( TaskListener listener )
    {
        taskListener = listener;
    }
    public void clearTaskListener()
    {
        taskListener = null;
    }
    public static abstract class TaskListener
    {
        public void onStarted() {}
        public void onFinished( Integer result ) {}
    }

    private void signalStarted()
    {
        if (taskListener != null)
            taskListener.onStarted();
    }
    private void signalFinished( Integer result )
    {
        if (taskListener != null)
            taskListener.onFinished(result);
    }

    public static Intent buildPlacesOpenFileIntent() {
        return ExportTask.getOpenFileIntent("text/*");
    }
}
