import java.awt.*;

class SolarSystem extends Canvas implements Runnable {
	
	static final double minRadius = 10.;
	static final double Kgravity = 6.672e-11; 	// N m^2 kg^-2
        static final double AU2m = 1.496e11;
	static final double SolMass = 332800.;		// in earth masses
	static final double earthMass = 5.976e24; 	// kg
	static final double re = 6.378e6; 			// m
	static final double year = 60.*60.*24.*365.25;	// secs per year
	
	public static double m2pix;
	public static double dt;

	Body Sol;
	Body Mercury;
	Body Venus;
	Body Earth;
	Body Moon;
	Body Mars;
	Body Jupiter;
	Body Saturn;
	Body Uranus;
	Body Neptune;
	Body Pluto;
	Body Callisto;
	Body Ganymede;
	Body Europa;
	Body Io;
	
	Body[] bodies = { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null };
	
	int centeredBody = 0;	// initially the Sun
	double time = 0;
	
	public SolarSystem( double IC_m2pix, double IC_dt ) {
		
		//					   R, AU;  Mass (em's);  r er's; color
		Sol     = new Body(    0.,  332800., 109.044, Color.yellow);
		Mercury = new Body(  .390,    0.05,   0.3825, Color.pink); 
		Venus   = new Body(  .720,    0.89,   0.9488, Color.green);
		Earth   = new Body( 1.000,    1.00,   1.0000, Color.blue); 
		Moon    = new Body( 1.00257,  1/80.,  0.2724, Color.gray);
		Mars    = new Body( 1.500,    0.11,   0.5326, Color.red); 
		Jupiter = new Body( 5.200,   318.0,  11.0000, Color.cyan);
		Callisto= new Body( 5.21259, 1.802e-2, .3768, Color.gray);
		Ganymede= new Body( 5.20715, .02482,  0.4130, Color.gray);
		Europa  = new Body( 5.20449, 8.04e-3, 0.2454, Color.gray);
		Io      = new Body( 5.20282, 1.49e-2, 0.2847, Color.gray);
		Saturn  = new Body( 9.500,    95.0,   9.0000, Color.magenta);
		Uranus  = new Body( 19.20,    17.0,   4.0000, Color.orange);
		Neptune = new Body( 30.10,    17.0,   4.0000, Color.white);
		Pluto   = new Body( 39.50,    0.002, 0.18,    Color.darkGray);
		
		Mercury.circularizeAbout(Sol);
		Venus.circularizeAbout(Sol);
		Earth.circularizeAbout(Sol);
		Mars.circularizeAbout(Sol);
		Jupiter.circularizeAbout(Sol);
		Saturn.circularizeAbout(Sol);
		Uranus.circularizeAbout(Sol);
		Neptune.circularizeAbout(Sol);
		Pluto.circularizeAbout(Sol);
		
		Moon.circularizeAbout(Earth);
		
		Callisto.circularizeAbout(Jupiter);
		Ganymede.circularizeAbout(Jupiter);
		Europa.circularizeAbout(Jupiter);
		Io.circularizeAbout(Jupiter);
		
		bodies[0] = Sol;		// Keep these indices so buttons work
		bodies[1] = Moon;
		bodies[2] = Mercury;
		bodies[3] = Venus;
		bodies[4] = Earth;
		bodies[5] = Mars;
		bodies[6] = Jupiter;
		bodies[7] = Saturn;
		bodies[8] = Uranus;
		bodies[9] = Neptune;
		bodies[10] = Pluto;
		bodies[11] = Callisto;
		bodies[12] = Ganymede;
		bodies[13] = Europa;
		bodies[14] = Io;
		
		m2pix = IC_m2pix;
		dt = IC_dt;
		
		repaint();
	}
	
	public void home()
	{
		centeredBody = 0;
		this.m2pix = orbit1.initial_m2pix;
		this.dt = orbit1.initial_dt;
		this.repaint();
	}
	
	public void setCenter( int planetNumber )
	{
		centeredBody = planetNumber;
		this.repaint();
	}
	
	public void setCenter( int planetNumber, int Magnification_power )
	{
		centeredBody = planetNumber;
		this.m2pix = orbit1.initial_m2pix*Math.pow(2,Magnification_power);
		this.repaint();
	}
	
	public void zoomIn()
	{
		this.m2pix = 2.0*m2pix;
		this.repaint();
	}
	
	public void zoomOut()
	{
		this.m2pix = 0.5*m2pix;
		this.repaint();
	}
	
	boolean dragging = false;
	
