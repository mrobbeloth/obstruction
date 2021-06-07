# obstruction
Obstruction is a research code for working with incomplete objects using LG-Graph approach and probablistic matching. This codebase supports by PhD research and dissertation. It is built using Java and uses Eclipse as it's default host IDE. 

ABSTRACT
Robbeloth, Michael Christopher Ph.D., Computer Science and Engineering Ph.D. program, Wright State University, 2019.  Recognition of Incomplete Objects based on Synthesis of Views Using a Geometric Based Local-Global Graphs

The recognition of single objects is an old research field with many techniques and robust results. The probabilistic recognition of incomplete objects, however, remains an active field with challenging issues associated to shadows, illumination and other visual characteristics. With object incompleteness, we mean missing parts of a known object and not low-resolution images of that object. The employment of various single machine-learning methodologies for accurate classification of the incomplete objects did not provide a robust answer to the challenging problem. In this dissertation, we present a suite of high-level, model-based computer vision techniques encompassing both geometric and machine learning approaches to generate probabilistic matches of objects with varying degrees and forms of non-deformed incompleteness.  

The recognition of incomplete objects requires the formulation of a database of six sided exemplar (e.g., model) images from which an identification can be made. The images are broken down by means of image preprocessing operations, K-means segmentation, and region growing code to generate fully defined region and segment image information from which local and global geometric and characteristic properties are generated in a process known as the Local-Global (L-G) Graph method. The characteristic properties are then stored into a database for processing against sample images featuring various types of missing features. The sample images are then characterized in the same manner. After this, a suite of methodologies is employed to match a sample against an exemplar image in a multithreaded manner. The approaches, which work with the multi-view model database characteristics in a parallel (e.g, multithreaded manner) determine probabilistically by application of weighted outcomes the application of various matching routines. These routines include treating segment border regions as chain codes which are then processed using various string matching algorithms, the matching by center of moments from global graph construction, the matching of chain code starting segment location, the differences in angles in the center of moments between the model and sample images to find the most similar graphs (e.g., image), and the use of Delaunay triangulations of the center of moments formed during global graph construction. The ability to find a most probable match is extensible in the future to adding additional detection methods with the appropriate weight adjustments. 

To enhance the detection of incomplete objects, separate investigations have been made into rotating the exemplars in standard increments and by object extraction of segment border regions’ chain codes and subsequent synthesis of objects from the multi-view database. This approach is potentially extensible to compositing across multi-view segmented regions at the borders between views by either human aided input of border relations or a systematic, automated evaluation of common border objects between the views of an exemplar.  

Overall, the results, while initially promising, show that there is still much work to be done in the area of better recognizing incomplete objects. It is evident that there are many additional avenues to explore related to different detection methodologies along with performance enhancements to be employed across both computational and memory limited resources to drive the recognition of incomplete objects in production systems. 

Using these tools: 
1. Java 11.0 (OpenJDK)
2. Eclipse Oxygen 4.18 (2020-12)
3. OpenCV 4.0 -- https://docs.opencv.org 
4. Apache POI 3.11 (jar are in lib folder of this project) 
5. PDFBox 1.8.8 (jar are in lib folder of this project) 
6. Apache Commons I/O 2.4 (jar are in lib folder of this project)  
7. Plplot 5.13 or later (libgd needed to support jpeg setopt call in code) 
8. HSQLDB 2.4

Prerequisite tools (mainly for building opencv source)
1. Boost for C++ (on ubuntu `sudo apt-get install libboost-all-dev` will 
install all needed packages)
2. Git 
3. CMake (3.6 or later) on Ubuntu 18.04, refer to 
https://askubuntu.com/questions/355565/how-do-i-install-the-latest-version-of-cmake-from-the-command-line
for help in installing 3.12 (much later than 3.6) cmake
4. GCC/G++ (5.x or higher) with C++ 11 support (C++ 17 is better)
5. GTK 2.x or higher
6. Python 2.7 or higher
7. Numpy 1.5 or higher
8. tbb, tbb2, dc1394 2.x, jpeg, png, tiff, jasper, and 1394-22 dev libraries
9. ffmpeg, avcodec, avformat, and swscale dev libraries

In general, follow directions at http://docs.opencv.org/3.0-last-rst/doc/tutorials/introduction/linux_install/linux_install.html

Note: if building plplot from source, many of the drivers can be found w/ installation of:
sudo apt-get install libgd2-xpm-dev

Note 2: Updating Java requires rebuliding OpenCV
Note 3: For OpenCV LAPACK support on Ubuntu 18.04+, use liblapacke-dev
Note 4: For OpenCV EIGEN support on Ubuntu 18.04+, use libeigen3-dev
