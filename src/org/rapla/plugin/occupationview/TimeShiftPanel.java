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

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.calendar.RaplaArrowButton;
import org.rapla.components.calendar.RaplaCalendar;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.calendar.RaplaTime;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.DateTools;
import org.rapla.components.util.DateTools.DateWithoutTimezone;
import org.rapla.components.util.DateTools.TimeWithoutTimezone;
import org.rapla.facade.CalendarModel;
import org.rapla.framework.Disposable;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RaplaWidget;

public class TimeShiftPanel extends RaplaGUIComponent implements Disposable, RaplaWidget
{
    Collection listenerList = new ArrayList();

    JPanel panel = new JPanel();
    JButton prevButton = null;

    RaplaCalendar dateSelection;
    JButton nextButton = null;

    int incrementSize = Calendar.WEEK_OF_YEAR;
    int timeShifts = 0;
    CalendarModel model;
    Listener listener = new Listener();

    JLabel timeShiftTimesLabel = new JLabel();
    RaplaNumber timeShiftTimes;
    JLabel startTimeLabel = new JLabel();
    RaplaTime startTime;
    JLabel endTimeLabel = new JLabel();
    RaplaTime endTime;
    RaplaNumber freeSlot;
    JLabel freeSlotLabel = new JLabel();
    int duration = 86400000;  
    JButton todayButton= new RaplaButton(getString("today"), RaplaButton.SMALL);
// BJO 00000101
    boolean isStartDayFirstDay = false;
// BJO 00000101
    JCheckBox compactField = new JCheckBox();
    
    public TimeShiftPanel(RaplaContext sm, CalendarModel model) throws RaplaException {
        super( sm );
        setChildBundleName( OccupationPlugin.RESOURCE_FILE);
        this.model = model;

        dateSelection = createRaplaCalendar();
// BJO 00000101
        ///dateSelection.setDate(setStartOfMonth(dateSelection.getDate()));
// BJO 00000101
        double pre = TableLayout.PREFERRED;
        //double fill = TableLayout.FILL;
        // columns 10, rows = 3
        double[][] sizes = {{0.02,pre,5,pre,2,pre,0.02,pre,5,0.02}
                            ,{/*0.5,*/pre/*,0.5*/}};
        TableLayout tableLayout = new TableLayout(sizes);
        JPanel calendarPanel = new JPanel();
        Border blackline = BorderFactory.createLineBorder(Color.black);
        
        TitledBorder dateBorder = BorderFactory.createTitledBorder(blackline,getI18n().getString("date")); 
        calendarPanel.setBorder(dateBorder);

        timeShiftTimes = new RaplaNumber(new Double(1),new Double(1),new Double(12), false);
        prevButton = new RaplaArrowButton('<', 28)
        {
            private static final long serialVersionUID = 1L;
            public String getToolTipText(MouseEvent e) {
                return getI18n().format("month","-",timeShiftTimes.getNumber().intValue());
            }
            
            public Point getToolTipLocation(MouseEvent event) {
                return new Point((event.getX()), (event.getY() + 20));
              }
        };
        
        prevButton.setToolTipText(""); // needed to activate tooltip
        
        nextButton = new RaplaArrowButton('>', 28)
        {
            private static final long serialVersionUID = 1L;
            
            public String getToolTipText(MouseEvent e) {
                
                return getI18n().format("month","+",timeShiftTimes.getNumber().intValue());
            }
            
            public Point getToolTipLocation(MouseEvent event) {
                return new Point((event.getX()), (event.getY() + 20));
              }
        };
        
        nextButton. setToolTipText(""); // needed to activate tooltip

        panel.setLayout(tableLayout);
        calendarPanel.add(dateSelection);
        calendarPanel.add(todayButton);
        calendarPanel.add(prevButton);
        calendarPanel.add(nextButton);
        panel.add(calendarPanel, "1, 0");
       
        startTimeLabel.setText(getString("start_time"));
        calendarPanel.add( startTimeLabel );
        Locale locale = getRaplaLocale().getLocale();
		TimeZone timeZone = getRaplaLocale().getTimeZone();
		startTime = new RaplaTime(locale, timeZone);
        startTime.setRowsPerHour(1);
        startTime.setTime(00,00);
        calendarPanel.add(startTime);
        startTime.addDateChangeListener(listener);

        endTimeLabel.setText(getString("end_time"));
        calendarPanel.add( endTimeLabel );
        endTime =  new RaplaTime( locale, timeZone);
        endTime.setRowsPerHour(1);
        endTime.setTime(00,00);
        calendarPanel.add(endTime);
        endTime.addDateChangeListener(listener);
        
        JPanel optionsPanel = new JPanel();
        TitledBorder optionsBorder = BorderFactory.createTitledBorder(blackline,getString("options.timeshift"));
        optionsPanel.setBorder(optionsBorder);
        panel.add(optionsPanel,"7,0");
        
        // columns = 7, rows = 2
        optionsPanel.setLayout( new TableLayout(new double[][] {{ pre, 5, pre, 5 , pre, 5, pre, 5, pre, 5, pre }, {10, pre }}));
        
        timeShiftTimesLabel.setText(getString("horizon"));
        optionsPanel.add(timeShiftTimesLabel,"0,1,l,f");
        
        optionsPanel.add(timeShiftTimes,"2,1,f,f");
        timeShiftTimes.addChangeListener(listener);
        timeShifts = getQuery().getPreferences(getUser()).getEntryAsInteger( OccupationOption.MONTHS,1);
        timeShiftTimes.setNumber(timeShifts);
                
        freeSlotLabel.setText(getString("free"));
        optionsPanel.add(freeSlotLabel,"4,1,l,f");
        freeSlot = new RaplaNumber(new Double(0),new Double(0),new Double(99), false);
        optionsPanel.add(freeSlot,"6,1,f,f");
        freeSlot.addChangeListener(listener);
        nextButton.addActionListener( listener );
        prevButton.addActionListener( listener );
        
        dateSelection.addDateChangeListener( listener );
        todayButton.addActionListener(listener);
        
        optionsPanel.add( new JLabel(getString("compact")),"8,1,f,f"  );
        optionsPanel.add( compactField,"10,1,f,f");
        compactField.addActionListener( listener );
        compactField.setSelected( getCalendarOptions().isCompactColumns() );

        update(true);
    }

