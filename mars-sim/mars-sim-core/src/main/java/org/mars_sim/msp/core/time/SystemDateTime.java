/**
 * Mars Simulation Project
 * SystemDateTime.java
 * @version 3.08 2015-03-28
 * @author Manny Kung
 */

package org.mars_sim.msp.core.time;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class SystemDateTime {

       protected SimpleDateFormat dateFormat = 
               new SimpleDateFormat("MM-dd-yyyy");
       
       protected SimpleDateFormat timeFormat =
               new SimpleDateFormat("hhmmssaa");

       private String dateStr;
       private String timeStr;
       private static String dateTimeStr;
    
       public SystemDateTime() {
       }

       public String getDateTimeStr() {

               Calendar currentCalendar = 
                   Calendar.getInstance();
               Date currentTime = 
                   currentCalendar.getTime();
               dateStr = dateFormat
                   .format(currentTime);
               timeStr = timeFormat
                   .format(currentTime);
               dateTimeStr = dateStr + "_" + timeStr;
               //System.out.println("dateTimeStr : " + dateTimeStr);
    	   return dateTimeStr;
       }
   }
