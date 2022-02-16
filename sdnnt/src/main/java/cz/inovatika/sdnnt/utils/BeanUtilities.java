package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.model.NotNullAwareObject;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//TODO: Remove
public class BeanUtilities {

    private BeanUtilities() {}

    public static List<String> getNotNullProperties(Object obj, Class<? extends NotNullAwareObject> clz) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        List<String> vals = new ArrayList<>();
        BeanInfo beanInfo = Introspector.getBeanInfo(clz);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (int i=0,ll=propertyDescriptors.length;i<ll;i++) {
            Method readMethod = propertyDescriptors[i].getReadMethod();
            Object retValue = readMethod.invoke(obj, new Object[]{});
            if (retValue != null) {
                vals.add(propertyDescriptors[i].getName());
            }
        }
        return vals;
    }


}
