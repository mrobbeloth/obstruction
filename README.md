# obstruction
Obstruction is a research code for working with incomplete objects using LG-Graph approach and probablistic matching. This codebase supports by PhD research and dissertation. It is built using Java and uses Eclipse as it's default host IDE. 

Using these tools: 
1. Java 8.0 (Oracle)
2. Eclipse Luna
3. OpenCV 3.0 -- follow the tutorial at http://docs.opencv.org/3.0.0/d1/d0a/tutorial_java_eclipse.html but use 
   /usr/local/lib for native library location and /usr/local/share/OpenCV/java/ for the location of the 
   opencv-<version>.jar (for 3.0 it's opencv-300.jar).
4. Apache POI 3.11 (jar are in lib folder of this project) 
5. PDFBox 1.8.8 (jar are in lib folder of this project) 
6. Apache Commons I/O 2.4 (jar are in lib folder of this project)  

Possible errors:
1."Failed to load OpenCL runtime"
   sudo apt-get install libopencv-gpu-dev