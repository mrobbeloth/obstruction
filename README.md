# obstruction
Obstruction is a research code for working with incomplete objects using LG-Graph approach and probablistic matching. This codebase supports by PhD research and dissertation. It is built using Java and uses Eclipse as it's default host IDE. 

Using these tools: 
1. Java 8.0 (Oracle)
2. Eclipse Oxygen 4.7.3a
3. OpenCV 4.0 -- https://docs.opencv.org 
4. Apache POI 3.11 (jar are in lib folder of this project) 
5. PDFBox 1.8.8 (jar are in lib folder of this project) 
6. Apache Commons I/O 2.4 (jar are in lib folder of this project)  
7. Plplot 5.13 or later (libgd needed to support jpeg setopt call in code) 

Prerequisite tools (mainly for building opencv source)
1. Boost for C++ (on ubuntu `sudo apt-get install libboost-all-dev` will 
install all needed packages)
2. Git 
3. CMake (3.6 or later) on Ubuntu 16.04, refer to 
https://askubuntu.com/questions/355565/how-do-i-install-the-latest-version-of-cmake-from-the-command-line
for help in installing 3.12 (much later than 3.6) cmake
4. GCC/G++ (5.x or higher) with C++ 11 support (C++ 17 is better)
5. GTK 2.x or higher
6. Python 2.6 or higher
7. Numpy 1.5 or higher
8. tbb, tbb2, dc1394 2.x, jpeg, png, tiff, jasper, and 1394-22 dev libraries
9. ffmpeg, avcodec, avformat, and swscale dev libraries

In general, follow directions at http://docs.opencv.org/3.0-last-rst/doc/tutorials/introduction/linux_install/linux_install.html

Note: if building plplot from source, many of the drivers can be found w/ installation of:
sudo apt-get install libgd2-xpm-dev
