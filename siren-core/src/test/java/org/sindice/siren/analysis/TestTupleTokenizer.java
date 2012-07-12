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
 * @author Renaud Delbru [ 8 Dec 2009 ]
 * @link http://renaud.delbru.fr/
 * @copyright Copyright (C) 2009 by Renaud Delbru, All rights reserved.
 */
package org.sindice.siren.analysis;

import static org.sindice.siren.analysis.MockSirenToken.node;

import java.io.StringReader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.IntsRef;
import org.junit.Test;
import org.sindice.siren.util.XSDDatatype;

public class TestTupleTokenizer
extends TokenizerHelper {

  private final Tokenizer _t = new TupleTokenizer(new StringReader(""));

  @Test
  public void testURI()
  throws Exception {
    this.assertTokenizesTo(_t, "<http://renaud.delbru.fr/>",
      new String[] { "http://renaud.delbru.fr/" }, new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<http://renaud.delbru.fr>",
      new String[] { "http://renaud.delbru.fr" }, new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<http://user@renaud.delbru.fr>",
      new String[] { "http://user@renaud.delbru.fr" }, new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<http://user:passwd@renaud.delbru.fr>",
      new String[] { "http://user:passwd@renaud.delbru.fr" },
      new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<http://user:passwd@renaud.delbru.fr:8080>",
      new String[] { "http://user:passwd@renaud.delbru.fr:8080" },
      new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<http://renaud.delbru.fr:8080>",
      new String[] { "http://renaud.delbru.fr:8080" }, new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<http://renaud.delbru.fr/subdir/page.html>",
      new String[] { "http://renaud.delbru.fr/subdir/page.html" },
      new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<http://renaud.delbru.fr/page.html#fragment>",
      new String[] { "http://renaud.delbru.fr/page.html#fragment" },
      new String[] { "<URI>" });
    this.assertTokenizesTo(
      _t,
      "<http://renaud.delbru.fr/page.html?query=a+query&hl=en&start=20&sa=N>",
      new String[] { "http://renaud.delbru.fr/page.html?query=a+query&hl=en&start=20&sa=N" },
      new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<ftp://renaud.delbru.fr/>",
      new String[] { "ftp://renaud.delbru.fr/" }, new String[] { "<URI>" });
    this.assertTokenizesTo(_t, "<mailto:renaud@delbru.fr>",
      new String[] { "mailto:renaud@delbru.fr" }, new String[] { "<URI>" });
  }

  @Test
  public void testBNode()
  throws Exception {
    this.assertTokenizesTo(_t, "_:x74562", new String[] { "x74562" },
      new String[] { "<BNODE>" });
    this.assertTokenizesTo(_t, "_:node1", new String[] { "node1" },
      new String[] { "<BNODE>" });
    this.assertTokenizesTo(_t, "_:httpsaojfsd", new String[] { "httpsaojfsd" },
      new String[] { "<BNODE>" });
    this.assertTokenizesTo(_t, "_:asd", new String[] { "asd" },
      new String[] { "<BNODE>" });
  }

  @Test
  public void testLiteral()
  throws Exception {
    this.assertTokenizesTo(_t, "\"Renaud\"", new String[] { "Renaud" },
      new String[] { "<LITERAL>" });
    this.assertTokenizesTo(_t, "\"1 and 2\"", new String[] { "1 and 2" },
      new String[] { "<LITERAL>" });
    this.assertTokenizesTo(_t, "\"renaud http://test/ \"", new String[] {
        "renaud http://test/ " }, new String[] { "<LITERAL>" });
    this.assertTokenizesTo(_t, "\"foo bar FOO BAR\"",
      new String[] { "foo bar FOO BAR" }, new String[] { "<LITERAL>" });
    this.assertTokenizesTo(_t, "\"ABC\\u0061\\u0062\\u0063\\u00E9\\u00e9ABC\"",
      new String[] { "ABCabcééABC" }, new String[] { "<LITERAL>" });
  }

  @Test
  public void testDot()
  throws Exception {
    this.assertTokenizesTo(_t, "<http://te.st> . \"ren . aud\" . ",
      new String[] { "http://te.st", ".", "ren . aud", "." }, new String[] {
          "<URI>", "<DOT>", "<LITERAL>", "<DOT>" });
    this.assertTokenizesTo(_t, "<aaa> \"bbb\". <bbb> <aaa>. <ccc> .",
      new String[] { "aaa", "bbb", ".", "bbb", "aaa", ".", "ccc", "." }, new String[] {
          "<URI>", "<LITERAL>", "<DOT>", "<URI>", "<URI>", "<DOT>", "<URI>", "<DOT>" });
  }

  // TODO: Check if language tag is correctly assigned when a
  // LanguageTagAttribute will be created
  @Test
  public void testLanguage()
  throws Exception {
    this.assertTokenizesTo(_t, "\"test\"@en", new String[] { "test" },
      new String[] { "<LITERAL>" });
    this.assertTokenizesTo(_t, "\"toto@titi.fr \"@fr", new String[] {
        "toto@titi.fr " },
        new String[] { "<LITERAL>", "<LITERAL>" });
  }

  @Test
  public void testDatatype()
  throws Exception {
    this.assertTokenizesTo(_t, "<http://test>",
      new String[] { "http://test" }, new String[] { "<URI>" },
      new String[] { XSDDatatype.XSD_ANY_URI } );
    this.assertTokenizesTo(_t, "\"test\"",
      new String[] { "test" }, new String[] { "<LITERAL>" },
      new String[] { XSDDatatype.XSD_STRING } );
    this.assertTokenizesTo(_t, "_:bnode1",
      new String[] { "bnode1" }, new String[] { "<BNODE>" },
      new String[] { "" } );
    this.assertTokenizesTo(_t, "\"test\"^^<http://type/test>",
      new String[] { "test" }, new String[] { "<LITERAL>" },
      new String[] { "http://type/test" } );
    this.assertTokenizesTo(_t, "\"te^^st\"^^<"+XSDDatatype.XSD_NAME+">",
      new String[] { "te^^st" }, new String[] { "<LITERAL>" },
      new String[] { XSDDatatype.XSD_NAME } );
  }

  @Test
  public void testStructuralNode()
  throws Exception {
    this.assertTokenizesTo(_t, "<http://renaud.delbru.fr/>",
      new String[] { "http://renaud.delbru.fr/" }, new String[] { "<URI>" },
      new int[] { 1 }, new IntsRef[] { node(0,0) });
    this.assertTokenizesTo(_t,
      "<http://renaud.delbru.fr/> <http://renaud.delbru.fr/>",
      new String[] { "http://renaud.delbru.fr/", "http://renaud.delbru.fr/" },
      new String[] { "<URI>", "<URI>" }, new int[] { 1, 1 },
      new IntsRef[] { node(0,0), node(0,1) });
    this.assertTokenizesTo(_t, "_:a1 _:a2 . _:a3 _:a4 . _:a5 _:a6 ",
      new String[] { "a1", "a2", ".", "a3", "a4", ".", "a5", "a6" },
      new String[] { "<BNODE>", "<BNODE>", "<DOT>", "<BNODE>", "<BNODE>",
          "<DOT>", "<BNODE>", "<BNODE>" },
      new int[] { 1, 1, 1, 1, 1, 1, 1, 1 },
      new IntsRef[] { node(0,0), node(0,1), node(0,2),
                      node(1,0), node(1,1), node(1,2),
                      node(2,0), node(2,1) });
    this.assertTokenizesTo(_t, "<http://te.st> . \"ren . aud\" . ",
      new String[] { "http://te.st", ".", "ren . aud", "." },
      new String[] { "<URI>", "<DOT>", "<LITERAL>", "<DOT>" },
      new int[] { 1, 1, 1, 1 },
      new IntsRef[] { node(0,0), node(0,1), node(1,0), node(1,1) });
  }

}
