function [border,chain_time,cc,start] = chaincoding1(img)
    directions = [ 1, 0
                   1,-1
                   0,-1
                  -1,-1
                  -1, 0
                  -1, 1
                   0, 1
                   1, 1];

    tic
    [indx,indy] = find((img),1);
    sz = size(img);
%     for i=2:sz(1)
%         for j=2:sz(2)
%             if img(i,j)~=img(i,j-1)
%                 indy=j-1;
%                 indx=i;
%                 break;
%             end
%         end
%     end

    cc = [];       % The chain code
    coord = [indx, indy]; % Coordinates of the current pixel
    start = coord;
    dir = 1;       % The starting direction
    while 1
       newcoord = coord + directions(dir+1,:);
       if  all(newcoord < sz) && img(newcoord(1),newcoord(2) )
          cc = [cc,dir];
          coord = newcoord;
          dir = mod(dir+2,8);
       else
          dir = mod(dir-1,8);
       end
       if all(coord==start) && dir==1 % back to starting situation
          break;
       end
    end

    border = zeros(sz);
    coords = start;
    for ii=1:length(cc)
       border(coords(1),coords(2)) = 1;
       coords = coords + directions(cc(ii)+1,:);
    end
    % joinchannels('rgb',img,border')*255

    % stride = [sz(2);1];
    % step = directions*stride;
    % indx = cumsum([start*stride;step(cc+1)]);
    chain_time = toc;
    % border = newim(sz,'bin');
    % border(indx) = 1;
