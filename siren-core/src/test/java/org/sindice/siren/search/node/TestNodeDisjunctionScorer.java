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

import static org.sindice.siren.analysis.MockSirenToken.node;
import static org.sindice.siren.search.AbstractTestSirenScorer.NodeBooleanClauseBuilder.should;
import static org.sindice.siren.search.AbstractTestSirenScorer.NodeBooleanQueryBuilder.nbq;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.sindice.siren.index.DocsAndNodesIterator;
import org.sindice.siren.index.codecs.RandomSirenCodec.PostingsFormatType;
import org.sindice.siren.search.AbstractTestSirenScorer;

public class TestNodeDisjunctionScorer extends AbstractTestSirenScorer {

  @Override
  protected void configure() throws IOException {
    this.setAnalyzer(AnalyzerType.TUPLE);
    this.setPostingsFormat(PostingsFormatType.RANDOM);
  }

  @Test
  public void testNextCandidateNextNode() throws Exception {
    this.addDocuments(
      "<http://renaud.delbru.fr/> . ",
      "<http://sindice.com/test/name> \"Renaud Delbru\" . ",
      "<http://sindice.com/test/type> <http://sindice.com/test/Person> . ",
      "<aaa> <bbb> . <http://sindice.com/test/name> \"R. Delbru\" . "
    );

    final NodeScorer scorer = this.getScorer(
      nbq(should("renaud"), should("delbru"))
    );

    assertTrue(scorer.nextCandidateDocument());
    assertEquals(0, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,0), scorer.node());
    final float d0score00 = scorer.scoreInNode();
    final float d0freq00 = scorer.freqInNode();
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertTrue(scorer.nextCandidateDocument());
    assertEquals(1, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,1), scorer.node());
    final float d1score01 = scorer.scoreInNode();
    final float d1freq01 = scorer.freqInNode();
    assertEquals(d0freq00, d1freq01, 0);
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertTrue(scorer.nextCandidateDocument());
    assertEquals(3, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(1,1), scorer.node());
    // only one term is matched in the node
    final float d3score11 = scorer.scoreInNode();
    final float d3freq11 = scorer.freqInNode();
    assertTrue(d0freq00 > d3freq11);
    assertTrue(d1freq01 > d3freq11);
    assertTrue(d3score11 + " < " + d1score01, d3score11 < d1score01);
    assertTrue(d3score11 + " < " + d0score00, d3score11 < d0score00);
    assertFalse(scorer.nextNode());
    assertEquals(DocsAndNodesIterator.NO_MORE_NOD, scorer.node());

    assertEndOfStream(scorer);
  }

  @Test
  public void testSkipToCandidateNextNode() throws Exception {
    final ArrayList<String> docs = new ArrayList<String>();
    for (int i = 0; i < 16; i++) {
      docs.add("\"aaa bbb\" \"aaa ccc\" . \"ccc\" \"bbb ccc\" .");
      docs.add("\"aaa ccc bbb\" . \"aaa aaa ccc bbb bbb\" . ");
    }
    this.addDocuments(docs);

    final NodeScorer scorer = this.getScorer(
      nbq(should("aaa"), should("bbb"))
    );

    assertTrue(scorer.nextCandidateDocument());
    assertEquals(0, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,0), scorer.node());
    assertTrue(scorer.skipToCandidate(16));
    assertEquals(16, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,0), scorer.node());
    assertTrue(scorer.nextCandidateDocument());
    assertEquals(17, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,0), scorer.node());
    assertTrue(scorer.skipToCandidate(20));
    assertEquals(20, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,0), scorer.node());
    assertTrue(scorer.nextCandidateDocument());
    assertEquals(21, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,0), scorer.node());
    assertTrue(scorer.skipToCandidate(30));
    assertEquals(30, scorer.doc());
    assertEquals(node(-1), scorer.node());
    assertTrue(scorer.nextNode());
    assertEquals(node(0,0), scorer.node());

    assertFalse(scorer.skipToCandidate(34));
    assertEndOfStream(scorer);
  }

}
