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
 * @author Renaud Delbru [ 10 Dec 2009 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2009 by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.search.node;

import java.io.IOException;

import org.apache.lucene.util.IntsRef;
import org.sindice.siren.util.NodeUtils;

/**
 * A Scorer for queries with a required part and an optional part. Delays
 * advance() on the optional part until a score() is needed.
 * <p>
 * Code taken from {@link ReqOptSumScorer} and adapted for the Siren use
 * case.
 */
public class NodeReqOptScorer extends NodeScorer {

  /**
   * The required scorer passed from the constructor.
   */
  private final NodeScorer reqScorer;

  /**
   * The optional scorer passed from the constructor, and used for boosting
   * score.
   */
  private NodeScorer optScorer;

  private boolean firstTimeOptScorer;

  /**
   * Construct a {@link NodeReqOptScorer}.
   *
   * @param reqScorer
   *          The required scorer. This must match.
   * @param optScorer
   *          The optional scorer. This is used for scoring only.
   */
  public NodeReqOptScorer(final NodeScorer reqScorer,
                          final NodeScorer optScorer) {
    super(reqScorer.getWeight());
    this.reqScorer = reqScorer;
    this.optScorer = optScorer;
  }

  @Override
  public boolean nextCandidateDocument() throws IOException {
    firstTimeOptScorer = true;
    return reqScorer.nextCandidateDocument();
  }

  @Override
  public boolean nextNode() throws IOException {
    firstTimeOptScorer = true;
    return reqScorer.nextNode();
  }

  @Override
  public boolean skipToCandidate(final int target) throws IOException {
    firstTimeOptScorer = true;
    return reqScorer.skipToCandidate(target);
  }

  @Override
  public int doc() {
    return reqScorer.doc();
  }

  @Override
  public IntsRef node() {
    return reqScorer.node();
  }

  @Override
  public float scoreInNode()
  throws IOException {
    final float reqScore = reqScorer.score();
  
    if (firstTimeOptScorer) {
      firstTimeOptScorer = false;
      // Advance to the matching cell
      if (!optScorer.skipToCandidate(this.doc())) {
        optScorer = null;
        return reqScore;
      }
    }
    else if (optScorer == null) {
      return reqScore;
    }
    else if (optScorer.doc() < this.doc() &&
             !optScorer.skipToCandidate(this.doc())) {
      optScorer = null;
      return reqScore;
    }
  
    // If the optional scorer matches the same cell, increase the score
    return (optScorer.doc() == this.doc() && NodeUtils.compare(optScorer.node(), this.node()) == 0)
           ? reqScore + optScorer.score()
           : reqScore;
  }

  @Override
  public String toString() {
    return "NodeReqOptScorer(" + weight + "," +
      this.doc() + "," + this.node() + ")";
  }

}
