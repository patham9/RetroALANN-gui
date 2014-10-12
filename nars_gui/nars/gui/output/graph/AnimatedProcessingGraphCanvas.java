package nars.gui.output.graph;

import nars.gui.output.graph.layout.FastOrganicLayout;
import org.jgrapht.graph.DirectedMultigraph;

/**
 *
 * @author me
 */


public class AnimatedProcessingGraphCanvas<V,E> extends ProcessingGraphCanvas<V,E> {
    DirectedMultigraph<V, E> graph;
    //private final FastOrganicLayout layout;
    private final FastOrganicLayout layout;

    public AnimatedProcessingGraphCanvas() {
        this(null);        
    }
    
    public AnimatedProcessingGraphCanvas(DirectedMultigraph<V,E> graph) {
        super();
        this.graph = graph;        
        setUpdateNext();
        layout = new FastOrganicLayout();
    }

    
    @Override
    protected DirectedMultigraph<V,E> getGraph() {
        if (graph!=null)
            return (DirectedMultigraph)graph.clone();            
        
        //otherwise, should override in subclasses
        return null;
    }

    @Override
    protected void updateVertices() {
        if (currentGraph == null)
            return;
        
        scale = 10f;
        layout.setInitialTemp(10f);
        layout.setMinDistanceLimit(50f);
        layout.setMaxDistanceLimit(200f);
        
        layout.setMaxIterations(5);
        
        
        layout.execute(currentGraph, vertices);
        
    }

    @Override
    public void draw() {
        drawn = false;
        
        super.draw(); //To change body of generated methods, choose Tools | Templates.
    }
    
    

    @Override
    protected boolean hasUpdate() {
        //temporary:
        setUpdateNext();
        
        return true;
    }
    
}
