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
import org.rapla.entities.domain.RepeatingEnding;
import org.rapla.entities.domain.RepeatingType;


/** This class contains the default user preferences for creation of reservations and appointment */
//BJO 00000120
public interface ReservationOptions {
    public static final String ROLE = ReservationOptions.class.getName();

    boolean isInfiniteRepeating();
	boolean isNtimesRepeating();
	boolean isUntilRepeating();
	int getnTimes();
	RepeatingEnding getRepeatingDuration();
	RepeatingType getRepeatingType();
	String getEventType();
	int getRepeating();
	//int getSplitTimeMinutes();
}