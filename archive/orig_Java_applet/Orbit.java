/*
   020110 E. B. Jackson for Orbit
*/

package gov.nasa.larc.bjax.Orbit;

import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;
import com.metrowerks.AppletFrame;


public class Orbit extends Applet
{
	static final double initial_dt = 1000.; 	// seconds per step
	static final double initial_m2pix = 1e-9; 	// 1 million km per pixel;
	
	public static int run = 1;
	
	public void init()
	{
		SolarSystem ss;
		ButtonPanel buttonPanel;
		
		setLayout( new BorderLayout() );
		setBackground( Color.black );
		
		ss = new SolarSystem( initial_m2pix, initial_dt );
		add( "Center", ss );

		buttonPanel = new ButtonPanel( ss );
		add( "West", buttonPanel );

		ss.start();
	} 

	public static void main(String args[])	// needed for standalone application
	{		
		AppletFrame.startApplet("gov.nasa.larc.bjax.Orbit", "Orbit", args);
	}

	public String getAppletInfo()
	{
		return "Hello";
	}
	
	static public void quitit()
	{
		System.exit(0);
	}
}

class ButtonPanel extends Panel {

	SolarSystem ss;

	public ButtonPanel( SolarSystem theSS ) {
		super(new GridLayout(14,1));
		
		ss = theSS;
		
		Button quit = new Button("Quit");
		quit.addActionListener( new ActionListener() { // anonymous inner class
			public void actionPerformed(ActionEvent e) { Orbit.quitit(); };
		});
		
		Button sol  = new Button("Sol");
		Button home = new Button("Home");
		Button merc = new Button("Mercury");
		Button venu = new Button("Venus");
		Button eart = new Button("Earth");
		Button mars = new Button("Mars");
		Button jupi = new Button("Jupiter");
		Button satu = new Button("Saturn");
		Button uran = new Button("Uranus");
		Button nept = new Button("Neptune");
		Button plut = new Button("Pluto");
		Button zoomin = new Button("+");
		Button zoomout = new Button("-");
		
		sol.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 0 ); }
		} );
				
		merc.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 2 ); }
		} );
				
		venu.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 3 ); }
		} );
				
		eart.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) { ss.setCenter( 4, 9 ); }
		} );

		mars.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 5 ); }
		} );
				
		jupi.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 6, 7 ); }
		} );
				
		satu.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 7 ); }
		} );
				
		uran.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 8 ); }
		} );
				
		nept.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 9 ); }
		} );
				
		plut.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.setCenter( 10 ); }
		} );
				
		zoomin.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.zoomIn(); }
		} );

		zoomout.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.zoomOut(); }
		} );
		
		home.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) { ss.home(); }
		} );

	// Add buttons to control panel
	
		this.add( home    );
		this.add( zoomin  );
		this.add( zoomout );
		this.add( sol     );
		this.add( merc    );
		this.add( venu    );
		this.add( eart    );
		this.add( mars    );
		this.add( jupi    );
		this.add( satu    );
		this.add( uran    );
		this.add( nept    );
		this.add( plut    );
		this.add( quit    );
		
	}
}

