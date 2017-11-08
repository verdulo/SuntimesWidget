/**
    Copyright (C) 2017 Forrest Guice
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

import android.app.Application;
import android.util.Log;

import net.time4j.android.ApplicationStarter;
import net.time4j.tz.spi.TimezoneRepositoryProviderSPI;

public class SuntimesApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        //ApplicationStarter.initialize(this, false);
        ApplicationStarter.prepareAssets(this, null);
        Log.d("DEBUG", "SuntimesApplication.onCreate :: " + new TimezoneRepositoryProviderSPI().getVersion());
    }
}
