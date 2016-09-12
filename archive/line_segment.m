 function [segment_x, segment_y, segment_time]=line_segment(cc, start, sensitiv, count)
directions = [ 1, 0
                   1,-1
                   0,-1
                  -1,-1
                  -1, 0
                  -1, 1
                   0, 1
                   1, 1];

    tic
    lines = 1;
    
    coords = start;
    runningMean = 0;
    runningMean1 = 0;
    curMean = 0;
    start_line = 2;
    end_line = 2;
    if1 =0;
    if2 =0;

    count1 = 0;
    for ii=2:length(cc)
        aa = mod(count1/sensitiv,2);
        if(count1==0)
            index_sens_start = ( start_line(lines) );
            index_sens_end = index_sens_start;
        else
            index_sens_start = (start_line(lines)+( (count1/sensitiv)-1 )*sensitiv);
            index_sens_end = (start_line(lines)+( (count1/sensitiv) )*sensitiv)-1   ;
        end
        count1;
        if( aa==1);        
%             runningMean1 = floor(runningMean/sensitiv);
                runningMean1 = ( mean(cc( index_sens_start:index_sens_end )) );
                cc( index_sens_start:index_sens_end );
        elseif( aa==0);        
%             curMean = floor(runningMean/sensitiv);
              curMean = (mean(cc( index_sens_start:index_sens_end )) );
              cc( index_sens_start:index_sens_end );
%               count1 =0;
        end
        if( ii==2 )%abs(curMean - runningMean1) < 2 )        
                if1=if1+1;                
                segment_x(lines,1) = coords(1);
                segment_y(lines,1) = coords(2);
            
%             runningMean = runningMean + cc(ii);
           elseif( abs(curMean - runningMean1) > 1 )
                runningMean1 = curMean;
                end_line(lines) = ii;
                segment_x(lines,2) = coords(1);
                segment_y(lines,2) = coords(2);              
                runningMean = 0;
                lines = lines+1;
                count1 = 0;
                start_line(lines) = ii;%%% new start 
                temp_coords =  coords + directions(cc(ii-1)+1,:); 
                segment_x(lines,1) = temp_coords(1);
                segment_y(lines,1) = temp_coords(2);
            
            end    
        border(coords(1),coords(2)) = 1;
        coords = coords + directions(cc(ii-1)+1,:);   
        count1=count1+1;
    end
    segment_time = toc;

