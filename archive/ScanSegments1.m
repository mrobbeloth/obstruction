
function [Segment,scan_time] = ScanSegments1 (I) 
% the scansegmens1 function takes a vector of labels 
% and returns the 2xn matrix with each row consisting
% of a binary image and time to create that image?

% Create a matrix with all rows and all columns of a label
 I = I(1:end,1:end,1);
 % n x m x 1 ?
 sz = size (I); 
 % for all rows in the segment 
 for i = 1:1:sz(1)
     % for all columns in the segment
     for j=1:1:sz(2)
         % if the pixel is zero, give it a nonzero, why? for use with find later on
         if I (i,j)==0;
             I(i,j) = 1;
         end
     end
 end
 
% %% Scan region 
%[I] = KmeanProcedure (I);
Temp = I;
% sz = size (Temp);
% 
% for i=1:1:sz(1)
%     for j=1:1:sz(2)
%         if Temp(i,j)< 24 
%             Temp (i,j)=0;
%         else if Temp(i,j) > 24 && Temp(i,j)<200
%                 Temp(i,j) = 35;
%             else Temp(i,j) = 255;
%             end
%         end
%     end
% end

% convert the input image to double precision
Temp = im2double (Temp);

n = 1;
t = 1;

% find indices and values of nonzero elements -- move forward through the data structure (default)
% look in a forward direction in the temp matrix for the first non-zero value and places the indices
% into this vector 
[indx,indy] = find((Temp),1,'first');
Segment = [];

while t  
if isempty(indx)
   break
end    
clear ind

% copy indices so they don't get copied or for style
i = indx;
j = indy;

% start a timer
tic

% pass the image segment to the region growing code along with the 
% coordinates of the seed and max intensity distance of 1x10e-5
[J,Temp] = regiongrowing (Temp, i, j, 0.00001);

% pad the resulting image with a 3x3 matrix of zeros
J = padarray(J, [3,3]);

Segment(:,:,n)= J; 
% figure(n), imshow(Segment(:,:,n))
n=n+1;
scan_time(n-1) = toc;

% find the next one 
[indx,indy] = find(Temp,1, 'first');

end
avg_time = mean(scan_time)
end
 
