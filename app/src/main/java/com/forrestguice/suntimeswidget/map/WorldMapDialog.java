/**
    Copyright (C) 2018 Forrest Guice
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

package com.forrestguice.suntimeswidget.map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.SuntimesCalculatorDescriptor;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.calculator.core.Location;
import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

public class WorldMapDialog extends BottomSheetDialogFragment
{
    public static final String LOGTAG = "WorldMapDialog";

    private TextView dialogTitle;
    private WorldMapView worldmap;
    private TextView empty;
    private View dialogContent = null;
    private TextView utcTime;
    private Spinner mapSelector;
    private View radioGroup;
    private ArrayAdapter<WorldMapWidgetSettings.WorldMapWidgetMode> mapAdapter;
    private WorldMapWidgetSettings.WorldMapWidgetMode mapMode = null;

    private SuntimesUtils utils = new SuntimesUtils();

    private SuntimesRiseSetDataset data;
    public void setData( SuntimesRiseSetDataset data )
    {
        this.data = data;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedState)
    {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(getActivity(), AppSettings.loadTheme(getContext()));    // hack: contextWrapper required because base theme is not properly applied
        dialogContent = inflater.cloneInContext(contextWrapper).inflate(R.layout.layout_dialog_worldmap, parent, false);
        WorldMapWidgetSettings.initDisplayStrings(dialogContent.getContext());

        initViews(getContext(), dialogContent);
        if (savedState != null) {
            Log.d(LOGTAG, "WorldMapDialog onCreate (restoreState)");
        }
        themeViews(dialogContent.getContext());

        return dialogContent;
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(onShowDialogListener);
        return dialog;
    }

    private DialogInterface.OnShowListener onShowDialogListener = new DialogInterface.OnShowListener()
    {
        @Override
        public void onShow(DialogInterface dialog)
        {
            Log.d(WorldMapView.LOGTAG, "onShowDialog: triggering update...");
            updateViews();
            startUpdateTask();
        }
    };

    private void expandSheet(Dialog dialog)
    {
        BottomSheetDialog bottomSheet = (BottomSheetDialog) dialog;
        FrameLayout layout = (FrameLayout) bottomSheet.findViewById(android.support.design.R.id.design_bottom_sheet);
        if (layout != null)
        {
            BottomSheetBehavior behavior = BottomSheetBehavior.from(layout);
            behavior.setHideable(true);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void startUpdateTask()
    {
        stopUpdateTask();
        if (dialogContent != null)
            dialogContent.post(updateTask);
    }
    private void stopUpdateTask()
    {
        if (dialogContent != null)
            dialogContent.removeCallbacks(updateTask);
    }

    @Override
    public void onStop()
    {
        stopUpdateTask();
        super.onStop();
    }

    public static final int UPDATE_RATE = 3000;
    private Runnable updateTask = new Runnable()
    {
        @Override
        public void run()
        {
            if (data != null)
            {
                Context context = getContext();
                if (utcTime != null && context != null)
                {
                    Calendar now = data.nowThen(data.calendar());
                    Calendar nowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    nowUtc.setTimeInMillis(now.getTimeInMillis());
                    SuntimesUtils.TimeDisplayText timeText = utils.calendarDateTimeDisplayString(context, nowUtc); // utils.calendarTimeShortDisplayString(context, nowUtc);
                    utcTime.setText(getString(R.string.datetime_format_verylong, timeText.toString(), nowUtc.getTimeZone().getID()));
                }
                // TODO: periodic update bitmap
            }
            if (dialogContent != null)
                dialogContent.postDelayed(this, UPDATE_RATE);
        }
    };

    public void initViews(final Context context, View dialogView)
    {
        dialogTitle = (TextView)dialogView.findViewById(R.id.worldmapdialog_title);
        utcTime = (TextView)dialogView.findViewById(R.id.info_time_utc);
        empty = (TextView)dialogView.findViewById(R.id.txt_empty);
        worldmap = (WorldMapView)dialogView.findViewById(R.id.info_time_worldmap);
        worldmap.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view) {
                return showContextMenu(context, view);
            }
        });

        ArrayList<WorldMapWidgetSettings.WorldMapWidgetMode> modes = new ArrayList<>(Arrays.asList(WorldMapWidgetSettings.WorldMapWidgetMode.values()));
        //modes.remove(WorldMapWidgetSettings.WorldMapWidgetMode.EQUIAZIMUTHAL_SIMPLE);  // option disabled; TODO: fix layout issues

        mapAdapter = new ArrayAdapter<WorldMapWidgetSettings.WorldMapWidgetMode>(context, R.layout.layout_listitem_oneline_alt, modes);
        mapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mapSelector = (Spinner)dialogView.findViewById(R.id.worldmap_selector);
        mapSelector.setAdapter(mapAdapter);

        mapMode = WorldMapWidgetSettings.loadSunPosMapModePref(context, 0, WorldMapWidgetSettings.MAPTAG_DEF);
        int modePosition = mapAdapter.getPosition(mapMode);
        mapSelector.setSelection((modePosition >= 0) ? modePosition : 0);
        worldmap.setMapMode(context, (WorldMapWidgetSettings.WorldMapWidgetMode) mapSelector.getSelectedItem());

        mapSelector.setOnItemSelectedListener(onMapSelected);

        WorldMapTask.WorldMapOptions options = worldmap.getOptions();
        updateOptions(getContext());

        radioGroup = dialogView.findViewById(R.id.radio_group);
        RadioButton option_sun = (RadioButton)dialogView.findViewById(R.id.radio_sun);
        RadioButton option_moon = (RadioButton)dialogView.findViewById(R.id.radio_moon);
        RadioButton option_sunmoon = (RadioButton)dialogView.findViewById(R.id.radio_sunmoon);

        if (options.showSunShadow && options.showMoonLight)
            option_sunmoon.setChecked(true);
        else if (options.showSunShadow)
            option_sun.setChecked(true);
        else option_moon.setChecked(true);

        option_sun.setOnClickListener(onRadioButtonClicked);
        option_moon.setOnClickListener(onRadioButtonClicked);
        option_sunmoon.setOnClickListener(onRadioButtonClicked);
    }

    @SuppressWarnings("ResourceType")
    public void themeViews(Context context)
    {
        if (themeOverride != null)
        {
            dialogTitle.setTextColor(themeOverride.getTitleColor());
            utcTime.setTextColor(themeOverride.getTimeColor());
            worldmap.themeViews(context, themeOverride);
        }
    }

    private SuntimesTheme themeOverride = null;
    public void themeViews(Context context, SuntimesTheme theme)
    {
        if (theme != null)
        {
            themeOverride = theme;
            if (worldmap != null) {
                themeViews(context);
            }
        }
    }

    public void updateOptions(Context context)
    {
        if (context != null)
        {
            Location location = WidgetSettings.loadLocationPref(context, 0);
            WorldMapTask.WorldMapOptions options = worldmap.getOptions();
            options.showSunShadow = WorldMapWidgetSettings.loadWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_SUNSHADOW, WorldMapWidgetSettings.MAPTAG_3x2);
            options.showMoonLight = WorldMapWidgetSettings.loadWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MOONLIGHT, WorldMapWidgetSettings.MAPTAG_3x2);
            options.showMajorLatitudes = WorldMapWidgetSettings.loadWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MAJORLATITUDES, WorldMapWidgetSettings.MAPTAG_3x2);

            if (WorldMapWidgetSettings.loadWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_LOCATION, WorldMapWidgetSettings.MAPTAG_3x2)) {
                options.locations = new double[][] {{location.getLatitudeAsDouble(), location.getLongitudeAsDouble()}};
            } else options.locations = null;

            options.modified = true;
        }
    }

    public void updateViews()
    {
        updateOptions(getContext());
        if (data != null)
            updateViews(data);
    }

    protected void updateViews( @NonNull SuntimesRiseSetDataset data )
    {
        stopUpdateTask();

        SuntimesCalculatorDescriptor calculatorDescriptor = data.calculatorMode();
        boolean featureSupported = calculatorDescriptor != null && calculatorDescriptor.hasRequestedFeature(SuntimesCalculator.FEATURE_POSITION);

        showEmptyView(!featureSupported);
        if (featureSupported)
        {
            worldmap.setMapTaskListener(new WorldMapView.WorldMapTaskListener()
            {
                @Override
                public void onFinished(Bitmap result) {
                    expandSheet(getDialog());
                }
            });
            worldmap.updateViews(data);
        }
        startUpdateTask();
    }

    private AdapterView.OnItemSelectedListener onMapSelected = new AdapterView.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            WorldMapWidgetSettings.WorldMapWidgetMode mode = (WorldMapWidgetSettings.WorldMapWidgetMode) parent.getItemAtPosition(position);
            Context context = getContext();
            if (context != null && mode != mapMode)
            {
                mapMode = mode;
                WorldMapWidgetSettings.saveSunPosMapModePref(context, 0, mapMode, WorldMapWidgetSettings.MAPTAG_DEF);
                worldmap.setMapMode(context, mapMode);
                Log.d(WorldMapView.LOGTAG, "onMapSelected: mapMode changed so triggering update...");
                updateViews();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    private View.OnClickListener onRadioButtonClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            //boolean checked = ((RadioButton) view).isChecked();
            Context context = getContext();
            switch(v.getId())
            {
                case R.id.radio_sun:
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_SUNSHADOW, WorldMapWidgetSettings.MAPTAG_3x2, true);
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MOONLIGHT, WorldMapWidgetSettings.MAPTAG_3x2,false);
                    break;

                case R.id.radio_moon:
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_SUNSHADOW, WorldMapWidgetSettings.MAPTAG_3x2, false);
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MOONLIGHT, WorldMapWidgetSettings.MAPTAG_3x2,true);
                    break;

                case R.id.radio_sunmoon:
                default:
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_SUNSHADOW, WorldMapWidgetSettings.MAPTAG_3x2, true);
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MOONLIGHT, WorldMapWidgetSettings.MAPTAG_3x2,true);
                    break;
            }
            Log.d(WorldMapView.LOGTAG, "onOptionSelected: sunlight/moonlight option changed so triggering update...");
            updateViews();
        }
    };

    private void showEmptyView( boolean show )
    {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
        worldmap.setVisibility(show ? View.GONE : View.VISIBLE);
        mapSelector.setVisibility(show ? View.GONE : View.VISIBLE);
        radioGroup.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    protected boolean showContextMenu(final Context context, View view)
    {
        PopupMenu menu = new PopupMenu(context, view);
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.mapmenu, menu.getMenu());
        menu.setOnMenuItemClickListener(onContextMenuClick);

        updateContextMenu(context, menu);
        SuntimesUtils.forceActionBarIcons(menu.getMenu());
        menu.show();
        return true;
    }

    private void updateContextMenu(Context context, PopupMenu menu)
    {
        MenuItem option_latitudes = menu.getMenu().findItem(R.id.mapOption_majorLatitudes);
        if (option_latitudes != null) {
            option_latitudes.setChecked(WorldMapWidgetSettings.loadWorldMapPref(context, 0,  WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MAJORLATITUDES, WorldMapWidgetSettings.MAPTAG_3x2));
        }

        MenuItem option_location = menu.getMenu().findItem(R.id.mapOption_location);
        if (option_location != null) {
            option_location.setChecked(WorldMapWidgetSettings.loadWorldMapPref(context, 0,  WorldMapWidgetSettings.PREF_KEY_WORLDMAP_LOCATION, WorldMapWidgetSettings.MAPTAG_3x2));
        }
    }

    private PopupMenu.OnMenuItemClickListener onContextMenuClick = new PopupMenu.OnMenuItemClickListener()
    {
        @Override
        public boolean onMenuItemClick(MenuItem item)
        {
            Context context = getContext();
            if (context == null) {
                return false;
            }

            boolean toggledValue;
            switch (item.getItemId())
            {
                // TODO: additional share options; e.g. animated over range

                case R.id.shareMap:
                    worldmap.shareBitmap();
                    return true;

                case R.id.mapOption_location:
                    toggledValue = !WorldMapWidgetSettings.loadWorldMapPref(context, 0,  WorldMapWidgetSettings.PREF_KEY_WORLDMAP_LOCATION, WorldMapWidgetSettings.MAPTAG_3x2);
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_LOCATION, WorldMapWidgetSettings.MAPTAG_3x2, toggledValue);
                    item.setChecked(toggledValue);
                    updateViews();
                    return true;

                case R.id.mapOption_majorLatitudes:
                    toggledValue = !WorldMapWidgetSettings.loadWorldMapPref(context, 0,  WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MAJORLATITUDES, WorldMapWidgetSettings.MAPTAG_3x2);
                    WorldMapWidgetSettings.saveWorldMapPref(context, 0, WorldMapWidgetSettings.PREF_KEY_WORLDMAP_MAJORLATITUDES, WorldMapWidgetSettings.MAPTAG_3x2, toggledValue);
                    item.setChecked(toggledValue);
                    updateViews();
                    return true;

                default:
                    return false;
            }
        }
    };

}
