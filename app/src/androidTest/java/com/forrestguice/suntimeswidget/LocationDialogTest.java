/**
    Copyright (C) 2017-2025 Forrest Guice
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

import android.app.Activity;
import android.content.Context;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

/**
 * Automated UI tests for the LocationDialog.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class LocationDialogTest extends SuntimesActivityTestBase
{
    @Rule
    public ActivityTestRule<SuntimesActivity> activityRule = new ActivityTestRule<>(SuntimesActivity.class);

    @Before
    public void beforeTest() throws IOException {
        setAnimationsEnabled(false);
    }
    @After
    public void afterTest() throws IOException {
        setAnimationsEnabled(true);
    }

    /**
     * UI Test
     * Show the location dialog, rotate, swap modes, rotate, repeatedly swap modes, and then cancel the dialog.
     */
    @Test
    public void test_locationDialog()
    {
        Activity activity = activityRule.getActivity();
        LocationDialogRobot robot = new LocationDialogRobot();
        robot.showDialog(activity)
                .captureScreenshot(activity, "suntimes-dialog-location0")
                .assertDialogShown(activity)
                .cancelDialog(activityRule.getActivity());
    }

    @Test
    public void test_locationDialog_current()
    {
        Activity activity = activityRule.getActivity();
        LocationDialogRobot robot = new LocationDialogRobot();
        robot.showDialog(activity)
                .assertDialogShown(activity);

        robot.selectLocationMode(WidgetSettings.LocationMode.CURRENT_LOCATION)
                .assertDialogMode_isCurrent()
                .rotateDevice(activityRule.getActivity())
                .assertDialogMode_isCurrent();

        robot.selectLocationMode(WidgetSettings.LocationMode.CUSTOM_LOCATION)
                .rotateDevice(activityRule.getActivity())
                .assertDialogMode_isCustom()
                .selectLocationMode(WidgetSettings.LocationMode.CURRENT_LOCATION)
                .selectLocationMode(WidgetSettings.LocationMode.CUSTOM_LOCATION)
                .assertDialogMode_isCustom();

        robot.cancelDialog(activityRule.getActivity());
    }

    @Test
    public void test_locationDialog_custom()
    {
        Activity activity = activityRule.getActivity();
        LocationDialogRobot robot = new LocationDialogRobot();
        robot.showDialog(activity)
                .assertDialogShown(activity);

        robot.selectLocationMode(WidgetSettings.LocationMode.CUSTOM_LOCATION)
                .assertDialogMode_isCustom()
                .assertDialogState_select()
                .rotateDevice(activity).sleep(1000)
                .assertDialogMode_isCustom()
                .assertDialogState_select();

        // edit -> save
        robot.clickLocationEditButton()
                .assertDialogState_edit()
                .inputLocationEditValues(TESTLOC_0_LABEL, TESTLOC_0_LAT, TESTLOC_0_LON)

                .rotateDevice(activity).sleep(1000)
                .assertDialogState_edit()
                .assertLocationEditCoordinates(TESTLOC_0_LAT, TESTLOC_0_LON)

                .clickLocationEditSaveButton()
                .assertLocationEditCoordinates(TESTLOC_0_LAT, TESTLOC_0_LON);

        // edit -> cancel
        robot.clickLocationEditButton()
                .inputLocationEditValues("1", "2", "3")
                .assertLocationEditCoordinates("2", "3")
                .clickLocationEditCancelButton()
                .assertDialogState_select()
                .assertLocationEditCoordinates(TESTLOC_0_LAT, TESTLOC_0_LON);

        robot.selectLocationMode(WidgetSettings.LocationMode.CURRENT_LOCATION)
                .rotateDevice(activity).sleep(1000)
                .assertDialogMode_isCurrent()
                .selectLocationMode(WidgetSettings.LocationMode.CUSTOM_LOCATION)
                .selectLocationMode(WidgetSettings.LocationMode.CURRENT_LOCATION)
                .assertDialogMode_isCurrent();

        robot.cancelDialog(activity);
    }

    private void editLocation( String name, String latitude, String longitude )
    {
        // click on the `edit` button
        LocationDialogRobot robot = new LocationDialogRobot();
        robot.clickLocationEditButton()
                .rotateDevice(activityRule.getActivity())
                .assertDialogState_edit();

        // fill in form fields
        robot.inputLocationEditValues(name, latitude, longitude)
                .rotateDevice(activityRule.getActivity())
                .assertLocationEditCoordinates(latitude, longitude);

        // click the `save` button
        robot.clickLocationEditSaveButton()
                .assertDialogState_select()
                .assertLocationEditCoordinates(latitude, longitude);
        //onView(withId(R.id.appwidget_location_nameSelect)).check(matches(withSpinnerText(containsString(name))));  // selected name matches input
    }

    /**
     * LocationDialogRobot
     */
    public static class LocationDialogRobot extends DialogTest.DialogRobotBase implements DialogTest.DialogRobot
    {
        @Override
        public LocationDialogRobot showDialog(Activity activity) {
            onView(withId(R.id.action_location_add)).perform(click());   // show dialog from actionbar
            return this;
        }
        @Override
        public LocationDialogRobot applyDialog(Context context) {
            onView(withId(R.id.dialog_button_accept)).perform(click());
            return this;
        }
        @Override
        public LocationDialogRobot cancelDialog(Context context) {
            onView(withId(R.id.dialog_button_cancel)).perform(click());
            return this;
        }
        @Override
        public LocationDialogRobot rotateDevice(Activity activity) {
            super.rotateDevice(activity);
            return this;
        }
        @Override
        public LocationDialogRobot sleep(long ms) {
            super.sleep(ms);
            return this;
        }

        public LocationDialogRobot selectLocationMode( WidgetSettings.LocationMode mode )
        {
            onView(withId(R.id.appwidget_location_mode)).perform(click());
            onData(allOf(is(instanceOf(WidgetSettings.LocationMode.class)), is(mode)))
                    .inRoot(isPlatformPopup()).perform(click());
            return this;
        }

        public LocationDialogRobot clickLocationEditButton() {
            onView(withId(R.id.appwidget_location_edit)).perform(click());
            return this;
        }
        public LocationDialogRobot clickLocationEditSaveButton() {
            onView(withId(R.id.appwidget_location_save)).perform(click());
            return this;
        }
        public LocationDialogRobot clickLocationEditCancelButton() {
            onView(withId(R.id.appwidget_location_cancel)).perform(click());
            return this;
        }
        public LocationDialogRobot inputLocationEditValues(String name, String latitude, String longitude) {
            inputLocationEditName(name);
            inputLocationEditLatitude(latitude);
            inputLocationEditLongitude(longitude);
            return this;
        }
        public LocationDialogRobot inputLocationEditName(String name) {
            onView(withId(R.id.appwidget_location_name)).perform(replaceText(name));    // fill in name
            return this;
        }
        public LocationDialogRobot inputLocationEditLatitude(String lat) {
            onView(withId(R.id.appwidget_location_lat)).perform(replaceText(lat));      // latitude and
            return this;
        }
        public LocationDialogRobot inputLocationEditLongitude(String lon) {
            onView(withId(R.id.appwidget_location_lon)).perform(replaceText(lon));      // longitude fields
            return this;
        }

        @Override
        public LocationDialogRobot assertDialogShown(Context context)
        {
            if (detectLocationMode() == WidgetSettings.LocationMode.CURRENT_LOCATION)
                assertDialogMode_isCurrent();
            else assertDialogMode_isCustom();
            return this;
        }

        public LocationDialogRobot assertDialogMode_isCurrent()
        {
            onView(withId(R.id.appwidget_location_auto)).check( ViewAssertionHelper.assertEnabled );
            onView(withId(R.id.appwidget_location_name)).check( ViewAssertionHelper.assertHidden );        // name textedit hidden
            onView(withId(R.id.appwidget_location_nameSelect)).check( ViewAssertionHelper.assertDisabled ); // name selector disabled
            onView(withId(R.id.appwidget_location_lat)).check( ViewAssertionHelper.assertDisabled );       // lat field disabled
            onView(withId(R.id.appwidget_location_lon)).check( ViewAssertionHelper.assertDisabled );       // lon field disabled
            onView(withId(R.id.appwidget_location_edit)).check( ViewAssertionHelper.assertHidden );        // edit button is hidden
            onView(withId(R.id.appwidget_location_save)).check( ViewAssertionHelper.assertHidden );        // save button hidden
            onView(withId(R.id.appwidget_location_getfix)).check( ViewAssertionHelper.assertHidden );
            return this;
        }
        public LocationDialogRobot assertDialogMode_isCustom()
        {
            if (viewIsDisplayed(R.id.appwidget_location_nameSelect))
                assertDialogState_select();
            else assertDialogState_edit();
            return this;
        }

        public LocationDialogRobot assertDialogState_select()
        {
            onView(withId(R.id.appwidget_location_name)).check( ViewAssertionHelper.assertHidden );        // name textedit hidden
            onView(withId(R.id.appwidget_location_nameSelect)).check( ViewAssertionHelper.assertEnabled ); // name selector enabled
            onView(withId(R.id.appwidget_location_lat)).check( ViewAssertionHelper.assertDisabled );       // lat field disabled
            onView(withId(R.id.appwidget_location_lon)).check( ViewAssertionHelper.assertDisabled );       // lon field disabled
            onView(withId(R.id.appwidget_location_edit)).check( ViewAssertionHelper.assertShown );         // edit button is shown
            onView(withId(R.id.appwidget_location_save)).check( ViewAssertionHelper.assertHidden );        // save button hidden
            onView(withId(R.id.appwidget_location_getfix)).check( ViewAssertionHelper.assertHidden );      // gps button is hidden
            onView(withId(R.id.appwidget_location_auto)).check( ViewAssertionHelper.assertHidden );
            return this;
        }
        public LocationDialogRobot assertDialogState_edit()
        {
            onView(withId(R.id.appwidget_location_name)).check( ViewAssertionHelper.assertFocused );      // name textedit enabled (and focused)
            onView(withId(R.id.appwidget_location_nameSelect)).check(ViewAssertionHelper.assertHidden);   // name selector hidden
            onView(withId(R.id.appwidget_location_lat)).check( ViewAssertionHelper.assertEnabled );       // lat, lon now enabled
            onView(withId(R.id.appwidget_location_lon)).check( ViewAssertionHelper.assertEnabled );
            onView(withId(R.id.appwidget_location_edit)).check( ViewAssertionHelper.assertHidden );       // edit button is hidden
            onView(withId(R.id.appwidget_location_save)).check( ViewAssertionHelper.assertShown );        // save button now shown
            onView(withId(R.id.appwidget_location_getfix)).check( ViewAssertionHelper.assertEnabled );    // gps button is enabled
            onView(withId(R.id.appwidget_location_auto)).check( ViewAssertionHelper.assertHidden );
            return this;
        }

        public LocationDialogRobot assertLocationEditCoordinates(String latitude, String longitude) {
            assertLocationEditLatitude(latitude);
            assertLocationEditLongitude(longitude);
            return this;
        }
        public LocationDialogRobot assertLocationEditLatitude(String latitude) {
            onView(withId(R.id.appwidget_location_lat)).check(matches(withText(latitude)));
            return this;
        }
        public LocationDialogRobot assertLocationEditLongitude(String longitude) {
            onView(withId(R.id.appwidget_location_lon)).check(matches(withText(longitude)));
            return this;
        }

        public static WidgetSettings.LocationMode detectLocationMode()
        {
            if (spinnerDisplaysText(R.id.appwidget_location_mode, WidgetSettings.LocationMode.CURRENT_LOCATION.toString()))
                return WidgetSettings.LocationMode.CURRENT_LOCATION;
            else if (spinnerDisplaysText(R.id.appwidget_location_mode, WidgetSettings.LocationMode.CUSTOM_LOCATION.toString()))
                return WidgetSettings.LocationMode.CUSTOM_LOCATION;
            else
                return null;   // unrecognized mode; fail with a null
        }

    }
}
