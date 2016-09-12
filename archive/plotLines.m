function lined = plotLines(bbw, coords)
    
%      for(k = 1:size(coords,1))
        cpts = linspace(coords(1,1),coords(2,1),1000);%points from x1 to x2
        rpts = linspace(coords(1,2),coords(2,2),1000);%points from y1 to y2
        [r,c] = size(bbw);
        index = sub2ind([r c],round(cpts),round(rpts));
        bbw(index) = 1;
%      end
    
    figure(100);imagesc(bbw);
    colormap gray;
    lined = bbw;
end