	public synchronized void Propagate() {
		
		if ( dragging ) {
			return;
		}
		
		for( int i = 0; i < bodies.length; i++ )
		if( bodies[i] != null )
		{
			bodies[i].resetForces();
			for( int j = 0; j < bodies.length; j++ )
			{
				if( i != j )
				if( bodies[j] != null )
					bodies[i].addForce( bodies[j] );
			}
			bodies[i].Integrate( dt );
		}
		
		time = time + dt;
		
		Graphics g = getGraphics();
		Rectangle b = bounds();
		
		int boundsHCenter = b.width  / 2;
		int boundsVCenter = b.height / 2;
		
		int hCenter = boundsHCenter - (int) Math.round(bodies[ centeredBody ].getX()*m2pix);
		int vCenter = boundsVCenter + (int) Math.round(bodies[ centeredBody ].getY()*m2pix);
		
		if ( g != null )
		{
		for( int i = 0; i < bodies.length; i++ )
			if ( bodies[i] != null )
					bodies[i].draw( g, hCenter, vCenter );	// draw relative to this point
		}
	}
		
	Thread thread = null;
	
	public void start() {
		if ( thread == null ) {
			thread = new Thread( this );
			thread.start();
		}
	}
	
	public void stop() {
		if ( thread != null && thread.isAlive() ) {
			thread.stop();
		}
		thread = null;
	}
	
	public void run() {
		while (true) {
			try { Thread.sleep(5); } catch (InterruptedException e){};
			if ( isVisible() && (orbit1.run == 1) ) {
				Propagate();
			}
		}
	}
	
}

class Body {

	double X, Y, mass;
	double xForce, yForce;
	double U, V;
	double Udot, Vdot;
	double radius;
	Color color;
		
	// Constructor: initialize all fields to default values
	public Body() {this(0., 0., 1.0, 0., 0., 1.0, Color.black); }
	
	// Explicit constructor
	public Body(double X_ic_AU, double Y_ic_AU, double rel_mass, double U_ic, double V_ic, double Radius_re, Color theColor) 
	{
		this.X = SolarSystem.AU2m*X_ic_AU; 
		this.Y = SolarSystem.AU2m*Y_ic_AU; 
		this.mass = SolarSystem.earthMass*rel_mass;
		this.U = U_ic;
		this.V = V_ic;
		this.Udot = 0.;
		this.Vdot = 0.;
		this.xForce = 0.;
		this.yForce = 0.;
		this.radius = SolarSystem.re*Radius_re;
		this.color = theColor;
	}
	
	// Most useful constructor
	public Body(double orbital_radius_AU, double Mass_me, double Radius_re, Color theColor) //, double planet_radius_re)
	{
		this(0., -orbital_radius_AU, Mass_me, 0., 0., Radius_re, theColor);
	}
	
	
	public double getX() {return this.X;}
	public double getY() {return this.Y;}
	
	public double getU() {return this.U;}
	public double getV() {return this.V;}
	
	public double getMass() { return this.mass; }
	
	public void setColor( Color theColor ) { this.color = theColor; }

	public void resetForces() { this.xForce = 0.; this.yForce = 0.; }
		
	public void circularizeAbout( Body centerMass )
	{
		double xDelta = this.getX() - centerMass.getX();
		double yDelta = this.getY() - centerMass.getY();
		double R = java.lang.Math.sqrt( xDelta*xDelta + yDelta*yDelta );
		double Vel_ic = 0.;
		this.U = centerMass.getU();
		this.V = centerMass.getV();
		if (R > 0.)
		{
			Vel_ic = java.lang.Math.sqrt(SolarSystem.Kgravity*centerMass.getMass()/R);
			
			this.U = this.U - Vel_ic*yDelta/R;
			this.V = this.V - Vel_ic*xDelta/R;
		}
	}

	public void addForce( Body otherBody )
	{
		double deltaX = otherBody.getX() - this.X;
		double deltaY = otherBody.getY() - this.Y;
		
		double radius = Math.sqrt( deltaX*deltaX + deltaY*deltaY );
		if( radius < SolarSystem.minRadius)
			radius = SolarSystem.minRadius;
			
		double Force = SolarSystem.Kgravity*this.mass*otherBody.getMass()/(radius*radius);
		
		this.xForce = this.xForce + Force*deltaX/radius;
		this.yForce = this.yForce + Force*deltaY/radius;	
	}
	
	public void draw( Graphics g, int hCenter, int vCenter)
	{
		int radius = (int) (Math.ceil(this.radius)*SolarSystem.m2pix);
		if (radius < 1) radius = 1;
		int left = hCenter + (int) Math.round( this.X*SolarSystem.m2pix ) - radius;
		int top  = vCenter - (int) Math.round( this.Y*SolarSystem.m2pix ) - radius;
		int width = radius*2;
		int height = width;
		g.setColor(this.color);
		g.fillOval(left, top, width, height);
	}
	
	public void Integrate( double dt )
	{
		this.Udot = this.xForce/this.mass;
		this.Vdot = this.yForce/this.mass;
		
		this.U = this.U + dt * this.Udot;
		this.V = this.V + dt * this.Vdot;
		
		this.X = this.X + dt * this.U;
		this.Y = this.Y + dt * this.V;
	}
}