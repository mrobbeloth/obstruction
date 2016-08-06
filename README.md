# obstruction
Obstruction is a research code for working with incomplete objects using LG-Graph approach and probablistic matching. This codebase supports by PhD research and dissertation. It is built using Java and uses Eclipse as it's default host IDE. 

Using these tools: 
1. Java 8.0 (Oracle)
2. Eclipse Neon 4.6
3. OpenCV 3.0 -- follow the tutorial at http://docs.opencv.org/3.0.0/d1/d0a/tutorial_java_eclipse.html but use 
   /usr/local/lib for native library location and /usr/local/share/OpenCV/java/ for the location of the 
   opencv-<version>.jar (for 3.0 it's opencv-300.jar).
4. Apache POI 3.11 (jar are in lib folder of this project) 
5. PDFBox 1.8.8 (jar are in lib folder of this project) 
6. Apache Commons I/O 2.4 (jar are in lib folder of this project)  

Prerequisite tools (mainly for building opencv source)
1. Boost for C++ (on ubuntu `sudo apt-get install libboost-all-dev` will 
install all needed packages)
2. Git 
3. CMake (2.8.7)
4. GCC (4.4.x or higher)
5. GTK 2.x or higher
6. Python 2.6 or higher
7. Numpy 1.5 or higher
8. tbb, tbb2, dc1394 2.x, jpeg, png, tiff, jasper, and 1394-22 dev libraries
9. ffmpeg, avcodec, avformat, and swscale dev libraries
10. Doxygen
11. PlantUML

In general, follow directions at http://docs.opencv.org/3.0-last-rst/doc/tutorials/introduction/linux_install/linux_install.html

Possible errors:
1."Failed to load OpenCL runtime"
   sudo apt-get install libopencv-gpu-dev
