/***************************************************************************

	TITLE:		orbit
	
----------------------------------------------------------------------------

	FUNCTION:	3-D Orbital Mechanics demo

----------------------------------------------------------------------------

	MODULE STATUS:	Developmental

----------------------------------------------------------------------------

	GENEALOGY:	Created 930716 as a test of GL capabilities

----------------------------------------------------------------------------

	DESIGNED BY:	E. B. Jackson
	
	CODED BY:	E. B. Jackson
	
	MAINTAINED BY:	E. B. Jackson

----------------------------------------------------------------------------

	MODIFICATION HISTORY:
	
	DATE	PURPOSE							BY

	951120	Changed all floats to doubles for speed increase; 
		added this header.					EBJ

	
	CURRENT RCS HEADER:

$Header: /net/ds9/home/bjax/orbit/RCS/orbit.c,v 1.2 1997/12/03 21:28:25 bjax Exp $
$Log: orbit.c,v $
Revision 1.2  1997/12/03 21:28:25  bjax
Changed window size to use define; slightly enlarged from 400 to
500x500.

Revision 1.1  1997/10/20 19:09:39  bjax
Initial revision


----------------------------------------------------------------------------

	REFERENCES:

----------------------------------------------------------------------------

	CALLED BY:

----------------------------------------------------------------------------

	CALLS TO:

----------------------------------------------------------------------------

	INPUTS:

----------------------------------------------------------------------------

	OUTPUTS:

--------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <gl/gl.h>
#include <gl/device.h>

#define RGB_BLACK   0x000000  
#define RGB_WHITE   0xffffff  

#define X	0
#define Y	1
#define Z	2
#define XY	2
#define XYZ	3

#define DEFWINDSIZE    500
#define NORMSIZE	0.001
#define BOUNDS	(1.0 + NORMSIZE)
#define EDGE	(1.1 * BOUNDS)
#define G 100
#define DECAY	0
#define RMIN    0.001

double boundary[4][XY] = {
    {-BOUNDS, -BOUNDS},
    {-BOUNDS,  BOUNDS},
    { BOUNDS,  BOUNDS},
    { BOUNDS, -BOUNDS}
};

struct mass_s
{
    double invmass;
    double size;
    double pos[XYZ];
    double vel[XYZ];
    double acc[XYZ];
    unsigned long col;
};

main(argc, argv)
int argc;
char *argv[];
{
    static char rcsid[] = "$Id: orbit.c,v 1.2 1997/12/03 21:28:25 bjax Exp $";
    int i, j;
    int nmass;
    long win1, win2;
    struct mass_s *mass;
    double total_mass, total_moment[XYZ], center_of_mass[XYZ];
    short val;
    long randseed;
    double a, ax, ay, az, dx, dy, dz, F, rsq, rinv, dt;

    if (getgdesc(GD_BITS_NORM_DBL_RED) == 0) {
	fprintf(stderr, "Double buffered RGB not available on this machine\n");
	return 1;
    }

    if (argc != 3) {
	fprintf(stderr, "Usage: %s <body count> <dt>\n", argv[0]);
	return 1;
    }

    nmass = atoi(argv[1]);

    if (!(mass = (struct mass_s *)malloc(nmass * sizeof(struct mass_s)))) {
	fprintf(stderr, "%s: malloc failed\n", argv[0]);
	return 1;

    }

    dt = atof(argv[2]);

    randseed = time(0);
    printf("randseed %ld\n", randseed);
    srand48(randseed);

    total_mass = 0.;
    for (i = 0; i < XYZ; i++) total_moment[i] = 0.;

    for (i = 0; i < nmass; i++) {
	for (j = 0; j < XYZ; j++) {
	    mass[i].pos[j] = 0.5 * (drand48() - 0.5);
	    mass[i].vel[j] = 0.0 * (drand48() - 0.5);
	}
	mass[i].invmass = .001*drand48() + 0.0001;
	mass[i].col = drand48() * 0xffffff;
	mass[i].size = 0.2*NORMSIZE/sqrt(mass[i].invmass);
	total_moment[0] += mass[i].pos[0]/mass[i].invmass;
	total_moment[1] += mass[i].pos[1]/mass[i].invmass;
	total_moment[2] += mass[i].pos[2]/mass[i].invmass;
	total_mass += 1./mass[i].invmass;
    }

    for (i = 0; i < XYZ; i++) center_of_mass[i] = total_moment[i]/total_mass;

    for (i = 0; i < nmass; i++)
	for ( j = 0; j < XYZ; j++ )
	    mass[i].pos[j] -= center_of_mass[j];

    prefsize(DEFWINDSIZE,DEFWINDSIZE);
    foreground();
    win1 = winopen(argv[0]);
    doublebuffer();
    RGBmode();
    gconfig();
    shademodel(FLAT);
    qdevice(ESCKEY);
    ortho2(-EDGE, EDGE, -EDGE, EDGE);

    while (!(qtest() && qread(&val) == ESCKEY && val == 0)) {
	for (i = 0; i < nmass; i++)
	{
	    mass[i].acc[X] = 0;
	    mass[i].acc[Y] = 0;
  	    mass[i].acc[Z] = 0;
	    for (j = 0; j < nmass; j++)
	      {
		if (i != j )
	    	{
		    dx = mass[i].pos[X] - mass[j].pos[X];
		    dy = mass[i].pos[Y] - mass[j].pos[Y];
		    dz = mass[i].pos[Z] - mass[j].pos[Z];
		    rsq = dx*dx + dy*dy + dz*dz;
		    if (rsq < RMIN) rsq = RMIN;
		    rinv = 1/sqrt(rsq);
		    F = G/rsq;
		    a = -F*mass[i].invmass;
		    mass[i].acc[X] += a*dx*rinv;
		    mass[i].acc[Y] += a*dy*rinv;
		    mass[i].acc[Z] += a*dz*rinv;
	    	}
	      }
        }


	for (i = 0; i < nmass; i++)
	{
	    mass[i].vel[X] = mass[i].vel[X] + dt*mass[i].acc[X];
	    mass[i].vel[Y] = mass[i].vel[Y] + dt*mass[i].acc[Y];
	    mass[i].vel[Z] = mass[i].vel[Z] + dt*mass[i].acc[Z];

	    mass[i].pos[X] = mass[i].pos[X] + dt*mass[i].vel[X];
	    mass[i].pos[Y] = mass[i].pos[Y] + dt*mass[i].vel[Y];
	    mass[i].pos[Z] = mass[i].pos[Z] + dt*mass[i].vel[Z];
		
	    if (mass[i].pos[X] >= 1.0)
		{
		    mass[i].pos[X] = 1.0;
		    mass[i].vel[X] = -mass[i].vel[X]*DECAY;
		}
	    if (mass[i].pos[X] <= -1.0)
		{
		    mass[i].pos[X] = -1.0;
		    mass[i].vel[X] = -mass[i].vel[X]*DECAY;
		}
	    if (mass[i].pos[Y] >= 1.0)
		{
		    mass[i].pos[Y] = 1.0;
		    mass[i].vel[Y] = -mass[i].vel[Y]*DECAY;
		}
	    if (mass[i].pos[Y] <= -1.0)
		{
		    mass[i].pos[Y] = -1.0;
		    mass[i].vel[Y] = -mass[i].vel[Y]*DECAY;
		}
	}

	cpack(RGB_BLACK);
	clear();
	cpack(RGB_WHITE);
	bgnclosedline();
	for (i = 0; i < 4; i++)
	    v2d(boundary[i]);
	endclosedline();
	for (i = 0; i < nmass; i++) {
	    cpack(mass[i].col);
	    circf(mass[i].pos[X], mass[i].pos[Y], mass[i].size);
	}

	swapbuffers();
    }
    gexit();
    return 0;
}

