/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.internal.changedetection.state

import com.google.common.collect.Iterators
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.cache.StringInterner
import org.gradle.api.internal.changedetection.rules.ChangeType
import org.gradle.api.internal.changedetection.rules.FileChange
import org.gradle.api.internal.file.TestFiles
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.internal.hash.DefaultFileHasher
import org.gradle.api.resources.normalization.ResourceNormalizationHandler
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.ChangeListener
import org.junit.Rule
import spock.lang.Specification

import static org.gradle.api.internal.changedetection.state.TaskFilePropertyCompareStrategy.*
import static org.gradle.api.internal.changedetection.state.TaskFilePropertySnapshotNormalizationStrategy.ABSOLUTE

class AbstractCollectionSnapshotterTest extends Specification {
    def stringInterner = new StringInterner()
    def fileSystemMirror = new DefaultFileSystemMirror([])
    def snapshotter = new AbstractFileCollectionSnapshotter(stringInterner, TestFiles.directoryFileTreeFactory(), new DefaultFileSystemSnapshotter(new DefaultFileHasher(), stringInterner, TestFiles.fileSystem(), TestFiles.directoryFileTreeFactory(), fileSystemMirror)) {
        @Override
        Class<? extends FileCollectionSnapshotter> getRegisteredType() {
            return AbstractFileCollectionSnapshotter
        }

        @Override
        FileCollectionSnapshot snapshot(FileCollection files, SnapshotNormalizationStrategy snapshotNormalizationStrategy, ResourceNormalizationHandler normalizationHandler) {
            throw new UnsupportedOperationException()
        }
    }
    def listener = Mock(ChangeListener)
    def normalizationHandler = Mock(ResourceNormalizationHandler)
    @Rule
    public final TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()

    def getFilesReturnsOnlyTheFilesWhichExisted() {
        given:
        TestFile file = tmpDir.createFile('file1')
        TestFile dir = tmpDir.createDir('dir')
        TestFile file2 = dir.createFile('file2')
        TestFile noExist = tmpDir.file('file3')

        when:
        def fileCollectionSnapshot = snapshot(files(file, dir, noExist), UNORDERED, ABSOLUTE, normalizationHandler)

        then:
        fileCollectionSnapshot.files as List == [file, file2]
    }

    def "retains order of files in the snapshot"() {
        given:
        TestFile file = tmpDir.createFile('file1')
        TestFile file2 = tmpDir.createFile('file2')
        TestFile file3 = tmpDir.createFile('file3')

        when:
        def fileCollectionSnapshot = snapshot(files(file, file2, file3), ORDERED, ABSOLUTE, normalizationHandler)

        then:
        fileCollectionSnapshot.files == [file, file2, file3]
    }

    def getElementsReturnsAllFilesRegardlessOfWhetherTheyExistedOrNot() {
        given:
        TestFile file = tmpDir.createFile('file1')
        TestFile noExist = tmpDir.file('file3')

        when:
        def fileCollectionSnapshot = snapshot(files(file, noExist), UNORDERED, ABSOLUTE, normalizationHandler)

        then:
        fileCollectionSnapshot.elements == [file, noExist]
    }

    def getElementsIncludesRootDirectories() {
        given:
        TestFile file = tmpDir.createFile('file1')
        TestFile dir = tmpDir.createDir('dir')
        TestFile dir2 = dir.createDir('dir2')
        TestFile file2 = dir2.createFile('file2')
        TestFile noExist = tmpDir.file('file3')

        when:
        def fileCollectionSnapshot = snapshot(files(file, dir, noExist), UNORDERED, ABSOLUTE, normalizationHandler)

        then:
        fileCollectionSnapshot.elements == [file, dir, dir2, file2, noExist]
    }

    def "retains order of elements in the snapshot"() {
        given:
        TestFile file = tmpDir.createFile('file1')
        TestFile file2 = tmpDir.file('file2')
        TestFile file3 = tmpDir.file('file3')
        TestFile file4 = tmpDir.createFile('file4')

        when:
        def fileCollectionSnapshot = this.snapshot(files(file, file2, file3, file4), ORDERED, ABSOLUTE, normalizationHandler)

        then:
        fileCollectionSnapshot.elements == [file, file2, file3, file4]
    }

    def generatesEventWhenFileAdded() {
        given:
        TestFile file1 = tmpDir.createFile('file1')
        TestFile file2 = tmpDir.createFile('file2')

        when:
        def fileCollectionSnapshot = snapshot(files(file1), UNORDERED, ABSOLUTE, normalizationHandler)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(file1, file2), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        1 * listener.added(file2.path)
        0 * _
    }

