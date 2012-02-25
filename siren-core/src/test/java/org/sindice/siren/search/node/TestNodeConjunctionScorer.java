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
 * @author Renaud Delbru [ 28 Apr 2009 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2009 by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.search.node;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.sindice.siren.index.DocsAndNodesIterator;
import org.sindice.siren.search.AbstractTestSirenScorer;

public class TestNodeConjunctionScorer extends AbstractTestSirenScorer {

  @Test
  public void testNextWithTermConjunction()
  throws Exception {
    this.deleteAll(writer);
    this.addDocumentsWithIterator(writer, new String[] { "<http://renaud.delbru.fr/> . ",
      "<http://sindice.com/test/name> \"Renaud Delbru\" . ",
      "<http://sindice.com/test/type> <http://sindice.com/test/Person> . " +
      "<http://sindice.com/test/name> \"Renaud Delbru\" . " });

    final NodeBooleanScorer scorer =
      this.getConjunctionScorer(new String[] {"renaud", "delbru"});

    assertTrue(scorer.nextCandidateDocument());
    assertEquals(0, scorer.doc());
    assertEquals(-1, scorer.node()[0]);
    assertTrue(scorer.nextNode());
    assertEquals(0, scorer.node()[0]);
    assertEquals(0, scorer.node()[1]);
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertTrue(scorer.nextCandidateDocument());
    assertEquals(1, scorer.doc());
    assertEquals(-1, scorer.node()[0]);
    assertTrue(scorer.nextNode());
    assertEquals(0, scorer.node()[0]);
    assertEquals(1, scorer.node()[1]);
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertTrue(scorer.nextCandidateDocument());
    assertEquals(2, scorer.doc());
    assertEquals(-1, scorer.node()[0]);
    assertTrue(scorer.nextNode());
    assertEquals(1, scorer.node()[0]);
    assertEquals(1, scorer.node()[1]);
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertFalse(scorer.nextCandidateDocument());
    assertEquals(DocsAndNodesIterator.NO_MORE_DOC, scorer.doc());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());
  }

  @Test
  public void testNoNode() throws IOException {
    this.deleteAll(writer);
    this.addDocument(writer, "\"eee\" . \"ddd\" . ");

    final NodeBooleanScorer scorer =
      this.getConjunctionScorer(new String[] {"ddd", "eee"});

    assertTrue(scorer.nextCandidateDocument());
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());
  }

  @Test
  public void testNoNextCandidate() throws IOException {
    this.deleteAll(writer);
    this.addDocument(writer, "\"eee\" . \"ddd\" . ");
    this.addDocument(writer, "\"eee\" . \"fff\" . ");

    final NodeBooleanScorer scorer =
      this.getConjunctionScorer(new String[] {"ddd", "fff"});

    assertFalse(scorer.nextCandidateDocument());
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_DOC, scorer.doc());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());
  }

// TODO
//  @Test
//  public void testNextWithPhraseConjunction()
//  throws Exception {
//    this.deleteAll(writer);
//    this.addDocumentsWithIterator(new String[] { "\"aaa bbb aaa\". ",
//      "\"aaa bbb aba\" \"aaa ccc bbb aaa\" . ",
//      "\"aaa bbb ccc\" \"aaa ccc aaa aaa ccc\" . " +
//      "\" bbb ccc aaa \" \"aaa bbb bbb ccc aaa ccc\" . "});
//
//    final NodeBooleanScorer scorer =
//      this.getConjunctionScorer(new String[][] {{"aaa", "bbb"}, {"aaa", "ccc"}});
//
//    assertFalse(scorer.nextDocument() == DocIdSetIterator.NO_MORE_DOCS);
//    assertEquals(2, scorer.doc());
//    assertEquals(1, scorer.node()[0]);
//    assertEquals(1, scorer.node()[1]);
//    assertTrue(scorer.nextDocument() == DocIdSetIterator.NO_MORE_DOCS);
//  }

  @Test
  public void testSkipToCandidate()
  throws Exception {
    this.deleteAll(writer);

    final ArrayList<String> docs = new ArrayList<String>();
    for (int i = 0; i < 32; i++) {
      docs.add("<http://sindice.com/test/name> \"Renaud Delbru\" . ");
      docs.add("<http://sindice.com/test/type> <http://sindice.com/test/Person> . ");
    }
    this.addDocumentsWithIterator(writer, docs);

    final NodeBooleanScorer scorer =
      this.getConjunctionScorer(new String[] {"renaud", "delbru"});

    assertTrue(scorer.skipToCandidate(16));
    assertEquals(16, scorer.doc());
    assertEquals(-1, scorer.node()[0]);
    assertTrue(scorer.nextNode());
    assertEquals(0, scorer.node()[0]);
    assertEquals(1, scorer.node()[1]);
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertTrue(scorer.skipToCandidate(41)); // should jump to next candidate doc 42
    assertEquals(42, scorer.doc());
    assertEquals(-1, scorer.node()[0]);

    assertTrue(scorer.skipToCandidate(42)); // should stay at the same position
    assertEquals(42, scorer.doc());
    assertEquals(-1, scorer.node()[0]);
    assertTrue(scorer.nextNode());
    assertEquals(0, scorer.node()[0]);
    assertEquals(1, scorer.node()[1]);
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertFalse(scorer.skipToCandidate(75));
    assertEquals(DocsAndNodesIterator.NO_MORE_DOC, scorer.doc());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());
  }

// TODO
//  @Test
//  public void testScoreWithTermConjunction()
//  throws Exception {
//    _helper.addDocumentsWithIterator(new String[] { "<http://renaud.delbru.fr/> . ",
//                                                    "<http://sindice.com/test/name> \"Renaud Delbru\" . ",
//                                                    "<http://sindice.com/test/type> <http://sindice.com/test/Person> . " +
//                                                    "<http://sindice.com/test/name> \"Renaud Delbru\" . ",
//                                                    "<http://sindice.com/test/type> <http://sindice.com/test/Person> . " +
//                                                    "<http://sindice.com/test/homepage> <http://renaud.delbru.fr/> . " +
//                                                    "<http://sindice.com/test/name> \"Renaud Delbru\" ."});
//
//    final NodeConjunctionScorer scorer =
//      this.getConjunctionScorer(new String[] {"renaud", "delbru"});
//
//    float lastScore = 0;
//    assertFalse(scorer.nextDocument() == DocIdSetIterator.NO_MORE_DOCS);
//    lastScore = scorer.score();
//    assertFalse(scorer.nextDocument() == DocIdSetIterator.NO_MORE_DOCS);
//    assertTrue(lastScore > scorer.score());
//    lastScore = scorer.score();
//    assertFalse(scorer.nextDocument() == DocIdSetIterator.NO_MORE_DOCS);
//    assertTrue(lastScore > scorer.score());
//    lastScore = scorer.score();
//    assertFalse(scorer.nextDocument() == DocIdSetIterator.NO_MORE_DOCS);
//    assertTrue(lastScore < scorer.score());
//    assertTrue(scorer.nextDocument() == DocIdSetIterator.NO_MORE_DOCS);
//  }

}