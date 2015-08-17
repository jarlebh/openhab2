package org.openhab.binding.tellstick.handler.live;

import javax.xml.bind.annotation.adapters.XmlAdapter;

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
