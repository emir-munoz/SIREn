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
 * @project siren-core
 * @author Renaud Delbru [ 24 Jan 2012 ]
 * @link http://renaud.delbru.fr/
 */
package org.sindice.siren.index;

import java.io.IOException;

import org.apache.lucene.util.IntsRef;
import org.sindice.siren.util.NodeUtils;

/**
 * This {@link DocsNodesAndPositionsEnum} wraps another
 * {@link DocsNodesAndPositionsEnum} and applies the interval constraints
 * over the node paths. It filters all the nodes that do not satisfy the
 * constraints.
 *
 * @see NodeUtils#isConstraintSatisfied(IntsRef, int[], int[][])
 */
public class SingleIntervalConstrainedNodesEnum extends ConstrainedNodesEnum {

  private final int level;
  private final int[] constraint;

  public SingleIntervalConstrainedNodesEnum(final DocsNodesAndPositionsEnum docsEnum,
                                            final int level,
                                            final int[] constraint) {
    super(docsEnum);
    this.level = level;
    this.constraint = constraint;
  }

  @Override
  public boolean nextNode() throws IOException {
    while (docsEnum.nextNode()) {
      if (NodeUtils.isConstraintSatisfied(docsEnum.node(), level, constraint)) {
        return true;
      }
    }
    return false;
  }

}