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
 * @author Renaud Delbru [ 28 Sep 2011 ]
 * @link http://renaud.delbru.fr/
 */
package org.sindice.siren.search.node;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.ToStringUtils;

/**
 * A query that wraps another query or a filter and simply returns a constant score equal to the
 * query boost for every document that matches the filter or query.
 * For queries it therefore simply strips of all scores and returns a constant one.
 */
public class NodeConstantScoreQuery extends NodePrimitiveQuery {

  protected final Filter filter;
  protected final NodeQuery query;

  /** Strips off scores from the passed in Query. The hits will get a constant score
   * dependent on the boost factor of this query. */
  public NodeConstantScoreQuery(final NodeQuery query) {
    if (query == null)
      throw new NullPointerException("Query may not be null");
    this.filter = null;
    this.query = query;
  }

// TODO: Activate filters when Siren will support filters.
//  /** Wraps a Filter as a Query. The hits will get a constant score
//   * dependent on the boost factor of this query.
//   * If you simply want to strip off scores from a Query, no longer use
//   * {@code new ConstantScoreQuery(new QueryWrapperFilter(query))}, instead
//   * use {@link #ConstantScoreQuery(Query)}!
//   */
//  public SirenConstantScoreQuery(final SirenFilter filter) {
//    if (filter == null)
//      throw new NullPointerException("Filter may not be null");
//    this.filter = filter;
//    this.query = null;
//  }
//
//  /** Returns the encapsulated filter, returns {@code null} if a query is wrapped. */
//  public SirenFilter getFilter() {
//    return filter;
//  }

  /**
   * Returns the encapsulated query, returns {@code null} if a filter is wrapped.
   */
  public NodeQuery getQuery() {
    return query;
  }

  @Override
  protected void setAncestorPointer(final NodeQuery ancestor) {
    super.setAncestorPointer(ancestor);
    // keep encapsulated query synchronised
    query.setAncestorPointer(ancestor);
  }

  @Override
  public void setNodeConstraint(final int lowerBound, final int upperBound) {
    super.setNodeConstraint(lowerBound, upperBound);
    // keep encapsulated query synchronised
    query.setNodeConstraint(lowerBound, upperBound);
  }

  @Override
  public void setLevelConstraint(final int levelConstraint) {
    super.setLevelConstraint(levelConstraint);
    // keep encapsulated query synchronised
    query.setLevelConstraint(levelConstraint);
  }

  @Override
  public NodeQuery rewrite(final IndexReader reader) throws IOException {
    if (query != null) {
      NodeQuery rewritten = (NodeQuery) query.rewrite(reader);
      if (rewritten != query) {
        rewritten = new NodeConstantScoreQuery(rewritten);
        rewritten.setBoost(this.getBoost());
        return rewritten;
      }
    }
    return this;
  }

  @Override
  public void extractTerms(final Set<Term> terms) {
    // TODO: OK to not add any terms when wrapped a filter
    // and used with MultiSearcher, but may not be OK for
    // highlighting.
    // If a query was wrapped, we delegate to query.
    if (query != null)
      query.extractTerms(terms);
  }

  protected class NodeConstantWeight extends Weight {

    private final Weight innerWeight;
    private float queryNorm;
    private float queryWeight;

    public NodeConstantWeight(final IndexSearcher searcher) throws IOException {
      this.innerWeight = (query == null) ? null : query.createWeight(searcher);
    }

    @Override
    public Query getQuery() {
      return NodeConstantScoreQuery.this;
    }

    @Override
    public float getValueForNormalization() throws IOException {
      // we calculate sumOfSquaredWeights of the inner weight, but ignore it (just to initialize everything)
      if (innerWeight != null) innerWeight.getValueForNormalization();
      queryWeight = NodeConstantScoreQuery.this.getBoost();
      return queryWeight * queryWeight;
    }

