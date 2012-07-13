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

import java.io.IOException;

import org.sindice.siren.benchmark.generator.lexicon.TermLexiconWriter.TermGroups;
import org.sindice.siren.benchmark.query.provider.KeywordQuery.Occur;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class KeywordQueryProvider extends TermLexiconQueryProvider {

  public KeywordQueryProvider (final Occur[] occurs,
                               final TermGroups[] groups)
  throws IOException {
    super(occurs, groups);
  }

  @Override
  public boolean hasNext() {
    return (queryPos < nbQueries) ? true : false;
  }

  @Override
  public Query next() {
    final KeywordQuery kq = new KeywordQuery();

    queryPos++;
    for (int i = 0; i < groups.length; i++) {
      try {
        kq.addKeyword(reader.getRandomTerm(groups[i]), occurs[i]);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
    return kq;
  }

  @Override
  public void remove() {
    throw new NotImplementedException();
  }

}