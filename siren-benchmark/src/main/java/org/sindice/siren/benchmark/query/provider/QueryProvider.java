/**
 * Copyright (c) 2009-2012 National University of Ireland, Galway. All Rights Reserved.
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
 * @project siren-benchmark
 * @author Renaud Delbru [ 6 Jul 2012 ]
 * @link http://renaud.delbru.fr/
 */
package org.sindice.siren.benchmark.query.provider;

import java.io.Closeable;
import java.util.Iterator;

import org.sindice.siren.benchmark.generator.lexicon.TermLexiconWriter.TermGroups;
import org.sindice.siren.benchmark.query.provider.KeywordQuery.Occur;

public abstract class QueryProvider implements Iterator<Query>, Closeable {

  protected int nbQueries = 50;
  protected int queryPos = 0;

  protected final TermGroups[] groups;
  protected final Occur[] occurs;

  public QueryProvider (final Occur[] occurs, final TermGroups[] groups) {
    if (occurs != null && groups != null && occurs.length != groups.length) {
      throw new Error("occurs and groups must have the same length.");
    }
    this.occurs = occurs;
    this.groups = groups;
  }

  public void setNbQueries(final int nbQueries) {
    this.nbQueries = nbQueries;
  }

  public final int getNbQueries() {
    return nbQueries;
  }

  @Override
  public String toString() {
    if (groups == null)
      return "";

    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < groups.length; i++) {
      builder.append(groups[i]);
      builder.append(":");
      builder.append(occurs[i]);
      builder.append("-");
    }
    builder.setLength(builder.length() - 1);
    return builder.toString();
  }

}