package occlusion;

import java.util.ArrayList;
import java.util.List;

/**
 * Bsp front to back occlusion culling span node.
 * 
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class BspOcclusionSpanNode {
    
    private static final int INITIAL_NODES_CACHE_SIZE = 50;
    
    private static class NodesCache {

        final List<BspOcclusionSpanNode> cache = new ArrayList<>();
        int index = 0;

        NodesCache(int initialSize) {
            for (int i = 0; i < initialSize; i++) {
                cache.add(new BspOcclusionSpanNode(this));
            }
        }

        BspOcclusionSpanNode get(int level, int start, int end) {
            if (index > cache.size() - 1) {
                cache.add(new BspOcclusionSpanNode(this));
            }
            BspOcclusionSpanNode node = cache.get(index++);
            node.reset(level, start, end);
            return node;
        }

        void reset() {
            index = 0;
        }

    }

    private final NodesCache nodesCache;
    private int level;
    private int start; 
    private int end; 
    private Integer partitionPoint; 
    private boolean occluded;
    private boolean partitioned;
    private BspOcclusionSpanNode left;
    private BspOcclusionSpanNode right;

    public BspOcclusionSpanNode(int start, int end) {
        this.level = 0;
        this.start = start;
        this.end = end;
        this.nodesCache = new NodesCache(INITIAL_NODES_CACHE_SIZE);
    }

    private BspOcclusionSpanNode(NodesCache nodesCache) {
        this.nodesCache = nodesCache;
    }
    
    public void reset() {
        nodesCache.reset();
        reset(0, start, end);
    }

    private void reset(int level, int start, int end) {
        this.level = level;
        this.start = start;
        this.end = end;
        //this.partitionPoint = (start + end) / 2; // center optional
        partitionPoint = null; 
        occluded = false;
        partitioned = false;
        left = null;
        right = null;
    }

    public int getLevel() {
        return level;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Integer getPartitionPoint() {
        return partitionPoint;
    }

    public boolean isOccluded() {
        return occluded;
    }

    public boolean isPartitioned() {
        return partitioned;
    }

    public BspOcclusionSpanNode getLeft() {
        return left;
    }

    public BspOcclusionSpanNode getRight() {
        return right;
    }

    // note: result list is empty if the span is completely occluded.
    //       otherwise it returns a pair of numbers that 
    //       indicate the start and end point of fragmented spans.
    public void addSpan(int start, int end, List<Integer> result) {
        if (occluded && start >= this.start && end <= this.end) {
            return;
        }
        
        if (start > this.end || end < this.start) {
            return;
        }
        
        if (start <= this.start) {
            start = this.start;
        }
        if (end >= this.end) {
            end = this.end;
        }
        
        // if the size of span is equal than the size 
        // of this partition and this is still not occluded,
        // then this can be marked as occluded and just return, 
        // BUT ONLY if this hasn't been partitioned yet.
        // if this is already partitioned, then it's necessary 
        // to keep the partition checks to find only parts of the 
        // span that are still visible.
        if (!occluded && !partitioned 
                && start <= this.start && end >= this.end) {
            
            if (!result.isEmpty() 
                    && result.get(result.size() - 1) == start - 1) {
                
                // merge continuos spans
                result.remove(result.size() - 1);
                result.add(end);
            }
            else {
                result.add(start);
                result.add(end);
            }
            occluded = true;
            return;
        }
        
        if (!partitioned) {
            if (partitionPoint == null) {
                if (start == this.start) {
                    partitionPoint = end;
                }
                else {
                    partitionPoint = start - 1;
                }
            }
            
            left = nodesCache.get(level + 1, this.start, partitionPoint);
            right = nodesCache.get(level + 1, partitionPoint + 1, this.end);
            partitioned = true;
        }
        
        if (start <= partitionPoint && end <= partitionPoint) {
            left.addSpan(start, end, result);
        }
        else if (start <= partitionPoint && end > partitionPoint) {
            left.addSpan(start, partitionPoint, result);
            right.addSpan(partitionPoint + 1, end, result);
        }
        else if (start > partitionPoint && end > partitionPoint) {
            right.addSpan(start, end, result);
        }

        if (left.occluded && right.occluded) {
            this.occluded = true;
        }
    }

}
