package com.navercorp.pinpoint.bootstrap.plugin;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public final class BytecodeUtils {

    private static final Method DEFINE_CLASS = getDefineClassMethod();

    private BytecodeUtils() {
    }


    private static Method getDefineClassMethod() {
        try {
            final Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            // link error
            throw new RuntimeException("defineClass not found. Caused:" + e.getMessage(), e);
        } catch (SecurityException e) {
            // link error
            throw new RuntimeException("defineClass error. Caused:" + e.getMessage(), e);
        }
    }

    public static Class<?> defineClass(ClassLoader classLoader, String className, byte[] classFile) {
        try {
            return (Class<?>) DEFINE_CLASS.invoke(classLoader, className, classFile, 0, classFile.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] getClassFile(ClassLoader classLoader, String className) {
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        if (className == null) {
            throw new NullPointerException("className must not be null");
        }

        final InputStream is = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");
        if (is == null) {
            throw new RuntimeException("No such class file: " + className);
        }

        ReadableByteChannel channel = Channels.newChannel(is);
        ByteBuffer buffer;

        try {
            buffer = ByteBuffer.allocate(is.available());

            while (channel.read(buffer) >= 0) {
                if (buffer.remaining() == 0) {
                    buffer.flip();
                    ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close(is);
        }

        return buffer.array();
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // skip
            }
        }
    }
}