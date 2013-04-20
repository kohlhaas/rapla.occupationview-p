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

import org.rapla.client.ClientServiceContainer;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.TypedComponentRole;
import org.rapla.plugin.RaplaClientExtensionPoints;

public class OccupationPlugin implements PluginDescriptor<ClientServiceContainer>
{
	static boolean ENABLE_BY_DEFAULT = false;
    public static final TypedComponentRole<I18nBundle> RESOURCE_FILE = new TypedComponentRole<I18nBundle>(OccupationPlugin.class.getPackage().getName() + ".OccupationResources");
    public static final String PLUGIN_CLASS = OccupationPlugin.class.getName();


    public String toString()
    {
        return "Occupation View";
    }

    public void provideServices(ClientServiceContainer container, Configuration config)
    {
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponent( RESOURCE_FILE, I18nBundleImpl.class,I18nBundleImpl.createConfig( RESOURCE_FILE.getId() ) );
        container.addContainerProvidedComponent( RaplaClientExtensionPoints.CALENDAR_VIEW_EXTENSION, OccupationFactory.class);
        container.addContainerProvidedComponent( RaplaClientExtensionPoints.USER_OPTION_PANEL_EXTENSION, OccupationOption.class);
    }

}

