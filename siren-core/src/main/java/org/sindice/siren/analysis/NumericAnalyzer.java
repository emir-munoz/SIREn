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
 * @author Stephane Campinas [ 16 Aug 2012 ]
 */
package org.sindice.siren.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType.NumericType;

public abstract class NumericAnalyzer
extends Analyzer {

  private final int precisionStep;

  public NumericAnalyzer(int precisionStep) {
    this.precisionStep = precisionStep;
  }

  public int getPrecisionStep() {
    return precisionStep;
  }

  public abstract NumericType getNumericType();

  @Override
  protected TokenStreamComponents createComponents(String fieldName,
                                                   Reader aReader) {
    final NumericTokenizer sink = getNumericTokenizer(aReader, precisionStep);
    return new TokenStreamComponents(sink, sink);
  }

  protected abstract NumericTokenizer getNumericTokenizer(Reader aReader,
                                                          int precisionStep);

}
