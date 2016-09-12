function [angle,angle1,angle_time] = segment_angles (segment_x, segment_y)

tic 
for(i=1:length(segment_x)-1)
    slope(i) = (segment_x(1,1) - segment_x(1,i+1) ) / (segment_y(1,1) - segment_y(1,i+1) );
    
end

for(i=1:length(slope))
    for(j=1:length(slope))
        s1 = atan(slope(1)/100);
        s2 = atan(slope(i+1)/100);
%         if(s1 < s2)    
            angle1(i,j) = s2-s1;
%         else
           
            angle(i,j) = 180+(s2-s1);
    %     angle(i) = atan( angle(i) );
    %     if(angle(i)<0 || angle(i)>180 )
    %         angle(i) = 180+abs(angle(i));
%     end
    end
end
angle_time = toc;
end