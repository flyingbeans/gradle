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

package org.gradle.api.internal.changedetection.state;

import com.google.common.hash.HashCode;
import org.gradle.api.Nullable;
import org.gradle.caching.internal.BuildCacheHasher;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 * Hashes resources (e.g., a class file in a jar or a class file in a directory)
 */
public interface ContentHasher {
    /**
     * Returns {@code null} if the file should be ignored.
     */
    @Nullable
    HashCode hash(RegularFileSnapshot fileSnapshot);

    /**
     * Returns {@code null} if the zip entry should be ignored.
     */
    @Nullable
    HashCode hash(ZipEntry zipEntry, InputStream zipInput) throws IOException;

    /**
     * Appends the identification of the implementation of the content hasher to a hasher.
     * The contract is that, if two implementation hashes agree then {@link #hash(RegularFileSnapshot)} and
     * {@link #hash(ZipEntry, InputStream)} return the same result whenever the arguments are the same.
     */
    void appendImplementationToHasher(BuildCacheHasher hasher);
}
