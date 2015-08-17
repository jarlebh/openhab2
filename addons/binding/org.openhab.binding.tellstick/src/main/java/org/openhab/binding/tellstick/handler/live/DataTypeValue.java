package org.openhab.binding.tellstick.handler.live;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.tellstick.enums.DataType;

@XmlRootElement(name = "data")
public class DataTypeValue {

    @XmlAttribute(name = "name")
    @XmlJavaTypeAdapter(value = NameToDataType.class)
    private DataType dataType;
    @XmlAttribute(name = "value")
    private String data;

    public DataType getName() {
        return dataType;
    }

    public void setName(DataType dataType) {
        this.dataType = dataType;
    }

    public String getValue() {
        return data;
    }

    public void setValue(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DataTypeValue [dataType=" + dataType + ", data=" + data + "]";
    }
}
