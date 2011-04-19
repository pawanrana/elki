package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.AbstractFilteredIterator;

/**
 * Visualizer utilities.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses VisualizationTask - - inspects
 */
public final class VisualizerUtil {
  /**
   * Utility function to test for Visualizer visibility.
   * 
   * @param task Visualization task
   * @return true when visible
   */
  public static boolean isVisible(VisualizationTask task) {
    // Currently enabled?
    Boolean enabled = task.getGenerics(VisualizationTask.META_VISIBLE, Boolean.class);
    if(enabled == null) {
      enabled = task.getGenerics(VisualizationTask.META_VISIBLE_DEFAULT, Boolean.class);
    }
    if(enabled == null) {
      enabled = true;
    }
    return enabled;
  }

  /**
   * Utility function to test for a visualizer being a "tool".
   * 
   * @param vis Visualizer to test
   * @return true for a tool
   */
  public static boolean isTool(VisualizationTask vis) {
    // Currently enabled?
    Boolean tool = vis.getGenerics(VisualizationTask.META_TOOL, Boolean.class);
    return (tool != null) && tool;
  }

  /**
   * Filter for number vector field representations
   * 
   * @param result Result to filter
   * @return Iterator over suitable representations
   */
  // TODO: move to DatabaseUtil?
  public static Iterator<Relation<? extends NumberVector<?, ?>>> iterateVectorFieldRepresentations(final Result result) {
    final Iterator<Relation<?>> parent = ResultUtil.filteredResults(result, Relation.class);
    return new AbstractFilteredIterator<Relation<?>, Relation<? extends NumberVector<?, ?>>>() {
      @Override
      protected Iterator<Relation<?>> getParentIterator() {
        return parent;
      }

      @SuppressWarnings("unchecked")
      @Override
      protected Relation<? extends NumberVector<?, ?>> testFilter(Relation<?> nextobj) {
        final SimpleTypeInformation<?> type = nextobj.getDataTypeInformation();
        if(!NumberVector.class.isAssignableFrom(type.getRestrictionClass())) {
          return null;
        }
        if(!(type instanceof VectorFieldTypeInformation)) {
          return null;
        }
        return (Relation<? extends NumberVector<?, ?>>) nextobj;
      }
    };
  }

  /**
   * Test whether a thumbnail is enabled for this visualizer.
   * 
   * @param vis Visualizer
   * @return boolean
   */
  public static boolean thumbnailEnabled(VisualizationTask vis) {
    Boolean nothumb = vis.getGenerics(VisualizationTask.META_NOTHUMB, Boolean.class);
    return (nothumb == null) || !nothumb;
  }

  /**
   * Test whether a detail plot is available for this task.
   * 
   * @param vis Task
   * @return boolean
   */
  public static boolean detailsEnabled(VisualizationTask vis) {
    Boolean nodetail = vis.getGenerics(VisualizationTask.META_NODETAIL, Boolean.class);
    return (nodetail == null) || !nodetail;
  }
}