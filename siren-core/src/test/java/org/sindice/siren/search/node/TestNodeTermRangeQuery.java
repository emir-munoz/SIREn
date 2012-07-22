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
 * @project siren-core
 * @author Renaud Delbru [ 29 Sep 2011 ]
 * @link http://renaud.delbru.fr/
 */
package org.sindice.siren.search.node;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeTermsEnum;
import org.sindice.siren.analysis.AnyURIAnalyzer;
import org.sindice.siren.analysis.TupleAnalyzer;
import org.sindice.siren.index.codecs.RandomSirenCodec.PostingsFormatType;
import org.sindice.siren.search.node.MultiNodeTermQuery;
import org.sindice.siren.search.node.NodeBooleanClause;
import org.sindice.siren.search.node.NodeBooleanQuery;
import org.sindice.siren.search.node.NodePrimitiveQuery;
import org.sindice.siren.search.node.NodeTermQuery;
import org.sindice.siren.search.node.NodeTermRangeQuery;
import org.sindice.siren.util.BasicSirenTestCase;

public class TestNodeTermRangeQuery extends BasicSirenTestCase {

  @Override
  protected void configure() throws IOException {
    this.setAnalyzer(
      new TupleAnalyzer(TEST_VERSION_CURRENT,
        new WhitespaceAnalyzer(TEST_VERSION_CURRENT),
        new AnyURIAnalyzer(TEST_VERSION_CURRENT))
    );
    this.setPostingsFormat(PostingsFormatType.RANDOM);
  }

  public void testExclusive1() throws Exception {
    this.addDocument("<A>");
    this.addDocument("<B>");
    this.addDocument("<C>");
    this.addDocument("<D>");

    final NodePrimitiveQuery q = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", false, false);

    final ScoreDoc[] hits = searcher.search(q, null, 1000).scoreDocs;
    assertEquals("A,B,C,D, only B in range", 1, hits.length);
  }

  public void testExclusive2() throws Exception {
    this.addDocument("<A>");
    this.addDocument("<B>");
    this.addDocument("<D>");

    final NodePrimitiveQuery q = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", false, false);

    ScoreDoc[] hits = searcher.search(q, null, 1000).scoreDocs;
    assertEquals("A,B,D, only B in range", 1, hits.length);

    this.addDocument("<C>");
    hits = searcher.search(q, null, 1000).scoreDocs;
    assertEquals("C added, still only B in range", 1, hits.length);
  }

  public void testInclusive1() throws Exception {
    this.addDocument("<A>");
    this.addDocument("<B>");
    this.addDocument("<C>");
    this.addDocument("<D>");

    final NodePrimitiveQuery q = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", true, true);

    final ScoreDoc[] hits = searcher.search(q, null, 1000).scoreDocs;
    assertEquals("A,B,C,D - A,B,C in range", 3, hits.length);
  }

  public void testInclusive2() throws Exception {
    this.addDocument("<A>");
    this.addDocument("<B>");
    this.addDocument("<D>");

    final NodePrimitiveQuery q = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", true, true);

    ScoreDoc[] hits = searcher.search(q, null, 1000).scoreDocs;
    assertEquals("A,B,D - A and B in range", 2, hits.length);

    this.addDocument("<C>");
    hits = searcher.search(q, null, 1000).scoreDocs;
    assertEquals("C added - A, B, C in range", 3, hits.length);
  }

