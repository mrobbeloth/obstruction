
function [border,skeleton,segm_skeleton,T,C]=localGlobal_graph(Segments,im2)
%Segments = ScanSegments1 (img);

n = length(Segments(1,1,:));

lg_time = 0;


for i=1:1:n
   tic
    [border,chain_time,cc,start] = chaincoding1(Segments(:,:,1));
    t1(i) = chain_time;
    
    [x,y,segment_time] = line_segment(cc, start, 1);
    t2(i)=segment_time;
    lg_time(i) = t1(i) + t2(i);
  
    segm_skeleton = bwmorph(Segments(:,:,i),'skel',inf);
    segm_skeleton = im2double (segm_skeleton);
    skeleton(:,:,i) = segm_skeleton;
    S(i)  = regionprops(Segments(:,:,i), 'centroid');
   
    t(i) = toc;
end

T = sum (t);
for i = 1:1:n
    C(:,i) = S(1,i).Centroid;
end

    C = floor (C);
    
    
    tic
    for i = 1:1:n-1
        
        if i == n-1 
            DirVector1 = C(:,1)' - C(:,1+i)';
            DirVector2 = C(:,1)' - C(:,2)';
            Angle(i) =acos( dot(DirVector1,DirVector2)/norm(DirVector1)/norm(DirVector2) );
        else
            DirVector1 = C(:,1)' - C(:,1+i)';
            DirVector2 = C(:,1)' - C(:,2+i)';
            Angle(i)=acos( dot(DirVector1,DirVector2)/norm(DirVector1)/norm(DirVector2) );
        end
    end
    angle_time = toc;
    
   lined = im2;
   for i = 1:1:n-1
            coords = [C(2,1) C(1,1);C(2,i+1) C(1,i+1)];
            lined = plotLines(lined, coords);    
   end
    
    [y1] = zeros(size(border));
    figure(101), imshow (lined)
    hold on 
    for i = 1:1:n
        th = 0:pi/50:2*pi;
        xunit = 2 * cos(th) + C(1,i);
        yunit = 2 * sin(th) + C(2,i);
        h = plot(xunit, yunit,'r');
    end 
    

end