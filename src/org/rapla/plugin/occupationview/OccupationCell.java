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

import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;

public class OccupationCell {
	
		// '<' not midnight border '[' midnight border
		char leftBound;
		// '>' not midnight border ']' midnight border
		char rightBound;

		//  3 : Conflict
		public final static int CONFLICT  =  3; 
		//  2 : Exception
		public final static int EXCEPTION =  2; 
		//  1 : Occupied
		public final static int OCCUPIED  =  1;
		//  0 : Free Cell
		public final static int FREE      =  0;		
		// -1 : Forbidden Cell Resource is not available at all, out of order or in maintenance
		public final static int FORBIDDEN = -1;
		// -2 : FirstFit Cell
		public final static int FIRSTFIT  = -2;
		// -3 : Filtered event
		public static final int FILTERED  = -3;		
		private int typeId;
		Object object;
		
		public Appointment getAppointment() {
			if( object instanceof Reservation)
				return null;
            return (Appointment) object;
        }
		
		public Reservation getReservation() {
            return (Reservation) object;
        }

        public OccupationCell(char leftBound, int typeId, char rightBound) 
		{
		    this(leftBound, typeId, rightBound, null);
		}
		public OccupationCell(char leftBound, int typeId, char rightBound, Object object) {
			this.leftBound = leftBound;
			this.typeId = typeId;
			this.rightBound = rightBound;
			this.object = object;
		}
		    

        public int getTypeId() {
            return typeId;
        }

        public void setTypeId(int type) {
            typeId = type;
        }

		public void setObject(Appointment object) {
			this.object = object;
		}
}