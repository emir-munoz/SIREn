/**
 * Copyright (c) 2009-2011 Sindice Limited. All Rights Reserved.
 *
 * Project and contact information: http://www.siren.sindice.com/
 *
 * This file is part of the SIREn project.
 *
 * SIREn is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SIREn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with SIREn. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * @project siren
 * @author Renaud Delbru [ 21 Jan 2011 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2010 by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.search.tuple;

import java.io.IOException;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Weight;
import org.sindice.siren.search.base.NodePrimitiveScorer;
import org.sindice.siren.search.base.NodeScorer;
import org.sindice.siren.search.primitive.NodeTermQuery;
import org.sindice.siren.search.primitive.NodePhraseQuery;

/**
 * A Query that matches cells matching boolean combinations of other primitive
 * queries, e.g. {@link NodeTermQuery}s, {@link NodePhraseQuery}s, etc.
 * Implements skipTo(), and has no limitations on the numbers of added scorers. <br>
 * Uses ConjunctionScorer, DisjunctionScorer, ReqOptScorer and ReqExclScorer.
 * <p> Code taken from {@link BooleanScorer2} and adapted for the Siren use case.
 */
class SirenCellScorer
extends NodeScorer {

  private NodePrimitiveScorer primitiveScorer;

//  private final int dataset = -1;
  private int docID = -1;
//  private int tuple = -1;
//  private int cell = -1;

  /**
   * The cell index constraints
   */
  private final int cellConstraintStart;
  private final int cellConstraintEnd;

  /**
   * Create a SirenBooleanScorer, that matches a boolean combination of
   * primitive siren scorers. In no required scorers are added, at least one of
   * the optional scorers will have to match during the search.
   *
   * @param weight
   *          The similarity to be used.
   * @param cellConstraintStart
   *          The minimum cell index that should match (inclusive)
   * @param cellConstraintEnd
   *          The maximum cell index that should match (inclusive)
   */
  public SirenCellScorer(final Weight weight,
                         final int cellConstraintStart,
                         final int cellConstraintEnd) {
    super(weight);
    this.cellConstraintStart = cellConstraintStart;
    this.cellConstraintEnd = cellConstraintEnd;
  }

  /**
   * Create a SirenBooleanScorer, that matches a boolean combination of
   * primitive siren scorers. In no required scorers are added, at least one of
   * the optional scorers will have to match during the search.
   *
   * @param weight
   *          The similarity to be used.
   */
  public SirenCellScorer(final Weight weight) {
    this(weight, 0, Integer.MAX_VALUE);
  }

  public void setScorer(final NodePrimitiveScorer scorer) {
    this.primitiveScorer = scorer;
  }

  /**
   * Scores and collects all matching documents.
   *
   * @param hc
   *          The collector to which all matching documents are passed through
   *          {@link HitCollector#collect(int, float)}. <br>
   *          When this method is used the {@link #explain(int)} method should
   *          not be used.
   */
  @Override
  public void score(final Collector collector) throws IOException {
    int doc;
    collector.setScorer(this);
    while ((doc = this.nextDocument()) != NO_MORE_DOCS) {
      collector.collect(doc);
    }
  }

  /**
   * Expert: Collects matching documents in a range. <br>
   * Note that {@link #nextDocument()} must be called once before this method is called
   * for the first time.
   *
   * @param hc
   *          The collector to which all matching documents are passed through
   *          {@link HitCollector#collect(int, float)}.
   * @param max
   *          Do not score documents past this.
   * @return true if more matching documents may remain.
   */
  @Override
  public boolean score(final Collector collector, final int max, final int firstDocID)
  throws IOException {
    int doc = firstDocID;
    collector.setScorer(this);
    while (doc < max) {
      collector.collect(doc);
      doc = this.nextDocument();
    }
    return doc != NO_MORE_DOCS;
  }

  @Override
  public int doc() {
    return docID;
  }

  /**
   * Position is invalid in high-level scorers. It will always return
   * {@link Integer.MAX_VALUE}.
   */
  @Override
  public int pos() {
    return Integer.MAX_VALUE;
  }

  @Override
  public int nextDocument() throws IOException {
    if (primitiveScorer.nextDocument() != NO_MORE_DOCS) {
      docID = this.doNext();
    }
    else {
      docID = NO_MORE_DOCS;
    }
    return docID;
  }

  /**
   * Perform a next without initial increment.
   * <p> The next is valid when the cellID matches the constraints.
   */
  private int doNext() throws IOException {
    boolean more = true;
//    int cell = primitiveScorer.node()[1];

    // while cell are not within the constraints, iterate
    while (more && (primitiveScorer.node()[1] < cellConstraintStart ||
                    primitiveScorer.node()[1] > cellConstraintEnd)) {
      if (primitiveScorer.nextPosition() == NO_MORE_POS) {
        more = (primitiveScorer.nextDocument() != NO_MORE_DOCS);
      }
//      cell = primitiveScorer.node()[1];
    }

    if (more) {
      docID = primitiveScorer.doc();
//      tuple = primitiveScorer.node()[0];
    }
    else {
      docID = NO_MORE_DOCS;
    }
    return docID;
  }

  @Override
  public int nextPosition() throws IOException {
    boolean more = false;
    do {
      more = (primitiveScorer.nextPosition() != NO_MORE_POS);
//      cell = primitiveScorer.cell();
    } while (more && (primitiveScorer.node()[1] < cellConstraintStart ||
                      primitiveScorer.node()[1] > cellConstraintEnd)); // while cell are not within the constraints, iterate
    if (more) {
//      tuple = primitiveScorer.tuple(); // update current tuple
      return 0; // position is invalid in this scorer, return 0
    }
    else {
//      tuple = Integer.MAX_VALUE; // set to sentinel value
//      cell = Integer.MAX_VALUE;
      primitiveScorer.node()[0] = Integer.MAX_VALUE;
      primitiveScorer.node()[1] = Integer.MAX_VALUE;
      return NO_MORE_POS;
    }
  }

  @Override
  public float score()
  throws IOException {
    return primitiveScorer.score();
  }

  @Override
  public int skipTo(final int entity) throws IOException {
    if (primitiveScorer.skipTo(entity) != NO_MORE_DOCS) {
      this.docID = this.doNext();
//      this.tuple = primitiveScorer.tuple();
//      this.cell = primitiveScorer.cell();
    } else {
      this.docID = NO_MORE_DOCS;
    }
    return this.docID;
  }

  @Override
  public int skipTo(int docID, int[] nodes)
  throws IOException {
    if (primitiveScorer.skipTo(docID, nodes) != NO_MORE_DOCS) {
      this.docID = this.doNext(); 
    } else {
      this.docID = NO_MORE_DOCS;
    }
    return doc();
  }

  @Override
  public int[] node() {
    return primitiveScorer.node();
  }
  
  @Override
  public String toString() {
    return "SingleCellScorer(" + this.doc() + "," + primitiveScorer + ")";
  }

}
