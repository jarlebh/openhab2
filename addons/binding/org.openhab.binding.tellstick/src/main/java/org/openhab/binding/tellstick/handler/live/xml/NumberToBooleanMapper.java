/**
 * Copyright (c)2016 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tellstick.handler.live.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class used to deserialize XML from Telldus Live.
 *
 * @author Jarle Hjortland
 *
 */
public class NumberToBooleanMapper extends XmlAdapter<Integer, Boolean> {

    @Override
    public Boolean unmarshal(Integer v) throws Exception {
        return v == 1 ? true : false;
    }

    @Override
    public Integer marshal(Boolean v) throws Exception {
        return v ? 1 : 0;
    }

}