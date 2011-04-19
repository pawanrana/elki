package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;

/**
 * Provides the local neighborhood to be considered in the PCA as the neighbors
 * within an epsilon range query of an object.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.uses RangeQuery
 * 
 * @param <NV> Vector type
 */
@Title("Range Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on epsilon range queries.")
public class RangeQueryFilteredPCAIndex<NV extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex<NV> {
  // TODO: lose DoubleDistance restriction.
  /**
   * Logger.
   */
  private static final Logging logger = Logging.getLogger(RangeQueryFilteredPCAIndex.class);

  /**
   * The kNN query instance we use
   */
  final private RangeQuery<NV, DoubleDistance> rangeQuery;

  /**
   * Query epsilon
   */
  final private DoubleDistance epsilon;

  /**
   * Constructor.
   * 
   * @param database Database to use
   * @param pca PCA Runner to use
   * @param rangeQuery Range Query to use
   * @param epsilon Query range
   */
  public RangeQueryFilteredPCAIndex(Relation<NV> database, PCAFilteredRunner<? super NV, DoubleDistance> pca, RangeQuery<NV, DoubleDistance> rangeQuery, DoubleDistance epsilon) {
    super(database, pca);
    this.rangeQuery = rangeQuery;
    this.epsilon = epsilon;
  }

  @Override
  protected List<DistanceResultPair<DoubleDistance>> objectsForPCA(DBID id) {
    return rangeQuery.getRangeForDBID(id, epsilon);
  }

  @Override
  public String getLongName() {
    return "kNN-based local filtered PCA";
  }

  @Override
  public String getShortName() {
    return "kNNFilteredPCA";
  }

  @Override
  public Logging getLogger() {
    return logger;
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses RangeQueryFilteredPCAIndex oneway - - «create»
   */
  public static class Factory<V extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex.Factory<V, RangeQueryFilteredPCAIndex<V>> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered in the PCA, must be suitable to the distance function
     * specified.
     * 
     * Key: {@code -localpca.epsilon}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("localpca.epsilon", "The maximum radius of the neighborhood to be considered in the PCA.");

    /**
     * Holds the value of {@link #EPSILON_ID}.
     */
    protected DoubleDistance epsilon;

    /**
     * Constructor.
     * 
     * @param pcaDistanceFunction distance function
     * @param pca PCA
     * @param epsilon range value
     */
    public Factory(DistanceFunction<V, DoubleDistance> pcaDistanceFunction, PCAFilteredRunner<V, DoubleDistance> pca, DoubleDistance epsilon) {
      super(pcaDistanceFunction, pca);
      this.epsilon = epsilon;
    }

    @Override
    public RangeQueryFilteredPCAIndex<V> instantiate(Relation<V> representation) {
      // TODO: set bulk flag, once the parent class supports bulk.
      RangeQuery<V, DoubleDistance> rangequery = representation.getDatabase().getRangeQuery(representation, pcaDistanceFunction);
      return new RangeQueryFilteredPCAIndex<V>(representation, pca, rangequery, epsilon);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<NV extends NumberVector<?, ?>> extends AbstractFilteredPCAIndex.Factory.Parameterizer<NV, RangeQueryFilteredPCAIndex<NV>> {
      protected DoubleDistance epsilon = null;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        DistanceParameter<DoubleDistance> epsilonP = new DistanceParameter<DoubleDistance>(EPSILON_ID, pcaDistanceFunction != null ? pcaDistanceFunction.getDistanceFactory() : null);
        if(config.grab(epsilonP)) {
          epsilon = epsilonP.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<NV>(pcaDistanceFunction, pca, epsilon);
      }
    }
  }
}