    boolean listenersEnabled = true;
    public void update(boolean init) throws RaplaException
    {
        listenersEnabled = false;
        try {
            if ( model.getSelectedDate() == null) {
                model.setSelectedDate( getQuery().today());
            }
            Date date = model.getSelectedDate();
            String startDay = getQuery().getPreferences(getUser()).getEntryAsString( OccupationOption.START_DAY,OccupationOption.NOSELECTION);
            isStartDayFirstDay = startDay.equals(OccupationOption.FIRSTDAY) ? true : false;
            dateSelection.setDate( setStartOfMonth(date));  
            if(init) {
            	int timeShifts = getQuery().getPreferences(getUser()).getEntryAsInteger( OccupationOption.MONTHS,1);
            	if(timeShifts == 0)
            		timeShifts = 1;
            	timeShiftTimes.setNumber(timeShifts);
            	compactField.setSelected( getCalendarOptions().isCompactColumns() );
            }

        } finally {
            listenersEnabled = true;
        }
    }

    public void dispose() {
    }

    public void setNavigationVisible( boolean enable) {
        nextButton.setVisible( enable);
        prevButton.setVisible( enable);
    }

    /** possible values are Calendar.DATE, Calendar.WEEK_OF_YEAR, Calendar.MONTH and Calendar.YEAR.
        Default is Calendar.WEEK_OF_YEAR.
     */
    public void setIncrementSize(int incrementSize) {
        this.incrementSize = incrementSize;
    }

    /** registers new DateChangeListener for this component.
     *  An DateChangeEvent will be fired to every registered DateChangeListener
     *  when the a different date is selected.
     * @see DateChangeListener
     * @see DateChangeEvent
    */
    public void addDateChangeListener(DateChangeListener listener) {
        listenerList.add(listener);
    }

    /** removes a listener from this component.*/
    public void removeDateChangeListener(DateChangeListener listener) {
        listenerList.remove(listener);
    }

    public DateChangeListener[] getDateChangeListeners() {
        return (DateChangeListener[])listenerList.toArray(new DateChangeListener[]{});
    }

    /** An ActionEvent will be fired to every registered ActionListener
     *  when the a different date is selected.
    */
    protected void fireDateChange(Date date) {
        if (listenerList.size() == 0)
            return;
        DateChangeListener[] listeners = getDateChangeListeners();
        DateChangeEvent evt = new DateChangeEvent(this,date);
        for (int i = 0;i<listeners.length;i++) {
            listeners[i].dateChanged(evt);
        }
    }

    public JComponent getComponent() {
        return panel;
    }


    public boolean getCompact() {
        return compactField.isSelected();
    }

    public int getFreeSlot() {
        return freeSlot.getNumber().intValue();
    }

    public Date getSelectedStartTime() {
        return DateTools.toDateTime( dateSelection.getDate(), startTime.getTime()); 
    }

    public int getMonths() {
    	return timeShiftTimes.getNumber().intValue();
    }
    
    public Date getSelectedEndTime() {
		Date date = dateSelection.getDate();
    	return new Date( date.getTime() + duration);
    }
    
