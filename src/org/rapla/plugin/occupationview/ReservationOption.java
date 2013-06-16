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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.layout.TableLayoutConstants;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.RepeatingEnding;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.DefaultConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.common.NamedListCellRenderer;
//BJO 00000120
public class ReservationOption extends RaplaGUIComponent implements OptionPanel //, ActionListener
{
    JPanel panel = new JPanel();
    Preferences preferences;
    ReservationOptions options;


    JComboBox repeatingDuration = new JComboBox( new RepeatingEnding[] { 
  												          RepeatingEnding.FOREVEVER
  												        , RepeatingEnding.N_TIMES                                                       
  												        // , RepeatingEnding.END_DATE
													  });
    RaplaNumber nTimesField = new RaplaNumber(new Double(1),new Double(1),new Double(365), false);



    JComboBox repeatingType = new JComboBox( new RepeatingType[] {
		          RepeatingType.DAILY
		        , RepeatingType.WEEKLY
		        , RepeatingType.MONTHLY
		        , RepeatingType.YEARLY
		        , RepeatingType.NONE
		  });

    JComboBox eventTypeSelector;
    //RaplaTime splitTime;
    

    public ReservationOption(RaplaContext sm) throws RaplaException {
        super( sm );
        double pre = TableLayoutConstants.PREFERRED;
        double fill = TableLayoutConstants.FILL;
        // rows = 8 columns = 4
        panel.setLayout( new TableLayout(new double[][] {{pre, 5, pre, 5 , pre, 5, pre}, {pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,fill}}));
        ListRenderer listRenderer = new ListRenderer();
        
// BJO 00000012 
       panel.add( new JLabel(getString("repeating")),"0,2"  );
// BJO 00000012 
// BJO 00000052        
        panel.add( repeatingType,"2,2");
        repeatingType.setRenderer( listRenderer );   
// BJO 00000052
// BJO 00000012 
        panel.add( repeatingDuration,"4,2");
        panel.add( nTimesField,"6,2");
       
        repeatingDuration.setRenderer( listRenderer );
        ActionListener repeatingListener = new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                if(repeatingType.getSelectedIndex() == 4) {
                	repeatingDuration.setEnabled(false);
                	nTimesField.setEnabled(false);
                } else {
                	repeatingDuration.setEnabled(true);
                	if(repeatingDuration.getSelectedIndex()==0)
                		nTimesField.setEnabled(false);
                	else
                		nTimesField.setEnabled(true);
                }     
            }     
        };
        repeatingType.addActionListener(repeatingListener);     
        repeatingDuration.addActionListener(repeatingListener);   
        
// BJO 00000063 
        panel.add( new JLabel(getString("reservation_type")),"0,4"  );
        DynamicType[] types = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);
        eventTypeSelector =  new JComboBox( types );
        panel.add( eventTypeSelector,"2,4");
        eventTypeSelector.setRenderer(new NamedListCellRenderer(getI18n().getLocale()));
        //eventTypeSelector.addActionListener( this );
// BJO 00000063 
        /*
        splitTime = createRaplaTime();
        splitTime.setRowsPerHour( 1 );
        panel.add( new JLabel(getString("splittime")),"0,6"  );
        panel.add( splitTime,"2,6");
        */
    }

    @Override
	public JComponent getComponent() {
        return panel;
    }
    @Override
	public String getName(Locale locale) {
        return getString("reservation");
    }

    @Override
	public void setPreferences( Preferences preferences) {
        this.preferences = preferences;
    }

    @Override
	public void show() throws RaplaException {
    	// get the options 
        RaplaConfiguration config = preferences.getEntry( ReservationOptionsImpl.RESERVATION_OPTIONS);
        if ( config != null) {
            options = new ReservationOptionsImpl( config );
        } else {
            options = new ReservationOptionsImpl();
        }

// BJO 00000012
        //repeating.setSelectedItem( options.isInfiniteRepeating() ? ReservationOptionsImpl.NTIMES : ReservationOptionsImpl.REPEATING_NTIMES );
        repeatingDuration.setSelectedItem( options.getRepeatingDuration());
        nTimesField.setNumber( new Long(options.getnTimes()));
        nTimesField.setEnabled(options.isNtimesRepeating());
// BJO 00000012
// BJO 00000052
        repeatingType.setSelectedItem( options.getRepeatingType());
// BJO 00000052
// BJO 00000063 
        String eventType = options.getEventType();
        DynamicType dt;
    	if(eventType==null)
    		dt = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION)[0];
		else 
			try {
				dt = getQuery().getDynamicType(eventType);
		    	} catch (EntityNotFoundException ex) {
		    		dt = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION)[0];
		    	}    	
       	eventTypeSelector.setSelectedItem(dt);
