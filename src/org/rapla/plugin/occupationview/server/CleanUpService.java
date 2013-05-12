package org.rapla.plugin.occupationview.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.storage.RefEntity;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.occupationview.CleanUpOption;
import org.rapla.server.ServerExtension;

public class CleanUpService extends RaplaComponent implements ServerExtension
{
   
    public CleanUpService( RaplaContext context ) throws RaplaException
    {
        super( context );
             
        long time = 0L;
        try {
        	String timeRun = getQuery().getPreferences( null ).getEntryAsString(CleanUpOption.CLEANUP_TIME, "00:00");
        	if(timeRun.equals("00:00"))
        		time = DateTools.MILLISECONDS_PER_DAY;        		
        	else {
        		SimpleDateFormat sdftime = new SimpleDateFormat ("HH:mm");
        		time = (sdftime.parse(timeRun)).getTime();
        	}
	        	        
        } catch (ParseException e) { }
        
        int cleanupAge = getQuery().getPreferences( null ).getEntryAsInteger(CleanUpOption.CLEANUP_AGE, 32);
        		
        Timer timer = new Timer(true);
        long period = cleanupAge * DateTools.MILLISECONDS_PER_DAY; // number of days
        CleanUpTask cleanUpTask = new CleanUpTask( period );
        // Call it each day
// BJO 00000057
        long timeRun = (DateTools.cutDate(new Date())).getTime() + time; // End of today + requested start time of archiving process 
        long repeat = DateTools.MILLISECONDS_PER_DAY;
        //long timeRun = new Date().getTime() + 30*1000; // debug: now + 30 seconds ( almost immediate) 
        //long repeat = 10*60*1000; // debug: only very 10 minutes
        Date start = new Date(timeRun);
        SimpleDateFormat sdfdatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdfHHMMSS = new SimpleDateFormat("HH:mm:ss aa");
        getLogger().info("Object CleanUp Service: Scheduled@" + sdfdatetime.format(start) + "(GMT) Objects older than " + cleanupAge + " days, repeat within " + sdfHHMMSS.format( repeat ));
// BJO 00000057
       	timer = new Timer(true); 
       	timer.schedule(cleanUpTask, start, repeat); // every x minutes
    }
 
    class CleanUpTask extends TimerTask
    {
        long period;
        public CleanUpTask( long period )
        {
            this.period = period;
        }

        public void run()
        {   
        	
            Date endDate = new Date(getClientFacade().today().getTime() - period);
// BJO 00000057
            SimpleDateFormat sdfdatetime = new SimpleDateFormat("yyyy-MM-dd");
        	getLogger().info("Cleanup Service: Started for Objects older than " + sdfdatetime.format(endDate));
// BJO 00000057        	
            /* 
             * remove old events in block for performance as individual removals will flush too much the data to storage. 
        	*/
            int eventCount = 0;
            try
            {
                Reservation[] events = getClientFacade().getReservations((User) null, null, endDate, null); //ClassificationFilter.CLASSIFICATIONFILTER_ARRAY );
                //List toRemove = new ArrayList();
                for ( int i=0;i< events.length;i++)
                {
                    Reservation event = events[i];
                    if ( isOlderThan( event, endDate))
                    {
                        getLogger().info("CleanUp Service: Cleaning event id=" + ((RefEntity)event).getShortId().toString());
    		    		getClientFacade().remove( event );
    		    		eventCount++;
                        /*
                    	toRemove.add(event);
                        getLogger().info("CleanUp Service: Cleaning event id=" + ((RefEntity)event).getShortId().toString());
                        */
                    }
                }
            }
            
            catch (RaplaException e)
            {
                getLogger().info("Cleanup Service: Could not remove old event " + e.getMessage());
            }
            
            // remove end of life resources
            int resourceCount = 0;    
        	try
            {
            	Allocatable[] allocatables = getQuery().getAllocatables(null);
            	
                //List toRemove = new ArrayList();
                for ( int i=0;i< allocatables.length;i++)
                {
                	Classification classification = allocatables[i].getClassification();
            		Allocatable alloc = allocatables[i];
                	try {

                		AttributeType type = checkClassifiableType(classification);
    		    		if(type == null)
    		    			continue;
    		    		else
    		    			if (type == AttributeType.BOOLEAN) // Manual delete 
    		    				continue;
    		    			else
    		    				if (type == AttributeType.DATE) { // @ specific date
    		    					Object endlife = classification.getValue("_endoflife");
    		    					if ( endlife != null && isOlderThan( endlife, getClientFacade().today()) ) {
    		    						
    		    						getLogger().info("CleanUp Service: Cleaning alloc id=" + ((RefEntity)alloc).getShortId().toString() + " EOL=" + sdfdatetime.format(endlife));
    		    						getClientFacade().remove( alloc );
    		    						resourceCount++;
    		    					}
    		    				}
                	}
                    catch (NoSuchElementException e)
                    {
                    	continue;
                    }     
                    catch (RaplaException e)
                    {
                        getLogger().info("CleanUp Service: Faild to cleanup resource: "
                        		         + e.getMessage()
                        		         + ": "
                        		         + alloc.getName(getLocale())
                        		         );
                    }
                }
            }
            catch (RaplaException e)
            {
                getLogger().error("Cleanup Service: Could not cleanup old resources ", e);
                return;
            }
            getLogger().info("CleanUp Service: Cleaned " + eventCount + " end of life events.");
            getLogger().info("CleanUp Service: Cleaned " + resourceCount + " end of life resources.");
// BJO 00000057
            getLogger().info("CleanUp Service: Ended");
// BJO 00000057
        }

    private boolean isOlderThan( Reservation event, Date maxAllowedDate )
    {
        Appointment[] appointments = event.getAppointments();
	    for ( int i=0;i<appointments.length;i++) {
            Appointment appointment = appointments[i];
            
            Date end = appointment.getMaxEnd();
            if ( end == null )
            	return false;
            
            Date start = appointment.getStart();
            if ( start.after( maxAllowedDate)) {
                // System.out.println("Start after Max: " + maxAllowedDate + "Start: " + start);
            	return false;
            }
            
            if ( end.after( maxAllowedDate)) {
                //System.out.println("End after Max: " + maxAllowedDate + " End: " + end);
            	return false;
            }
		}
        return true;
    }

    private boolean isOlderThan( Object endLife, Date maxAllowedDate )
    {
    	if ( endLife == null ){
        	return false;
    	}
    	
        if ( ((Date) endLife).after( maxAllowedDate)) {
        	return false;
        }
        return true;
    }
    
    /** We check if an attribute with the _endoflife exists 
     *  and look if it is set to manual if boolean type is used
     *  or at a specific date if a date is used 
     **/
    
    private AttributeType checkClassifiableType(Classification classification) {
    	if ( classification == null)
            return null;
        final Attribute attribute = classification.getType().getAttribute("_endoflife");
        if(attribute == null)
        	return null;
        AttributeType  type = attribute.getType();
		if (type == AttributeType.BOOLEAN || type == AttributeType.DATE)
			return type;
		else
			return null;
    }
}
}
