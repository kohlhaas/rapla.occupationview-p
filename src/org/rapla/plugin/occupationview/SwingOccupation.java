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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.rapla.client.ClientService;
import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.calendar.RaplaTime;
import org.rapla.components.tablesorter.TableSorter;
import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
import org.rapla.entities.CategoryAnnotations;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.User;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.CalendarModel;
import org.rapla.facade.CalendarSelectionModel;
import org.rapla.facade.QueryModule;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.SwingCalendarView;
import org.rapla.gui.internal.action.AppointmentAction;
import org.rapla.gui.toolkit.AWTColorUtil;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaFrame;


public class SwingOccupation extends RaplaGUIComponent implements SwingCalendarView, Printable
{

	OccupationTableModel occupationTableModel;
    JTable table;
    final CalendarSelectionModel model;
    TimeShiftPanel timeShift;
    JComponent container;

    boolean checkRestrictions=false;
    Appointment[] appointments;
    //Map<Appointment, Set<Allocatable>> appointmentMap = new HashMap();
    User user;
    Reservation mutableReservation;

    int calendarShift = Calendar.MONTH;
    JPopupMenu popupMenu = new JPopupMenu();
    List<Allocatable> allocatableList=null;
   
    boolean coloredEvents = false;
    
    String eventType=null;
    
    AllocatableCellRenderer alcRenderer = new AllocatableCellRenderer();
    Locale locale = getLocale();
    int cleanupAge = getQuery().getPreferences( null ).getEntryAsInteger(CleanUpOption.CLEANUP_AGE, 32);
	private DecimalFormat formatDaysInOut = new DecimalFormat("#");
	Date today = getQuery().today();
	int columnCount = 0;
    QueryModule qry = getQuery();
	 
    TableSorter  sorter;
   	private boolean isReadOnlyUser = true;

	boolean isTableEditableTable=true;	
    Set excludeDays = null;
    Reservation selectedInterval;
    
    RaplaLocale raplaLocale = getRaplaLocale();
    TimeZone timezone = raplaLocale.getTimeZone();
    Calendar calendarDS = raplaLocale.createCalendar();
    Calendar calendarDE = raplaLocale.createCalendar();