  public void testAllDocs() throws Exception {
    this.addDocuments(new String[]{"<A>", "<B>", "<C>", "<D>"});

    NodeTermRangeQuery query = new NodeTermRangeQuery(DEFAULT_TEST_FIELD, null, null, true, true);
    final Terms terms = MultiFields.getTerms(searcher.getIndexReader(), DEFAULT_TEST_FIELD);
    assertFalse(query.getTermsEnum(terms) instanceof TermRangeTermsEnum);
    assertEquals(4, searcher.search(query, null, 1000).scoreDocs.length);
    query = new NodeTermRangeQuery(DEFAULT_TEST_FIELD, null, null, false, false);
    assertFalse(query.getTermsEnum(terms) instanceof TermRangeTermsEnum);
    assertEquals(4, searcher.search(query, null, 1000).scoreDocs.length);
    query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "", null, true, false);
    assertFalse(query.getTermsEnum(terms) instanceof TermRangeTermsEnum);
    assertEquals(4, searcher.search(query, null, 1000).scoreDocs.length);
    // and now anothe one
    query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "B", null, true, false);
    assertTrue(query.getTermsEnum(terms) instanceof TermRangeTermsEnum);
    assertEquals(3, searcher.search(query, null, 1000).scoreDocs.length);
    reader.close();
  }

  /** This test should not be here, but it tests the fuzzy query rewrite mode (TOP_TERMS_SCORING_BOOLEAN_REWRITE)
   * with constant score and checks, that only the lower end of terms is put into the range */
  public void testTopTermsRewrite() throws Exception {
    this.addDocuments(new String[]{"<A>", "<B>", "<C>", "<D>", "<E>", "<F>",
                                   "<G>", "<H>", "<I>", "<J>", "<K>"});

    final NodeTermRangeQuery query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "B", "J", true, true);
    this.checkBooleanTerms(query, "B", "C", "D", "E", "F", "G", "H", "I", "J");

    final int savedClauseCount = NodeBooleanQuery.getMaxClauseCount();
    try {
      NodeBooleanQuery.setMaxClauseCount(3);
      this.checkBooleanTerms(query, "B", "C", "D");
    } finally {
      NodeBooleanQuery.setMaxClauseCount(savedClauseCount);
    }
  }

  private void checkBooleanTerms(final NodeTermRangeQuery query, final String... terms)
  throws IOException {
    query.setRewriteMethod(new MultiNodeTermQuery.TopTermsScoringNodeBooleanQueryRewrite(50));
    final NodeBooleanQuery bq = (NodeBooleanQuery) searcher.rewrite(query);
    final Set<String> allowedTerms = new HashSet<String>(Arrays.asList(terms));
    assertEquals(allowedTerms.size(), bq.clauses().size());
    for (final NodeBooleanClause c : bq.clauses()) {
      assertTrue(c.getQuery() instanceof NodeTermQuery);
      final NodeTermQuery tq = (NodeTermQuery) c.getQuery();
      final String term = tq.getTerm().text();
      assertTrue("invalid term: "+ term, allowedTerms.contains(term));
      allowedTerms.remove(term); // remove to fail on double terms
    }
    assertEquals(0, allowedTerms.size());
  }

  public void testEqualsHashcode() {
    Query query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", true, true);

    query.setBoost(1.0f);
    Query other = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", true, true);
    other.setBoost(1.0f);

    assertEquals("query equals itself is true", query, query);
    assertEquals("equivalent queries are equal", query, other);
    assertEquals("hashcode must return same value when equals is true", query.hashCode(), other.hashCode());

    other.setBoost(2.0f);
    assertFalse("Different boost queries are not equal", query.equals(other));

    other = NodeTermRangeQuery.newStringRange("notcontent", "A", "C", true, true);
    assertFalse("Different fields are not equal", query.equals(other));

    other = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "X", "C", true, true);
    assertFalse("Different lower terms are not equal", query.equals(other));

    other = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "Z", true, true);
    assertFalse("Different upper terms are not equal", query.equals(other));

    query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, null, "C", true, true);
    other = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, null, "C", true, true);
    assertEquals("equivalent queries with null lowerterms are equal()", query, other);
    assertEquals("hashcode must return same value when equals is true", query.hashCode(), other.hashCode());

    query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "C", null, true, true);
    other = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "C", null, true, true);
    assertEquals("equivalent queries with null upperterms are equal()", query, other);
    assertEquals("hashcode returns same value", query.hashCode(), other.hashCode());

    query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, null, "C", true, true);
    other = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "C", null, true, true);
    assertFalse("queries with different upper and lower terms are not equal", query.equals(other));

    query = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", false, false);
    other = NodeTermRangeQuery.newStringRange(DEFAULT_TEST_FIELD, "A", "C", true, true);
    assertFalse("queries with different inclusive are not equal", query.equals(other));
  }

}