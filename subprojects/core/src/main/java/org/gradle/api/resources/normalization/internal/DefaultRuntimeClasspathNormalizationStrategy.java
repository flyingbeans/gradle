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

package org.gradle.api.resources.normalization.internal;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.pattern.PatternMatcherFactory;
import org.gradle.api.specs.Spec;

import java.util.Set;

public class DefaultRuntimeClasspathNormalizationStrategy implements RuntimeClasspathNormalizationStrategyInternal {
    private final Set<String> ignores = Sets.newHashSet();

    @Override
    public void ignore(String pattern) {
        ignores.add(pattern);
    }

    @Override
    public Set<Spec<RelativePath>> buildIgnores() {
        ImmutableSet.Builder<Spec<RelativePath>> builder = ImmutableSet.builder();
        for (String ignore : ignores) {
            Spec<RelativePath> matcher = PatternMatcherFactory.getPatternMatcher(false, true, ignore);
            builder.add(matcher);
        }
        return builder.build();
    }
}
