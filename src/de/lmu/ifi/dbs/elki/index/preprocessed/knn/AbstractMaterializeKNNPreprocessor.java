package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.List;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for KNN Preprocessors.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public abstract class AbstractMaterializeKNNPreprocessor<O, D extends Distance<D>> extends AbstractPreprocessorIndex<O, List<DistanceResultPair<D>>> implements KNNIndex<O> {
  /**
   * The query k value.
   */
  protected final int k;

  /**
   * The distance function to be used.
   */
  protected final DistanceFunction<? super O, D> distanceFunction;

  /**
   * The distance query we used.
   */
  protected final DistanceQuery<O, D> distanceQuery;

  /**
   * Holds the listener.
   */
  protected final EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor.
   * 
   * @param representation
   * @param distanceFunction
   * @param k
   */
  public AbstractMaterializeKNNPreprocessor(Relation<O> representation, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(representation);
    this.k = k;
    this.distanceFunction = distanceFunction;
    this.distanceQuery = distanceFunction.instantiate(rep);
  }

  /**
   * Get the distance factory.
   * 
   * @return distance factory
   */
  public D getDistanceFactory() {
    return distanceFunction.getDistanceFactory();
  }

  /**
   * The distance query we used.
   * 
   * @return Distance query
   */
  public DistanceQuery<O, D> getDistanceQuery() {
    return distanceQuery;
  }

  /**
   * Get the value of 'k' supported by this preprocessor.
   * 
   * @return k
   */
  public int getK() {
    return k;
  }

  /**
   * Perform the preprocessing step.
   */
  protected abstract void preprocess();

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(DistanceFunction<? super O, S> distanceFunction, Object... hints) {
    if(!this.distanceFunction.equals(distanceFunction)) {
      return null;
    }
    // k max supported?
    for(Object hint : hints) {
      if(hint instanceof Integer) {
        if(((Integer) hint) > k) {
          return null;
        }
      }
    }
    return new PreprocessorKNNQuery<O, S>(rep, (MaterializeKNNPreprocessor<O, S>) this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    if(!this.distanceFunction.equals(distanceQuery.getDistanceFunction())) {
      return null;
    }
    // k max supported?
    for(Object hint : hints) {
      if(hint instanceof Integer) {
        if(((Integer) hint) > k) {
          return null;
        }
      }
    }
    return new PreprocessorKNNQuery<O, S>(rep, (MaterializeKNNPreprocessor<O, S>) this);
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses AbstractMaterializeKNNPreprocessor oneway - - «create»
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static abstract class Factory<O, D extends Distance<D>> implements IndexFactory<O, KNNIndex<O>> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * materialized. must be an integer greater than 1.
     * <p>
     * Key: {@code -materialize.k}
     * </p>
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

    /**
     * Parameter to indicate the distance function to be used to ascertain the
     * nearest neighbors.
     * <p/>
     * <p>
     * Default value: {@link EuclideanDistanceFunction}
     * </p>
     * <p>
     * Key: {@code materialize.distance}
     * </p>
     */
    public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

    /**
     * Holds the value of {@link #K_ID}.
     */
    protected int k;

    /**
     * Hold the distance function to be used.
     */
    protected DistanceFunction<? super O, D> distanceFunction;

    /**
     * Index factory.
     * 
     * @param k k parameter
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction) {
      super();
      this.k = k;
      this.distanceFunction = distanceFunction;
    }

    @Override
    abstract public AbstractMaterializeKNNPreprocessor<O, D> instantiate(Relation<O> representation);

    /**
     * Get the distance function.
     * 
     * @return Distance function
     */
    // TODO: hide this?
    public DistanceFunction<? super O, D> getDistanceFunction() {
      return distanceFunction;
    }

    /**
     * Get the distance factory.
     * 
     * @return Distance factory
     */
    public D getDistanceFactory() {
      return distanceFunction.getDistanceFactory();
    }
    
    @Override
    public TypeInformation getInputTypeRestriction() {
      return distanceFunction.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static abstract class Parameterizer<O, D extends Distance<D>> extends AbstractParameterizer {
      /**
       * Holds the value of {@link #K_ID}.
       */
      protected int k;

      /**
       * Hold the distance function to be used.
       */
      protected DistanceFunction<? super O, D> distanceFunction;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        // number of neighbors
        final IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(1));
        if(config.grab(kP)) {
          k = kP.getValue();
        }

        // distance function
        final ObjectParameter<DistanceFunction<? super O, D>> distanceFunctionP = new ObjectParameter<DistanceFunction<? super O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
        if(config.grab(distanceFunctionP)) {
          distanceFunction = distanceFunctionP.instantiateClass(config);
        }
      }

      @Override
      abstract protected Factory<O, D> makeInstance();
    }
  }
}