    @Override
    public void normalize(final float norm, final float topLevelBoost) {
      this.queryNorm = norm * topLevelBoost;
      queryWeight *= this.queryNorm;
      // we normalize the inner weight, but ignore it (just to initialize everything)
      if (innerWeight != null) innerWeight.normalize(norm, topLevelBoost);
    }

    @Override
    public Scorer scorer(final AtomicReaderContext context, final boolean scoreDocsInOrder,
                         final boolean topScorer, final Bits acceptDocs)
    throws IOException {

      // TODO: Activate filters when Siren will support filters.

//      if (filter != null) {
//        assert query == null;
//        final DocIdSet dis = filter.getDocIdSet(reader);
//        if (dis == null)
//          return null;
//        disi = dis.iterator();
//      } else {

      assert query != null && innerWeight != null;
      final NodeScorer scorer = (NodeScorer) innerWeight.scorer(context, scoreDocsInOrder, topScorer, acceptDocs);

//      }

      if (scorer == null) {
        return null;
      }

      return new NodeConstantScorer(scorer, this, queryWeight);
    }

    @Override
    public boolean scoresDocsOutOfOrder() {
      return (innerWeight != null) ? innerWeight.scoresDocsOutOfOrder() : false;
    }

    @Override
    public Explanation explain(final AtomicReaderContext context, final int doc)
    throws IOException {
      final NodeScorer cs = (NodeScorer) this.scorer(context, true, false,
        context.reader().getLiveDocs());
      final boolean exists = (cs != null &&
                              cs.skipToCandidate(doc) &&
                              cs.doc() == doc &&
                              cs.nextNode());

      final ComplexExplanation result = new ComplexExplanation();
      if (exists) {
        result.setDescription(NodeConstantScoreQuery.this.toString() + ", product of:");
        result.setValue(queryWeight);
        result.setMatch(Boolean.TRUE);
        result.addDetail(new Explanation(NodeConstantScoreQuery.this.getBoost(), "boost"));
        result.addDetail(new Explanation(queryNorm, "queryNorm"));
      } else {
        result.setDescription(NodeConstantScoreQuery.this.toString() + " doesn't match id " + doc);
        result.setValue(0);
        result.setMatch(Boolean.FALSE);
      }
      return result;
    }
  }

  protected class NodeConstantScorer extends NodeScorer {

    final NodeScorer scorer;
    final float theScore;

    public NodeConstantScorer(final NodeScorer scorer,
                              final Weight w, final float theScore)
    throws IOException {
      super(w);
      this.theScore = theScore;
      this.scorer = scorer;
    }

    @Override
    public boolean nextCandidateDocument() throws IOException {
      return scorer.nextCandidateDocument();
    }

    @Override
    public boolean nextNode() throws IOException {
      return scorer.nextNode();
    }

    @Override
    public int doc() {
      return scorer.doc();
    }

    @Override
    public IntsRef node() {
      return scorer.node();
    }

    @Override
    public float scoreInNode() throws IOException {
      return theScore;
    }

    @Override
    public float freqInNode() throws IOException {
      return scorer.freqInNode();
    }

    @Override
    public boolean skipToCandidate(final int target) throws IOException {
      return scorer.skipToCandidate(target);
    }

  }

  @Override
  public Weight createWeight(final IndexSearcher searcher) throws IOException {
    return new NodeConstantScoreQuery.NodeConstantWeight(searcher);
  }

  @Override
  public String toString(final String field) {
    return new StringBuilder("NodeConstantScore(")
      .append((query == null) ? filter.toString() : query.toString(field))
      .append(')')
      .append(ToStringUtils.boost(this.getBoost()))
      .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!super.equals(o))
      return false;
    if (o instanceof NodeConstantScoreQuery) {
      final NodeConstantScoreQuery other = (NodeConstantScoreQuery) o;
      return
        ((this.filter == null) ? other.filter == null : this.filter.equals(other.filter)) &&
        ((this.query == null) ? other.query == null : this.query.equals(other.query));
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() +
      ((query == null) ? filter : query).hashCode();
  }

}