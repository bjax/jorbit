SHELL	= /bin/sh
PROGS	= `ls *.c | sed 's/\.c$$//'` 

# If you want to run a debugger on these programs, remove the -s from LDFLAGS
# and change the -O in CFLAGS to -g.
CFLAGS	= -float -prototypes -g
LDFLAGS = -lgl_s -lc_s -lm 

default all:
	@${MAKE} ${PROGS}

clean clobber:
	rm -f ${PROGS} *.o
