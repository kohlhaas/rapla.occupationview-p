/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.calendar.RaplaTime;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.TypedComponentRole;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.RaplaButton;

public class CleanUpOption extends RaplaGUIComponent implements OptionPanel  {
   
	RaplaTime cleanupTime = createRaplaTime();
    RaplaButton runCleanup;
    RaplaNumber cleanupAge = new RaplaNumber(new Double(31),new Double(1),new Double(999), false);
    Preferences preferences;
    JPanel panel = new JPanel();
    JCheckBox activateCleanUp = new JCheckBox();
    
	public final static TypedComponentRole<Integer> CLEANUP_AGE = new TypedComponentRole<Integer>("org.rapla.plugin.cleanup.cleanup-age");
	public final static TypedComponentRole<String> CLEANUP_TIME = new TypedComponentRole<String>("org.rapla.plugin.cleanup.cleanup-time");
	public final static TypedComponentRole<Boolean> CLEANUP_ACTIVE = new TypedComponentRole<Boolean>("org.rapla.plugin.cleanup.cleanup-active");
    
    public CleanUpOption(RaplaContext sm) throws RaplaException {
        super(sm);
        setChildBundleName( OccupationPlugin.RESOURCE_FILE);

        double[][] sizes = new double[][] {
             {5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED}
            ,{TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED}
        };
        TableLayout tableLayout = new TableLayout(sizes);
        panel.setLayout(tableLayout);

        activateCleanUp.setText("");        
        panel.add( new JLabel(getString("active")),"1,0");
        panel.add( activateCleanUp,"3,0");
        // When 
       	panel.add( new JLabel(getI18n().getString("cleanup_time")), "1,2");
        Long timeRun = new Date().getTime();
        Date start = new Date(timeRun);
        String date = getRaplaLocale().formatDateLong(start);
        cleanupTime = createRaplaTime();
        panel.add(cleanupTime,"3,2");

        panel.add( new JLabel(getI18n().format("cleanup_date",date)), "5,2");

        panel.add( new JLabel(getString("cleanup_age")),"1,4");
        panel.add( cleanupAge ,"3,4");
        panel.add( new JLabel(getString("days")),"5,4");       

        return;
    }

    public JComponent getComponent() {
        return panel;
    }

    public void show() throws RaplaException  {
        int age = preferences.getEntryAsInteger( CLEANUP_AGE,32);
        cleanupAge.setNumber(age);

        boolean active = preferences.getEntryAsBoolean(CLEANUP_ACTIVE,true);
        activateCleanUp.setEnabled(active);
        cleanupAge.setEnabled(active);
        cleanupTime.setEnabled(active);
        
        try {        
	        String time = preferences.getEntryAsString(CLEANUP_TIME,"00:00");
	        SimpleDateFormat sdftime = new SimpleDateFormat ("HH:mm");
	        Date date = (Date)sdftime.parse(time);
	        Calendar calendar = getRaplaLocale().createCalendar();
	        calendar.setTime(date);
	        cleanupTime.setTime( calendar.getTime() );
	        } catch (ParseException e) { }
    }
  
    public void commit() throws RaplaException {
        int age = cleanupAge.getNumber().intValue();
        preferences.putEntry( CLEANUP_AGE,age);     
        
        Calendar calendar = getRaplaLocale().createCalendar();
        calendar.setTime( cleanupTime.getTime());
        SimpleDateFormat sdftime= new SimpleDateFormat ("HH:mm");
        String time = sdftime.format(calendar.getTime());
        preferences.putEntry( CLEANUP_TIME, time);
        boolean active = activateCleanUp.isSelected();
        preferences.putEntry( CLEANUP_ACTIVE,active);  
        
    }
    
    public void setPreferences( Preferences preferences) {
        this.preferences = preferences;

    }
    
    public String getName(Locale locale) {
        return getString("OccupationCleanup");
    }
}
