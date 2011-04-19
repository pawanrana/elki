package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Collection;
import java.util.Iterator;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Cluster order visualizer.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ClusterOrderResult oneway - - visualizes
 */
// TODO: listen for CLUSTER ORDER changes.
public class ClusterOrderVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> implements DataStoreListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Predecessor Graph";

  /**
   * CSS class name
   */
  private static final String CSSNAME = "predecessor";

  /**
   * The result we visualize
   */
  protected ClusterOrderResult<?> result;

  public ClusterOrderVisualization(VisualizationTask task) {
    super(task);
    result = task.getResult();
    context.addDataStoreListener(this);
    incrementalRedraw();
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDataStoreListener(this);
  }

  @Override
  public void redraw() {
    CSSClass cls = new CSSClass(this, CSSNAME);
    context.getStyleLibrary().lines().formatCSSClass(cls, 0, context.getStyleLibrary().getLineWidth(StyleLibrary.CLUSTERORDER));

    svgp.addCSSClassOrLogError(cls);

    for(ClusterOrderEntry<?> ce : result) {
      DBID thisId = ce.getID();
      DBID prevId = ce.getPredecessorID();
      if(thisId == null || prevId == null) {
        continue;
      }
      double[] thisVec = proj.fastProjectDataToRenderSpace(rep.get(thisId));
      double[] prevVec = proj.fastProjectDataToRenderSpace(rep.get(prevId));

      Element arrow = svgp.svgLine(prevVec[0], prevVec[1], thisVec[0], thisVec[1]);
      SVGUtil.setCSSClass(arrow, cls.getName());

      layer.appendChild(arrow);
    }
  }

  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent e) {
    synchronizedRedraw();
  }

  /**
   * Visualize an OPTICS cluster order by drawing connection lines.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ClusterOrderVisualization oneway - - «create»
   * 
   * @param <NV> object type
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ClusterOrderVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext context, Result result) {
      Iterator<Relation<? extends NumberVector<?, ?>>> reps = VisualizerUtil.iterateVectorFieldRepresentations(context.getDatabase());
      for(Relation<? extends NumberVector<?, ?>> rep : IterableUtil.fromIterator(reps)) {
        Collection<ClusterOrderResult<DoubleDistance>> cos = ResultUtil.filterResults(result, ClusterOrderResult.class);
        for(ClusterOrderResult<DoubleDistance> co : cos) {
          final VisualizationTask task = new VisualizationTask(NAME, context, co, rep, this, P2DVisualization.class);
          task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
          task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA - 1);
          context.addVisualizer(co, task);
        }
      }
    }

    @Override
    public Class<? extends Projection> getProjectionType() {
      return Projection2D.class;
    }
  }
}