// BJO 00000063 
       	/*
        int splitMinutes = options.getSplitTimeMinutes();
        Calendar calendar = getRaplaLocale().createCalendar();
        calendar.set( Calendar.HOUR_OF_DAY, splitMinutes / 60);
        calendar.set( Calendar.MINUTE, splitMinutes % 60);
        splitTime.setTime( calendar.getTime() );
        */
    }

    @Override
	public void commit() {
    	// Save the options
        DefaultConfiguration reservationOptions = new DefaultConfiguration("reservation-options");
      
// BJO 00000012   
        DefaultConfiguration repeating = new DefaultConfiguration(ReservationOptionsImpl.REPEATING); 
        RepeatingEnding repeatingValue = (RepeatingEnding) this.repeatingDuration.getSelectedItem();
        if ( repeatingValue != null )
        	repeating.setValue(  repeatingValue.toString() );
        else
        	repeating.setValue(  RepeatingEnding.FOREVEVER.toString() );
        reservationOptions.addChild( repeating );
        
        DefaultConfiguration nTimes = new DefaultConfiguration(ReservationOptionsImpl.NTIMES);
        nTimes.setValue( nTimesField.getNumber().intValue());
        reservationOptions.addChild( nTimes);
// BJO 00000012

// BJO 00000052   
        DefaultConfiguration repeatingType = new DefaultConfiguration(ReservationOptionsImpl.REPEATINGTYPE); 
        RepeatingType repeatingTypeValue =  (RepeatingType)this.repeatingType.getSelectedItem();
        if ( repeatingTypeValue != null )
        	repeatingType.setValue(  repeatingTypeValue.toString());
        else
        	repeatingType.setValue( RepeatingType.DAILY.toString() );        
        reservationOptions.addChild( repeatingType);
// BJO 00000052

// BJO 00000063        
        DefaultConfiguration eventType = new DefaultConfiguration(ReservationOptionsImpl.EVENTTYPE);
        DynamicType dynamicType = (DynamicType) eventTypeSelector.getSelectedItem();
        if ( dynamicType != null )
        {
        	eventType.setValue( dynamicType.getElementKey() );
        }
        reservationOptions.addChild( eventType );
// BJO 00000063
        /*
        DefaultConfiguration splittime = new DefaultConfiguration(ReservationOptionsImpl.SPLITTIME);
        Calendar calendar = getRaplaLocale().createCalendar();
        calendar.setTime( splitTime.getTime());
        int splitMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60;
        splitMinutes += calendar.get(Calendar.MINUTE);
        splittime.setValue(  splitMinutes );
        reservationOptions.addChild( splittime);
	*/
        preferences.putEntry( ReservationOptionsImpl.RESERVATION_OPTIONS,new RaplaConfiguration( reservationOptions));
	}

/*	
    public void actionPerformed(ActionEvent event) {

            Object source = event.getSource();
            if (source == eventTypeSelector ) {
                DynamicType dynamicType = (DynamicType) eventTypeSelector.getSelectedItem();
            }

    }
*/
	private class ListRenderer extends DefaultListCellRenderer  {
		private static final long serialVersionUID = 1L;
		
		@Override
		public Component getListCellRendererComponent(JList list,Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if ( value != null) {
                setText(getString(  value.toString()));
            }
            return this;
        }
	}	
}