/*--------------------------------------------------------------------------*
 | Copyright (C) 2011 Bob Jordaens                                          |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.occupationview;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.RepeatingEnding;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.framework.Configuration;
import org.rapla.framework.RaplaException;
import org.rapla.framework.TypedComponentRole;

/** <strong>WARNING!!</strong> This class should not be public to the outside. Please use the interface */
public class ReservationOptionsImpl implements ReservationOptions {
    public final static TypedComponentRole<RaplaConfiguration> RESERVATION_OPTIONS= new TypedComponentRole<RaplaConfiguration>("org.rapla.reservation");

    public final static String REPEATING="repeating"; 
    public final static String NTIMES="repeating.ntimes"; 
    
    public final static String REPEATING_INFINITE="repeating.forever"; 
    public final static String REPEATING_NTIMES="repeating.n_times"; 
    public final static String REPEATING_UNTIL="repeating.end_date"; 
    
    int nTimes; 
    RepeatingEnding repeatingField;
    RepeatingType repeatingType;    
    public final static String REPEATINGTYPE="repeatingtype"; 
    public final static String EVENTTYPE = "eventtype";
    Configuration config;

    String eventType;

    public ReservationOptionsImpl(Configuration config ) throws RaplaException {
        this.config = config; 
        repeatingField = RepeatingEnding.findForString(config.getChild( REPEATING ).getValue( RepeatingEnding.END_DATE.toString() ));
        nTimes = config.getChild( NTIMES ).getValueAsInteger( 1 );

        repeatingType = RepeatingType.findForString(config.getChild( REPEATINGTYPE ).getValue( "" ));

        eventType = config.getChild( EVENTTYPE ).getValue(null);
        
    }

    public ReservationOptionsImpl() throws RaplaException {
        repeatingField = RepeatingEnding.END_DATE;
        nTimes = 1;
        repeatingType = RepeatingType.WEEKLY;
        eventType = null;
    }

    public Configuration getConfig() {
        return config;
    }


    @Override
	public RepeatingEnding getRepeatingDuration() {
        return repeatingField;
    }

    @Override
	public boolean isInfiniteRepeating() {
        return repeatingField.equals( RepeatingEnding.FOREVEVER );
    }
    
    @Override
	public boolean isUntilRepeating() {
        return repeatingField.equals( RepeatingEnding.END_DATE );
    }
    
    public boolean isNTimesRepeating() {
        return repeatingField.equals( RepeatingEnding.N_TIMES );
    }

    @Override
	public boolean isNtimesRepeating() {
    	boolean rt=false;
        if(repeatingField.equals( RepeatingEnding.FOREVEVER ))
        		rt = false;
        	else if(repeatingField.equals( RepeatingEnding.N_TIMES ))
    					rt = true;
        			else if(repeatingField.equals( RepeatingEnding.END_DATE ))
        				rt = true;
        return rt;
    }
   
	@Override
	public int getnTimes() {
		return nTimes;
	}

	@Override
	public RepeatingType getRepeatingType() {
		return repeatingType;
	}
	
	@Override
	public int getRepeating() {
		if( isUntilRepeating())
			return 0;
		else if(isNTimesRepeating())
				return getnTimes();
			 else if(isInfiniteRepeating())
			 	return -1;
		return 1; // 1-time 
	}
	
    @Override
	public String getEventType() {
        return eventType;
    }
}
