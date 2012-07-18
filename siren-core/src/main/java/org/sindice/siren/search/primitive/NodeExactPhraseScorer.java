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
package org.sindice.siren.search.primitive;

import java.io.IOException;

import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.sindice.siren.index.PositionsIterator;

class NodeExactPhraseScorer extends NodePhraseScorer {

  NodeExactPhraseScorer(final Weight weight,
                        final NodePhraseQuery.PostingsAndPosition[] postings,
                        final Similarity.SloppySimScorer sloppyScorer,
                        final Similarity.ExactSimScorer exactScorer)
  throws IOException {
    super(weight, postings, sloppyScorer, exactScorer);
  }

  @Override
  int phraseFreq() throws IOException {
    int freq = 1; // set to one to count the first phrase found
    while (this.nextPhrase()) {
      freq++;
    }
    return freq;
  }

  @Override
  public String toString() {
    return "NodeExactPhraseScorer(" + weight + "," + this.doc() + "," + this.node() + ")";
  }

  @Override
  boolean nextPhrase() throws IOException {
    int first = 0;
    NodePhrasePosition lastPosition = phrasePositions[phrasePositions.length - 1];
    NodePhrasePosition firstPosition = phrasePositions[first];

    // scan forward in last
    if (lastPosition.pos == PositionsIterator.NO_MORE_POS || !lastPosition.nextPosition()) {
      return false;
    }

    while (firstPosition.pos < lastPosition.pos) {
      do {
        if (!firstPosition.nextPosition()) {  // scan forward in first
          return false;
        }
      } while (firstPosition.pos < lastPosition.pos);
      lastPosition = firstPosition;
      first = (first == (phrasePositions.length - 1)) ? 0 : first + 1;
      firstPosition = phrasePositions[first];
    }
    // all equal: a match
    return true;
  }

  @Override
  boolean firstPhrase() throws IOException {
    for (final NodePhrasePosition phrasePosition : phrasePositions) {
      phrasePosition.init();
    }
    // check for phrase
    return this.nextPhrase();
  }

}
