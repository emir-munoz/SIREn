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
 * @author Renaud Delbru [ 27 May 2012 ]
 * @link http://renaud.delbru.fr/
 */
package org.sindice.siren.analysis.attributes;

import java.io.Serializable;

import org.apache.lucene.util.AttributeImpl;

/**
* The position of a token within a cell.
*/
public class PositionAttributeImpl extends AttributeImpl
implements PositionAttribute, Cloneable, Serializable {

  private int position;

  private static final long serialVersionUID = 3697118302108956429L;

  /**
   * Returns this Token's position within the cell.
   */
  public int position() {
    return position;
  }

  /**
   * Set the position.
   * @see #position()
   */
  public void setPosition(final int position) {
    this.position = position;
  }

  @Override
  public void clear() {
    position = 0;
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    }

    if (other instanceof PositionAttributeImpl) {
      final PositionAttributeImpl o = (PositionAttributeImpl) other;
      return position == o.position;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return position;
  }

  @Override
  public void copyTo(final AttributeImpl target) {
    final PositionAttribute t = (PositionAttribute) target;
    t.setPosition(position);
  }

}
