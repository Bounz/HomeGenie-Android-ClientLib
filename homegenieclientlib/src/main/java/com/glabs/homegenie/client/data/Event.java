/*
    This file is part of HomeGenie for Android.

    HomeGenie for Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HomeGenie for Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with HomeGenie for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 *     Author: Generoso Martello <gene@homegenie.it>
 */

package com.glabs.homegenie.client.data;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

/**
 * Created by Gene on 29/04/14.
 */
public class Event implements Serializable {
// {"Timestamp":"2014-04-29T14:19:19.945118Z","Domain":"HomeAutomation.ZWave","Source":"8","Description":"ZWave Node","Property":"Meter.Watts","Value":"0.0","UnixTimestamp":1398781159945.1179}
    //public Date Timestamp;
    public String Domain;
    public String Source;
    public String Description;
    public String Property;
    public String Value;
    public Date Timestamp;
    public Double UnixTimestamp;
}
