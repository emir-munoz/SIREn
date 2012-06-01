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
 * @author Renaud Delbru [ 21 Apr 2009 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2009 by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.search;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader.AtomicReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sindice.siren.analysis.AnyURIAnalyzer;
import org.sindice.siren.analysis.TupleAnalyzer;
import org.sindice.siren.analysis.AnyURIAnalyzer.URINormalisation;
import org.sindice.siren.search.primitive.NodePhraseQuery;
import org.sindice.siren.search.tuple.SirenCellQuery;
import org.sindice.siren.search.tuple.SirenTupleQuery;
import org.sindice.siren.search.tuple.SirenTupleClause.Occur;

public class TestSirenPhraseQuery extends LuceneTestCase {

  private QueryTestingHelper _helper = null;

  @Before
  public void setUp()
  throws Exception {
    super.setUp();
    _helper = new QueryTestingHelper(new TupleAnalyzer(TEST_VERSION_CURRENT,
      new StandardAnalyzer(TEST_VERSION_CURRENT),
      new AnyURIAnalyzer(TEST_VERSION_CURRENT)));

  }

  @After
  public void tearDown()
  throws Exception {
    super.tearDown();
    _helper.close();
  }

  /**
   * Ensures slop of 0 works for exact matches, but not reversed
   */
  @Test
  public void testExact1() throws Exception {
    _helper.addDocuments(new String[] { "\"Renaud Delbru\" . ",
                                                    "\"Renaud\" . " });

    NodePhraseQuery query = new NodePhraseQuery();
    // slop is zero by default
    query.add(new Term("content", "renaud"));
    query.add(new Term("content", "delbru"));
    ScoreDoc[] hits = _helper.search(query);
    assertEquals("exact match", 1, hits.length);

    query = new NodePhraseQuery();
    query.add(new Term("field", "delbru"));
    query.add(new Term("field", "renaud"));
    hits = _helper.search(query);
    assertEquals("reverse not exact", 0, hits.length);
  }

  /**
   * Ensures slop of 0 works for exact matches within a longer literal, but not
   * reversed
   */
  @Test
  public void testExact2() throws Exception {
    _helper.addDocuments(new String[] { "\"word1 word2 Renaud Delbru word3 \" . ",
                                                    "\"Renaud word1 Delbru\" . "                            
    });

    NodePhraseQuery query = new NodePhraseQuery();
    // slop is zero by default
    query.add(new Term("content", "renaud"));
    query.add(new Term("content", "delbru"));
    ScoreDoc[] hits = _helper.search(query);
    assertEquals("exact match", 1, hits.length);

    query = new NodePhraseQuery();
    query.add(new Term("field", "delbru"));
    query.add(new Term("field", "renaud"));
    hits = _helper.search(query);
    assertEquals("reverse not exact", 0, hits.length);
  }

  /**
   * Ensures slop of 0 works for exact matches, but not in separate cells or
   * tuples
   */
  @Test
  public void testExact3() throws Exception {
    _helper.addDocuments(new String[] { "\"word1 word2\" \"Renaud Delbru word3 \" . ",
                                                    "\"Renaud\" \"Delbru\" . ",
                                                    "\"Renaud\" . \"Delbru\" . " });

    final NodePhraseQuery query = new NodePhraseQuery();
    // slop is zero by default
    query.add(new Term("content", "renaud"));
    query.add(new Term("content", "delbru"));
    final ScoreDoc[] hits = _helper.search(query);
    assertEquals("exact match", 1, hits.length);
  }

  /**
   * Ensures slop of 0 works for exact matches, but not in separate cells or
   * tuples
   * <br>
   * Same test with no norms in order to check [SRN-44].
   */
  @Test
  public void testExact3WithNoNorms() throws Exception {
    _helper.addDocuments(new String[] { "\"word1 word2\" \"Renaud Delbru word3 \" . ",
                                                    "\"Renaud\" \"Delbru\" . ",
                                                    "\"Renaud\" . \"Delbru\" . " });

    final NodePhraseQuery query = new NodePhraseQuery();
    // slop is zero by default
    query.add(new Term("content", "renaud"));
    query.add(new Term("content", "delbru"));
    final ScoreDoc[] hits = _helper.search(query);
    assertEquals("exact match", 1, hits.length);
  }

  @Test
  public void testPhraseQueryOnLocalname()
  throws Exception {
    final AnyURIAnalyzer uri = new AnyURIAnalyzer(TEST_VERSION_CURRENT);
    uri.setUriNormalisation(URINormalisation.LOCALNAME);
    _helper = new QueryTestingHelper(new TupleAnalyzer(TEST_VERSION_CURRENT, 
    	new StandardAnalyzer(TEST_VERSION_CURRENT), uri));
    
    final String triple = "<http://dbpedia.org/resource/The_Kingston_Trio> " +
                          "<http://purl.org/dc/terms/subject>  " +
                          "<http://dbpedia.org/resource/Category:Decca_Records_artists> .";
    _helper.addDocument(triple);
    
    final NodePhraseQuery q1 = new NodePhraseQuery();
    q1.add(new Term("content", "decca"));
    q1.add(new Term("content", "records"));
    final NodePhraseQuery q2 = new NodePhraseQuery();
    q2.add(new Term("content", "kingston"));
    q2.add(new Term("content", "trio"));
    
    final SirenCellQuery cq1 = new SirenCellQuery(q1);
    final SirenCellQuery cq2 = new SirenCellQuery(q2);
    SirenTupleQuery bq = new SirenTupleQuery();
    bq.add(cq1, Occur.MUST);
    bq.add(cq2, Occur.MUST);
    
    ScoreDoc[] hits = _helper.search(bq);
    assertEquals(1, hits.length);
  }
  
  @Test
  public void testExplain() throws IOException {
    _helper.addDocuments(new String[] { "\"Renaud Delbru\" . ",
                                                    "\"Renaud Delbru\" . \"Renaud Delbru\" . "});

    final Term t1 = new Term(QueryTestingHelper.DEFAULT_FIELD, "renaud");
    final Term t2 = new Term(QueryTestingHelper.DEFAULT_FIELD, "delbru");
    final NodePhraseQuery query = new NodePhraseQuery();
    query.add(t1); query.add(t2);
    final Weight w = query.createWeight(_helper.getIndexSearcher());
    final IndexReader reader = _helper.getIndexReader();
    
    // Explain entity 0 : 1 match
    Explanation explanation = w.explain((AtomicReaderContext) reader.getSequentialSubReaders()[0].getTopReaderContext(), 0);
    assertNotNull("explanation is null and it shouldn't be", explanation);
    
    final TFIDFSimilarity sim = (TFIDFSimilarity) _helper.getIndexSearcher().getSimilarityProvider().get(QueryTestingHelper.DEFAULT_FIELD);
     System.out.println("Explanation: " + explanation.toString());
    //All this Explain does is return the term frequency
    assertEquals("term frq is not 1",
      sim.tf(1), explanation.getDetails()[1].getDetails()[0].getValue(), 0.01);

    // Explain entity 1 : 2 match
    explanation = w.explain((AtomicReaderContext) reader.getSequentialSubReaders()[0].getTopReaderContext(), 1);
    assertNotNull("explanation is null and it shouldn't be", explanation);
     System.out.println("Explanation: " + explanation.toString());
    //All this Explain does is return the term frequency
    assertEquals("term frq is not 2",
      sim.tf(2), explanation.getDetails()[1].getDetails()[0].getValue(), 0f);

    // Explain non existing entity
    explanation = w.explain((AtomicReaderContext) reader.getSequentialSubReaders()[0].getTopReaderContext(), 2);
    assertNotNull("explanation is null and it shouldn't be", explanation);
    //System.out.println("Explanation: " + explanation.toString());
    //All this Explain does is return the term frequency
    assertEquals("term frq is not 0", 0f, explanation.getValue(), 0f);
  }

}
