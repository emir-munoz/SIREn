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
 * @author Campinas Stephane [ 21 Sep 2011 ]
 * @link stephane.campinas@deri.org
 */
package org.sindice.siren.search.node;

import java.io.IOException;

import org.apache.lucene.index.SingleTermsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.FuzzyTermsEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.ToStringUtils;
import org.apache.lucene.util.automaton.LevenshteinAutomata;

/**
 * Implements the fuzzy search query. The similarity measurement
 * is based on the Levenshtein (edit distance) algorithm.
 *
 * <p>This query uses {@link MultiTermQuery.TopTermsScoringBooleanQueryRewrite}
 * as default. So terms will be collected and scored according to their
 * edit distance. Only the top terms are used for building the {@link NodeBooleanQuery}.
 * It is not recommended to change the rewrite mode for fuzzy queries.
 *
 * <p> Code taken from {@link FuzzyQuery} and adapted for SIREn.
 */
public class NodeFuzzyQuery extends MultiNodeTermQuery {

  public final static int     defaultMaxEdits       = LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE;
  public final static int     defaultPrefixLength   = 0;
  public final static int     defaultMaxExpansions  = 50;
  public final static boolean defaultTranspositions = true;

  private final int maxEdits;
  private final int maxExpansions;
  private final boolean transpositions;
  private final int prefixLength;
  private final Term term;

  /**
   * Create a new SirenFuzzyQuery that will match terms with a similarity
   * of at least <code>minimumSimilarity</code> to <code>term</code>.
   * If a <code>prefixLength</code> &gt; 0 is specified, a common prefix
   * of that length is also required.
   *
   * @param term the term to search for
   * @param maxEdits must be >= 0 and <= {@link LevenshteinAutomata#MAXIMUM_SUPPORTED_DISTANCE}.
   * @param prefixLength length of common (non-fuzzy) prefix
   * @param maxExpansions the maximum number of terms to match. If this number is
   *  greater than {@link NodeBooleanQuery#getMaxClauseCount} when the query is rewritten,
   *  then the maxClauseCount will be used instead.
   * @param transpositions true if transpositions should be treated as a primitive
   *        edit operation. If this is false, comparisons will implement the classic
   *        Levenshtein algorithm.
   */
  public NodeFuzzyQuery(final Term term, final int maxEdits, final int prefixLength, final int maxExpansions, final boolean transpositions) {
    super(term.field());

    if (maxEdits < 0 || maxEdits > LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE) {
      throw new IllegalArgumentException("maxEdits must be between 0 and " + LevenshteinAutomata.MAXIMUM_SUPPORTED_DISTANCE);
    }
    if (prefixLength < 0) {
      throw new IllegalArgumentException("prefixLength cannot be negative.");
    }
    if (maxExpansions < 0) {
      throw new IllegalArgumentException("maxExpansions cannot be negative.");
    }

    this.term = term;
    this.maxEdits = maxEdits;
    this.prefixLength = prefixLength;
    this.transpositions = transpositions;
    this.maxExpansions = maxExpansions;
    this.setRewriteMethod(new MultiNodeTermQuery.TopTermsScoringNodeBooleanQueryRewrite(maxExpansions));
  }

  /**
   * Calls {@link #SirenFuzzyQuery(Term, float) SirenFuzzyQuery(term, minimumSimilarity, prefixLength, Integer.MAX_VALUE)}.
   */
  public NodeFuzzyQuery(final Term term, final int maxEdits, final int prefixLength) {
    this(term, maxEdits, prefixLength, defaultMaxExpansions, defaultTranspositions);
  }

  /**
   * Calls {@link #SirenFuzzyQuery(Term, float) SirenFuzzyQuery(term, minimumSimilarity, 0, Integer.MAX_VALUE)}.
   */
  public NodeFuzzyQuery(final Term term, final int maxEdits) {
    this(term, maxEdits, defaultPrefixLength);
  }

  /**
   * Calls {@link #SirenFuzzyQuery(Term, float) SirenFuzzyQuery(term, 0.5f, 0, Integer.MAX_VALUE)}.
   */
  public NodeFuzzyQuery(final Term term) {
    this(term, defaultMaxEdits);
  }

  /**
   * @return the maximum number of edit distances allowed for this query to match.
   */
  public int getMaxEdits() {
    return maxEdits;
  }

  /**
   * Returns the non-fuzzy prefix length. This is the number of characters at the start
   * of a term that must be identical (not fuzzy) to the query term if the query
   * is to match that term.
   */
  public int getPrefixLength() {
    return prefixLength;
  }

  @Override
  protected TermsEnum getTermsEnum(final Terms terms, final AttributeSource atts) throws IOException {
    if (maxEdits == 0 || prefixLength >= term.text().length()) {  // can only match if it's exact
      return new SingleTermsEnum(terms.iterator(null), term.bytes());
    }
    return new FuzzyTermsEnum(terms, atts, this.getTerm(), maxEdits, prefixLength, transpositions);
  }

  /**
   * Returns the pattern term.
   */
  public Term getTerm() {
    return term;
  }

  @Override
  public String toString(final String field) {
    final StringBuilder buffer = new StringBuilder();
    if (!term.field().equals(field)) {
        buffer.append(term.field());
        buffer.append(":");
    }
    buffer.append(term.text());
    buffer.append('~');
    buffer.append(Integer.toString(maxEdits));
    buffer.append(ToStringUtils.boost(this.getBoost()));
    return buffer.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + maxEdits;
    result = prime * result + prefixLength;
    result = prime * result + maxExpansions;
    result = prime * result + (transpositions ? 0 : 1);
    result = prime * result + ((term == null) ? 0 : term.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (this.getClass() != obj.getClass())
      return false;
    final NodeFuzzyQuery other = (NodeFuzzyQuery) obj;
    if (maxEdits != other.maxEdits)
      return false;
    if (prefixLength != other.prefixLength)
      return false;
    if (maxExpansions != other.maxExpansions)
      return false;
    if (transpositions != other.transpositions)
      return false;
    if (term == null) {
      if (other.term != null)
        return false;
    } else if (!term.equals(other.term))
      return false;
    return true;
  }

}
