package org.rapla.plugin.occupationview;

import java.util.Date;

import org.rapla.entities.domain.Reservation;

class Interval {
Date start;
Date end;
Reservation reservation;

	public Interval(Date start,Date end, Reservation reservation) {
		this.start = start;
		this.end = end;
		this.reservation = reservation;
	}
	
	public Reservation getReservation() {
		return reservation;
	}

	public Date getIntervalStart() {
		return start;
	}

	public Date getIntervalEnd() {
		return end;
	}
}