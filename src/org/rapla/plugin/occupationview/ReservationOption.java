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
    private static final String NO_REPEATING = "no_repeating";
	JPanel panel = new JPanel();
    Preferences preferences;
    ReservationOptions options;


    JComboBox repeatingDuration = new JComboBox( new RepeatingEnding[] { 
  												          RepeatingEnding.FOREVEVER
  												        , RepeatingEnding.N_TIMES                                                       
  												        // , RepeatingEnding.END_DATE
													  });
    RaplaNumber nTimesField = new RaplaNumber(new Double(1),new Double(1),new Double(365), false);



    JComboBox repeatingType = new JComboBox( new String[] {
    		NO_REPEATING, "daily", "weekly", "monthly", "yearly"
		  });

    JComboBox eventTypeSelector;
    
    public ReservationOption(RaplaContext sm) throws RaplaException {
        super( sm );
        double pre = TableLayoutConstants.PREFERRED;
        double fill = TableLayoutConstants.FILL;
        // rows = 8 columns = 4
        panel.setLayout( new TableLayout(new double[][] {{pre, 5, pre, 5 , pre, 5, pre}, {pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,fill}}));
        ListRenderer listRenderer = new ListRenderer();
        
       panel.add( new JLabel(getString("repeating")),"0,2"  );
      
        panel.add( repeatingType,"2,2");
        repeatingType.setRenderer( listRenderer );   

        panel.add( repeatingDuration,"4,2");
        panel.add( nTimesField,"6,2");
       
        repeatingDuration.setRenderer( listRenderer );
        ActionListener repeatingListener = new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                if(repeatingType.getSelectedIndex() == 0) {
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
        
        panel.add( new JLabel(getString("reservation_type")),"0,4"  );
        DynamicType[] types = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);
        eventTypeSelector =  new JComboBox( types );
        panel.add( eventTypeSelector,"2,4");
        eventTypeSelector.setRenderer(new NamedListCellRenderer(getI18n().getLocale()));
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

        repeatingDuration.setSelectedItem( options.getRepeatingDuration());
        nTimesField.setNumber( new Long(options.getnTimes()));
        nTimesField.setEnabled(options.isNtimesRepeating());

        RepeatingType repeatingTypeValue = options.getRepeatingType();
        if ( repeatingTypeValue != null)
        {
        	repeatingType.setSelectedItem( repeatingTypeValue.toString());
        }
        else
        {
        	repeatingType.setSelectedIndex(0);
        }
 
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
    }

    @Override
	public void commit() {
    	// Save the options
        DefaultConfiguration reservationOptions = new DefaultConfiguration("reservation-options");
        
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
   
        DefaultConfiguration repeatingType = new DefaultConfiguration(ReservationOptionsImpl.REPEATINGTYPE); 
        String repeatingTypeValue =  (String)this.repeatingType.getSelectedItem();
        if ( repeatingTypeValue != null )
        {
        	if ( repeatingTypeValue.equals(NO_REPEATING))
        	{
        		repeatingType.setValue(  null);
        	}
        	else
        	{
        		repeatingType.setValue(  repeatingTypeValue.toString());
        	}
        }
        else
        {
        	repeatingType.setValue( null );     
        }
        reservationOptions.addChild( repeatingType);

       
        DefaultConfiguration eventType = new DefaultConfiguration(ReservationOptionsImpl.EVENTTYPE);
        DynamicType dynamicType = (DynamicType) eventTypeSelector.getSelectedItem();
        if ( dynamicType != null )
        {
        	eventType.setValue( dynamicType.getElementKey() );
        }
        reservationOptions.addChild( eventType );

        preferences.putEntry( ReservationOptionsImpl.RESERVATION_OPTIONS,new RaplaConfiguration( reservationOptions));
	}


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