

% produce the segmented image using Kmean algorithm
labels = kmeanfun;
% scan the image and produce one binary image for each segment 
[Segment,scan_time] = ScanSegments1 (labels);
% calculate the local global graph
[border,skeleton,segm_skeleton,T,C]=localGlobal_graph(Segment,labels);