    private Integer getTimeMs(Date time)
    {
        final TimeWithoutTimezone time2 = DateTools.toTime(time.getTime());
        return (( time2.hour * 60 + time2.minute) * 60 + time2.second) * 1000 + time2.milliseconds ;
    }
    
	public Date setStartOfMonth(Date startDate) {
//	    Calendar startCal = Calendar.getInstance();
//        startCal.setTime(startDate);
	    if(isStartDayFirstDay) {
	    	final DateWithoutTimezone date = DateTools.toDate( startDate.getTime());
	    	final long date2 = DateTools.toDate( date.year, date.month, 1);
            return new Date(date2);
	    }
	    else
	    {
	        return startDate;
	    }
	    // do not set model startDate and endDate as it influences other views.
	    /*
	    model.setStartDate(startCal.getTime());
        Calendar endCal = (Calendar) startCal.clone();
	    endCal.add(Calendar.MONTH, getMonths() - 1);
	    endCal.set(Caleatendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
	    model.setEndDate(endCal.getTime());
	    */
	    //return startCal.getTime();
	}


    class Listener implements ActionListener, DateChangeListener, ChangeListener {

        public void actionPerformed(ActionEvent evt) {
            if (!listenersEnabled)
                return;
            
            Date date;
            try {
	            listenersEnabled = false;
	            
	            Date calendar = getClientFacade().today();
	            final Date date2 = dateSelection.getDate();
                //calendar.setTime(date2);
	            final int intValue = timeShiftTimes.getNumber().intValue();
                if (evt.getSource() == prevButton)
                {
                    calendar = addIncrement(calendar, -intValue);
                }
                else
	            	if (evt.getSource() == nextButton)
	            	    calendar = addIncrement( calendar,intValue);
	            	else
	            		if (evt.getSource() == todayButton)
	            			calendar = getClientFacade().today();
		            	else 
		            		if (evt.getSource() == compactField)
		            			;
	            date = calendar;	
	            fireDateChange( date);
            } finally {
                listenersEnabled = true;
            }
            
            dateSelection.setDate(date);
        }

        private Date addIncrement(Date date, int intValue)
        {
            if ( incrementSize == Calendar.MONTH)
            {
                return date = DateTools.addMonths( date, intValue);
            }
            else if ( incrementSize == Calendar.YEAR)
            {
                return date = DateTools.addYears( date, intValue);
            }
            else if ( incrementSize == Calendar.DATE)
            {
                return date = DateTools.addDays( date, intValue);
            }
            else if ( incrementSize == Calendar.WEEK_OF_YEAR)
            {
                return date = DateTools.addWeeks( date, intValue);
            }
            return date;
        }

        public void dateChanged(DateChangeEvent evt) {
            if ( !listenersEnabled)
                return;

        	try {
        		listenersEnabled = false;
        		
            	if (evt.getSource() == nextButton) {
            		Date date = evt.getDate();
                    updateDates(date);
            	}
            	else
	            	if (evt.getSource() == prevButton) {
	            		Date date = evt.getDate();
	            		updateDates(date);
	            	}
	            	else
		        		if (evt.getSource() == dateSelection) {
		        			Date date = evt.getDate();
		                    updateDates(date);
		            	}
		        		else
			            	if (evt.getSource() == startTime) {
			            		Date date = dateSelection.getDate();
			            		long newEnd   = getTimeMs(endTime.getTime());
			            		long newStart = getTimeMs(startTime.getTime());
			            		if (newEnd == 0) {
			            			if(newStart >= 0) {
			            				updateDates(date);
			            			}
			            		}
			            		else
			            			if( newStart < newEnd) {
			            				updateDates(date);
			            			}
			            	}
			            	else
				            	if (evt.getSource() == endTime) {
				            		Date date = dateSelection.getDate();
				            		long newEnd   = getTimeMs(endTime.getTime());
				            		long newStart = getTimeMs(startTime.getTime());
				            		if (newStart < newEnd) {
			            				updateDates(date);
			            			}
				            	}
            					//System.out.println("Unknown event: " + evt.toString());
            } finally {
                listenersEnabled = true;
            }
        }

        private void updateDates(Date date) { 	
            model.setSelectedDate( date );
            
        	duration = getTimeMs(endTime.getTime()); // 24:00 duration = 0
        	if(duration == 0)
        		duration += 86400000;
        	duration -= getTimeMs(startTime.getTime()); 
        	//System.out.println("Start=" + date.toString() + " Duration="+duration);
            fireDateChange( date ); 
        }

		public void stateChanged(ChangeEvent e) {
            Calendar calendar = getRaplaLocale().createCalendar();
            calendar.setTime(dateSelection.getDate());
            Date date = calendar.getTime();
            fireDateChange( date);   
		}
    }
}