#

BJAX_CLASS_DIR = gov/nasa/larc/bjax

ORBIT_CLASS_DIR = $(BJAX_CLASS_DIR)/Orbit

CLASSFILES = \
	$(ORBIT_CLASS_DIR)/Orbit.class			\
	$(ORBIT_CLASS_DIR)/SolarSystem.class		\
	$(ORBIT_CLASS_DIR)/Body.class			\
	$(ORBIT_CLASS_DIR)/ButtonPanel.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$1.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$2.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$3.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$4.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$5.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$6.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$7.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$8.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$9.class		\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$10.class	\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$11.class	\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$12.class	\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$13.class	\
	$(ORBIT_CLASS_DIR)/ButtonPanel$$14.class

make: orbit.jar

orbit.jar: $(CLASSFILES) Manifest.txt com/metrowerks/AppletFrame.class
	jar cfm orbit.jar Manifest.txt $(CLASSFILES) com/metrowerks/AppletFrame.class

$(CLASSFILES) : orbit.java SolarSystem.java
	javac -classpath ../myclasses -d . orbit.java SolarSystem.java


