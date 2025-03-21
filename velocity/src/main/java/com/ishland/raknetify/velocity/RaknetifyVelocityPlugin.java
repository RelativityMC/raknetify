/*
 * This file is a part of the Velocity implementation of the Raknetify
 * project, licensed under GPLv3.
 *
 * Copyright (c) 2022-2025 ishland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ishland.raknetify.velocity;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Plugin(
        id = "raknetify"
)
public class RaknetifyVelocityPlugin {

    private static final ArrayList<Pattern> excludeRegex = new ArrayList<>();

    static {
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.*Int2ObjectLinked*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.*Int2ObjectSorted*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.*Int2ObjectAVL*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.*Int2ObjectRB*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.*Int2ObjectArray*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.IntList*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.AbstractIntList*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.IntSorted*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.AbstractIntSorted*"));
        excludeRegex.add(Pattern.compile("it.unimi.dsi.fastutil.ints.IntRB*"));
    }

    private static boolean isExcluded(String name) {
        for (Pattern regex : excludeRegex) {
            if (regex.matcher(name).find()) return true;
        }
        return false;
    }

    public static ProxyServer PROXY;
    public static Logger LOGGER;
    public static RaknetifyVelocityPlugin INSTANCE;

    public static URLClassLoader WRAPPER;

    @Inject
    private ProxyServer proxy;
    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent e) {
        INSTANCE = this;
        PROXY = this.proxy;
        LOGGER = this.logger;

        final CodeSource codeSource = RaknetifyVelocityPlugin.class.getProtectionDomain().getCodeSource();
        if (codeSource != null && !isDevLaunch()) {
            try {
                LOGGER.info("Bootstrapping raknetify in wrapped environment");
                final URLClassLoader urlClassLoader = new RaknetifyURLClassLoader("raknetify wrapper", new URL[]{codeSource.getLocation()}, RaknetifyVelocityPlugin.class.getClassLoader());
                final Class<?> launchWrapper = urlClassLoader.loadClass("com.ishland.raknetify.velocity.RaknetifyVelocityLaunchWrapper");
                Preconditions.checkState(launchWrapper.getClassLoader() == urlClassLoader, "Not launched in wrapper");
                WRAPPER = urlClassLoader;
                launchWrapper.getMethod("launch").invoke(null);
                return;
            } catch (Throwable t) {
                LOGGER.error("Error bootstrapping raknetify inside wrapped environment, running in normal environment instead", t);
            }
        }

        try {
            Class.forName("com.ishland.raknetify.velocity.RaknetifyVelocityLaunchWrapper").getMethod("launch").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isDevLaunch() {
        try {
            ClassLoader.getSystemClassLoader().loadClass("com.ishland.raknetify.velocity.RaknetifyVelocityPlugin");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static class RaknetifyURLClassLoader extends URLClassLoader {

        public RaknetifyURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public RaknetifyURLClassLoader(URL[] urls) {
            super(urls);
        }

        public RaknetifyURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
            super(urls, parent, factory);
        }

        public RaknetifyURLClassLoader(String name, URL[] urls, ClassLoader parent) {
            super(name, urls, parent);
        }

        public RaknetifyURLClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
            super(name, urls, parent, factory);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!(name.startsWith("com.ishland.raknetify") && !name.equals("com.ishland.raknetify.velocity.RaknetifyVelocityPlugin"))
                    && !name.startsWith("network.ycc.raknet")
                    && !isExcluded(name)) {
                try {
                    return Class.forName(name, true, this.getParent());
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // ignored, try our own loader
                }
            }

            synchronized (getClassLoadingLock(name)) {
                try {
                    final Class<?> clazz = this.findClass(name);
                    if (resolve) this.resolveClass(clazz);
                    return clazz;
                } catch (ClassNotFoundException e1) {
                    // then fail here, there's nothing more we can do
                    throw new ClassNotFoundException(name, e1);
                }
            }
        }
    }
}
