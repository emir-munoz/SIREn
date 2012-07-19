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
 * @author Renaud Delbru [ 27 Sep 2011 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2010, 2011, by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.search.primitive;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.PrefixTermsEnum;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.ToStringUtils;

/**
 * A Query that matches documents containing terms with a specified prefix. A
 * PrefixQuery is built by QueryParser for input like <code>app*</code>.
 *
 * <p>This query uses the {@link
 * MultiNodeTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT}
 * rewrite method.
 *
 * <p> Code taken from {@link PrefixQuery} and adapted for SIREn.
 **/
public class NodePrefixQuery extends MultiNodeTermQuery {

  private final Term prefix;

  /**
   * Constructs a query for terms starting with <code>prefix</code>.
   **/
  public NodePrefixQuery(final Term prefix) {
    super(prefix.field());
    this.prefix = prefix;
  }

  /**
   * Returns the prefix of this query.
   **/
  public Term getPrefix() { return prefix; }

  @Override
  protected TermsEnum getTermsEnum(final Terms terms, final AttributeSource atts)
  throws IOException {
    final TermsEnum tenum = terms.iterator(null);

    if (prefix.bytes().length == 0) {
      // no prefix -- match all terms for this field:
      return tenum;
    }
    return new PrefixTermsEnum(tenum, prefix.bytes());
  }

  /** Prints a user-readable version of this query. */
  @Override
  public String toString(final String field) {
    final StringBuilder buffer = new StringBuilder();
    if (!this.getField().equals(field)) {
      buffer.append(this.getField());
      buffer.append(":");
    }
    buffer.append(prefix.text());
    buffer.append('*');
    buffer.append(ToStringUtils.boost(this.getBoost()));
    return buffer.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
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
    final NodePrefixQuery other = (NodePrefixQuery) obj;
    if (prefix == null) {
      if (other.prefix != null)
        return false;
    } else if (!prefix.equals(other.prefix))
      return false;
    return true;
  }

}

