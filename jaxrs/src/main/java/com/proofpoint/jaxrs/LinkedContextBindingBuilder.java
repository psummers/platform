/*
 * Copyright 2015 Proofpoint, Inc.
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
package com.proofpoint.jaxrs;

import com.google.common.base.Supplier;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

import java.lang.reflect.Constructor;

public class LinkedContextBindingBuilder<T>
{
    private final Class<T> type;
    private final MapBinder<Class, Supplier> contextBinder;

    LinkedContextBindingBuilder(Class<T> type, MapBinder<Class, Supplier> contextBinder)
    {
        this.type = type;
        this.contextBinder = contextBinder;
    }
    
    public void to(Class<? extends Supplier<? extends T>> implementation)
    {
        contextBinder.addBinding(type).to(implementation);
    }

    public void to(TypeLiteral<? extends Supplier<? extends T>> implementation)
    {
        contextBinder.addBinding(type).to(implementation);
    }

    public void to(Key<? extends Supplier<? extends T>> targetKey)
    {
        contextBinder.addBinding(type).to(targetKey);
    }

    public void toInstance(Supplier<? extends T> supplier) {
        contextBinder.addBinding(type).toInstance(supplier);
    }

    public void toProvider(Provider<? extends Supplier<? extends T>> provider)
    {
        contextBinder.addBinding(type).toProvider(provider);
    }

    public void toProvider(
            Class<? extends javax.inject.Provider<? extends Supplier<? extends T>>> providerType)
    {
        contextBinder.addBinding(type).toProvider(providerType);
    }

    public void toProvider(
            TypeLiteral<? extends javax.inject.Provider<? extends Supplier<? extends T>>> providerType)
    {
        contextBinder.addBinding(type).toProvider(providerType);
    }

    public void toProvider(
            Key<? extends javax.inject.Provider<? extends Supplier<? extends T>>> providerKey)
    {
        contextBinder.addBinding(type).toProvider(providerKey);
    }

    public <S extends Supplier<? extends T>> void toConstructor(Constructor<S> constructor)
    {
        contextBinder.addBinding(type).toConstructor(constructor);
    }

    public <S extends Supplier<? extends T>>  void toConstructor(
            Constructor<S> constructor, TypeLiteral<? extends S> type)
    {
        contextBinder.addBinding(this.type).toConstructor(constructor, type);
    }
}