    public SwingOccupation( RaplaContext context, final CalendarModel model, final boolean editable ) throws RaplaException
    {
        super( context ); 
        setChildBundleName( OccupationPlugin.RESOURCE_FILE);
        isTableEditableTable = editable;
        isReadOnlyUser = (!canCreateReservation());
        table = new JTable()
        {
            private static final long serialVersionUID = 1L;
            
            public String getToolTipText(MouseEvent evt) {
                if (!editable)
                    return null;
                
                int r = rowAtPoint( evt.getPoint() );
                int c = columnAtPoint( evt.getPoint() );
                if(c<OccupationTableModel.CALENDAR_EVENTS) // no menu on fixed columns
                	return null;
                               	
                Object value = occupationTableModel.getValueAt(r, c);
                if(value instanceof OccupationCell) {
                	OccupationCell occCell = (OccupationCell) value;
                	final Appointment appointment = occCell.getAppointment(); 
                    if(occCell.getTypeId() == OccupationCell.OCCUPIED || occCell.getTypeId() == OccupationCell.CONFLICT) {
                        if(appointment != null) 
                        	return getInfoFactory().getToolTip( appointment );
		        	}
                	if(occCell.getTypeId() == OccupationCell.FILTERED) {
							return getString( "not_selected.help" );
					}
                	if(occCell.getTypeId() == OccupationCell.FORBIDDEN) {
							return getString("forbidden");
                	}
		            if(occCell.getTypeId() == OccupationCell.FREE) {
		                if(excludeDays.size() != 0)
		                	return getString("excludedaysactive");
		                else
							return getString("free");
					}
                }
                return null;
            }
            /*
            public Point getToolTipLocation(MouseEvent evt) {

            	return new Point(getRowHeight(), rowAtPoint( evt.getPoint() ) * getRowHeight());

              }
              */
        };
               		
		TableCellRenderer	renderer = new OccupationTableCellRenderer();
			    
		table.setDefaultRenderer( Object.class, renderer );
        container = new JScrollPane( table);
        if ( editable )
        {
            container.setPreferredSize( new Dimension(600,800));
            PopupTableHandler popupHandler = new PopupTableHandler();
            container.addMouseListener( popupHandler);;
            table.addMouseListener( popupHandler );
        }
        else
        {
        	Dimension size = table.getPreferredSize();
            container.setBounds( 0,0,(int)6000, (int)size.getHeight());
            PopupTableHandler popupHandler = new PopupTableHandler();
            container.addMouseListener( popupHandler);;
            table.addMouseListener( popupHandler );
        }
        this.model = (CalendarSelectionModel) model;
       
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(true);
        table.getTableHeader().setReorderingAllowed(false); // no column reordering 
        table.setColumnModel(new GroupableTableColumnModel());
        table.setTableHeader(new GroupableTableHeader((GroupableTableColumnModel)table.getColumnModel()));
        table.setRowHeight(25);   
        //table.getColumnModel().getColumn( int column ).setWidth( int width );  
       
        MouseMotionAdapter mma;
        mma = new MouseMotionAdapter ()
        {
          public void mouseMoved (MouseEvent e)
          {
            Point p = e.getPoint ();
            if (table.columnAtPoint (p) ==table.getSelectedColumn() &&  table.rowAtPoint (p) == table.getSelectedRow())
              table.setCursor (Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            else
              table.setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        };

        table.addMouseMotionListener (mma);

        //model.setStartDate(null); // null = today()
        timeShift = new TimeShiftPanel( context, model);
        timeShift.setIncrementSize(calendarShift); // increment 1 Month 
        this.user = getUser();
        
        timeShift.addDateChangeListener( new DateChangeListener() {
            public void dateChanged( DateChangeEvent evt )
            {
                try {
                		update(  );
                } catch (RaplaException ex ){
                    showException( ex, getComponent());
                }
            }
        });
        
        update();
    }

    
    public void update() throws RaplaException
    {
    	RaplaFrame frame = (RaplaFrame) getService( ClientService.MAIN_COMPONENT );
    	try {			
    			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    			//long start = System.currentTimeMillis();   			 
    			updateIt();
   			 	//System.out.println("1- time taken : " + (System.currentTimeMillis() - start) + " msec.");
    		} finally {
    			frame.setCursor(Cursor.getDefaultCursor());
    		}
    }
   
    public void updateIt() throws RaplaException
    {  
		((JScrollPane)container).getVerticalScrollBar().setValue(0);
		((JScrollPane)container).getHorizontalScrollBar().setValue(0);
        // get default user preferences from user profile
    	
        excludeDays = getCalendarOptions().getExcludeDays();  	
    	eventType = getReservationOptions().getEventType();

        try {
        	getClientFacade().getDynamicType(eventType);
        } catch (EntityNotFoundException ex) {
        	eventType = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION)[0].getElementKey();
        }	
     	
    	timeShift.update(false);
    	int months = timeShift.getMonths();
        
        ClassificationFilter[] cfilters = model.getReservationFilter();
        allocatableList = getAllAllocatables();
        Collections.sort(allocatableList, new AllocatableSortByName());
        int rowCount = allocatableList.size();
        if(rowCount == 0)
        	return;
        Iterator<Allocatable> it = allocatableList.iterator();
        calendarDS = timeShift.getSelectedStartTime(); // start midnight
        calendarDE = timeShift.getSelectedEndTime(); // end midnight

        Date startTime = calendarDS.getTime();
		//Date endTime = calendarDE.getTime();
		//String startString = raplaLocale.formatDateShort(startTime) +" "+ raplaLocale.formatTime(startTime);
		//String endString = raplaLocale.formatDateShort(endTime) +" "+ raplaLocale.formatTime(endTime);
		//getLogger().info("Selected - Start: " +         startString  + " Stop: " + endString);

        // calculate number of columns required to display from calendar
        Calendar calendarTmp = (Calendar) calendarDS.clone();
        int totalDays= -1 * calendarTmp.get(Calendar.DAY_OF_MONTH) + 1;
        for ( int i = 0; i< months; i++) {
        	totalDays += calendarTmp.getActualMaximum(Calendar.DAY_OF_MONTH);
        	calendarTmp.add(calendarShift, 1);
        }
        int columnCount = totalDays + OccupationTableModel.CALENDAR_EVENTS ; // + for fixed columns
        //Object occupationTable[][] = new Object[rowCount][columnCount]; 
        occupationTableModel = new OccupationTableModel(getI18n(), rowCount, columnCount, startTime);
     
        
// Sorting block

       sorter = new TableSorter(occupationTableModel,table.getTableHeader());
       sorter.setSortable(0, false);

		sorter.setColumnComparator(1, new Comparator<AllocationCell>() {
	        public int compare(AllocationCell o1, AllocationCell o2) {
					String s1 = o1.allocatable.getName(locale);
	   				String s2 = o2.allocatable.getName(locale);
		            return s1.compareTo(s2);
	        }
	    });

		sorter.setSortable(2, false);

		sorter.setColumnComparator(3, new Comparator<Object>() {
	        public int compare(Object o1, Object o2) { 
	        	Integer i1 = (Integer) o1;
	        	Integer i2 = (Integer) o2;
	        	return i1.compareTo(i2);
	        }
	    });

		sorter.setColumnComparator(4, new Comparator<Object>() {
	        public int compare(Object o1, Object o2) { 
	        	Integer i1 = (Integer) o1;
	        	Integer i2 = (Integer) o2;
	        	return i1.compareTo(i2);
	        }
	    });

		//sorter.setSortable(4, false);
		sorter.setSortable(5, false);
		
		//table.setRowSorter(sorter);
		
        int r = 0;
        char leftBound = ' ';
        char rightBound = ' ';
        // long start0 = System.currentTimeMillis(); 
        while (it.hasNext()) { 
        	
        	// get resource data
    		Allocatable alloc = it.next();
    		AllocationCell alcCell = new AllocationCell(alloc);
    		
    		// get reservation data
            Calendar calendarTDS = (Calendar) calendarDS.clone();
            Calendar calendarTDE = (Calendar) calendarDE.clone();
			
    		occupationTableModel.setValueAt( alcCell, r, OccupationTableModel.CALENDAR_RESOURCE);
            //mutableReservation.addAllocatable(alloc);
            //String occupationType = null;
            occupationTableModel.setColumnCount(OccupationTableModel.CALENDAR_OUT_DAYS);
            //long start1 = System.currentTimeMillis(); 
            //Reservation previousReservation = null;
        	for ( int k = OccupationTableModel.CALENDAR_EVENTS ; k <= columnCount-1; k++) {
                //long start = System.currentTimeMillis(); 
        		
        		if ( excludeDays.contains(calendarTDS.get(Calendar.DAY_OF_WEEK)))  {
        			calendarTDS.add(Calendar.DATE, 1); // next startday
        			calendarTDE.add(Calendar.DATE, 1); // next endday
        			continue;
        		}
        		int c = occupationTableModel.addColumn(1);
        		sorter.setSortable(c, false);
        		if(DateTools.cutDate(today).equals(DateTools.cutDate(calendarTDS.getTime())))
        			occupationTableModel.setTodayColumn(c);
    			// Not Free
		    	Collection<Reservation> filteredReservations = null;
		        if((cfilters != null))
		        	filteredReservations = new HashSet<Reservation>(Arrays.asList(qry.getReservationsForAllocatable(new Allocatable[] { alloc },calendarTDS.getTime(),calendarTDE.getTime(), cfilters)));
    			Reservation[] reservationDay = qry.getReservationsForAllocatable(new Allocatable[] { alloc },calendarTDS.getTime(),calendarTDE.getTime(), null);
				//  A from-to will be split is days like [ [ or ] [ or ] [ or ] ] "[" is left boundary = startdate, "]" is right boundary = enddate
				//   ] and [ used in the middle.
				// System.out.println("Name: " + alloc.getName(locale) + "Start: " + calendarTDS.getTime() + " Stop: " + calendarTDE.getTime());
                Reservation reservation = null;
                Appointment app = null;
                //long start = System.currentTimeMillis(); 
        		occupationTableModel.setValueAt( new OccupationCell('N',OccupationCell.FREE,'N', null), r, c); // initialize to FREE
                for(int j=0;j < reservationDay.length;j++) {
                	reservation = reservationDay[j];
                    app = null;
                    Appointment[] apps = reservation.getAppointmentsFor(alloc);
                    for(int i=0;i < apps.length;i++) {
                    	app = apps[i];	
                    	if(!app.overlaps(calendarTDS.getTime(), calendarTDE.getTime()))
                    		continue;
	                    //System.out.println(alloc.getName(locale));
	                    //System.out.println("Start= " + app.getStart() + " TDS= " + calendarTDS.getTime());
                    	if(excludeDays.size() == 0 ) {
                    		Date minStartDate = app.getStart();
		                    if(DateTools.isSameDay(minStartDate.getTime(),calendarTDS.getTime().getTime()))
		                    	if(DateTools.isMidnight(minStartDate))
		                    		leftBound = '[';
		                    	else
		                    		leftBound = '<';
		                    else
		                    	if(DateTools.isMidnight(minStartDate))
		                    		leftBound = ']';
		                    	else
		                    		leftBound = '>';
		                    //Repeating rep = app[0].getRepeating();
		                    //if (rep != null) System.out.println("Repating= " + rep);             
		                    //System.out.println("MaxEnd = " + app.getMaxEnd() + " End= " + app.getEnd()); 
		                    Date maxendDate = app.getMaxEnd();
		            		rightBound = '[';
		                    if(maxendDate!=null)
		                    	if(DateTools.isMidnight(maxendDate)) { // endDate 00:00:00 = previous date
		                    		if(DateTools.isSameDay((DateTools.subDay(maxendDate)).getTime(),calendarTDS.getTime().getTime()))
		                    			rightBound = ']';
		                    	}
		                    	else
			                    	if(DateTools.isSameDay(maxendDate.getTime(),calendarTDS.getTime().getTime()))
			                    		rightBound = '>';	                    			           
                    	}
                    	else {
                			leftBound  = ']';
                			rightBound = '[';
                    	}	
                    	OccupationCell occCellOld = (OccupationCell) occupationTableModel.getValueAt(r, c);
                    	if( occCellOld == null || occCellOld.getTypeId() == OccupationCell.FREE) {
                    		occCellOld.leftBound = leftBound;
                    		occCellOld.rightBound = rightBound;
                    		occCellOld.setObject(app);
                    		occCellOld.setTypeId(OccupationCell.OCCUPIED);
                    		Repeating repeating = app.getRepeating();
                    		if(repeating != null)
                    			if(repeating.isException(calendarTDS.getTime().getTime()))
                    				occCellOld.setTypeId(OccupationCell.EXCEPTION);
                    		//occupationTableModel.setValueAt( occCellOld, r, c);
                    	}
                    	else {
                    		if(app.overlaps(occCellOld.getAppointment()))
                    				occCellOld.setTypeId(OccupationCell.CONFLICT);
		            		//occupationTableModel.setValueAt( occCellOld, r, c);
                    	}
                    	
                    	boolean isEventSelected = filteredReservations.contains(reservation);
    			    	if(!isEventSelected) {
    			    		occCellOld.setTypeId(OccupationCell.FILTERED);
    			    	}	                    	
	            		
	            		if(c == occupationTableModel.getTodayColumn())
	            			setDaysInOut(app, r, today);
	            		else
	            			if(c >= OccupationTableModel.CALENDAR_EVENTS)
	            				setDaysInOut(app, r, calendarTDS.getTime());
	            		
	        			//System.out.println(res[0].toString() + " Length:" + res.length);
                    }
                }
                //System.out.println("2- time taken : " + (System.currentTimeMillis() - start) + " msec.");
        		/* debug
        		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String DS = sdf.format(calendarTDS.getTime());
                String DE = sdf.format(calendarTDE.getTime());                
        		System.out.println("Row="+r+" Column=" + c + " StartDate=" + DS + " EndDate= " + DE + " Type = " + occupationType);
                */
        		calendarTDS.add(Calendar.DATE, 1); // next startday
                calendarTDE.add(Calendar.DATE, 1); // next endday
        	}
            occupationTableModel.addColumn(1); // adjust count +1
        	// Compress ?
        	if( timeShift.getCompact() ) {
	        	boolean allfree = true; 
	        	for ( int k = OccupationTableModel.CALENDAR_EVENTS ; k <= columnCount-1; k++) {
	        		//System.out.println("Count:" + columnCount + "Row:" + r + " Column:" + k);
	        		if(occupationTableModel.getValueAt(r, k) != null) {
	        			int type = ((OccupationCell) occupationTableModel.getValueAt(r, k)).getTypeId();
	        			if(type != OccupationCell.FREE && type != OccupationCell.FILTERED) {
	        				allfree = false;
	        				break;
	        			}
	        		}
	        	}
	        	
	        	if(allfree)
	        		continue; // next Allocatable
        	}
    		occupationTableModel.addRow(1);
            r++;

            //System.out.println("End Resource time taken : " + (System.currentTimeMillis() - start1) + " msec.");
        }
        // System.out.println("End All Resources time taken : " + (System.currentTimeMillis() - start0) + " msec.");
        table.setModel(  sorter );
        
		table.getColumnModel().getColumn(OccupationTableModel.CALENDAR_CHECK).setCellRenderer(alcRenderer);

        occupationTableModel.setFreeSlot(timeShift.getFreeSlot());
        //setLineNumbers();

        occupationTableModel.firstFit();

        
        // Any rows found ? Compact may have removed all rows
        //if(occupationTableModel.getRowCount() == 0) 
        	//return;
        
        TableColumnModel cm = table.getColumnModel();
    	
    	// Sequence line number
        TableColumn column = cm.getColumn(OccupationTableModel.CALENDAR_SEQUENCE_NUMBER);
        column.setPreferredWidth(30); 
        column.setMaxWidth(50);
        
        // Check column Header 
        column = cm.getColumn(OccupationTableModel.CALENDAR_RESOURCE); 
        column.setPreferredWidth(200); 
        column.setMaxWidth(300);
        
        // 
        column = cm.getColumn(OccupationTableModel.CALENDAR_CHECK); 
        column.setPreferredWidth(200); 
        column.setMaxWidth(300);

        // In column Header 
        column = cm.getColumn(OccupationTableModel.CALENDAR_IN_DAYS); 
        column.setPreferredWidth(40); 
        column.setMaxWidth(50);

        // Out column Header 
        column = cm.getColumn(OccupationTableModel.CALENDAR_OUT_DAYS); 
        column.setPreferredWidth(40);
        column.setMaxWidth(50);
        
        calendarTmp = (Calendar) calendarDS.clone();
        SimpleDateFormat sdfYYYYMM = new SimpleDateFormat("yyyy/MM",locale);
    	sdfYYYYMM.setTimeZone(DateTools.getTimeZone());
        SimpleDateFormat sdfdd = new SimpleDateFormat("dd",locale);
    	sdfdd.setTimeZone(DateTools.getTimeZone());
        SimpleDateFormat sdfww = new SimpleDateFormat("ww",locale);
    	sdfdd.setTimeZone(DateTools.getTimeZone());

        //SimpleDateFormat sdfEE  = new SimpleDateFormat("EE",locale);
    	//sdfEE.setTimeZone(DateTools.getTimeZone());
    	GroupableTableColumnModel gcm = (GroupableTableColumnModel)table.getColumnModel();
    	String oldGR=null; // old month groupHeader label
    	String newGR=null; // new month groupHeader label
    	ColumnGroup g_GR = null;
    	int k = 0 ;
    	for ( int i = OccupationTableModel.CALENDAR_EVENTS ; i <= columnCount-1; i++) {
    		
    		Date dateTmp = calendarTmp.getTime();
    		int day = calendarTmp.get(Calendar.DAY_OF_WEEK);
            
    		if ( excludeDays.contains( day ) ) {
    			calendarTmp.add(Calendar.DATE, 1); // next startday
    			k++; 
    			continue;
    		}
            if(excludeDays.size() == 0 )
                //set columnGroupHeader label == YYYY/MM (Month) 01, ... , 12
                newGR = sdfYYYYMM.format(dateTmp);
            else 
                //set columnGroupHeader label == WW (Week) 01, ... , 52
                newGR = 'w' + sdfww.format(dateTmp);

            if(!newGR.equals(oldGR)) {
            	if(oldGR!=null)
            		gcm.addColumnGroup(g_GR);
            	g_GR = new ColumnGroup(new GroupableTableCellRenderer(),newGR);
            	oldGR = newGR;
            }
            //set columnGroupHeader label == dd (Day) 01, ...,31
            ColumnGroup g_dd = new ColumnGroup(new GroupableTableCellRenderer(), sdfdd.format(dateTmp));  // daynumber
            g_GR.add(g_dd);
            
            String tag = "";
            if((i-k) == occupationTableModel.getTodayColumn())
            	tag = "ToDay";
            ColumnGroup g_dw = new ColumnGroup(new DayOfWeekHeaderRenderer(tag), Integer.toString(day)); //  day of week in words
            g_dd.add(g_dw);
            
            //System.out.println("Day= " + sdfdd.format(dateTmp) + " colCount=" + columnCount + " i= " + i +" k= " + k + "i-k=" +(i-k) + "getColCount= " + occupationTableModel.getColumnCount());
           	column = cm.getColumn(i - k);
           	g_dw.add(column);
            
            // set column sizes
            column.setMinWidth(19);
            column.setMaxWidth(26);
            column.setPreferredWidth(26);
            //set columnHeader label == Day of the week  Mo, .... , Su
            int  selectedCount = occupationTableModel.getRowCount(i - k);
            column.setHeaderValue(selectedCount);
           	column.setHeaderRenderer(new countRenderer());
            calendarTmp.add(Calendar.DATE,1);
    	}
        gcm.addColumnGroup(g_GR); // do not forget to add last group
    }
    
    public void setDaysInOut(Appointment app, int r, Date referenceDate) {

    	if(referenceDate.before(today)) {
    		occupationTableModel.setValueAt(null, r, OccupationTableModel.CALENDAR_OUT_DAYS);
    		occupationTableModel.setValueAt(null, r, OccupationTableModel.CALENDAR_IN_DAYS);
    		return;
    	}
    	else 
    		if(referenceDate.after(today))
    			if(occupationTableModel.getValueAt(r,OccupationTableModel.CALENDAR_IN_DAYS) == null)
    				return;
    	
	    int days = (int) ((app.getStart().getTime() -  today.getTime()) / DateTools.MILLISECONDS_PER_DAY);
		occupationTableModel.setValueAt( days, r, OccupationTableModel.CALENDAR_IN_DAYS);
    		
		Repeating rpt = app.getRepeating();
		Date edate = null;
		if ( rpt == null )
			edate = app.getEnd();
		else 
			if ( rpt.getEnd() != null && !rpt.isFixedNumber() ) 
				edate =  rpt.getEnd();		
			else 
				if (rpt.getEnd() != null)
					edate = rpt.getEnd(); 
		if(edate == null)
			occupationTableModel.setValueAt(null, r, OccupationTableModel.CALENDAR_OUT_DAYS);
		else {
			days = (int) ((edate.getTime() -  today.getTime() + 1) / DateTools.MILLISECONDS_PER_DAY);
			if(days> 20000)
				occupationTableModel.setValueAt( "+\u221E", r, OccupationTableModel.CALENDAR_OUT_DAYS);
			else
				occupationTableModel.setValueAt( days, r, OccupationTableModel.CALENDAR_OUT_DAYS);
		}
		return;
	}
       
    public JComponent getDateSelection()
    {
        return timeShift.getComponent();
    }

    public void scrollToStart()
    {
    	/*
    	try {
    		scrolling = true;
			//update();
			scrolling = false;
		} catch (RaplaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
    }

    public JComponent getComponent()
    {
    	return container;
    }

    private List<Allocatable> getAllAllocatables() throws RaplaException {
	    Allocatable[] allocatables = model.getSelectedAllocatables();
	    return Arrays.asList( allocatables );
	 }
       
    public class OccupationTableCellRenderer extends DefaultTableCellRenderer 
    {
		private static final long serialVersionUID = 1L;
		
		public OccupationTableCellRenderer () {
			super();
		}

		public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int r, int c) 
        {

            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, r, c);
			int row = convertRowIndexToModel(table, r);		
            super.setHorizontalAlignment(SwingConstants.CENTER);

            LinesBorder cellBorder = new LinesBorder(Color.BLACK); 
            cell.setBackground( Color.WHITE ); // full cell is painted WHITE
           
	        if( value instanceof OccupationCell ) {
				Calendar calendar = Calendar.getInstance(getRaplaLocale().getImportExportTimeZone());
				cellBorder.setLocation(r,c); // for debug purposes
	        	OccupationCell occCell = (OccupationCell) value;
	        	final Appointment appointment = occCell.getAppointment();
	        	
	        	int thickness = 2;
	        	if(occCell.getTypeId() == OccupationCell.FILTERED)
	        		thickness = 1;
	        		
	        	if( appointment != null ) {
		            cellBorder.setThickness(thickness, NORTH);
	        		if(occCell.leftBound=='[') {
	        			cellBorder.setBound(occCell.leftBound);		
	        			cellBorder.setThickness(thickness, WEST);
	        		}
	        		else 
	        			if(occCell.leftBound=='<') {
		        			cellBorder.setBound(occCell.leftBound);	
	        				calendar.setTime(getRaplaLocale().fromRaplaTime(getRaplaLocale().getImportExportTimeZone(),appointment.getStart()));
	        				cellBorder.setThickness(calendar.get(Calendar.HOUR_OF_DAY), WEST);
	        				cellBorder.setColor(Color.LIGHT_GRAY, WEST);
	        			}
	        			else
	        				cellBorder.setThickness(0, WEST);
	    	        
	        		if(occCell.rightBound==']') {
	        			cellBorder.setBound(occCell.rightBound);	
	        			cellBorder.setThickness(thickness, EAST);
	        		}
	        		else 
	        			if(occCell.rightBound=='>') {
		        			cellBorder.setBound(occCell.rightBound);
	        				calendar.setTime(getRaplaLocale().fromRaplaTime(getRaplaLocale().getImportExportTimeZone(),appointment.getEnd()));
	        				cellBorder.setThickness(24 - calendar.get(Calendar.HOUR_OF_DAY), EAST); 
    						cellBorder.setColor(Color.LIGHT_GRAY, EAST);
	        			}
	        			else 
	            			cellBorder.setThickness(0, EAST);
	        		
 	    	        cellBorder.setThickness(thickness, SOUTH);

	        		Color color = getColorForClassifiable( appointment.getReservation() );
	    	       	if(color==null)
	           			cell.setBackground( Color.WHITE );
	           		else {
	           			if(occCell.getTypeId() == OccupationCell.OCCUPIED)
	           				cell.setBackground( color);
	           			if(occCell.getTypeId() == OccupationCell.FILTERED) { 
	           				cell.setBackground( adjustColor(  AWTColorUtil.getHexForColor(color), 60 ));
	    	           	if(occCell.getTypeId() == OccupationCell.CONFLICT)
	    	           		cell.setBackground( color);

	           			}
	           		}
	       			setBorder(cellBorder);
	        	}
	           	else
		           	if( occCell.getTypeId() == OccupationCell.FREE) { // Free 
		           		cell.setBackground( Color.LIGHT_GRAY);
		            }
		           	else 
		           		if( occCell.getTypeId() == OccupationCell.FORBIDDEN) { // Forbidden
		           			cell.setBackground( Color.BLACK );	                    
		           		}
		           		else  
		           			if( occCell.getTypeId() == OccupationCell.FIRSTFIT) { // FirstFit 		    	    	
		           				cellBorder.setThickness(1, NORTH);
		           				if(occCell.leftBound=='[')
		           					cellBorder.setThickness(2, WEST);
		           				else
		           					cellBorder.setThickness(0, WEST);	        				
		           				cellBorder.setThickness(1, SOUTH);
		           				if(occCell.rightBound==']')
		           					cellBorder.setThickness(2, EAST);
		           				else
		           					cellBorder.setThickness(0, EAST);	
		           				setBorder(cellBorder);
		           				cell.setBackground( Color.DARK_GRAY);	                    
		           			}
        		setText("");
	        	if( occCell.getTypeId() == OccupationCell.FILTERED)
	        		setText("x");
	        	if( occCell.getTypeId() == OccupationCell.CONFLICT)
	        		setText("C");
	        	if( occCell.getTypeId() == OccupationCell.EXCEPTION)
	        		setText("X");
	        	// identify the selected cell
                if( c == table.getSelectedColumn() &&  r == table.getSelectedRow()) { 
                	// Selector context Popup
                	cellBorder.ResetLinesBorder(Color.BLACK,1);
       				cellBorder.setThickness(3, NORTH);
       				cellBorder.setThickness(3, WEST);
       				cellBorder.setThickness(3, SOUTH);
       				cellBorder.setThickness(3, EAST);	
                	//cell.setBackground( Color.GRAY );
       				setBorder(cellBorder);
	        	}
	        	
	        }
	        
	        if( value instanceof AllocationCell )
	        {
	        	super.setHorizontalAlignment(SwingConstants.LEFT);
	        	Font textFont = new Font("SanSerif", Font.BOLD, 15);
       			cell.setFont(textFont);
       			AllocationCell allcCell = (AllocationCell) value;
       			// handle the first column: Resources
       			Allocatable allc = allcCell.allocatable;
   				cell.setBackground( Color.WHITE);
            	setText(allc.getName(locale));
       			cell.setBackground( Color.WHITE );
	        	Color color = getColorForClassifiable( allc );
	    	    if(color!=null)
	           		cell.setBackground( color);
	       	}
			
	        if( value instanceof Integer )
	        {
	      		if( c == OccupationTableModel.CALENDAR_IN_DAYS) {
		      		int days = (Integer) value;
	      			if(days == Integer.MAX_VALUE) {
	      					setText("");
	      			}
		      		else {
		      			cell.setBackground( Color.WHITE);
			        	if(days >= 0) {
			        		formatDaysInOut.setPositivePrefix("+"); 
			        		setText(formatDaysInOut.format(days));
			        	}
			        	else {
			      			formatDaysInOut.setPositivePrefix("-"); 
			      			setText(formatDaysInOut.format(days));
			          	}
	      			}
		      	}
	      		else {
		      		if( c == OccupationTableModel.CALENDAR_OUT_DAYS ) {
		      			cell.setBackground( Color.WHITE);
			      		int days = (Integer) value;
		      			if(days == Integer.MAX_VALUE) {
	          				Object daysIn = occupationTableModel.getValueAt(row,OccupationTableModel.CALENDAR_IN_DAYS);				
      						if((Integer) daysIn == Integer.MAX_VALUE)
      							setText("");
      						else
      							setText("?");
          				}
			      		else {
				        	if(days >= 0)
				        		formatDaysInOut.setPositivePrefix("+"); 
				        	else 
				      			formatDaysInOut.setPositivePrefix("-"); 
				        	setText(formatDaysInOut.format(days));
		      			}
			      	}
	      		}
	        }
	        
      		if(c == OccupationTableModel.CALENDAR_SEQUENCE_NUMBER) {
   				Font font = cell.getFont();
   				cell.setFont(font.deriveFont(Font.BOLD));
   				AllocationCell allcCell = (AllocationCell) occupationTableModel.getValueAt(row,OccupationTableModel.CALENDAR_RESOURCE);
   				if(allcCell != null) {
   					cell.setBackground( Color.WHITE);
   					Allocatable alloc = allcCell.allocatable;
	        		AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
	        		
	        		if(type != null) {
		        		Classification classification = alloc.getClassification();
		       	    	Object endlife = classification.getValue("_endoflife");
		       	    	if( endlife != null )
		       	    		cell.setBackground( Color.RED);
		       	    	else {
		       	    		Object daysOutObject  = occupationTableModel.getValueAt(row, OccupationTableModel.CALENDAR_OUT_DAYS);
		       	    		if(daysOutObject != null) {
		       	    			int daysOut  =(Integer) daysOutObject;
		       	    			int daysIn  =(Integer) occupationTableModel.getValueAt(row, OccupationTableModel.CALENDAR_IN_DAYS);
		       	    			if(daysIn == Integer.MAX_VALUE )
		       	    				cell.setBackground( Color.GREEN);
		       	    			else
		       	    				if( daysOut != Integer.MAX_VALUE && cleanupAge + daysOut <= 0) // old enough to be archived
		       	    					cell.setBackground( Color.GREEN);
		       	    		}
		       	    	}
	        		}
   				}
   	    		setText(Integer.toString(r+1));
   	    		return cell;
   			}

	        if( value instanceof String ) {
	        	if( c == OccupationTableModel.CALENDAR_IN_DAYS || c == OccupationTableModel.CALENDAR_OUT_DAYS) {
	        		cell.setBackground( Color.WHITE);
	        		setText((String) value);
       			}
       		}
	        
	        if(value==null) {
   				cell.setBackground( Color.WHITE);
   				setText((value == null) ? "" : "Unknown"); 
   			}

	        //setText((value == null) ? "" : value.toString()); 
            return cell;
        }
    }
    
    private int convertRowIndexToModel(JTable table, int r) {
        return sorter.modelIndex(r);
    }

    private Color getColorForClassifiable( Classifiable classifiable ) {
    	Classification c = classifiable.getClassification();
        Attribute colorAttribute = c.getAttribute("color");
        String annotation = c.getType().getAnnotation(DynamicTypeAnnotations.KEY_COLORS);
        if ( annotation != null && annotation.equals( DynamicTypeAnnotations.COLORS_DISABLED))
        {
        	return null;
        }
        String color = null;
        if ( colorAttribute != null) {
            Object hexValue = c.getValue( colorAttribute );
            if ( hexValue != null) {
                if ( hexValue instanceof Category) {
                    hexValue = ((Category) hexValue).getAnnotation( CategoryAnnotations.KEY_NAME_COLOR );
                }
                if ( hexValue != null) {
                    color = hexValue.toString();
                }
            }
        }
        if ( color != null)
        {
            try
            {
                return Color.decode(color);
            }
            catch (NumberFormatException ex)
            {
                getLogger().warn( "Can't parse Color " + color  + " " +ex.getMessage());
            }
        }
        return null;
    }
    
    class GroupableTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int r, int c) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setBackground(Color.WHITE);
                Font font = getFont();
                setFont(font.deriveFont(Font.BOLD));
            }

            setHorizontalAlignment(SwingConstants.CENTER);
            setText(value != null ? value.toString() : " ");
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            return this;
        }
    }
    
	private final String dayNames[] = new DateFormatSymbols(locale).getShortWeekdays();
    class DayOfWeekHeaderRenderer extends DefaultTableCellRenderer {
    	private String tag;
		private static final long serialVersionUID = 1L;
        public DayOfWeekHeaderRenderer(String tag) {
			this.tag = tag;
		}
                               
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int r, int c) {
        	if(value instanceof String)
        	{
        		int dd = Integer.parseInt(((String) value));
        		setText((dayNames[dd]).substring(0, 2));
        		if( dd == Calendar.SUNDAY || dd == Calendar.SATURDAY)
        			setBackground(Color.LIGHT_GRAY);
        		else
                    setBackground(Color.WHITE);
                Font font = getFont();
                setFont(font.deriveFont(Font.BOLD));
        	}
        	else {
                setText(value != null ? value.toString() : " ");
        	}
        
            if(tag.equals("ToDay")) {
            	setBackground(Color.BLACK);
            	this.setForeground(Color.WHITE);
            }
            
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            return this;
        }
    }
    
    class countRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int r, int c) {

            setText(value != null ? value.toString() : " ");
            LinesBorder cellBorder = new LinesBorder(Color.GRAY);
            cellBorder.setThickness(2, NORTH);
            cellBorder.setThickness(1, WEST);
            cellBorder.setThickness(1, EAST);
            cellBorder.setThickness(0, SOUTH);
            setBorder(cellBorder);
            setBackground( Color.WHITE);
            setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    public class PopupTableHandler extends MouseAdapter {

	    void showPopup(MouseEvent me) throws RaplaException{

	        Point p = new Point(me.getX(), me.getY());
	        int r = table.getSelectedRow();
	        int c = table.getSelectedColumn();
	        if( r  < 0 || c < 0)
	        	return;
	        r = convertRowIndexToModel(table,r);

	        Object obj = occupationTableModel.getValueAt(r, c);
	        if (obj instanceof OccupationCell ) {
	        	OccupationCell occCell = (OccupationCell) obj;
		        if(occCell.getTypeId() == OccupationCell.OCCUPIED || occCell.getTypeId() == OccupationCell.CONFLICT) 
		        	editPopup(occCell, r, c, p);
		        if(occCell.getTypeId() == OccupationCell.FREE || occCell.getTypeId() == OccupationCell.FIRSTFIT) 
		        	newPopup(occCell, r, c, p);
		        }
	        
	        if (obj instanceof AllocationCell ) {
	        		AllocationCell alcCell = (AllocationCell) obj;
	        		if(alcCell.allocatable !=null ) {
	        			Object daysOut = occupationTableModel.getValueAt(r, OccupationTableModel.CALENDAR_OUT_DAYS);
	        			Object daysIn  = occupationTableModel.getValueAt(r, OccupationTableModel.CALENDAR_IN_DAYS);
	        			if(daysOut != null) {
	        					if( ((Integer) daysOut <= 0)) 
	        						archivePopup(alcCell, r, c, p,(Integer) daysOut);
	        			}
	        			if(daysIn == null) 
	        				archivePopup(alcCell, r, c, p, -cleanupAge);
	        		}	
	        }
	        return;
	    }
	    
		public void newPopup(OccupationCell occCell, int r, int c, Point point) throws RaplaException { 
			if( occCell.getTypeId() == OccupationCell.FORBIDDEN) 
				return;

			if(!isTableEditableTable || isReadOnlyUser || excludeDays.size() != 0)
				return;
			
	        Calendar calendarStart = null;
	        Calendar calendarEnd = null;

	        if( occCell.getTypeId() == OccupationCell.FIRSTFIT) { 
	        	int cs = occupationTableModel.findStartSlot(r, c, -2) - OccupationTableModel.CALENDAR_EVENTS; // corrected start
				calendarStart = (Calendar) calendarDS.clone();
				calendarStart.add(Calendar.DATE, cs);
				calendarEnd = (Calendar) calendarStart.clone();
	        	calendarEnd.add(Calendar.DATE, occupationTableModel.getFreeSlot());
			} else 
				if( occCell.getTypeId() == OccupationCell.FREE) { // Free Cell
					calendarStart = (Calendar) calendarDS.clone();
	        		calendarStart.add(Calendar.DATE, c - OccupationTableModel.CALENDAR_EVENTS);
					calendarEnd = (Calendar) calendarStart.clone();
					calendarEnd.add(Calendar.DATE, getReservationOptions().getnTimes() - 1);
				}
  
	        JPopupMenu popup = new JPopupMenu();
	        newAdapter menuAction = new newAdapter(null,null, point);
	        JMenuItem newItem = new JMenuItem(getString("move_in"),getIcon( "icon.new"));
	        newItem.setActionCommand("new");
	        newItem.addActionListener(menuAction);
	        popup.add(newItem);
	        popup.show( table, point.x, point.y); 
	    }

		SimpleDateFormat sdfdatetime = new SimpleDateFormat("yyyy-MM-dd");
		public void archivePopup(AllocationCell alcCell, int r, int c, Point point, int days) throws RaplaException {    
	    	Allocatable alloc = alcCell.allocatable; 
	    	
    		AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
    		if(type == null)
				return;

	        JPopupMenu popup = new JPopupMenu();
	        Date cleanupDate = today;
	        JMenuItem archiveItem;
	        if(canModify(alloc)) {
	        	if (type.equals(AttributeType.BOOLEAN))
	        		archiveItem = new JMenuItem(getI18n().getString("archive_yn"),getIcon( "icon.archive"));
	        	else {
			        if((cleanupAge + days) > 0) {
			        	cleanupDate = DateTools.addDays(today, (int) (cleanupAge + days));
			        	archiveItem = new JMenuItem(getI18n().format( 
			        												  "forcearchive_lt"
			        												, new Object [] { 
			        														          sdfdatetime.format(cleanupDate)
			        								 				                , Math.abs(days)
			        								 				                , cleanupAge
			        															    }
			        												)
			        												, getIcon( "icon.archive")
			        												);
			            archiveItem.setBackground(Color.ORANGE);
			        }	 
			        else {
		        		archiveItem = new JMenuItem(getI18n().format(
		        													  "archive_gt"
		        													, new Object [] {
		        																	  sdfdatetime.format(cleanupDate)
		        													                , cleanupAge
		        																    }
		        													)
		        													,getIcon( "icon.archive")
		        													);
			        	archiveItem.setBackground(Color.GREEN);
			        }
			   }
			   archiveItem.setEnabled(true);
		       archiveItem.setActionCommand("archive");
	        }
	        else {
	        	 archiveItem = new JMenuItem(getString("permission.denied"),getIcon("icon.no_perm"));
	 	         archiveItem.setEnabled(true);
	        }
		    newAdapter menuAction = new newAdapter(alloc, cleanupDate, point);
	        archiveItem.addActionListener(menuAction);
	        popup.add(archiveItem);
	        popup.show( table, point.x, point.y); 
	    }
				    
	    public void editPopup(OccupationCell occCell, int r, int c, Point point) throws RaplaException {  
	    	Appointment appointment = occCell.getAppointment();
       		Reservation reservation = appointment.getReservation();
		    JPopupMenu popup = new JPopupMenu();
		    newAdapter menuActionAppointment = new newAdapter(appointment, null, point);
		    newAdapter menuActionReservation = new newAdapter(reservation, null, point);

			if(isTableEditableTable && !isReadOnlyUser) {
		        JMenuItem editItem = new JMenuItem(getString("edit"),getIcon( "icon.edit"));
			    editItem.setActionCommand("edit");
			    editItem.addActionListener(menuActionAppointment);
			    editItem.setEnabled(canModify(reservation) || getQuery().canExchangeAllocatables(reservation));
			    popup.add(editItem);

		        JMenuItem splitItem = new JMenuItem(getString("move_internal"),getIcon( "icon.split"));
		        splitItem.setActionCommand("split");
		        splitItem.addActionListener(menuActionAppointment);
		        splitItem.setEnabled(canModify(reservation) || getQuery().canExchangeAllocatables(reservation));
			    popup.add(splitItem);

		        JMenuItem endItem = new JMenuItem(getString("move_out"),getIcon( "icon.exit"));
		        endItem.setActionCommand("end");
		        endItem.addActionListener(menuActionAppointment);
		        endItem.setEnabled(canModify(reservation) || getQuery().canExchangeAllocatables(reservation));
			    popup.add(endItem);
 				
			    JMenuItem deleteItem = new JMenuItem(getString("delete"),getIcon( "icon.delete"));
			    deleteItem.setActionCommand("delete");
			    deleteItem.addActionListener(menuActionAppointment);
			    deleteItem.setEnabled(canModify(reservation));
			    popup.add(deleteItem);
			}
			// BJO 0000000104
	        JMenuItem viewItemAppointment = new JMenuItem(getString("info-appointment"),getIcon( "icon.help"));
	        viewItemAppointment.setActionCommand("info");
	        viewItemAppointment.addActionListener(menuActionAppointment);
	        User owner = reservation.getOwner();
	        try 
	        {
	            User user = getUser();
	            boolean canView = getQuery().canReadReservationsFromOthers( user) || user.equals( owner);
	            viewItemAppointment.setEnabled( canView);
	        } 
	        catch (RaplaException ex)
	        {
	            getLogger().error( "Can't get user",ex);
	        }
	        popup.add(viewItemAppointment);           
	        popup.show( table, point.x, point.y); 
	    }
	    
	    /** Implementation-specific. Should be private.*/
	    public void mousePressed(MouseEvent me) {
	        if (me.isPopupTrigger())
				try {
					showPopup(me);
				} catch (RaplaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	    
	    /** Implementation-specific. Should be private.*/
	    public void mouseReleased(MouseEvent me) {
	        if (me.isPopupTrigger())
				try {
					showPopup(me);
				} catch (RaplaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	    
	    /** double click*/
	    public void mouseClicked(MouseEvent me) {
	        if (me.getClickCount() > 1 ) {

            	if(isReadOnlyUser)
            		return;

	            //Point p = new Point(me.getX(), me.getY());
		        int r = table.getSelectedRow();
		        int c = table.getSelectedColumn();
		        if( r  < 0 || c < 0)
		        	return;
		        r = convertRowIndexToModel(table, r);
	            if(c<OccupationTableModel.CALENDAR_EVENTS) // no menu on fixed columns
	            	return;
	            Object occupation = occupationTableModel.getValueAt(r, c);
	            if(occupation instanceof OccupationCell) {
	            	OccupationCell occCell = (OccupationCell) occupation;
	            	if(occCell.object!=null && (occCell.getTypeId() == OccupationCell.OCCUPIED || occCell.getTypeId() == OccupationCell.CONFLICT)) {
	            		if(occCell.object instanceof Appointment) {
	            			Reservation reservation = occCell.getAppointment().getReservation();
				    		try {
				    			if(canModify(reservation) || getQuery().canExchangeAllocatables(reservation)) {
				        			AppointmentBlock appBlock = new AppointmentBlock(occCell.getAppointment());
				    				getReservationController().edit(appBlock);//, occupationTableModel.getColumnDate(c));
				    			}
				        	} catch (RaplaException e) {
				        			// TODO Auto-generated catch block
				        			e.printStackTrace();
				        	}
				    	}
	            	}
	            }
	    	}
	    }
	}
    
    public class newAdapter implements ActionListener {
    	private Object obj;
    	private Date cleanupDate;
    	Point point;

    	newAdapter(Object obj, Date cleanupDate, Point point) {
    		this.obj = obj;
    		this.cleanupDate = cleanupDate;
    		this.point = point;
    	}
    	
    	private Point getPoint() {
    		return this.point;
    	}

	public void actionPerformed(ActionEvent evt) {
        try {
        	int c = table.getSelectedColumn();
        	int r = table.getSelectedRow();     

        	Point point = new Point(100,25);
              
        	Date selectedDate = occupationTableModel.getColumnDate(c);
			if(evt.getActionCommand().equals("new")) {
        		Appointment newApp = newReservation(selectedDate, (AllocationCell) occupationTableModel.getValueAt(r, OccupationTableModel.CALENDAR_RESOURCE));
        		if(newApp !=null) {
        			AppointmentBlock appBlock = new AppointmentBlock(newApp);
        			getReservationController().edit(appBlock);//, occupationTableModel.getColumnDate(c));
        		}
        	}
        	else if(evt.getActionCommand().equals("edit")) {
        			AppointmentBlock appBlock = new AppointmentBlock((Appointment)obj);
        			getReservationController().edit(appBlock);//, occupationTableModel.getColumnDate(c));
        	}
        	else if(evt.getActionCommand().equals("delete")) {
        			AppointmentAction deleteAction = new AppointmentAction( getContext(), getComponent(), getPoint());
        			// get selected date
        			AppointmentBlock appBlock = new AppointmentBlock((Appointment)obj);
        			deleteAction.setDelete(appBlock);//,occupationTableModel.getColumnDate(c));
            		deleteAction.actionPerformed(evt);
        	}	
        	
        	else if(evt.getActionCommand().equals("split")) {
    			// get selected date
    			AppointmentBlock appBlock = new AppointmentBlock((Appointment)obj);
    			splitAppointment(appBlock, selectedDate, getComponent(), point);
        	}	

        	else if(evt.getActionCommand().equals("end")) {
       			AppointmentBlock appBlock = new AppointmentBlock((Appointment)obj);
    			endAppointment(appBlock, selectedDate, getComponent(), point);
        	}	
			
        	else if(evt.getActionCommand().equals("info")) {
        			AppointmentAction viewAction = new AppointmentAction( getContext(), getComponent(), getPoint());
        			AppointmentBlock appBlock = new AppointmentBlock((Appointment)obj);
    				viewAction.setView(appBlock);
    				viewAction.actionPerformed(evt); 
        		}
	        else if(evt.getActionCommand().equals("archive")) {
	        		Allocatable alloc = (Allocatable) obj;
	                AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
	                if(type == null)
	                	return;
	                    		
	    			AppointmentAction archiveAction = new AppointmentAction( getContext(), getComponent(), getPoint());
	    			if(sendOKMsg("confirm", cleanupDate) == 0) // OK
	    				endOfLifeAllocatable (alloc, cleanupDate);
	    				
	    			archiveAction.actionPerformed(evt);
	    		}
        		
		} catch (RaplaException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	  }
	}
    
 // BJO 00000040
	private void deleteSilentAppointment(AppointmentBlock appointmentBlock,Component sourceComponent,Point point) throws RaplaException {
    	Appointment appointment = appointmentBlock.getAppointment();
        Reservation reservation = appointment.getReservation();
        getModification().remove( reservation);
    }
	// BJO 00000040

    // BJO 00000141
	private void splitAppointment(AppointmentBlock appointmentBlock,Date selectedDate,Component sourceComponent,Point point) throws RaplaException {
    	Appointment appointment = appointmentBlock.getAppointment();
    	// no Repeating and Repeating
    	Date splitTime = new Date(selectedDate.getTime());
    	splitTime = showTimeDialog(selectedDate, sourceComponent, point,"move_internal");
    	if(splitTime == null)
    		return;
    	
    	Repeating repeating   = appointment.getRepeating();
    	if(repeating != null) {
    		// Repeating
        	if(!repeating.isDaily())
        		return;
        	
        	int days = repeating.getNumber();

        	if(DateTools.isMidnight(splitTime) && (days < 2 && days != -1))
    			return;
	        
	    	// left part 
        	Reservation modifiableReservation = getModification().edit(appointment.getReservation());
        	Appointment leftpart = modifiableReservation.findAppointment(appointment);
        	leftpart.getRepeating().setEnd(splitTime); // repeat until date
    	    
    	    // right part
    	    Appointment rightpart  = getModification().newAppointment(splitTime, DateTools.addDay(splitTime), repeating.getType(), 0);
    	    rightpart.getRepeating().setEnd(appointment.getMaxEnd()); // repeat until date
    	    modifiableReservation.addAppointment(rightpart);   
    	    
    	    getModification().store( modifiableReservation); 
    	}
    	else { 
    		// No repeating
	    	if(DateTools.isMidnight(splitTime) & DateTools.countDays(appointment.getStart(), appointment.getEnd()) < 2)
	    			return;
	    	
	    	// left part 
        	Reservation modifiableReservation = getModification().edit(appointment.getReservation());
        	Appointment leftpart = modifiableReservation.findAppointment(appointment);
        	leftpart.move(appointment.getStart(), splitTime);
	               	
	        // right part
    	    Appointment rightpart  = getModification().newAppointment(splitTime, appointment.getEnd());
	        modifiableReservation.addAppointment(rightpart);
	        
	        getModification().store( modifiableReservation);       
        }
    }
    
	private Date showTimeDialog(Date selectedDate, Component sourceComponent, Point point, String menu) throws RaplaException {
	    RaplaTime selectedTime;
	    selectedTime = createRaplaTime();
	    selectedTime.setRowsPerHour( 2 );
	    long endTime = getCalendarOptions().getWorktimeEndMinutes();
	    selectedTime.setTime((int) endTime / 60, (int) endTime % 60)
	    ;
	    //selectedTime.setTime(splitStartDate);	    
	    final DialogUI dialog = DialogUI.create(
	             getContext()
	            ,sourceComponent
	            ,true
	            ,selectedTime
	            ,new String[] { getString("apply"),getString("cancel")}
	            );
	    dialog.setTitle(getI18n().getString(menu) + " " + getI18n().getString("time"));
	    dialog.setIcon(getIcon("icon.split"));
	    dialog.start(point);
	    if (dialog.getSelectedIndex() == 0) {
			return getRaplaLocale().toDate(selectedDate,selectedTime.getTime());
	    }
    	return null;
	}
	
	private void endAppointment(AppointmentBlock appointmentBlock,Date selectedDate,Component sourceComponent,Point point) throws RaplaException {
    	Appointment appointment = appointmentBlock.getAppointment();
    	// no Repeating and Repeating
    	Date splitTime = new Date(selectedDate.getTime());
    	splitTime = showTimeDialog(selectedDate, sourceComponent, point,"move_out");
    	if(splitTime == null)
    		return;
    	
	    Appointment original  = getModification().clone(appointment);    	
    	Repeating repeating   = original.getRepeating();
    	if(repeating != null) {
    		// Repeating
        	if(!repeating.isDaily())
        		return;
        	
        	int days = repeating.getNumber();

        	if(DateTools.isMidnight(splitTime) && (days < 2 && days != -1))
    			return;
    	       
	    	// left part 
        	Reservation modifiableReservation = getModification().edit(appointment.getReservation());
        	Appointment leftpart = modifiableReservation.findAppointment(appointment);
        	leftpart.getRepeating().setEnd(splitTime); // repeat until date
	        getModification().store( modifiableReservation);
    	}
    	else { 
    		// No repeating
	    	if(DateTools.isMidnight(splitTime) & DateTools.countDays(appointment.getStart(), appointment.getEnd()) < 2)
	    			return;

	    	// left part 
        	Reservation modifiableReservation = getModification().edit(appointment.getReservation());
        	Appointment leftpart = modifiableReservation.findAppointment(appointment);
        	leftpart.move(original.getStart(), splitTime);
	        getModification().store( modifiableReservation);
         }
    }
    // BJO 00000141  
    
    public Appointment newReservation(Date selectedDate, AllocationCell alcCell) throws RaplaException {
    	Calendar calendarStart = raplaLocale.createCalendar();
    	calendarStart.setTime(selectedDate);
    	long startTime = getCalendarOptions().getWorktimeStartMinutes() * DateTools.MILLISECONDS_PER_MINUTE;
    	calendarStart.add(Calendar.MILLISECOND, (int) startTime);

    	int rc = 0;
		try {
        	Object value = alcCell.allocatable.getClassification().getValue("_periodselector"); 
        	if(value == null || ((Boolean) value) == false)
        		rc = 1;
        	else
            	rc = selectIntervalDialog(calendarStart.getTime(), alcCell.allocatable);
		} catch (NoSuchElementException e) {
    		rc = 3;
		}
    	
		if(rc == 0) // Canceled dialog
			return null;
		
		else if(rc == 1 || rc == 3) { // 1 = New Period requested  3 = no "_periodselector" element found 
			mutableReservation = getClientFacade().newReservation();
			Classification classification = getClientFacade().getDynamicType(eventType).newClassification();
			mutableReservation.setClassification(classification);
		}
		else if(rc == 2) { // 2 = Selected period
			mutableReservation = (Reservation)getModification().edit(selectedInterval);
		}

    	Appointment appointment = null;
    	/*
    	Calendar calendarStart = raplaLocale.createCalendar();
    	calendarStart.setTime(selectedDate);
    	long startTime = getCalendarOptions().getWorktimeStartMinutes() * DateTools.MILLISECONDS_PER_MINUTE;
    	calendarStart.add(Calendar.MILLISECOND, (int) startTime);
		*/
    	Calendar calendarEnd = raplaLocale.createCalendar();
    	calendarEnd.setTime(selectedDate);
    	int nTimes = getReservationOptions().getnTimes();
		calendarEnd.add(Calendar.DATE, nTimes - 1 );
    	long endTime = (getCalendarOptions().isWorktimeOvernight() ? DateTools.MILLISECONDS_PER_DAY : 0)
			         + getCalendarOptions().getWorktimeEndMinutes() * DateTools.MILLISECONDS_PER_MINUTE;  	
    	calendarEnd.add(Calendar.MILLISECOND, (int) endTime);

    	appointment = getClientFacade().newAppointment( calendarStart.getTime(), calendarEnd.getTime());
    	RepeatingType repeatingType = getReservationOptions().getRepeatingType();
        if(repeatingType == null) {
        	appointment.setWholeDays(false);
        	appointment.setRepeatingEnabled( false);
        }
        else {
        	appointment.setRepeatingEnabled( true);
        	Repeating repeating = appointment.getRepeating();
			repeating.setType( repeatingType);
			// -1:infinite; >0:=n-times
			int repeatingDuration = getReservationOptions().isInfiniteRepeating() ? -1 : (1 * nTimes);
			repeating.setNumber(repeatingDuration);
			appointment.setWholeDays(true);
        }

        mutableReservation.addAppointment(appointment);
        mutableReservation.addAllocatable(alcCell.allocatable);
        return appointment;
    }
    
    private ReservationOptions getReservationOptions() {
        RaplaConfiguration conf = null;
        ReservationOptions options = null;
        User user;
		try {
			user = getUser();

        if ( user != null) 
            conf = getQuery().getPreferences(user).getEntry(ReservationOptionsImpl.RESERVATION_OPTIONS);
        
        // Default settings enforced by admin
        if ( conf == null )
            conf = getQuery().getPreferences(null).getEntry(ReservationOptionsImpl.RESERVATION_OPTIONS);        
    	if ( conf == null) {
    		options = new ReservationOptionsImpl();
			//throw new EntityNotFoundException(getString("event.config.error"));
    	}
    	else
    		options = new ReservationOptionsImpl(conf);
		} catch (RaplaException e) {
			getLogger().error(e.getMessage());
			showException(e, getMainComponent() );
		}
		return options;
    }
// BJO 00000120


	private int selectIntervalDialog(Date selectedDate, Allocatable allocatable) throws RaplaException {
		Reservation[] reservations = getQuery().getReservationsForAllocatable(new Allocatable[] { allocatable }, null, null, null);
		if(reservations.length == 0)
			return 3;
		
		int defaultInterval = -1;
		List<String> intervalLabels = new ArrayList<String>();
		List<Reservation> intervalDates = new ArrayList<Reservation>();

		for(Reservation reservation: reservations) {
			try {
		    	Date periodStartEvent = (Date) reservation.getClassification().getValue("_periodstart"); 
		    	if(periodStartEvent == null) 
		    		continue;
		    	
				Calendar periodStart = Calendar.getInstance(timezone);
				periodStart.setTime(periodStartEvent);
				periodStart.add(Calendar.MINUTE, getCalendarOptions().getWorktimeStartMinutes());
				
				Calendar periodEnd = Calendar.getInstance(timezone);
				periodEnd.setTime((Date) periodStartEvent);
				periodEnd.add(Calendar.YEAR, 1);
				periodEnd.add(Calendar.MINUTE, getCalendarOptions().getWorktimeEndMinutes());
				
				Date start = periodStart.getTime();
				Date end = periodEnd.getTime();

		    	if (start.before(selectedDate) && end.after(selectedDate))
		    		defaultInterval = intervalLabels.size();
		    	intervalLabels.add(raplaLocale.formatTimestamp((Date) start,timezone) + " " + raplaLocale.formatTimestamp((Date) end,timezone));
		    	intervalDates.add( reservation);
		    	
			} catch (NoSuchElementException ex) {
				continue;
			}
		}
        if(defaultInterval == -1) { // new 
        	return 1;
        }
        else {
        	selectedInterval = intervalDates.get(defaultInterval); 
    		return 2;  
        }
    }

	public  void endOfLifeAllocatable (Allocatable alloc, Date endDate) throws RaplaException {
		Classification classification = alloc.getClassification();
		AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
		if(type == null)
			return;
		try {
			
			Object endlife = classification.getValue("_endoflife");
			if(type.equals(AttributeType.DATE)) {
				if(endlife == null || endDate.after((Date) endlife)) {	
		    		Allocatable editAllocatable = (Allocatable)getClientFacade().edit( alloc);
		    		editAllocatable.getClassification().setValue("_endoflife", endDate);
		            getClientFacade().store( editAllocatable );
				}
			}
			else if(type.equals(AttributeType.BOOLEAN)) {
		    	Allocatable editAllocatable = (Allocatable)getClientFacade().edit( alloc);
		    	editAllocatable.getClassification().setValue("_endoflife", true);
		        getClientFacade().store( editAllocatable );
			}
		}
        catch (NoSuchElementException e)
        {
        	sendOKMsg("noendoflife",null);
        }
        return;
	}
	
	public int sendOKMsg(String msg, Date endOfLife) throws RaplaException {
		String message=null;
		if(endOfLife== null)
			message = getString(msg);
		else
			message = getI18n().format(msg,endOfLife);
		DialogUI info = DialogUI.create(getContext(),getComponent(),true,getString("ok"),message);
        info.start();
        return info.getSelectedIndex(); // 0 = OK -1 = OS UI Windows Close
    }
	
	public class AllocatableSortByName implements Comparator<Allocatable> {
		public int compare(Allocatable o1, Allocatable o2) {
			return o1.getName(locale).compareTo(o2.getName(locale));
		}
	}
	
	public class LinesBorder extends AbstractBorder implements SwingConstants { 
		
		private static final long serialVersionUID = 1L;
		protected int northThickness;
		protected int southThickness;
		protected int eastThickness;
		protected int westThickness;  
		protected Color northColor;
		protected Color southColor;
		protected Color eastColor;
		protected Color westColor;
		protected int row,column; 
		protected char bound;
		protected Color borderColor;
		  
		public LinesBorder(Color color) {
			this(color, 1);
		}

		public void setLocation(int r, int c) {
			row = r;
			column = c;		
		}
		
		public void setBound(char bound) {
			this.bound=bound;	
		}

		public LinesBorder(Color color, int thickness)  {
		    setColor(color);
		    setThickness(thickness);
	    }

		public void ResetLinesBorder(Color color, int thickness)  {
		    setColor(color);
		    setThickness(thickness);
	    }

		public LinesBorder(Color color, int thickness, int offset, int direction)  {
		    setColor(color);
		    setThickness(thickness);
	    }

		public LinesBorder(Color color, Insets insets)  {
		    setColor(color);
		    setThickness(insets);
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		    //System.out.println("cellLocation= (" +  row +", " + column +")");
			Color oldColor = g.getColor();
			//System.out.println("x=" + x + " ,y=" + y + " ,width=" + width + " ,height=" + height);
		    g.setColor(northColor);
		    for (int i = 0; i < northThickness; i++)  {
		      g.drawLine(x, y+i, x+width-1, y+i);
		    }
		    
		    g.setColor(southColor);
		    for (int i = 0; i < southThickness; i++)  {
		      g.drawLine(x, y+height-i-1, x+width-1, y+height-i-1);
		    }

		    g.setColor(westColor);
		    //System.out.println("westThickness=" + westThickness);
		    int i = 0;
		    for (; i < westThickness - 2; i++)  {
		      g.drawLine(x+i, y, x+i, y+height-1);
		      //System.out.println("West X1=" + (x+i) +" ,Y1=" + y + " ,X2=" + (x+i) +" ,Y2=" + (y+height-1));
		    }
		    if(bound == '<') { // StartBorder
			    g.setColor(borderColor);
		    	g.drawLine(x+i, y, x+i, y+height-1);
		    	i++;
		    	g.drawLine(x+i, y, x+i, y+height-1); 
		    }
		    
		    //System.out.println("eastThickness=" + eastThickness);
		    if(bound == '>') { // EndBorder 
			    g.setColor(borderColor);
			    g.drawLine(x+width-eastThickness-2, y, x+width-eastThickness-2, y+height-1);
			    g.drawLine(x+width-eastThickness-1, y, x+width-eastThickness-1, y+height-1);
		    }
		    g.setColor(eastColor);
		    for (int j = 0; j < eastThickness; j++)  {
		      g.drawLine(x+width-eastThickness+j, y, x+width-eastThickness+j, y+height-1);
		      //System.out.println("East X1=" + (x+width-eastThickness+j) +" ,Y1=" + y + " ,X2=" + (x+width-eastThickness+j) +" ,Y2=" + (y+height-1));
		    }
		    
		    g.setColor(oldColor);
		  }
		
		public Insets getBorderInsets(Component c)       {
		    return new Insets(northThickness, westThickness, southThickness, eastThickness);
		}

		public Insets getBorderInsets(Component c, Insets insets) {
		    return new Insets(northThickness, westThickness, southThickness, eastThickness);    
		}

		public boolean isBorderOpaque() {
			return true;
		}

		public void setColor(Color c) {
		    northColor = c;
		    southColor = c;
		    eastColor  = c;
		    westColor  = c;
		    borderColor = c;
		}
		  
		public void setColor(Color c, int direction) {
			switch (direction) {
				case NORTH: northColor = c; break;
				case SOUTH: southColor = c; break;
				case EAST:  eastColor  = c; break;
				case WEST:  westColor  = c; break;
				default: 
			}	
		}
		    
		public void setThickness(int n) {
		    northThickness = n;
		    southThickness = n;
		    eastThickness  = n;
		    westThickness  = n;
		}
		    
		public void setThickness(Insets insets) {
		    northThickness = insets.top;
		    southThickness = insets.bottom;
		    eastThickness  = insets.right;
		    westThickness  = insets.left;
		}
		  
		public void setThickness(int n, int direction) {
		    switch (direction) {
		     	case NORTH: northThickness = n; break;
		     	case SOUTH: southThickness = n; break;
		     	case EAST:  eastThickness  = n; break;
		     	case WEST:  westThickness  = n; break;
		     	default: 
		    }
		}

		public void append(LinesBorder b, boolean isReplace) {
			if (isReplace) {
				northThickness = b.northThickness;
				southThickness = b.southThickness;
				eastThickness  = b.eastThickness;
				westThickness  = b.westThickness;
		    } else {
		    	northThickness = Math.max(northThickness ,b.northThickness);
		    	southThickness = Math.max(southThickness ,b.southThickness);
		    	eastThickness  = Math.max(eastThickness  ,b.eastThickness);
		    	westThickness  = Math.max(westThickness  ,b.westThickness);
		    }
		  }

		  public void append(Insets insets, boolean isReplace) {
			  if (isReplace) {
				  northThickness = insets.top;
				  southThickness = insets.bottom;
				  eastThickness  = insets.right;
				  westThickness  = insets.left;
		    } else {
		    	northThickness = Math.max(northThickness ,insets.top);
		    	southThickness = Math.max(southThickness ,insets.bottom);
		    	eastThickness  = Math.max(eastThickness  ,insets.right);
		    	westThickness  = Math.max(westThickness  ,insets.left);
		    }
		  }
		}
	
	
	class AllocatableCellRenderer extends JComponent implements TableCellRenderer {
		  
		private static final long serialVersionUID = 1L;

		public AllocatableCellRenderer() {
		    super();
		  }
		  
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int r, int c) {
			int row =convertRowIndexToModel(table,r);
			OccupationCell occCell= null;
			int columnCount = occupationTableModel.getColumnCount() - 1;
			for ( int column = OccupationTableModel.CALENDAR_EVENTS ; column <= columnCount; column++) {
		        Object obj = occupationTableModel.getValueAt(row, column);
		        if (obj instanceof OccupationCell ) {
		        	occCell = (OccupationCell) obj;
		        	if(occCell.object!=null)
		        		break;
		        }	
			}
			/*
			if(occupationTableModel.getTodayColumn()!= 0)
				occCell = (OccupationCell) occupationTableModel.getValueAt(row,occupationTableModel.getTodayColumn());
			else
				occCell = (OccupationCell) occupationTableModel.getValueAt(row,OccupationTableModel.CALENDAR_EVENTS);
			*/	
			if(occCell!=null) {
				final Appointment app = occCell.getAppointment();
                if( app != null  ) {
    				//final Reservation reservation = occCell.getAppointment().getReservation();
					AllocatableColors controlColors = new AllocatableColors(app); 
			   		return controlColors;
			    }
			}
			return this;
		}
	}
	
	public class AllocatableColors extends JComponent {
   		Font textFont = new Font("SanSerif", Font.BOLD, 15);
   	    FontMetrics fontMetrics = getFontMetrics(textFont);
   		
		private static final long serialVersionUID = 1L;
		
		Appointment appointment;
		
		AllocatableColors(Appointment appointment) {
			this.appointment = appointment;
		}
		
		public void paint(Graphics g) {
			
				Classification classification = null;
        		
        		// Resource check points
				Reservation reservation = appointment.getReservation();
		        List arrayList =  Arrays.asList(reservation.getAllocatablesFor(appointment));
		        
		        Comparator comp = new Comparator() {
	                public int compare(Object o1, Object o2) {
	                    if ((((Allocatable) o1).getClassification().getType().getName(locale)).compareTo(((Allocatable) o2).getClassification().getType().getName(locale)) > 0)
	                        return 1;
	                    if ((((Allocatable) o1).getClassification().getType().getName(locale)).compareTo(((Allocatable) o2).getClassification().getType().getName(locale)) < 0)
	                        return -1;
	                    return 0;
	                }
	            };
	            
		        Collections.sort(arrayList,comp);
		        Iterator it = arrayList.iterator();
		        int i=0;
		        Color color = null;
		        while (it.hasNext()) {
		            Allocatable allocatable = (Allocatable) it.next();
	    			classification = allocatable.getClassification();
	    			// border
	    			g.setColor(Color.BLACK);
	    			int height = table.getRowHeight();
	    			int width = 19;
	    			int distance = 21;
	    			g.fillRect(i*distance, 0, 2, height); //Left border
	   	         	g.fillRect(i*distance, 0, width, 2); //Top border
	   	         	g.fillRect(i*distance + width-2, 0, 2, height); //Right border
	   	         	g.fillRect(i*distance, height-2, width, 2); //Bottom border
	    			//g.drawRect (1 + i*22, 8, 19, table.getRowHeight()-2);

		            color = getColorForClassifiable( allocatable );
		            // Color
		            if ( color == null )
		                g.setColor(Color.WHITE); //RaplaColorList.getHexForColor( RaplaColorList.getResourceColor(i));
		            else
		    			g.setColor(color);
	    			g.fillRect (2 + i*distance, 2, width-4, height-4);
	    			// Text
	    			g.setFont(textFont);
	    			g.setColor(Color.BLACK);
		            g.drawString(classification.getType().getName(locale).substring(0,1), 4 + i*22, table.getRowHeight()- 8); // First character from name
	           		i++;
		        }
		        
		        int width = i * 22 + 2; // start of text comments
		        
		        it = arrayList.iterator();

		        while (it.hasNext()) {
		            Allocatable allocatable = (Allocatable) it.next();
	    			classification = allocatable.getClassification();
	    			width = getControlData(classification, g, width);
		        }
		        
                // Event check points
        		classification = reservation.getClassification();
        		width = getControlData(classification, g, width);	        

			
		}
		
		private int getControlData(Classification classification, Graphics g, int inWidth) {
			Attribute[] attributes = classification.getAttributes();
			String color = null;
			String txt = null;

			int outWidth = inWidth;
			for (int k=0; k<attributes.length; k++)
			{      
				String attributeKey = attributes[k].getKey();
				if(attributes[k].getKey().contains("check_")) {
					if (attributes[k].getType() == AttributeType.CATEGORY) {
						Category cat = (Category) classification.getValue(attributeKey);  
						Category rootCategory = (Category) attributes[k].getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
						if ( cat != null ) {
				            txt = " " + cat.getPath(rootCategory, locale);
							color = cat.getAnnotation(CategoryAnnotations.KEY_NAME_COLOR, null) ;
					        if ( color == null )
					           	g.setColor(Color.BLACK);
					        else
						      	g.setColor(Color.decode(color));
					        g.setFont(textFont);
						    g.drawString(txt, outWidth , 12);
						    outWidth += fontMetrics.stringWidth(txt);
				        }
					}
				}
			}
			return outWidth;
		}
	}
	
	class RowHeaderRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 1L;
		RowHeaderRenderer(JTable table) {
			JTableHeader header = table.getTableHeader();
			setOpaque(true);
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			setHorizontalAlignment(CENTER);
			setForeground(header.getForeground());
			setBackground(header.getBackground());
			setFont(header.getFont());
		}
	
		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}
	
    Printable printable = null;
    /**
     * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
     */
    public int print(Graphics graphics, PageFormat format, int page) throws PrinterException {
        MessageFormat header = new MessageFormat( model.getNonEmptyTitle());
        MessageFormat footer = new MessageFormat("- {0} -");
    	Printable  printable = table.getPrintable( JTable.PrintMode.FIT_WIDTH,header, footer );
        return printable.print( graphics, format, page);   
    }
    
    static Map<Integer,Map<String,Color>> alphaMap = new HashMap<Integer, Map<String,Color>>();
    private Color adjustColor( String org, int alpha )
    {
		Map<String,Color> colorMap = (Map<String,Color>) alphaMap.get( alpha );
        if ( colorMap == null )
        {
            colorMap = new HashMap<String,Color>();
            alphaMap.put( alpha, colorMap );
        }
        Color color = colorMap.get( org );
        if ( color == null )
        {
            Color or;
            try
            {
                or = AWTColorUtil.getColorForHex( org );
            }
            catch ( NumberFormatException nf )
            {
                or = AWTColorUtil.getColorForHex( "#FFFFFF" );
            }
            color = new Color( or.getRed(), or.getGreen(), or.getBlue(), alpha );
            colorMap.put( org, color );
        }

        return color;
    }
}
