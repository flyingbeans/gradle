/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.resources;

import org.gradle.caching.internal.BuildCacheHasher;

import java.nio.CharBuffer;

public abstract class AbstractPath implements NormalizedPath {
    @Override
    public int compareTo(NormalizedPath o) {

        CharSequence path = getPath();
        CharSequence otherPath = o.getPath();
        if (path instanceof CharBuffer && otherPath instanceof CharBuffer) {
            return ((CharBuffer) path).compareTo((CharBuffer) otherPath);
        }
        return path.toString().compareTo(otherPath.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractPath that = (AbstractPath) o;

        return getPath().equals(that.getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public void appendToHasher(BuildCacheHasher hasher) {
        hasher.putString(getPath());
    }
}
