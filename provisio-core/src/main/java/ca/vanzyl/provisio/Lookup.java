/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Lookup {
  public void setObjectProperty(Object o, String propertyName, Object value) {
    Class<?> c = o.getClass();
    // First see if name is a property ala javabeans
    String methodSuffix = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1, propertyName.length());
    String methodName = "set" + methodSuffix;
    //
    //TODO Handles bad case where in tests i set the output directory to null 
    //
    if (value == null) {
      return;
    }
    Class<? extends Object> type = value.getClass();
    if (List.class.isAssignableFrom(type)) {
      type = List.class;
    } else if (Map.class.isAssignableFrom(type)) {
      type = Map.class;
    } else if (Boolean.class.isAssignableFrom(type)) {
      // We need to use an Object but we need to look at the target field
      type = boolean.class;
    }

    Method m = getMethod(c, methodName, new Class[] {type});
    if (m != null) {
      try {
        invokeMethod(m, o, value);
      } catch (Exception e) {
      }
    } 
  }

  protected Object newInstance(String name) {
    Object o = null;
    try {
      o = Class.forName(name).newInstance();
    } catch (Exception e) {
      //System.out.println("can't make instance of " + name);
    }
    return o;
  }

  protected Method getMethod(Class c, String methodName, Class[] args) {
    Method m;
    try {
      m = c.getMethod(methodName, args);
    } catch (NoSuchMethodException nsme) {
      m = null;
    }
    return m;
  }

  protected Object invokeMethod(Method m, Object o) throws IllegalAccessException, InvocationTargetException {
    return invokeMethod(m, o, null);
  }

  protected Object invokeMethod(Method m, Object o, Object value) throws IllegalAccessException, InvocationTargetException {
    Object[] args = null;
    if (value != null) {
      args = new Object[] {
          value
      };
    }
    value = m.invoke(o, args);
    return value;
  }
}
