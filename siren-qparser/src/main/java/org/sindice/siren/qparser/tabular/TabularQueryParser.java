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
 * @author Renaud Delbru [ 25 Jul 2010 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2010 by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.qparser.tabular;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import java_cup.runtime.Scanner;
import java_cup.runtime.Symbol;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.sindice.siren.analysis.attributes.DatatypeAttribute;
import org.sindice.siren.qparser.analysis.TabularQueryTokenizerImpl;
import org.sindice.siren.qparser.tabular.query.ScatteredTabularQueryBuilder;
import org.sindice.siren.qparser.tabular.query.SimpleTabularQueryBuilder;
import org.sindice.siren.qparser.tabular.query.model.Literal;
import org.sindice.siren.qparser.tabular.query.model.LiteralPattern;
import org.sindice.siren.qparser.tabular.query.model.TabularQuery;
import org.sindice.siren.qparser.tabular.query.model.URIPattern;
import org.sindice.siren.qparser.tree.NodeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a Siren {@link Query} from a tabular query. The number of cells
 * is unlimited and each cells must have a cell position specified. Each cell
 * can be either an {@link URIPattern}, a {@link LiteralPattern} or a {@link Literal}.
 */
public class TabularQueryParser {

  private static final Logger logger = LoggerFactory.getLogger(TabularQueryParser.class);
  
  /**
   * Parse a Tabular query and return a Lucene {@link Query}. The query is built
   * over one Lucene's field.
   * 
   * @param qstr The query string
   * @param matchVersion the Lucene version to use
   * @param field The field to query
   * @param tabularAnalyzer The analyzers for tabular
   * @param datatypeConfig datatype configuration, which maps a datatype key to a
   * specific {@link Analyzer}.
   * @param op default boolean operator
   * @return A Lucene's {@link Query}
   * @throws ParseException If something is wrong with the query string
   */
  public static final Query parse(final String qstr,
                                  final Version matchVersion,
                                  final String field,
                                  final Analyzer tabularAnalyzer,
                                  final Map<String, Analyzer> datatypeConfig,
                                  final StandardQueryConfigHandler.Operator op)
  throws ParseException {
    // Parse NTriple and create abstract syntax tree
    final TokenStream tokenStream = prepareTokenStream(qstr, tabularAnalyzer);
    final Symbol sym = createAST(tokenStream);
    // Translate the AST into query objects
    return buildSingleFieldQuery(sym, matchVersion, field, datatypeConfig, op);
  }
  
  /**
   * Parse a Tabular query and return a Lucene {@link Query}.
   * <br>
   * Different query builders are used depending on the number of fields to
   * query.
   * 
   * @param qstr The query string
   * @param matchVersion the Lucene version to use
   * @param boosts The field boosts
   * @param tabularAnalyzer The set of analyzers (tabular, uri, literal) for each
   * queried field
   * @param datatypeConfigs datatype configuration for each field, which maps a
   * datatype key to a specific {@link Analyzer}.
   * @param op default boolean operator
   * @param scattered
   * @return A Lucene's {@link Query}
   * @throws ParseException If something is wrong with the query string
   */
  public static final Query parse(final String qstr,
                                  final Version matchVersion,
                                  final Map<String, Float> boosts,
                                  final Analyzer tabularAnalyzer,
                                  final Map<String, Map<String, Analyzer>> datatypeConfigs,
                                  final StandardQueryConfigHandler.Operator op,
                                  final boolean scattered)
  throws ParseException {
    if (boosts.isEmpty()) {
      throw new ParseException("Cannot parse query: no field specified");
    }

    // Parse Tabular and create abstract syntax tree
    final TokenStream tokenStream = prepareTokenStream(qstr, tabularAnalyzer);
    final Symbol sym = createAST(tokenStream);

    // Translate the AST into query objects
    if (scattered) {
      return buildScatteredMultiFieldQuery(sym, matchVersion, boosts, datatypeConfigs, op);
    }
    else {
      return buildMultiFieldQuery(sym, matchVersion, boosts, datatypeConfigs, op);
    }
  }

  /**
   * Prepare the token stream of the tabular query using the tabular analyzer.
   * @param qstr The tabular query
   * @param tabularAnalyzer A Tabluar Analyzer
   * @return A stream of tokens
   * @throws ParseException 
   */
  private static TokenStream prepareTokenStream(final String qstr,
                                                final Analyzer tabularAnalyzer)
  throws ParseException {
    final TokenStream tokenStream;
    try {
      tokenStream = tabularAnalyzer.tokenStream("", new StringReader(qstr));
    } catch (IOException e) {
      // TODO: Is it the right thing to do ?
      throw new ParseException(e.getLocalizedMessage());
    }
    return tokenStream;
  }

  /**
   * Create the Abstract Syntax Tree of the Tabular query based on the given
   * token stream.
   * @param tokenStream The token stream of the NTriple query
   * @return The Abstract Syntax Tree of the query
   * @throws ParseException If a irreparable error occurs during parsing
   */
  private static Symbol createAST(final TokenStream tokenStream)
  throws ParseException {
    final TabularQParserImpl lparser = new TabularQParserImpl(new CupScannerWrapper(tokenStream));
    Symbol sym = null;
    try {
      sym = lparser.parse();
    }
    catch (final Exception e) {
      logger.error("Parse error", e);
      if (e != null) throw new ParseException(e.toString());
    }
    return sym;
  }

  /**
   * throw an error if the visitor failed
   * @param translator
   * @throws ParseException
   */
  private static void queryBuildingError(final SimpleTabularQueryBuilder translator)
  throws ParseException {
    if (translator.hasError()) {
      throw new ParseException(translator.getErrorDescription());
    }
  }

  /**
   * Throws an error if the visitor failed
   * @param translator
   * @throws ParseException
   */
  private static void queryBuildingError(final ScatteredTabularQueryBuilder translator)
  throws ParseException {
    if (translator.hasError()) {
      throw new ParseException(translator.getErrorDescription());
    }
  }
  
  /**
   * Translate the AST and build a single field query
   * @param sym The AST
   * @param matchVersion The Lucene version to use
   * @param field The field to query
   * @param datatypeConfig datatype configuration, which maps a datatype key to a
   * specific {@link Analyzer}.
   * @param op default boolean operator
   * @return A Lucene {@link Query} object
   * @throws ParseException
   */
  private static Query buildSingleFieldQuery(final Symbol sym,
                                             final Version matchVersion,
                                             final String field,
                                             final Map<String, Analyzer> datatypeConfig,
                                             final StandardQueryConfigHandler.Operator op)
  throws ParseException {
    final SimpleTabularQueryBuilder translator = new SimpleTabularQueryBuilder(matchVersion, field, datatypeConfig);
    translator.setDefaultOperator(op);
    final TabularQuery nq = (TabularQuery) sym.value;
    nq.traverseBottomUp(translator);
    queryBuildingError(translator);
    return nq.getQuery();
  }

  /**
   * 
   * @param sym The AST
   * @param matchVersion The Lucene version to use
   * @param boosts The field boosts
   * @param datatypeConfigs datatype configuration for each field, which maps a
   * datatype key to a specific {@link Analyzer}.
   * @param op default boolean operator
   * @return A Lucene {@link Query} object
   * @throws ParseException
   */
  private static Query buildMultiFieldQuery(final Symbol sym,
                                            final Version matchVersion,
                                            final Map<String, Float> boosts,
                                            final Map<String, Map<String, Analyzer>> datatypeConfigs,
                                            final StandardQueryConfigHandler.Operator op)
  throws ParseException {
    final BooleanQuery bq = new BooleanQuery(true);
    for (final String field : boosts.keySet()) {
      final SimpleTabularQueryBuilder translator = new SimpleTabularQueryBuilder(matchVersion,
        field, datatypeConfigs.get(field));
      translator.setDefaultOperator(op);
      final TabularQuery nq = (TabularQuery) sym.value;
      nq.traverseBottomUp(translator);
      queryBuildingError(translator);
      final Query q = nq.getQuery();
      q.setBoost(boosts.get(field));
      bq.add(q, Occur.SHOULD);
    }
    return bq;
  }
  
  /**
   * Translate the AST and build a scattered multi-field query. A scattered
   * multi-field query performs a conjunction of tabular patterns, in which
   * each tabular pattern can appear in at least on of the fields. Each field
   * has a boost.
   * @param sym The AST
   * @param matchVersion The Lucene version to use
   * @param boosts The field boosts
   * @param datatypeConfigs datatype configuration for each field, which maps a
   * datatype key to a specific {@link Analyzer}.
   * @param op default boolean operator
   * @return A Lucene {@link Query} object
   * @throws ParseException
   */
  private static Query buildScatteredMultiFieldQuery(final Symbol sym,
                                                     final Version matchVersion,
                                                     final Map<String, Float> boosts,
                                                     final Map<String, Map<String, Analyzer>> datatypeConfigs,
                                                     final StandardQueryConfigHandler.Operator op)
  throws ParseException {
    final ScatteredTabularQueryBuilder translator = new ScatteredTabularQueryBuilder(matchVersion, boosts, datatypeConfigs);
    translator.setDefaultOperator(op);
    final TabularQuery nq = (TabularQuery) sym.value;
    nq.traverseBottomUp(translator);
    queryBuildingError(translator);
    return nq.getQuery();
  }

  public static class CupScannerWrapper implements Scanner {

    private final TokenStream _stream;
    private final CharTermAttribute cTermAtt;
    private final TypeAttribute typeAtt;
    private final DatatypeAttribute dataTypeAtt;
    private final PayloadAttribute plAtt;

    public CupScannerWrapper(final TokenStream stream) {
      _stream = stream;
      cTermAtt = _stream.getAttribute(CharTermAttribute.class);
      typeAtt = _stream.getAttribute(TypeAttribute.class);
      dataTypeAtt = stream.getAttribute(DatatypeAttribute.class);
      plAtt = stream.getAttribute(PayloadAttribute.class);
    }

    /* (non-Javadoc)
     * @see java_cup.runtime.Scanner#next_token()
     */
    public Symbol next_token() throws Exception {
      if (_stream.incrementToken()) {

        int idx = -1;
        for (int i = 0; i < TabularQueryTokenizerImpl.TOKEN_TYPES.length; i++) {
          if (typeAtt.type().equals(TabularQueryTokenizerImpl.TOKEN_TYPES[i])) {
            idx = i;
          }
        }

        if (idx == -1) {
          logger.error("Received unknown token: {}", cTermAtt.toString());
        }
        logger.debug("Received token {} ({})", cTermAtt.toString(), TabularQueryTokenizerImpl.TOKEN_TYPES[idx]);
        
        if (idx == TabularQueryTokenizerImpl.URIPATTERN ||
            idx == TabularQueryTokenizerImpl.LITERAL ||
            idx == TabularQueryTokenizerImpl.LPATTERN) {
          return new Symbol(idx, new NodeValue(dataTypeAtt.datatypeURI(),
                                               cTermAtt.toString(),
                                               byteArrayToInt(plAtt.getPayload().bytes)));
        } else {
          return new Symbol(idx);
        }
      }
      return null;
    }

    private int byteArrayToInt(byte[] a) {
      return a[0] | (a[1] << 8) | (a[2] << 16) | (a[3] << 24);
    }
  }

}