    def doesNotGenerateEventWhenFileAddedAndAddEventsAreFiltered() {
        given:
        TestFile file1 = tmpDir.createFile('file1')
        TestFile file2 = tmpDir.file('file2')
        TestFile file3 = tmpDir.createFile('file3')
        TestFile file4 = tmpDir.createDir('file4')

        when:
        def fileCollectionSnapshot = snapshot(files(file1, file2), UNORDERED, ABSOLUTE, normalizationHandler)
        file2.createFile()
        def target = snapshot(files(file1, file2, file3, file4), OUTPUT, ABSOLUTE, normalizationHandler)
        Iterators.size(target.iterateContentChangesSince(fileCollectionSnapshot, "TYPE")) == 0

        then:
        0 * _
    }

    def generatesEventWhenFileRemoved() {
        given:
        TestFile file1 = tmpDir.createFile('file1')
        TestFile file2 = tmpDir.createFile('file2')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(files(file1, file2), UNORDERED, ABSOLUTE, normalizationHandler)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(file1), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        1 * listener.removed(file2.path)
        0 * _
    }

    def doesNotGenerateEventForFileWhoseTypeAndMetaDataAndContentHaveNotChanged() {
        given:
        TestFile file = tmpDir.createFile('file')
        file.setLastModified(1234L)

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)
        file.setLastModified(45600L)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        0 * listener._
    }

    def generatesEventWhenFileBecomesADirectory() {
        given:
        TestFile root = tmpDir.createDir('root')
        TestFile file = root.createFile('file')
        def fileCollection = files(root)

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler)
        file.delete()
        file.createDir()
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        1 * listener.changed(file.path)
        0 * _
    }

    def generatesEventWhenFileContentChanges() {
        TestFile file = tmpDir.createFile('file')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler)
        file.write('new content')
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        1 * listener.changed(file.path)
        0 * _
    }

    def doesNotGenerateEventForDirectoryThatHasNotChanged() {
        TestFile dir = tmpDir.createDir('dir')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(files(dir), UNORDERED, ABSOLUTE, normalizationHandler)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(dir), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        0 * _
    }

    def generatesEventForDirectoryThatBecomesAFile() {
        TestFile root = tmpDir.createDir('root')
        def fileCollection = files(root)
        TestFile dir = root.createDir('dir')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler)
        dir.deleteDir()
        dir.createFile()
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        1 * listener.changed(dir.path)
        0 * listener._
    }

    def doesNotGenerateEventForMissingFileThatStillIsMissing() {
        TestFile file = tmpDir.file('unknown')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        0 * _
    }

    def generatesEventWhenMissingFileIsCreated() {
        TestFile root = tmpDir.createDir('root')
        def fileCollection = files(root)
        TestFile file = root.file('newfile')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler)
        file.createFile()
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        1 * listener.added(file.path)
    }

    def generatesEventWhenFileIsDeleted() {
        TestFile root = tmpDir.createDir('root')
        def fileCollection = files(root)
        TestFile file = root.createFile('file')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler)
        file.delete()
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(fileCollection, UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        1 * listener.removed(file.path)
    }

    def ignoresDuplicatesInFileCollection() {
        TestFile file1 = tmpDir.createFile('file')
        TestFile file2 = tmpDir.createFile('file')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = snapshot(files(file1, file2), UNORDERED, ABSOLUTE, normalizationHandler)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(snapshot(files(file1), UNORDERED, ABSOLUTE, normalizationHandler), fileCollectionSnapshot, listener)

        then:
        0 * _
    }

    def canCreateEmptySnapshot() {
        TestFile file = tmpDir.createFile('file')

        when:
        FileCollectionSnapshot fileCollectionSnapshot = FileCollectionSnapshot.EMPTY
        FileCollectionSnapshot newSnapshot = snapshot(files(file), UNORDERED, ABSOLUTE, normalizationHandler)
        fileSystemMirror.beforeTaskOutputsGenerated()
        changes(newSnapshot, fileCollectionSnapshot, listener)

        then:
        fileCollectionSnapshot.files.empty
        1 * listener.added(file.path)
        0 * listener._
    }

    def snapshot(FileCollection fileCollection, TaskFilePropertyCompareStrategy compareStrategy, TaskFilePropertySnapshotNormalizationStrategy normalizationStrategy, ResourceNormalizationHandler normalizationHandler) {
        snapshotter.snapshot(fileCollection, new FileCollectionSnapshotBuilder(compareStrategy, normalizationStrategy, stringInterner))
    }

    private static void changes(FileCollectionSnapshot newSnapshot, FileCollectionSnapshot oldSnapshot, ChangeListener<String> listener) {
        newSnapshot.iterateContentChangesSince(oldSnapshot, "TYPE").each { FileChange change ->
            switch (change.type) {
                case ChangeType.ADDED:
                    listener.added(change.path)
                    break
                case ChangeType.MODIFIED:
                    listener.changed(change.path)
                    break
                case ChangeType.REMOVED:
                    listener.removed(change.path)
                    break
            }
        }
    }

    private static FileCollection files(File... files) {
        new SimpleFileCollection(files)
    }

}
