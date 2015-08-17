package org.openhab.binding.tellstick.handler.live;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.tellstick.enums.DataType;

public class NameToDataType extends XmlAdapter<String, DataType> {

    @Override
    public DataType unmarshal(String v) throws Exception {
        switch (v) {
            case "temp":
                return DataType.TEMPERATURE;
            case "humidity":
                return DataType.HUMIDITY;
            default:
                return null;
        }
    }

    @Override
    public String marshal(DataType v) throws Exception {
        switch (v) {
            case TEMPERATURE:
                return "temp";
            case HUMIDITY:
                return "humidity";
            default:
                return null;

        }
    }

}