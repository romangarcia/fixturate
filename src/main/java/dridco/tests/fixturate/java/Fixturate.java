/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dridco.tests.fixturate.java;

import dridco.tests.fixturate.Fixture;

public class Fixturate<T> {
    public static <T> Fixturate<T> fixture(Class<T> modelClass) {
        return new Fixturate<T>(modelClass);
    }

    private Class<T> modelClass;
    private String variant = "default";

    public Fixturate(Class<T> modelClass) {
        this.modelClass = modelClass;
    }

    public Fixturate(Class<T> modelClass, String variant) {
        this(modelClass);
        this.variant = variant;
    }

    public Fixturate<T> in(String variant) {
        return new Fixturate<T>(modelClass, variant);
    }

    public T get() {
        return new Fixture<T>(modelClass).get(variant);
    }

}
