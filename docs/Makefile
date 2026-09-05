IMAGES = images/XY.png \
	       images/gravity.png

make: Orbit.pdf Orbit.html

Orbit.pdf: Orbit.txt ${IMAGES}
	a2x -vf pdf -d article -a formatpdf Orbit.txt

Orbit.html: Orbit.txt ${IMAGES}
	a2x -vf xhtml -a latex -a formathtml Orbit.txt

clean:
	-rm *.md5
	-rm *.png
	-rm *.xml
	-rm *.tex
	-rm *.aux
	-rm *.log
