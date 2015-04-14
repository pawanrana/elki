package experimentalcode.erich.approxknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.MillisTimeDuration;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import experimentalcode.erich.approxknn.SpacefillingKNNPreprocessor.SpatialRef;

/**
 * Simple experiment to estimate the effects of approximating the kNN with space
 * filling curves.
 * 
 * @author Erich Schubert
 */
public class EffectivenessExperiment extends AbstractSFCExperiment {
  private static final Logging LOG = Logging.getLogger(EffectivenessExperiment.class);

  PrimitiveDistanceFunction<? super NumberVector> distanceFunction = ManhattanDistanceFunction.STATIC;

  @Override
  public void run() {
    Duration load = new MillisTimeDuration("approxknn.load").begin();
    Database database = LoadImageNet.loadDatabase("ImageNet-Haralick-1", true);
    // Database database = LoadALOI.loadALOI("hsb-7x2x2", true);
    Relation<NumberVector> rel = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    DBIDs ids = rel.getDBIDs();
    LOG.statistics(load.end());
    LOG.statistics(new LongStatistic("approxknn.dataset.numobj", ids.size()));
    LOG.statistics(new LongStatistic("approxknn.dataset.dims", RelationUtil.dimensionality(rel)));

    final int numcurves = 9;
    List<ArrayList<SpatialRef>> curves = initializeCurves(rel, ids, numcurves);
    WritableDataStore<int[]> positions = indexPositions(ids, numcurves, curves);

    // True kNN value
    final int k = 101;
    // Half window widths
    // final int[] halfwins = { 2, 4, 10 };
    final int[] halfwins = { 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 30, 40 };
    Random rnd = new Random(0);
    final int samplesize = ids.size(); // 10000;
    final DBIDs subset = (samplesize == ids.size()) ? ids : DBIDUtil.randomSample(ids, samplesize, rnd);

    // Counting distance queries, for r-tree evaluation.
    final CountingManhattanDistanceFunction cdist = new CountingManhattanDistanceFunction();
    DistanceQuery<NumberVector> cdistq = database.getDistanceQuery(rel, cdist);
    KNNQuery<NumberVector> cknnq = database.getKNNQuery(cdistq, k);

    // The curve combinations to test:
    String[] sfc_names = {//
    "z1", "p1", "h1",//
    "z2", "p2", "h2",//
    "z3", "p3", "h3", //
    "z123", "p123", "h123", //
    "zph1", "zph2", "zph3", //
    "all9", //
    "random", //
    };
    int[] sfc_masks = {//
    1, 2, 4, //
    8, 16, 32,//
    64, 128, 256, //
    1 | 8 | 64, 2 | 16 | 128, 4 | 32 | 256, //
    1 | 2 | 4, 8 | 16 | 32, 64 | 128 | 256, //
    0x1FF, //
    0, //
    };
    int[] sfc_halfscale = {//
    25, 25, 25, //
    25, 25, 25, //
    25, 25, 25, //
    10, 10, 10, //
    10, 10, 10, //
    4, //
    25, //
    };
    assert (sfc_names.length == sfc_masks.length);
    final int numvars = sfc_masks.length * halfwins.length;

    Duration qtime = new MillisTimeDuration("approxnn.querytime").begin();
    MeanVariance[] distcmv = MeanVariance.newArray(numvars + 1);
    MeanVariance[] recallmv = MeanVariance.newArray(numvars);
    MeanVariance[] kdistmv = MeanVariance.newArray(numvars);
    for (DBIDIter id = subset.iter(); id.valid(); id.advance()) {
      NumberVector vec = rel.get(id);
      // Get the exact nearest neighbors (use an index, luke!)
      long pre = cdist.counter;
      final KNNList trueNN = cknnq.getKNNForObject(vec, k);
      distcmv[numvars].put((double) (cdist.counter - pre));
      SetDBIDs trueNNS = DBIDUtil.newHashSet(trueNN);
      double truedist = trueNN.getKNNDistance();

      int[] posi = positions.get(id);

      for (int c = 0; c < sfc_masks.length; c++) {
        for (int w = 0; w < halfwins.length; w++) {
          final int varnum = w * sfc_masks.length + c;
          if (sfc_masks[c] > 0) {
            DBIDs cands = mergeCandidates(ids, numcurves, sfc_masks[c], curves, halfwins[w] * sfc_halfscale[c], id, posi);
            // Number of distance computations; exclude self.
            distcmv[varnum].put(cands.size() - 1);
            // Recall of true kNNs
            recallmv[varnum].put(Math.min(1., DBIDUtil.intersectionSize(trueNNS, cands) / (double) k));
            // Compute kdist in approximated kNNs:
            if (truedist > 0 && cands.size() >= k) {
              KNNHeap heap = DBIDUtil.newHeap(k);
              for (DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
                heap.insert(distanceFunction.distance(vec, rel.get(iter)), id);
              }
              kdistmv[varnum].put((heap.getKNNDistance() - truedist) / truedist);
            } else {
              // Actually must be correct on duplicates, avoid div by 0.
              kdistmv[varnum].put(1.0);
            }
          } else {
            // Random sampling:
            ModifiableDBIDs cands = DBIDUtil.randomSample(ids, Math.min(k, halfwins[w] * 2 * sfc_halfscale[c]), rnd);
            cands.add(id);
            // Number of distance computations; exclude self.
            distcmv[varnum].put(cands.size() - 1);
            // Recall of true kNNs
            recallmv[varnum].put(Math.min(1., DBIDUtil.intersectionSize(trueNNS, cands) / (double) k));
            // Compute kdist in approximated kNNs:
            if (truedist > 0 && cands.size() >= k) {
              KNNHeap heap = DBIDUtil.newHeap(k);
              for (DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
                heap.insert(distanceFunction.distance(vec, rel.get(iter)), id);
              }
              kdistmv[varnum].put((heap.getKNNDistance() - truedist) / truedist);
            } else {
              // NOT WELL DEFINED, unfortunately. Ignore for now.
            }
          }
        }
      }
    }
    LOG.statistics(qtime.end());
    LOG.statistics(new LongStatistic("approxnn.query.size", ids.size()));
    LOG.statistics(new DoubleStatistic("approxnn.query.time.average", qtime.getDuration() / (double) ids.size()));
    for (int c = 0; c < sfc_masks.length; c++) {
      for (int w = 0; w < halfwins.length; w++) {
        final int varnum = w * sfc_masks.length + c;
        final String prefix = "approxnn." + sfc_names[c] + "-" + (2 * halfwins[w] * sfc_halfscale[c] / (double) (k - 1));
        LOG.statistics(new DoubleStatistic(prefix + ".distc.mean", distcmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".distc.stddev", distcmv[varnum].getSampleStddev()));
        LOG.statistics(new DoubleStatistic(prefix + ".recall.mean", recallmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".recall.stddev", recallmv[varnum].getSampleStddev()));
        LOG.statistics(new DoubleStatistic(prefix + ".kdist.mean", kdistmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".kdist.stddev", kdistmv[varnum].getSampleStddev()));
      }
    }
    LOG.statistics(new DoubleStatistic("approxnn.rtree.distc.mean", distcmv[numvars].getMean()));
    LOG.statistics(new DoubleStatistic("approxnn.rtree.distc.stddev", distcmv[numvars].getSampleStddev()));
  }

