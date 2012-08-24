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
 * @project solr-plugins
 * @author Renaud Delbru [ 8 mars 2008 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2010 by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.qparser.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;
import org.sindice.siren.analysis.attributes.DatatypeAttribute;

/**
 * Tokenizes a tabular query. The behaviour is similar to {@link NTripleQueryTokenizer},
 * excepts that the query syntax adds a mandatory cell position information.
 */
public final class TabularQueryTokenizer extends Tokenizer {

  private final CharTermAttribute cTermAtt;
  private final TypeAttribute typeAtt;
  private final DatatypeAttribute dataTypeAtt;
  // Contains Node constraints values
  private final PayloadAttribute plAtt;
  
  /** A private instance of the JFlex-constructed scanner */
  private final TabularQueryTokenizerImpl _scanner;

  /** Token definition */
  public static final int EOF = 0;
  public static final int ERROR = 1;
  public static final int AND = 2;
  public static final int OR = 3;
  public static final int MINUS = 4;
  public static final int LPAREN = 5;
  public static final int RPAREN = 6;
  public static final int URIPATTERN = 7;
  public static final int LITERAL = 8;
  public static final int LPATTERN = 9;
  
  /**
   * Creates a new instance of the {@link TabularQueryTokenizer}. Attaches the
   * <code>input</code> to a newly created JFlex scanner.
   */
  public TabularQueryTokenizer(final Reader input) {
    super(input);
    this._scanner = new TabularQueryTokenizerImpl(input);
    cTermAtt = this.addAttribute(CharTermAttribute.class);
    typeAtt = this.addAttribute(TypeAttribute.class);
    dataTypeAtt = this.addAttribute(DatatypeAttribute.class);
    plAtt = this.addAttribute(PayloadAttribute.class);
  }

  @Override
  public final boolean incrementToken()
  throws IOException {
    final int tokenType = _scanner.getNextToken();

    switch (tokenType) {

      case TabularQueryTokenizer.ERROR:
      case TabularQueryTokenizer.AND:
      case TabularQueryTokenizer.OR:
      case TabularQueryTokenizer.MINUS:
      case TabularQueryTokenizer.LPAREN:
      case TabularQueryTokenizer.RPAREN:
        typeAtt.setType(TabularQueryTokenizerImpl.TOKEN_TYPES[tokenType]);
        cTermAtt.setEmpty();
        cTermAtt.append(TabularQueryTokenizerImpl.TOKEN_TYPES[tokenType]);
        break;

      case TabularQueryTokenizer.URIPATTERN:
        typeAtt.setType(TabularQueryTokenizerImpl.TOKEN_TYPES[tokenType]);
        cTermAtt.setEmpty();
        cTermAtt.append(_scanner.getURIText());
        dataTypeAtt.setDatatypeURI(_scanner.getDatatypeURI());
        plAtt.setPayload(new BytesRef(intToByteArray(_scanner.getNodeConstraint())));
        break;

      case TabularQueryTokenizer.LITERAL:
        typeAtt.setType(TabularQueryTokenizerImpl.TOKEN_TYPES[tokenType]);
        cTermAtt.setEmpty();
        cTermAtt.append(_scanner.getLiteralText());
        dataTypeAtt.setDatatypeURI(_scanner.getDatatypeURI());
        plAtt.setPayload(new BytesRef(intToByteArray(_scanner.getNodeConstraint())));
        break;

      case TabularQueryTokenizer.LPATTERN:
        typeAtt.setType(TabularQueryTokenizerImpl.TOKEN_TYPES[tokenType]);
        cTermAtt.setEmpty();
        cTermAtt.append(_scanner.getLiteralText());
        dataTypeAtt.setDatatypeURI(_scanner.getDatatypeURI());
        plAtt.setPayload(new BytesRef(intToByteArray(_scanner.getNodeConstraint())));
        break;

      case TabularQueryTokenizer.EOF:
      default:
        return false;
    }
    return true;
  }

  private byte[] intToByteArray(int v) {
    final byte[] a = new byte[4];

    a[0] = (byte) (v & 0xFF);
    a[1] = (byte) ((v >> 8) & 0xFF);
    a[2] = (byte) ((v >> 16) & 0xFF);
    a[3] = (byte) ((v >> 24) & 0xFF);
    return a;
  }

  /**
   * Reset the tokenizer and the underlying flex scanner with a new reader.
   * <br>
   * This method is called by Analyzers in its resuableTokenStream method.
   */
  @Override
  public void reset(final Reader input) throws IOException {
    super.reset(input);
    _scanner.yyreset(input);
  }

  @Override
  public void close()
  throws IOException {
    _scanner.yyclose();
  }

}
