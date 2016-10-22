function [Label] = kmeanfun()

fprintf('\nChoose an image for Segmentation (name.type)\t');
    Input=input('','s');
    Input=imread(Input);
    Input = Input(1:end,1:end,1);
    Input=im2double(Input);
    [nrows,ncolumns]=size(Input);
    fprintf('\nInsert number of clusters from 2 to 16\t');
    nclusters=input('');
    fprintf('\nInsert number of iterations 16\t');
    niterations=input('');
     
    %%%%%%%%%% random seed %%%%%%%%%% 
    
    % random test data cell2.pgm 16 clusters, 16 iterations
%          Temprows(1) = 29;
%          Temprows(2) = 114;
%          Temprows(3) =  15;
%          Temprows(4) = 25;
%          Temprows(5) = 171;
%          Temprows(6) = 79;
%          Temprows(7) = 108;
%          Temprows(8) = 168;
%          Temprows(9) = 179;
%          Temprows(10) = 27;
%          Temprows(11) = 69;
%          Temprows(12) = 52;
%          Temprows(13) = 122;
%          Temprows(14) = 101;
%          Temprows(15) = 36;
%          Temprows(16) = 48;
%          
%          Tempcol(1) = 116;
%          Tempcol(2) = 34;
%          Tempcol(3) = 16;
%          Tempcol(4) = 4;
%          Tempcol(5) = 243;
%          Tempcol(6) = 44;
%          Tempcol(7) = 189;
%          Tempcol(8) = 212;
%          Tempcol(9) = 167;
%          Tempcol(10) = 246;
%          Tempcol(11) = 61;
%          Tempcol(12) = 25;
%          Tempcol(13) = 12;
%          Tempcol(14) = 148;
%          Tempcol(15) = 91;
%          Tempcol(16) = 141;
        
         Temprows=randsample(nrows,nclusters);
         Tempcol=randsample(ncolumns,nclusters);
         Indrows = Temprows;
         Indcolumns = Tempcol;
  
    
    counter=0;
    for k=1:1:nclusters
    avIntensity(k)=Input(Indrows(k),Indcolumns(k));
    end
    
while counter < niterations
    
    for k=1:1:nclusters %%%% assign the cluster center %%%%%%
            ClusterCenter(k,1) = Indrows(k);
            ClusterCenter(k,2) = Indcolumns(k);
            ClusterCenter(k,3) = avIntensity(k);
            count(k)=0;
            sumInt(k)=0;
            sumy(k) = 0;
            sumx(k) = 0;
    end
    %%%%%%%%%%%%%%%%%%%%%%%%%%% assign pixel to clusters %%%%%%%%%%%%%%%%%%%%%%%%%%% 
     for i=1:1:nrows
        for j=1:1:ncolumns                    
            for k=1:1:nclusters
               distance(k)= sqrt(power((i-Indrows(k)),2) + power((j-Indcolumns(k)),2) + power((255*Input(i,j))- ClusterCenter(k,3),2));    
            end   
            [Intensity, Cluster] = min (distance);
            Length=255/k;
            Label(i,j)= Length*Cluster;
            count(Cluster) = count(Cluster)+1;
            sumInt(Cluster)= sumInt(Cluster) + Input(i,j);
            sumy(Cluster) = sumy(Cluster) + i;
            sumx(Cluster) = sumx(Cluster) + j;
        end
     end
     
     for k=1:1:nclusters
         avrows(k) = round(sumy(k)/count(k));  % average of rows
         avcolumns(k) = round(sumx(k)/count(k)); % average of columns
         avIntensity(k) = round(255*(sumInt(k)/count(k)));  % average of intensity
     end
     
       Indrows=avrows;
       Indcolumns=avcolumns;
    
         ClusterCenter            
         iteration=counter+1
         hold off
         %subplot(1,2,1), imshow(Input)
         %subplot(1,2,2), 
         imagesc(Label)
         hold on
         pause (0.02)
         counter=counter+1;                 
                      
end     


for i=3:1:nrows-2
    for j=3:1:ncolumns-2
        count_pixel = 0;
        for l = i-2:1:i+2
            for k = j-2:1:j+2
                 if Label(l,k) == Label(i,j);
                     count_pixel = count_pixel + 1;
                 end
            end
        end
         if count_pixel > 18
             for l = i-1:1:i+1
                    for k = j-1:1:j+1
                    Label (l,k) = Label(i,j);
                    end
            end
        end
             
    end
end
figure (2) , imagesc (Label)

end