  public ModifiableDBIDs mergeCandidates(DBIDs ids, final int numcurves, int mask, List<ArrayList<SpatialRef>> curves, final int halfwin, DBIDIter id, int[] posi) {
    assert (mask > 0);
    ModifiableDBIDs cands = DBIDUtil.newHashSet();
    cands.add(id);
    for (int c = 0; c < numcurves; c++) {
      // Skip curve if not selected.
      if (((1 << c) & mask) == 0) {
        continue;
      }
      ArrayList<SpatialRef> curve = curves.get(c);
      assert (DBIDUtil.equal(curve.get(posi[c]).id, id));
      if (posi[c] <= halfwin) {
        for (int off = 0; off <= 2 * halfwin; off++) {
          cands.add(curve.get(off).id);
        }
      } else if (posi[c] + halfwin >= ids.size()) {
        for (int off = ids.size() - 2 * halfwin - 1; off < ids.size(); off++) {
          cands.add(curve.get(off).id);
        }
      } else {
        for (int off = 1; off <= halfwin; off++) {
          cands.add(curve.get(posi[c] - off).id);
          cands.add(curve.get(posi[c] + off).id);
        }
      }
    }
    return cands;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(EffectivenessExperiment.class, args);
  }

  @SuppressWarnings("deprecation")
  static class CountingManhattanDistanceFunction extends ManhattanDistanceFunction {
    long counter = 0;

    @Override
    public double distance(NumberVector v1, NumberVector v2) {
      counter++;
      return super.distance(v1, v2);
    }

    @Override
    public double norm(NumberVector v) {
      counter++;
      return super.norm(v);
    }

    @Override
    public double minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
      counter++;
      return super.minDist(mbr1, mbr2);
    }
  }
}