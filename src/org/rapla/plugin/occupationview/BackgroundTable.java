package org.rapla.plugin.occupationview;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class BackgroundTable 
{ 
	public static void main(String[] args) 
	{
		JFrame frame = new JFrame("Table Example"); 
		frame.addWindowListener( new WindowAdapter() { 
			public void windowClosing(WindowEvent e) 
			{ 
				Window w = e.getWindow(); 
				w.setVisible(false); 
				w.dispose(); 
				System.exit(0); 
			} 
		} ); 
	
	
		JTable imTable = new JTable( 35, 3 ) { 
			private static final long serialVersionUID = 1L;
			public Component prepareRenderer (TableCellRenderer renderer, int row, int column) { 
				Component c = super.prepareRenderer( renderer, row, column); 
				// We want renderer component to be transparent so background image is visible 
				if( c instanceof JComponent ) 
					((JComponent)c).setOpaque(false); 
				return c; 
			} 
	
			ImageIcon image = new ImageIcon( "images/edit.gif" ); 
			public void paint( Graphics g ) { 
				// tile the background image 
				/*
				Dimension d = getSize(); 
				for( int x = 0; x < d.width; x += image.getIconWidth() ) 
					for( int y = 0; y < d.height; y += image.getIconHeight() ) 
						g.drawImage( image.getImage(), x, y, null, null ); 
						*/
				System.out.println(image.getImageLoadStatus());
				if(image.getImage() != null) {
				g.drawImage( image.getImage(), 10, 10, null, null ); 
				// Now let the paint do its usual work 
				super.paint(g); 
				}
			} 
		}; 
	
		//make the table transparent 
		imTable.setOpaque(false); 
	
		JScrollPane jsp = new JScrollPane(imTable); 
		frame.getContentPane().add(jsp); 
		frame.pack(); 
		frame.show(); 
	} 
}
