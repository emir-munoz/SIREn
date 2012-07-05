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
 * A Scorer for queries within a node with a required subscorer and an excluding
 * (prohibited) subscorer.
 * <p>
 * Only ndoes matching the required subscorer and not matching the prohibited
 * subscorer are kept.
 * <p>
 * Code taken from {@link ReqExclScorer} and adapted for the Siren use
 * case.
 */
public class NodeReqExclScorer extends NodeScorer {

  /**
   * The required and excluded primitive Siren scorers.
   */
  private final NodeScorer reqScorer;
  private NodeScorer exclScorer;

  /**
   * Construct a {@link NodeReqExclScorer}.
   *
   * @param reqScorer
   *          The scorer that must match, except where
   * @param exclScorer
   *          indicates exclusion.
   */
  public NodeReqExclScorer(final NodeScorer reqScorer,
                           final NodeScorer exclScorer) {
    super(reqScorer.getWeight());
    this.reqScorer = reqScorer;
    this.exclScorer = exclScorer;
  }

  @Override
  public boolean nextCandidateDocument() throws IOException {
    if (!reqScorer.nextCandidateDocument()) {
      return false;
    }

    if (exclScorer == null) {
      return true; // reqScorer.nextCandidateDocument() already returned true
    }

    return this.toNonExcludedCandidateDocument();
  }

  /**
   * Advance to non excluded candidate document. <br>
   * On entry:
   * <ul>
   * <li>reqScorer != null,
   * <li>exclScorer != null,
   * <li>reqScorer was advanced once via {@link #nextCandidateDocument()} or
   * {@link #skipToCandidate(int)} and reqScorer.doc() may still be excluded.
   * </ul>
   * Advances reqScorer a non excluded candidate document, if any.
   * <p>
   * If reqScorer.doc() is equal to exclScorer.doc(), reqScorer.doc() cannot
   * be excluded immediately, i.e., it is a valid candidate document. We have to
   * check the exclusion of reqScorer.node().
   *
   * @return true iff there is a non excluded candidate document.
   */
  private boolean toNonExcludedCandidateDocument() throws IOException {
    do {
      int exclDoc = exclScorer.doc();
      final int reqDoc = reqScorer.doc(); // may be excluded

      if (reqDoc <= exclDoc) {
        return true; // reqScorer advanced to or before exclScorer, not excluded
      }
      else if (reqDoc > exclDoc) {
        if (!exclScorer.skipToCandidate(reqDoc)) {
          exclScorer = null; // exhausted, no more exclusions
          return true;
        }
        exclDoc = exclScorer.doc();
        if (exclDoc >= reqDoc) {
          return true; // exclScorer advanced to or before reqScorer, not excluded
        }
      }
    } while (reqScorer.nextCandidateDocument());

    return false;
  }

  @Override
  public boolean nextNode() throws IOException {
    if (!reqScorer.nextNode()) { // Move to the next matching node
      return false; // exhausted, nothing left
    }

    if (exclScorer == null || exclScorer.doc() != reqScorer.doc()) {
      return true; // reqScorer.nextNode() already returned true
    }

    // reqScorer and exclScorer are positioned on the same candidate document
    return this.toNonExcludedNode();
  }

  /**
   * Advance to an excluded cell. <br>
   * On entry:
   * <ul>
   * <li>reqScorer != null,
   * <li>exclScorer != null,
   * <li>reqScorer and exclScorer were advanced once via
   * {@link #nextCandidateDocument()} or {@link #skipToCandidate(int)} and were
   * positioned on the same candidate document number
   * <li> reqScorer.doc() and reqScorer.node() may still be excluded.
   * </ul>
   * Advances reqScorer to the next non excluded required node, if any.
   *
   * @return true iff the current candidate document has a non excluded required
   * node.
   */
  private boolean toNonExcludedNode() throws IOException {
    IntsRef reqNode = reqScorer.node(); // may be excluded
    IntsRef exclNode = exclScorer.node();

    int comparison;
    while ((comparison = NodeUtils.compare(reqNode, exclNode)) >= 0) {
      // if node equal, advance to next node in reqScorer
      if (comparison == 0 && !reqScorer.nextNode()) {
        return false;
      }

      // if node equal or excluded node ancestor, advance to next node
      if (!exclScorer.nextNode()) {
        return true;
      }

      reqNode = reqScorer.node();
      exclNode = exclScorer.node();
    }
    return true;
  }

  @Override
  public int doc() {
    return reqScorer.doc(); // reqScorer may be null when next() or skipTo()
                            // already return false
  }

  @Override
  public IntsRef node() {
    return reqScorer.node();
  }

  @Override
  public float score()
  throws IOException {
    // TODO
    throw new UnsupportedOperationException();
//    return reqScorer.score(); // reqScorer may be null when next() or skipTo()
//                              // already return false
  }

  @Override
  public boolean skipToCandidate(final int target) throws IOException {
    if (exclScorer == null) {
      return reqScorer.skipToCandidate(target);
    }

    if (!reqScorer.skipToCandidate(target)) {
      return false;
    }

    return this.toNonExcludedCandidateDocument();
  }

  @Override
  public String toString() {
    return "NodeReqExclScorer(" + weight + "," + this.doc() + "," +
      this.node() + ")";
  }

}
