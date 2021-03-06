/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class OpValue {

    /**
     * @zm-api-field-description Operation to apply to an address
     * <br />
     * <ul>
     * <li> <b>+</b> : add, ignored if the value already exists
     * <li> <b>-</b> : remove, ignored if the value does not exist
     * </ul>
     * if not present, replace the entire list with provided values.
     */
    @XmlAttribute(name=AccountConstants.A_OP, required=false)
    private final String op;

    /**
     * @zm-api-field-description Email address
     */
    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private OpValue() {
        this((String) null, (String) null);
    }

    public OpValue(String op, String value) {
        this.op = op;
        this.value = value;
    }

    public String getOp() { return op; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("op", op)
            .add("value", value)
            .toString();
    }
}
