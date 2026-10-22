package org.slf4j; public interface Logger { default void info(String m,Object... a){} default void warn(String m,Object... a){} default void error(String m,Object... a){} }
