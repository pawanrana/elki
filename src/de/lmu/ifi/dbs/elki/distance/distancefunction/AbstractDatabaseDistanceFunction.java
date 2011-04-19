package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDatabaseDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract super class for distance functions needing a database context.
 * 
 * @author Elke Achtert
 * 
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used
 */
public abstract class AbstractDatabaseDistanceFunction<O, D extends Distance<D>> implements DistanceFunction<O, D> {
  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style
   * classes.
   */
  public AbstractDatabaseDistanceFunction() {
    super();
  }

  @Override
  abstract public D getDistanceFactory();

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  abstract public static class Instance<O, D extends Distance<D>> extends AbstractDatabaseDistanceQuery<O, D> {
    /**
     * Parent distance
     */
    DistanceFunction<? super O, D> parent;
    
    /**
     * Constructor.
     * 
     * @param database Database
     * @param parent Parent distance
     */
    public Instance(Relation<O> database, DistanceFunction<? super O, D> parent) {
      super(database);
      this.parent = parent;
    }

    @Override
    public DistanceFunction<? super O, D> getDistanceFunction() {
      return parent;
    }
  }
}