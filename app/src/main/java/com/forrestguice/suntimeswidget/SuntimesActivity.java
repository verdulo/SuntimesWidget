/**
    Copyright (C) 2014 Forrest Guice
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

package com.forrestguice.suntimeswidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.forrestguice.suntimeswidget.calculator.SuntimesData;
import com.forrestguice.suntimeswidget.calculator.SuntimesDataset;
import com.forrestguice.suntimeswidget.notes.NoteChangedListener;
import com.forrestguice.suntimeswidget.notes.NoteData;
import com.forrestguice.suntimeswidget.notes.SuntimesNotes;
import com.forrestguice.suntimeswidget.notes.SuntimesNotes1;
import com.forrestguice.suntimeswidget.notes.SuntimesNotes2;
import com.forrestguice.suntimeswidget.notes.SuntimesNotes3;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.SolarEvents;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.lang.reflect.Method;
import java.text.DateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class SuntimesActivity extends AppCompatActivity
{
    protected static SuntimesUtils utils = new SuntimesUtils();

    private ActionBar actionBar;
    private WidgetSettings.Location location;
    private SuntimesNotes notes;
    private SuntimesDataset dataset;

    // clock views
    private TextView txt_time;
    private TextView txt_time_suffix;
    private TextView txt_timezone;

    // note views
    private ViewFlipper note_flipper;
    private Animation anim_note_inPrev;
    private Animation anim_note_inNext;
    private Animation anim_note_outNext;
    private Animation anim_note_outPrev;

    private ImageView ic_time1_note;
    private TextView txt_time1_note1;
    private TextView txt_time1_note2;
    private TextView txt_time1_note3;

    private ImageView ic_time2_note;
    private TextView txt_time2_note1;
    private TextView txt_time2_note2;
    private TextView txt_time2_note3;

    // time card views
    private ViewFlipper card_flipper;
    private Animation anim_card_inPrev;
    private Animation anim_card_inNext;
    private Animation anim_card_outNext;
    private Animation anim_card_outPrev;

    private ImageButton btn_flipperNext_today;
    private ImageButton btn_flipperPrev_today;
    private ImageButton btn_flipperNext_tomorrow;
    private ImageButton btn_flipperPrev_tomorrow;

    private TextView txt_date;
    private TextView txt_sunrise_actual;
    private TextView txt_sunrise_civil;
    private TextView txt_sunrise_nautical;
    private TextView txt_sunrise_astro;
    private TextView txt_sunset_actual;
    private TextView txt_sunset_civil;
    private TextView txt_sunset_nautical;
    private TextView txt_sunset_astro;
    private TextView txt_solarnoon;
    private TextView txt_daylength;
    private TextView txt_lightlength;

    private TextView txt_date2;
    private TextView txt_sunrise2_actual;
    private TextView txt_sunrise2_civil;
    private TextView txt_sunrise2_nautical;
    private TextView txt_sunrise2_astro;
    private TextView txt_sunset2_actual;
    private TextView txt_sunset2_civil;
    private TextView txt_sunset2_nautical;
    private TextView txt_sunset2_astro;
    private TextView txt_solarnoon2;
    private TextView txt_daylength2;
    private TextView txt_lightlength2;

    private HashMap<SolarEventField, TextView> timeFields;

    public SuntimesActivity()
    {
        super();
    }



    /**
     * OnCreate: the Activity initially created
     * @param icicle
     */
    @Override
    public void onCreate(Bundle icicle)
    {
        Context context = SuntimesActivity.this;
        calculateData(context);
        setTheme(AppSettings.loadTheme(this, dataset));

        super.onCreate(icicle);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.layout_main);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        //WidgetThemes.initThemes(context);

        AppSettings.initDisplayStrings(context);
        WidgetSettings.initDisplayStrings(context);
        initViews(context);


        notes.resetNoteIndex();
    }

    /**
     * OnStart: the Activity becomes visible
     */
    @Override
    public void onStart()
    {
        super.onStart();
        Context context = SuntimesActivity.this;
        updateViews(context);
    }

    /**
     * OnResume: the user is now interacting w/ the Activity (running state)
     */
    @Override
    public void onResume()
    {
        super.onResume();
    }

    /**
     * OnPause: the user about to interact w/ another Activity
     */
    @Override
    public void onPause()
    {
        super.onPause();
    }

    /**
     * OnStop: the Activity no longer visible
     */
    @Override
    public void onStop()
    {
        stopTimeTask();
        super.onStop();
    }

    /**
     * OnDestroy: the activity destroyed
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    /**
     * initialize ui/views
     * @param context
     */
    protected void initViews(Context context)
    {
        Toolbar menuBar = (Toolbar) findViewById(R.id.app_menubar);
        setSupportActionBar(menuBar);
        actionBar = getSupportActionBar();

        initAnimations(context);
        initClockViews(context);
        initNoteViews(context);
        initCardViews(context);
    }

    /**
     * initialize the note flipper and associated views
     * @param context
     */
    private void initNoteViews(Context context)
    {
        note_flipper = (ViewFlipper) findViewById(R.id.info_note_flipper);
        if (note_flipper != null)
        {
            note_flipper.setOnTouchListener(noteTouchListener);

        } else {
            Log.w("initNoteViews", "Failed to set touchListener; note_flipper is null!");
        }

        LinearLayout note1 = (LinearLayout) findViewById(R.id.info_time_note1);
        if (note1 != null)
        {
            txt_time1_note1 = (TextView) note1.findViewById(R.id.text_timenote1);
            txt_time1_note2 = (TextView) note1.findViewById(R.id.text_timenote2);
            txt_time1_note3 = (TextView) note1.findViewById(R.id.text_timenote3);
            ic_time1_note = (ImageView) note1.findViewById(R.id.icon_timenote);
            ic_time1_note.setVisibility(View.INVISIBLE);

        } else {
            Log.w("initNoteViews", "Failed to init note layout1; was null!");
        }

        LinearLayout note2 = (LinearLayout) findViewById(R.id.info_time_note2);
        if (note2 != null)
        {
            txt_time2_note1 = (TextView) note2.findViewById(R.id.text_timenote1);
            txt_time2_note2 = (TextView) note2.findViewById(R.id.text_timenote2);
            txt_time2_note3 = (TextView) note2.findViewById(R.id.text_timenote3);
            ic_time2_note = (ImageView) note2.findViewById(R.id.icon_timenote);
            ic_time2_note.setVisibility(View.INVISIBLE);

        } else {
            Log.w("initNoteViews", "Failed to init note layout2; was null!");
        }
    }

    /**
     * initialize the card flipper and associated views
     * @param context
     */
    private void initCardViews(Context context)
    {
        timeFields = new HashMap<SolarEventField, TextView>();
        card_flipper = (ViewFlipper) findViewById(R.id.info_time_flipper);
        if (card_flipper != null)
        {
            card_flipper.setOnTouchListener(timeCardTouchListener);
        } else {
            Log.w("initCardViews", "Failed to set touchListener; card_flipper was null!");
        }

        // Today's times
        LinearLayout viewToday = (LinearLayout)findViewById(R.id.info_time_all_today);
        if (viewToday != null)
        {
            txt_date = (TextView) viewToday.findViewById(R.id.text_date);
            txt_date.setOnClickListener(onNextCardClick);

            txt_sunrise_actual = (TextView) viewToday.findViewById(R.id.text_time_sunrise_actual);
            txt_sunset_actual = (TextView) viewToday.findViewById(R.id.text_time_sunset_actual);
            timeFields.put(new SolarEventField(SolarEvents.SUNRISE, false), txt_sunrise_actual);
            timeFields.put(new SolarEventField(SolarEvents.SUNSET, false), txt_sunset_actual);

            txt_sunrise_civil = (TextView) viewToday.findViewById(R.id.text_time_sunrise_civil);
            txt_sunset_civil = (TextView) viewToday.findViewById(R.id.text_time_sunset_civil);
            timeFields.put(new SolarEventField(SolarEvents.MORNING_CIVIL, false), txt_sunrise_civil);
            timeFields.put(new SolarEventField(SolarEvents.EVENING_CIVIL, false), txt_sunset_civil);

            txt_sunrise_nautical = (TextView) viewToday.findViewById(R.id.text_time_sunrise_nautical);
            txt_sunset_nautical = (TextView) viewToday.findViewById(R.id.text_time_sunset_nautical);
            timeFields.put(new SolarEventField(SolarEvents.MORNING_NAUTICAL, false), txt_sunrise_nautical);
            timeFields.put(new SolarEventField(SolarEvents.EVENING_NAUTICAL, false), txt_sunset_nautical);

            txt_sunrise_astro = (TextView) viewToday.findViewById(R.id.text_time_sunrise_astro);
            txt_sunset_astro = (TextView) viewToday.findViewById(R.id.text_time_sunset_astro);
            timeFields.put(new SolarEventField(SolarEvents.MORNING_ASTRONOMICAL, false), txt_sunrise_astro);
            timeFields.put(new SolarEventField(SolarEvents.EVENING_ASTRONOMICAL, false), txt_sunset_astro);

            txt_solarnoon = (TextView) viewToday.findViewById(R.id.text_time_noon);
            timeFields.put(new SolarEventField(SolarEvents.NOON, false), txt_solarnoon);

            txt_daylength = (TextView) viewToday.findViewById(R.id.text_daylength);
            txt_lightlength = (TextView) viewToday.findViewById(R.id.text_lightlength);

            btn_flipperNext_today = (ImageButton)viewToday.findViewById(R.id.info_time_nextbtn);
            btn_flipperNext_today.setOnClickListener(onNextCardClick);
            btn_flipperNext_today.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent)
                {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        btn_flipperNext_today.setColorFilter(ContextCompat.getColor(SuntimesActivity.this, R.color.btn_tint_pressed));
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        btn_flipperNext_today.setColorFilter(null);
                    }
                    return false;
                }
            });

            btn_flipperPrev_today = (ImageButton)viewToday.findViewById(R.id.info_time_prevbtn);
            btn_flipperPrev_today.setOnClickListener(onPrevCardClick);
            btn_flipperPrev_today.setVisibility(View.GONE);

        } else {
            Log.w("initCardViews", "Failed to init card layout1; was null!");
        }

        // Tomorrow's times
        LinearLayout viewTomorrow = (LinearLayout)findViewById(R.id.info_time_all_tomorrow);
        if (viewTomorrow != null)
        {
            txt_date2 = (TextView) viewTomorrow.findViewById(R.id.text_date);
            txt_date2.setOnClickListener(onPrevCardClick);

            txt_sunrise2_actual = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_actual);
            txt_sunset2_actual = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_actual);
            timeFields.put(new SolarEventField(SolarEvents.SUNRISE, true), txt_sunrise2_actual);
            timeFields.put(new SolarEventField(SolarEvents.SUNSET, true), txt_sunset2_actual);

            txt_sunrise2_civil = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_civil);
            txt_sunset2_civil = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_civil);
            timeFields.put(new SolarEventField(SolarEvents.MORNING_CIVIL, true), txt_sunrise2_civil);
            timeFields.put(new SolarEventField(SolarEvents.EVENING_CIVIL, true), txt_sunset2_civil);

            txt_sunrise2_nautical = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_nautical);
            txt_sunset2_nautical = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_nautical);
            timeFields.put(new SolarEventField(SolarEvents.MORNING_NAUTICAL, true), txt_sunrise2_nautical);
            timeFields.put(new SolarEventField(SolarEvents.EVENING_NAUTICAL, true), txt_sunset2_nautical);

            txt_sunrise2_astro = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_astro);
            txt_sunset2_astro = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_astro);
            timeFields.put(new SolarEventField(SolarEvents.MORNING_ASTRONOMICAL, true), txt_sunrise2_astro);
            timeFields.put(new SolarEventField(SolarEvents.EVENING_ASTRONOMICAL, true), txt_sunset2_astro);

            txt_solarnoon2 = (TextView) viewTomorrow.findViewById(R.id.text_time_noon);
            timeFields.put(new SolarEventField(SolarEvents.NOON, true), txt_solarnoon2);

            txt_daylength2 = (TextView) viewTomorrow.findViewById(R.id.text_daylength);
            txt_lightlength2 = (TextView) viewTomorrow.findViewById(R.id.text_lightlength);

            btn_flipperNext_tomorrow = (ImageButton)viewTomorrow.findViewById(R.id.info_time_nextbtn);
            btn_flipperNext_tomorrow.setOnClickListener(onNextCardClick);
            btn_flipperNext_tomorrow.setVisibility(View.GONE);

            btn_flipperPrev_tomorrow = (ImageButton)viewTomorrow.findViewById(R.id.info_time_prevbtn);
            btn_flipperPrev_tomorrow.setOnClickListener(onPrevCardClick);
            btn_flipperPrev_tomorrow.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent)
                {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        btn_flipperPrev_tomorrow.setColorFilter(ContextCompat.getColor(SuntimesActivity.this, R.color.btn_tint_pressed));
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        btn_flipperPrev_tomorrow.setColorFilter(null);
                    }
                    return false;
                }
            });

        } else {
            Log.w("initCardViews", "Failed to init card layout2; was null!");
        }
    }

    /**
     * initialize the clock ui
     * @param context
     */
    private void initClockViews(Context context)
    {
        txt_time = (TextView) findViewById(R.id.text_time);
        if (txt_time != null)
        {
            txt_time.setOnClickListener(onClockClick);
        }

        txt_time_suffix = (TextView) findViewById(R.id.text_time_suffix);
        txt_timezone = (TextView) findViewById(R.id.text_timezone);
    }

    /**
     * initialize view animations
     * @param context
     */
    private void initAnimations(Context context)
    {
        anim_note_inPrev = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        anim_note_inNext = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        anim_note_outPrev = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        anim_note_outNext = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        //anim_note_outPrev = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        //anim_note_outNext = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);

        anim_card_inPrev = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        anim_card_inNext = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        anim_card_outPrev = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        anim_card_outNext = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
    }

    /**
     * Initialize note object and onChanged listener.
     */
    private void initNotes()
    {
        notes = new SuntimesNotes3();
        notes.init(this, dataset);
        notes.setOnChangedListener(new NoteChangedListener()
        {
            @Override
            public void onNoteChanged(NoteData note, int transition)
            {
                updateNoteUI(note, transition);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);
        return true;
    }

    /**
     * from http://stackoverflow.com/questions/18374183/how-to-show-icons-in-overflow-menu-in-actionbar
     */
    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu)
    {
        if (menu != null)
        {
            if (menu.getClass().getSimpleName().equals("MenuBuilder"))
            {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);

                } catch (Exception e) {
                    Log.e("SuntimesActivity", "failed to set show overflow icons", e);
                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                showSettings();
                return true;

            case R.id.action_about:
                showAbout();
                return true;

            case R.id.action_help:
                showHelp();
                return true;

            case R.id.action_location_add:
                configLocation();
                return true;

            case R.id.action_location_show:
                showMap();
                return true;

            case R.id.action_timezone:
                configTimeZone();
                return true;

            case R.id.action_alarm:
                scheduleAlarm();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void configLocation()
    {
        LocationConfigView.LocationConfigDialog locationDialog = new LocationConfigView.LocationConfigDialog(this);
        locationDialog.getLocationConfigView().setHideTitle(true);

        locationDialog.setOnAcceptedListener(new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                updateViews(SuntimesActivity.this);
            }
        });
        AlertDialog locationAlert = locationDialog.toAlertDialog();
        locationAlert.show();
    }

    protected void configTimeZone()
    {
        TimeZoneDialog timezoneDialog = new TimeZoneDialog(this);
        timezoneDialog.setOnAcceptedListener(new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                updateViews(SuntimesActivity.this);
            }
        });
        AlertDialog timezoneAlert = timezoneDialog.toAlertDialog();
        timezoneAlert.show();
    }

    protected void showMap()
    {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        mapIntent.setData(location.getUri());

        if (mapIntent.resolveActivity(getPackageManager()) != null)
        {
            startActivity(mapIntent);
        }
    }

    protected void showHelp()
    {
        HelpDialog helpDialog = new HelpDialog(this);
        helpDialog.onPrepareDialog(getString(R.string.help_general_timeMode));
        helpDialog.show();
    }

    protected void showAbout()
    {
        AboutDialog aboutDialog = new AboutDialog(this);
        aboutDialog.onPrepareDialog();
        aboutDialog.show();
    }

    protected void showSettings()
    {
        Intent settingsIntent = new Intent(this, SuntimesSettingsActivity.class);
        startActivity(settingsIntent);
    }

    protected void scheduleAlarm()
    {
        final AlarmDialog alarmDialog = new AlarmDialog(this, dataset);
        alarmDialog.setOnAcceptedListener(new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                SolarEvents choice = alarmDialog.getChoice();
                String alarmLabel = choice.getShortDisplayString();
                Calendar now = dataset.now();
                Calendar calendar = alarmDialog.getCalendarForAlarmChoice(choice, now);
                scheduleAlarm(alarmLabel, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
            }
        });
        AlertDialog alarmAlert = alarmDialog.toAlertDialog();
        alarmAlert.show();
    }

    protected void scheduleAlarmFromNote()
    {
        scheduleAlarmFromNote(notes.getNote());
    }

    protected void scheduleAlarmFromNote(NoteData note)
    {
        String alarmLabel = note.noteText;
        Calendar calendar = dataset.now();
        calendar.setTimeInMillis(note.time.getTime());
        scheduleAlarm(alarmLabel, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    protected void scheduleAlarm(String label, int hour, int minutes)
    {
        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, label);
        alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);

        if (alarmIntent.resolveActivity(getPackageManager()) != null)
        {
            startActivity(alarmIntent);
        }
    }

    protected void showAlarms()
    {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
         {
             Intent alarmsIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
             alarmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

             if (alarmsIntent.resolveActivity(getPackageManager()) != null)
             {
                 startActivity(alarmsIntent);
             }
         }
    }

    private void initData( Context context )
    {
        SuntimesData data_actualTime = new SuntimesData(context, AppWidgetManager.INVALID_APPWIDGET_ID);
        data_actualTime.setCompareMode(WidgetSettings.CompareMode.TOMORROW);
        data_actualTime.setTimeMode(WidgetSettings.TimeMode.OFFICIAL);

        SuntimesData data_civilTime = new SuntimesData(data_actualTime);
        data_civilTime.setTimeMode(WidgetSettings.TimeMode.CIVIL);

        SuntimesData data_nauticalTime = new SuntimesData(data_actualTime);
        data_nauticalTime.setTimeMode(WidgetSettings.TimeMode.NAUTICAL);

        SuntimesData data_astroTime = new SuntimesData(data_actualTime);
        data_astroTime.setTimeMode(WidgetSettings.TimeMode.ASTRONOMICAL);

        SuntimesData data_noon = new SuntimesData(data_actualTime);
        data_noon.setTimeMode(WidgetSettings.TimeMode.NOON);

        dataset = new SuntimesDataset(data_actualTime, data_civilTime, data_nauticalTime, data_astroTime, data_noon);
    }

    protected void calculateData( Context context )
    {
        initData(context);
        dataset.calculateData();
        initNotes();
    }

    protected void updateViews( Context context )
    {
        stopTimeTask();

        calculateData(context);

        location = WidgetSettings.loadLocationPref(context, AppWidgetManager.INVALID_APPWIDGET_ID);
        String locationTitle = location.getLabel();
        String locationSubtitle = location.toString();

        if (actionBar != null)
        {
            actionBar.setTitle(locationTitle);
            actionBar.setSubtitle(locationSubtitle);
        }

        //
        // today's view
        //
        SuntimesUtils.TimeDisplayText sunriseString_actualTime = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunriseCalendarToday());
        SuntimesUtils.TimeDisplayText sunriseString_civilTime = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunriseCalendarToday());
        SuntimesUtils.TimeDisplayText sunriseString_nauticalTime = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunriseCalendarToday());
        SuntimesUtils.TimeDisplayText sunriseString_astroTime = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunriseCalendarToday());

        SuntimesUtils.TimeDisplayText noonString = utils.calendarTimeShortDisplayString(context, dataset.dataNoon.sunriseCalendarToday() );

        SuntimesUtils.TimeDisplayText sunsetString_actualTime = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunsetCalendarToday());
        SuntimesUtils.TimeDisplayText sunsetString_civilTime = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunsetCalendarToday());
        SuntimesUtils.TimeDisplayText sunsetString_nauticalTime = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunsetCalendarToday());
        SuntimesUtils.TimeDisplayText sunsetString_astroTime = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunsetCalendarToday());

        txt_sunrise_actual.setText(sunriseString_actualTime.toString());
        txt_sunrise_civil.setText(sunriseString_civilTime.toString());
        txt_sunrise_nautical.setText(sunriseString_nauticalTime.toString());
        txt_sunrise_astro.setText(sunriseString_astroTime.toString());

        txt_solarnoon.setText(noonString.toString());

        txt_sunset_actual.setText(sunsetString_actualTime.toString());
        txt_sunset_civil.setText(sunsetString_civilTime.toString());
        txt_sunset_nautical.setText(sunsetString_nauticalTime.toString());
        txt_sunset_astro.setText(sunsetString_astroTime.toString());

        SuntimesUtils.TimeDisplayText dayLengthDisplay = utils.timeDeltaLongDisplayString(0, dataset.dataActual.dayLengthToday());
        dayLengthDisplay.setSuffix("");
        txt_daylength.setText(dayLengthDisplay.toString());

        SuntimesUtils.TimeDisplayText lightLengthDisplay = utils.timeDeltaLongDisplayString(0, dataset.dataCivil.dayLengthToday());
        lightLengthDisplay.setSuffix("");
        txt_lightlength.setText(lightLengthDisplay.toString());

        Date data_date = dataset.dataActual.date();
        //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());       // 4/11/2016
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getApplicationContext());   // Apr 11, 2016
        //DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(getApplicationContext());   // April 11, 2016
        txt_date.setText(getString(R.string.today) + "\n" + dateFormat.format(data_date));

        //
        // tomorrow's view
        //
        SuntimesUtils.TimeDisplayText sunriseString_actualTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunriseCalendarOther());
        SuntimesUtils.TimeDisplayText sunriseString_civilTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunriseCalendarOther());
        SuntimesUtils.TimeDisplayText sunriseString_nauticalTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunriseCalendarOther());
        SuntimesUtils.TimeDisplayText sunriseString_astroTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunriseCalendarOther());

        SuntimesUtils.TimeDisplayText noonString2 = utils.calendarTimeShortDisplayString(context, dataset.dataNoon.sunriseCalendarOther() );

        SuntimesUtils.TimeDisplayText sunsetString_actualTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunsetCalendarOther());
        SuntimesUtils.TimeDisplayText sunsetString_civilTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunsetCalendarOther());
        SuntimesUtils.TimeDisplayText sunsetString_nauticalTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunsetCalendarOther());
        SuntimesUtils.TimeDisplayText sunsetString_astroTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunsetCalendarOther());

        txt_sunrise2_actual.setText(sunriseString_actualTime2.toString());
        txt_sunrise2_civil.setText(sunriseString_civilTime2.toString());
        txt_sunrise2_nautical.setText(sunriseString_nauticalTime2.toString());
        txt_sunrise2_astro.setText(sunriseString_astroTime2.toString());

        txt_solarnoon2.setText(noonString2.toString());

        txt_sunset2_actual.setText(sunsetString_actualTime2.toString());
        txt_sunset2_civil.setText(sunsetString_civilTime2.toString());
        txt_sunset2_nautical.setText(sunsetString_nauticalTime2.toString());
        txt_sunset2_astro.setText(sunsetString_astroTime2.toString());

        SuntimesUtils.TimeDisplayText dayLengthDisplay2 = utils.timeDeltaLongDisplayString(0, dataset.dataActual.dayLengthOther());
        dayLengthDisplay2.setSuffix("");
        txt_daylength2.setText(dayLengthDisplay2.toString());

        SuntimesUtils.TimeDisplayText lightLengthDisplay2 = utils.timeDeltaLongDisplayString(0, dataset.dataCivil.dayLengthOther());
        lightLengthDisplay2.setSuffix("");
        txt_lightlength2.setText(lightLengthDisplay2.toString());

        Date data_date2 = dataset.dataActual.dateOther();
        txt_date2.setText(getString(R.string.tomorrow) + "\n" + dateFormat.format(data_date2));

        //
        // clock
        //
        txt_timezone.setText(dataset.timezone());
        startTimeTask();
    }

    /**
     * Start updates to the clock ui.
     */
    private void startTimeTask()
    {
        txt_time.post(updateTimeTask);
    }

    /**
     * Stop updates to the clock ui.
     */
    private void stopTimeTask()
    {
        txt_time.removeCallbacks(updateTimeTask);
    }

    /**
     * Clock ui update rate; once every few seconds.
     */
    public static int UPDATE_RATE = 3000;

    /**
     * Update the clock ui at regular intervals to reflect current time (and note).
     */
    private Runnable updateTimeTask = new Runnable()
    {
        @Override
        public void run()
        {
            if (!dataset.isCalculated())
            {
                Log.w("SuntimesActivity", "updateTimeTask called before data was ready!");
                return;
            }
            updateTimeViews(SuntimesActivity.this);
            txt_time.postDelayed(this, UPDATE_RATE);
        }
    };

    /**
     * Update the clock ui to reflect current time.
     * @param context the Activity context
     */
    protected void updateTimeViews(Context context)
    {
        Calendar now = dataset.now();
        SuntimesUtils.TimeDisplayText timeText = utils.calendarTimeShortDisplayString(this, now);
        txt_time.setText(timeText.getValue());
        txt_time_suffix.setText(timeText.getSuffix());
        notes.updateNote(context, now);
    }

    /**
     * onTouch swipe between the prev/next items in the view_flipper
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return super.onTouchEvent(event);
    }

    /**
     * viewFlipper "note" onTouchListener; swipe between available notes
     */
    private View.OnTouchListener noteTouchListener = new View.OnTouchListener()
    {
        public int MOVE_SENSITIVITY = 25;
        public int FLING_SENSITIVITY = 0;
        public float firstTouchX, secondTouchX;

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    firstTouchX = event.getX();
                    break;

                case MotionEvent.ACTION_UP:
                    secondTouchX = event.getX();
                    if ((firstTouchX - secondTouchX) >= FLING_SENSITIVITY)
                    {
                        notes.showNextNote();

                    } else if ((secondTouchX - firstTouchX) > FLING_SENSITIVITY) {
                        notes.showPrevNote();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    final View currentView = note_flipper.getCurrentView();
                    int moveDeltaX = (int)(event.getX() - firstTouchX);
                    if (Math.abs(moveDeltaX) < MOVE_SENSITIVITY)
                    {
                        currentView.layout(moveDeltaX, currentView.getTop(), currentView.getWidth(), currentView.getBottom());
                    }
                    break;
            }
            return true;
        }
    };

    /**
     * viewFlipper "time card" onTouchListener; swipe left/right between viewflipper layouts (today/tomorrow)
     */
    private View.OnTouchListener timeCardTouchListener = new View.OnTouchListener()
    {
        public int MOVE_SENSITIVITY = 150;
        public int FLING_SENSITIVITY = 25;
        public float firstTouchX, secondTouchX;

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    firstTouchX = event.getX();
                    break;

                case MotionEvent.ACTION_UP:
                    secondTouchX = event.getX();
                    if ((secondTouchX - firstTouchX) > FLING_SENSITIVITY)
                    {   // swipe right; back to previous view
                        showPreviousCard();

                    } else if (firstTouchX - secondTouchX > FLING_SENSITIVITY) {
                        // swipe left; advance to next view
                        showNextCard();

                    } else {
                        // swipe cancel; reset current view
                        final View currentView = card_flipper.getCurrentView();
                        currentView.layout(0, currentView.getTop(), currentView.getWidth(), currentView.getBottom());
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    float currentTouchX = event.getX();
                    int moveDelta = (int)(currentTouchX - firstTouchX);
                    boolean isSwipeRight = (moveDelta > 0);

                    final View currentView = card_flipper.getCurrentView();
                    int currentIndex = card_flipper.getDisplayedChild();
                    int otherIndex = (isSwipeRight ? currentIndex - 1
                            : currentIndex + 1);
                    if (otherIndex >= 0 && otherIndex < card_flipper.getChildCount())
                    {
                        // in-between child views; flip between them
                        currentView.layout( moveDelta, currentView.getTop(),
                                moveDelta + currentView.getWidth(), currentView.getBottom() );

                        // extended movement; manually trigger swipe/fling
                        if (moveDelta > MOVE_SENSITIVITY || moveDelta < MOVE_SENSITIVITY * -1)
                        {
                            event.setAction(MotionEvent.ACTION_UP);
                            return onTouch(view, event);
                        }

                    } else {
                        // at-a-boundary (the first/last view);
                        // TODO: animate somehow to let user know there aren't additional views
                    }

                    break;
            }

            return true;
        }
    };



    /**
     * Show the 'next' set of data displayed by the main view_flipper.
     */
    public boolean showNextCard()
    {
        if (hasNextCard())
        {
            card_flipper.setOutAnimation(anim_card_outNext);
            card_flipper.setInAnimation(anim_card_inNext);
            card_flipper.showNext();
            return true;
        }
        return false;
    }

    public boolean hasNextCard()
    {
        int current = card_flipper.getDisplayedChild();
        return ((current + 1) < card_flipper.getChildCount());
    }

    /**
     * Show the 'previous' set of data displayed by the main view_flipper.
     */
    public boolean showPreviousCard()
    {
        if (hasPreviousCard())
        {
            card_flipper.setOutAnimation(anim_card_outPrev);
            card_flipper.setInAnimation(anim_card_inPrev);
            card_flipper.showPrevious();
            return true;
        }
        return false;
    }

    public boolean hasPreviousCard()
    {
        int current = card_flipper.getDisplayedChild();
        int prev = current - 1;
        return (prev >= 0);
    }

    View.OnClickListener onNextCardClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            showNextCard();
        }
    };

    View.OnClickListener onPrevCardClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            showPreviousCard();
        }
    };

    View.OnClickListener onNextNoteClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            notes.showNextNote();
        }
    };

    View.OnClickListener onPrevNoteClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            notes.showPrevNote();
        }
    };

    View.OnClickListener onClockClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            AppSettings.ClockTapAction action = AppSettings.loadClockTapActionPref(SuntimesActivity.this);
            if (action == AppSettings.ClockTapAction.NOTHING)
            {
                return;
            }

            if (action == AppSettings.ClockTapAction.ALARM)
            {
                scheduleAlarm();
                return;
            }

            if (action == AppSettings.ClockTapAction.NEXT_NOTE)
            {
                notes.showNextNote();
                return;
            }

            if (action == AppSettings.ClockTapAction.PREV_NOTE)
            {
                notes.showPrevNote();
                return;
            }

            Log.w("SuntimesActivity", "Unrecognized ClockTapAction (so doing nothing)" );
        }
    };


    public void highlightField( SolarEventField highlightField )
    {
        int nextCardOffset = 0;
        int currentCard = this.card_flipper.getDisplayedChild();

        for (SolarEventField field : timeFields.keySet())
        {
            TextView txtField = timeFields.get(field);
            if (txtField != null)
            {
                if (field.equals(highlightField))
                {
                    txtField.setTypeface(txtField.getTypeface(), Typeface.BOLD);
                    txtField.setPaintFlags(txtField.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                    if (currentCard == 0 && field.tomorrow)
                    {
                        nextCardOffset = 1;

                    } else if (currentCard == 1 && !field.tomorrow) {
                        nextCardOffset = -1;
                    }

                } else {
                    txtField.setTypeface(Typeface.create(txtField.getTypeface(), Typeface.NORMAL), Typeface.NORMAL);
                    txtField.setPaintFlags(txtField.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
                }
            }
        }

        if (nextCardOffset > 0)
        {
            showNextCard();

        } else if (nextCardOffset < 0) {
            showPreviousCard();
        }
    }

    protected void updateNoteUI( NoteData note, int transition )
    {
        if (note_flipper.getDisplayedChild() == 0)
        {
            // currently using view1, ready view2
            ic_time2_note.setBackgroundResource(note.noteIconResource);
            ic_time2_note.setVisibility(View.VISIBLE);
            txt_time2_note1.setText(note.timeText.toString());
            txt_time2_note2.setText(note.prefixText);
            txt_time2_note3.setText(note.noteText);
            txt_time2_note3.setTextColor(note.noteColor);

        } else {
            // currently using view2, ready view1
            ic_time1_note.setBackgroundResource(note.noteIconResource);
            ic_time1_note.setVisibility(View.VISIBLE);
            txt_time1_note1.setText(note.timeText.toString());
            txt_time1_note2.setText(note.prefixText);
            txt_time1_note3.setText(note.noteText);
            txt_time1_note3.setTextColor(note.noteColor);
        }

        if (transition == NoteChangedListener.TRANSITION_NEXT)
        {
            note_flipper.setInAnimation(anim_note_inNext);
            note_flipper.setOutAnimation(anim_note_outNext);
            note_flipper.showNext();

        } else {
            note_flipper.setInAnimation(anim_note_inPrev);
            note_flipper.setOutAnimation(anim_note_outPrev);
            note_flipper.showPrevious();
        }

        highlightField(new SolarEventField(note.noteMode, note.tomorrow));
    }

    /**
     * SolarEventField
     */
    private class SolarEventField
    {
        public SolarEvents event = SolarEvents.NOON;
        public Boolean tomorrow = false;

        public SolarEventField(SolarEvents event, boolean tomorrow)
        {
            this.event = event;
            this.tomorrow = tomorrow;
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof SolarEventField))
            {
                return false;

            } else {
                SolarEventField that = (SolarEventField)obj;
                return (this.event.equals(that.event) && (this.tomorrow == that.tomorrow));
            }
        }

        public int hashCode()
        {
            int hash = this.event.hashCode();
            hash = hash * 37 + (tomorrow ? 0 : 1);
            return hash;
        }
    }
}