package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Bean;
import com.ll.framework.ioc.annotations.Component;
import com.ll.standard.util.Ut;
import com.ll.standard.util.Ut.str;
import java.io.ObjectStreamException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import jdk.dynalink.linker.MethodHandleTransformer;
import lombok.RequiredArgsConstructor;
import org.reflections.Reflections;

@RequiredArgsConstructor
public class ApplicationContext {

  private final String basePackage;
  private final Map<String, Object> beanMap = new HashMap<>();

  public void init() {
    Reflections reflections = new Reflections(basePackage);
    Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Component.class);

    for (Class<?> clazz : classes) {
      if (!clazz.isInterface()) {
        String clazzBeanName = str.lcfirst(clazz.getSimpleName());
        if (!beanMap.containsKey(clazzBeanName)) {
          getClassBean(clazz);
        }

        List<Method> methodList = new ArrayList<>();
        Method[] methods = clazz.getMethods();
        Object clazzInstance = beanMap.get(clazzBeanName);
        for (Method method : methods) {
          if (method.isAnnotationPresent(Bean.class)) {
            methodList.add(method);
          }
        }

        for (int i = 0; i < 2; i++) {
          createMethodBean(clazzInstance, methodList);
          System.out.println(methodList.size());
        }
//        while (!methodList.isEmpty()) {
//        }
      }
    }

    for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
      System.out.println(entry.getKey() + " : " + entry.getValue());
    }
  }

  public <T> T genBean(String beanName) {
    return (T) beanMap.get(beanName);
  }

  private Object getClassBean(Class<?> clazz) {
    String beanName = Ut.str.lcfirst(clazz.getSimpleName());
    if (beanMap.containsKey(beanName)) {
      return beanMap.get(beanName);
    }

    // @RequiredArgsConstructor 만 사용하니 생성자는 반드시 1개
    Constructor<?> constructor = clazz.getConstructors()[0];
    Object[] params = Arrays.stream(constructor.getParameterTypes())
        .map(this::getClassBean)
        .toArray();

    try {
      beanMap.put(beanName, constructor.newInstance(params));
      return beanMap.get(beanName);
    } catch (Exception e) {
      System.out.println(beanName + "빈 생성 중 오류가 발생했습니다.");
      return null;
    }
  }

  private void createMethodBean(Object clazzInstance, List<Method> methodList) {
    for (Method method : methodList) {
      Parameter[] parameters = method.getParameters();
      Object[] params = new Object[parameters.length];

      boolean isParamArrFull = true;
      for (int i = 0; i < parameters.length; i++) {
        String paramName = Ut.str.lcfirst(parameters[i].getName());
        System.out.println(paramName);
        if (!beanMap.containsKey(paramName)) {
          isParamArrFull = false;
          break;
        }
        params[i] = beanMap.get(paramName);
      }

      if (isParamArrFull) {
        methodList.remove(method);
        try {
          beanMap.put(method.getName(), method.invoke(clazzInstance, params));
        } catch (Exception e) {
          throw new RuntimeException(
              "Creating method bean failed. Method bean: %s".formatted(method.getName()), e);
        }
        return;
      }
    }
  }
}
