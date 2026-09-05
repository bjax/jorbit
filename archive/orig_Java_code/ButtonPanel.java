//
//  ButtonPanel.java
//  orbit1
//
//  Created by bjax on Tue Mar 26 2002.
//  Copyright (c) 2001 __MyCompanyName__. All rights reserved.
//

import java.awt.*;

public class ButtonPanel extends Panel {
	SolarSystem ss;

	public ButtonPanel( SolarSystem theSS ) {
		super(new GridLayout(14,1));
		
		ss = theSS;
		
		Button quit = new Button("Quit");
//		quit.addActionListener( new java.awt.event.ActionListener() { // anonymous inner class
//			public void actionPerformed(java.awt.event.ActionEvent e) { orbit1.handleQuit(); };
//		});
		
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
		
		sol.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 0 ); }
		} );
				
		merc.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 2 ); }
		} );
				
		venu.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 3 ); }
		} );
				
		eart.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 4, 9 ); }
		} );

		mars.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 5 ); }
		} );
				
		jupi.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 6, 7 ); }
		} );
				
		satu.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 7 ); }
		} );
				
		uran.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 8 ); }
		} );
				
		nept.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 9 ); }
		} );
				
		plut.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.setCenter( 10 ); }
		} );
				
		zoomin.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.zoomIn(); }
		} );

		zoomout.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.zoomOut(); }
		} );
		
		home.addActionListener( new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) { ss.home(); }
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
