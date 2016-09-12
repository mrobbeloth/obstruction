function [border,lg_time]=local_graph(img)

[border,chain_time,cc,start] = chaincoding1(img);
[segment_time] = line_segment1(cc, start, 0.2);
lg_time = chain_time + segment_time;
chain_time
end