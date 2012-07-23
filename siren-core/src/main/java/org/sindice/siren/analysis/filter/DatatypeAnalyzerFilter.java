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
 * @project siren-core_rdelbru
 * @author Campinas Stephane [ 3 Oct 2011 ]
 * @link stephane.campinas@deri.org
 */
package org.sindice.siren.analysis.filter;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.CharArrayMap;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.Version;
import org.sindice.siren.analysis.JsonTokenizer;
import org.sindice.siren.analysis.TupleAnalyzer;
import org.sindice.siren.analysis.TupleTokenizer;
import org.sindice.siren.analysis.attributes.DatatypeAttribute;
import org.sindice.siren.analysis.attributes.NodeAttribute;
import org.sindice.siren.util.ReusableCharArrayReader;
import org.sindice.siren.util.XSDDatatype;

/**
 * This class performs post-processing operation on the tokens extracted by the
 * {@link TupleTokenizer} class, e.g., Literal, URI, based on their datatype
 * attribute.
 * <p>
 * This filter provides a {@link #register(char[], Analyzer)} method which allows
 * to register an analyzer to a specific datatype URI. It can be called through
 * the {@link TupleAnalyzer} class.
 * <p>
 * The {@link TupleTokenizer} and {@link JsonTokenizer} assign by default the
 * datatype {@link XSDDatatype.XSD_STRING} to literals and
 * {@link XSDDatatype.XSD_ANY_URI} to URI.
 */
public class DatatypeAnalyzerFilter extends TokenFilter {

  private final CharArrayMap<Analyzer> dtsAnalyzer;

  private CharTermAttribute termAtt;
  private OffsetAttribute offsetAtt;
  private PositionIncrementAttribute posIncrAtt;
  private TypeAttribute typeAtt;
  private DatatypeAttribute dtypeAtt;
  private NodeAttribute nodeAtt;

  private CharTermAttribute tokenTermAtt;
  private OffsetAttribute tokenOffsetAtt;
  private PositionIncrementAttribute tokenPosIncrAtt;
  private TypeAttribute tokenTypeAtt;

  private boolean isConsumingToken = false;
  private TokenStream currentStream;

  private ReusableCharArrayReader reusableCharArray;

  public DatatypeAnalyzerFilter(final Version version,
                                final TokenStream input) {
    super(input);
    dtsAnalyzer = new CharArrayMap<Analyzer>(version, 64, false);
    this.initAttributes();
  }

  /**
   * Create a {@link DatatypeAnalyzerFilter} with the given default {@link Analyzer}
   * for URI an Literal.
   * @param version The Lucene version to use
   * @param input the input token stream
   * @param stringAnalyzer the default Literal {@link Analyzer}
   * @param anyURIAnalyzer the default URI {@link Analyzer}
   */
  public DatatypeAnalyzerFilter(final Version version,
                                final TokenStream input,
                                final Analyzer stringAnalyzer,
                                final Analyzer anyURIAnalyzer) {
    this(version, input);
    // register the default analyzers
    this.register(XSDDatatype.XSD_STRING.toCharArray(), stringAnalyzer);
    this.register(XSDDatatype.XSD_ANY_URI.toCharArray(), anyURIAnalyzer);
  }

  /**
   * Initialise the attributes of the main stream
   */
  private void initAttributes() {
    termAtt = input.getAttribute(CharTermAttribute.class);
    offsetAtt = input.getAttribute(OffsetAttribute.class);
    posIncrAtt = input.getAttribute(PositionIncrementAttribute.class);
    typeAtt = input.getAttribute(TypeAttribute.class);
    dtypeAtt = input.getAttribute(DatatypeAttribute.class);
    nodeAtt = this.addAttribute(NodeAttribute.class); // TODO: why is it this, not input
  }

  /**
   * Initialise the attributes of the inner stream used to tokenize the incoming token.
   */
  private void initTokenAttributes() {
    tokenTermAtt = currentStream.addAttribute(CharTermAttribute.class);
    tokenOffsetAtt = currentStream.addAttribute(OffsetAttribute.class);
    tokenPosIncrAtt = currentStream.addAttribute(PositionIncrementAttribute.class);
    tokenTypeAtt = currentStream.addAttribute(TypeAttribute.class);
  }

  /**
   * Map the given analyzer to that dataTypeURI
   * @param dataTypeURI
   * @param analyzer
   */
  public void register(final char[] dataTypeURI, final Analyzer analyzer) {
    if (!dtsAnalyzer.containsKey(dataTypeURI)) {
      dtsAnalyzer.put(dataTypeURI, analyzer);
    }
  }

  @Override
  public final boolean incrementToken()
  throws IOException {
    /*
     * the use of the loop is necessary in the case where it was consuming a token
     * but that token stream reached the end, and so incrementToken return false.
     * The loop makes sure that the next token is processed.
     */
    do {
      if (!isConsumingToken) {
        if (!input.incrementToken())
          return false;

        final char[] dt = dtypeAtt.datatypeURI();
        if (dt == null || dt.length == 0) { // empty datatype, e.g., a bnode
          return true;
        }

        // the datatype is not registered, leave the token as it is
        if (!dtsAnalyzer.containsKey(dt))
          return true;

        final Analyzer analyzer = dtsAnalyzer.get(dt);
        if (reusableCharArray == null) {
          reusableCharArray = new ReusableCharArrayReader(termAtt.buffer(), 0, termAtt.length());
        } else {
          reusableCharArray.reset(termAtt.buffer(), 0, termAtt.length());
        }
        currentStream = analyzer.tokenStream("", reusableCharArray);
        this.initTokenAttributes();
      }
      // Consume the token with the registered analyzer
      isConsumingToken = currentStream.incrementToken();
    } while(!isConsumingToken);
    this.copyInnerStreamAttributes();
    return true;
  }

  /**
   * Copy the inner's stream attributes values to the main stream's ones. This filter
   * uses an inner stream, therefore it needs to be cleared so that other filters
   * have clean attributes data. Because of that, the attributes datatypeURI and
   * node have to saved in order to be restored after.
   */
  private void copyInnerStreamAttributes() {
    // backup datatype and node path
    final IntsRef nodePath = IntsRef.deepCopyOf(nodeAtt.node());
    final char[] dt = dtypeAtt.datatypeURI();
    // clear attributes
    input.clearAttributes();
    // copy inner attributes
    final int len = tokenTermAtt.length();
    termAtt.copyBuffer(tokenTermAtt.buffer(), 0, len);
    offsetAtt.setOffset(tokenOffsetAtt.startOffset(), tokenOffsetAtt.endOffset());
    posIncrAtt.setPositionIncrement(tokenPosIncrAtt.getPositionIncrement());
    typeAtt.setType(tokenTypeAtt.type());
    // TupleTokenizer handles the setting of tuple/cell values and the datatype URI

    // restore datatype and node
    nodeAtt.copyNode(nodePath);
    dtypeAtt.setDatatypeURI(dt);
  }

  @Override
  public void close()
  throws IOException {
    try {
      if (currentStream != null)
        currentStream.close();
    } finally {
      super.close();
    }
  }

}
