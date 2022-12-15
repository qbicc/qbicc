package org.qbicc.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.eclipse.sisu.Nullable;
import org.eclipse.sisu.Typed;
import org.jboss.logging.Logger;

/**
 * A simple bean injection container which uses SISU annotations and files.
 */
public final class QbiccBeanContainer {
    private static final Logger log = Logger.getLogger("org.qbicc.main.bean-container");

    private final List<Bean<?>> services;

    public QbiccBeanContainer() {
        final ArrayList<Bean<?>> services = new ArrayList<>();
        final ClassLoader cl = QbiccBeanContainer.class.getClassLoader();
        try {
            final Enumeration<URL> e = cl.getResources("META-INF/sisu/javax.inject.Named");
            while (e.hasMoreElements()) {
                final URL url = e.nextElement();
                final URLConnection conn = url.openConnection();
                try (InputStream is = conn.getInputStream()) {
                    try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        try (BufferedReader br = new BufferedReader(isr)) {
                            String line;
                            while ((line = (br.readLine())) != null) {
                                final String className = line.trim();
                                if (className.isBlank()) {
                                    continue;
                                }
                                final Class<?> clazz;
                                try {
                                    clazz = Class.forName(className, false, cl);
                                } catch (ClassNotFoundException ex) {
                                    throw new RuntimeException("No service for class " + className);
                                }
                                final Named named = clazz.getAnnotation(Named.class);
                                final Typed typed = clazz.getAnnotation(Typed.class);
                                makeService(services, clazz, named, typed);
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        this.services = List.copyOf(services);
    }

    private <T> void makeService(final List<Bean<?>> beans, final Class<T> clazz, final Named named, final Typed typed) {
        final String actualNamed = named == null ? "" : named.value();
        final Set<Class<?>> actualTyped = typed == null ? null : Set.of(typed.value());
        if (Provider.class.isAssignableFrom(clazz)) {
            // the typed set is restricted to itself since @Typed applies to the provided class
            final Bean<T> baseBean = new Bean<>(actualNamed, clazz, Set.of(clazz));
            beans.add(baseBean);
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType pt && pt.getRawType() == Provider.class) {
                    final Type typeArg = pt.getActualTypeArguments()[0];
                    if (typeArg instanceof Class<?> provided) {
                        makeService(beans, provided, actualNamed, actualTyped, baseBean);
                        return;
                    }
                }
            }
            log.warnf("Failed to decode a provider interface from %s", clazz);
        } else {
            beans.add(new Bean<>(actualNamed, clazz, actualTyped));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void makeService(final List<Bean<?>> services, final Class<T> clazz, final String actualNamed, Set<Class<?>> actualTyped, Provider<?> provider) {
        services.add(new Bean<T>(actualNamed, clazz, actualTyped, () -> ((Provider<T>)provider.get()).get()));
        // todo: if the nested service class in turn implements Provider...
    }

    public <T> T get(Class<T> serviceType) {
        for (Bean<?> service : services) {
            if (service.typed == null || service.typed.contains(serviceType)) {
                if (serviceType.isAssignableFrom(service.clazz)) {
                    // good enough
                    final Object instance = service.get();
                    if (instance != FAILED) {
                        return serviceType.cast(instance);
                    }
                }
            }
        }
        throw new RuntimeException("No services matching " + serviceType);
    }

    public <T> T get(Class<T> serviceType, String name) {
        for (Bean<?> service : services) {
            if (serviceType.isAssignableFrom(service.clazz)) {
                if (service.typed == null || service.typed.contains(serviceType)) {
                    // good enough...?
                    if (service.name.equals(name)) {
                        final Object instance = service.get();
                        if (instance != FAILED) {
                            return serviceType.cast(instance);
                        }
                    }
                }
            }
        }
        throw new RuntimeException("No services matching " + serviceType);
    }

    public <T> List<T> getAll(Class<T> serviceType) {
        ArrayList<T> all = new ArrayList<>();
        for (Bean<?> service : services) {
            if (service.typed == null || service.typed.contains(serviceType)) {
                if (serviceType.isAssignableFrom(service.clazz)) {
                    final Object instance = service.get();
                    if (instance != FAILED) {
                        all.add(serviceType.cast(instance));
                    }
                }
            }
        }
        return List.copyOf(all);
    }

    public <T> List<T> getAll(Class<T> serviceType, String name) {
        ArrayList<T> all = new ArrayList<>();
        for (Bean<?> service : services) {
            if (service.typed == null || service.typed.contains(serviceType)) {
                if (serviceType.isAssignableFrom(service.clazz)) {
                    if (service.name.equals(name)) {
                        final Object instance = service.get();
                        if (instance != FAILED) {
                            all.add(serviceType.cast(instance));
                        }
                    }
                }
            }
        }
        return List.copyOf(all);
    }

    public <T> Map<String, T> getAllAsMap(Class<T> serviceType) {
        HashMap<String, T> all = new HashMap<>();
        for (Bean<?> service : services) {
            if (service.typed == null || service.typed.contains(serviceType)) {
                if (serviceType.isAssignableFrom(service.clazz)) {
                    final Object instance = service.get();
                    if (instance != FAILED && all.putIfAbsent(service.name, serviceType.cast(instance)) != null) {
                        throw new IllegalStateException("Duplicate service named \"" + service.name + "\" for " + service.clazz);
                    }
                }
            }
        }
        return Map.copyOf(all);
    }

    private static final Object FAILED = new Object();

    final class ReflectiveProvider<T> implements Provider<T> {
        final Class<T> clazz;
        volatile T instance;

        ReflectiveProvider(Class<T> clazz) {
            this.clazz = clazz;
        }

        public T get() {
            T instance = this.instance;
            if (instance == null) {
                synchronized (QbiccBeanContainer.this) {
                    instance = this.instance;
                    if (instance == null) {
                        instance = this.instance = instantiate();
                    }
                }
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        private Constructor<T> findConstructor() {
            try {
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    final Inject inject = ctor.getAnnotation(Inject.class);
                    if (inject != null) {
                        ctor.setAccessible(true);
                        return (Constructor<T>) ctor;
                    }
                }
                // try again, find the no-arg one
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    if (ctor.getParameterTypes().length == 0) {
                        ctor.setAccessible(true);
                        return (Constructor<T>) ctor;
                    }
                }
            } catch (NoClassDefFoundError error) {
                return null;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private T instantiate() {
            final Constructor<T> ctor = findConstructor();
            if (ctor == null) {
                log.debugf("Rejecting %s because it had no matching constructor: %s", clazz);
                return (T) FAILED;
            }
            // find arguments
            final Parameter[] parameters = ctor.getParameters();
            final Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                final Type genericType = parameter.getParameterizedType();
                final Class<?> parameterType = parameter.getType();
                Object result = processItem(genericType, parameter.getAnnotation(Named.class), parameterType);
                if (result == FAILED) {
                    if (parameter.getAnnotation(Nullable.class) != null) {
                        result = null;
                    } else {
                        return (T) FAILED;
                    }
                }
                args[i] = result;
            }
            final T instance;
            try {
                instance = ctor.newInstance(args);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                log.debugf("Rejecting %s because of an exception: %s", clazz, e);
                return (T) FAILED;
            }
            // set optimistically
            this.instance = instance;
            Class<?> current = clazz;
            do {
                for (Field field : current.getDeclaredFields()) {
                    final int mods = field.getModifiers();
                    if (Modifier.isStatic(mods) || Modifier.isFinal(mods)) {
                        continue;
                    }
                    if (field.getAnnotation(Inject.class) == null) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object res = processItem(field.getGenericType(), field.getAnnotation(Named.class), field.getType());
                    if (res == FAILED) {
                        if (field.getAnnotation(Nullable.class) != null) {
                            res = null;
                        } else {
                            return (T) FAILED;
                        }
                    }
                    try {
                        field.set(instance, res);
                    } catch (IllegalAccessException e) {
                        log.debugf("Rejecting %s because of an exception: %s", clazz, e);
                        return (T) FAILED;
                    }
                }
                current = current.getSuperclass();
            } while (current != Object.class    );
            return instance;
        }

        private Object processItem(Type genericType, final Named named, final Class<?> parameterType) {
            Object result;
            if (parameterType == Map.class) {
                if (getTypeArgument(genericType, 0) != String.class) {
                    throw new IllegalArgumentException("Invalid map key type");
                }
                Class<?> pc = getTypeArgument(genericType, 1);
                result = getAllAsMap(pc);
            } else if (parameterType == List.class) {
                Class<?> pc = getTypeArgument(genericType, 0);
                result = getAll(pc);
            } else if (parameterType == Set.class) {
                Class<?> pc = getTypeArgument(genericType, 0);
                final List<?> all = named == null ? getAll(pc) : getAll(pc, named.value());
                result = Set.copyOf(all);
            } else {
                try {
                    result = named == null ? QbiccBeanContainer.this.get(parameterType) : QbiccBeanContainer.this.get(parameterType, named.value());
                } catch (Exception e) {
                    log.debugf("Rejecting %s because of an exception: %s", clazz, e);
                    result = FAILED;
                }
            }
            return result;
        }

        private Class<?> getTypeArgument(final Type genericType, final int idx) {
            if (genericType instanceof ParameterizedType pt) {
                final Type actualArg = pt.getActualTypeArguments()[idx];
                if (actualArg instanceof Class<?> cls) {
                    return cls;
                }
            }
            log.debugf("Rejecting %s because of a bad type argument", clazz);
            throw new IllegalStateException("Bad type argument");
        }

        @Override
        public String toString() {
            return String.format("reflective provider for %s", clazz);
        }
    }

    final class Bean<T> implements Provider<T> {
        final String name;
        final Class<T> clazz;
        final Set<Class<?>> typed;
        final Provider<T> provider;
        volatile T instance;

        Bean(String name, Class<T> clazz, Set<Class<?>> typed, Provider<T> provider) {
            this.name = name;
            this.clazz = clazz;
            this.typed = typed;
            this.provider = provider;
        }

        Bean(String name, Class<T> clazz, Set<Class<?>> typed) {
            this.name = name;
            this.clazz = clazz;
            this.typed = typed;
            provider = new ReflectiveProvider<>(clazz);
        }

        @SuppressWarnings("unchecked")
        public T get() {
            T instance = this.instance;
            if (instance == null) {
                synchronized (QbiccBeanContainer.this) {
                    instance = this.instance;
                    if (instance == null) try {
                        instance = provider.get();
                    } catch (Throwable t) {
                        log.debugf(t, "Rejecting %s because its provider failed with an exception", clazz);
                        instance = (T) FAILED;
                    }
                    this.instance = instance;
                }
            }
            return instance;
        }

        @Override
        public String toString() {
            return String.format("Service \"%s\" for %s, provided by %s, instance is %s", name, clazz, provider, instance);
        }
    }
}

