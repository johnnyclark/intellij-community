/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.diff;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Represents sub text of other content. Original content should provide not null document.
 */
public class FragmentContent extends DiffContent {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.diff.FragmentContent");
  private final DiffContent myOriginal;
  private final FileType myType;
  private final MyDocumentsSynchonizer mySynchonizer;
  public static final Key<Document> ORIGINAL_DOCUMENT = new Key<Document>("ORIGINAL_DOCUMENT");

  public FragmentContent(DiffContent original, TextRange range, Project project, VirtualFile file) {
    this(original, range, project, file != null ? DiffContentUtil.getContentType(file) : null);
  }

  public FragmentContent(DiffContent original, TextRange range, Project project, FileType type) {
    this(original.getDocument().createRangeMarker(range.getStartOffset(), range.getEndOffset(), true), original, type, project);
  }

  private FragmentContent(RangeMarker rangeMarker, DiffContent original, FileType fileType, Project project) {
    mySynchonizer = new MyDocumentsSynchonizer(project, rangeMarker);
    myOriginal = original;
    myType = fileType;
  }

  public FragmentContent(DiffContent original, TextRange range, Project project) {
    this(original, range, project, (FileType)null);
  }

  private String subText(Document document, int startOffset, int length) {
    return document.getCharsSequence().subSequence(startOffset, startOffset + length).toString();
  }


  public void onAssigned(boolean isAssigned) {
    myOriginal.onAssigned(isAssigned);
    mySynchonizer.listenDocuments(isAssigned);
    super.onAssigned(isAssigned);
  }

  public Document getDocument() {
    return mySynchonizer.getCopy();
  }

  public OpenFileDescriptor getOpenFileDescriptor(int offset) {
    return myOriginal.getOpenFileDescriptor(offset + mySynchonizer.getStartOffset());
  }

  public VirtualFile getFile() { return null; }

  @Nullable
  public FileType getContentType() {
    return myType != null ? myType : myOriginal.getContentType();
  }

  public byte[] getBytes() throws IOException {
    return getDocument().getText().getBytes();
  }

  public static FragmentContent fromRangeMarker(RangeMarker rangeMarker, Project project) {
    Document document = rangeMarker.getDocument();
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    FileType type = file.getFileType();
    return new FragmentContent(new DocumentContent(project, document), TextRange.create(rangeMarker), project, type);
  }

  private class MyDocumentsSynchonizer extends DocumentsSynchonizer {
    private final RangeMarker myRangeMarker;

    public MyDocumentsSynchonizer(Project project, RangeMarker originalRange) {
      super(project);
      myRangeMarker = originalRange;
    }

    public int getStartOffset() { return myRangeMarker.getStartOffset(); }

    protected void onOriginalChanged(DocumentEvent event, Document copy) {
      if (!myRangeMarker.isValid()) {
        fireContentInvalid();
        return;
      }
      replaceString(copy, 0, copy.getTextLength(),
                    subText(event.getDocument(), myRangeMarker.getStartOffset(), getLength()));
    }

    protected void beforeListenersAttached(Document original, Document copy) {
      boolean writable = copy.isWritable();
      Document copyEx = copy;
      if (!writable) copyEx.setReadOnly(false);
      replaceString(copy, 0, copy.getTextLength(),
                    subText(original, myRangeMarker.getStartOffset(), getLength()));
      copyEx.setReadOnly(!writable);
    }

    private int getLength() {
      return myRangeMarker.getEndOffset() - myRangeMarker.getStartOffset();
    }

    protected Document createOriginal() {
      return myRangeMarker.getDocument();
    }

    protected Document createCopy() {
      final Document originalDocument = myRangeMarker.getDocument();
      String textInRange = originalDocument.getCharsSequence().subSequence(myRangeMarker.getStartOffset(), myRangeMarker.getEndOffset()).toString();
      final Document result = EditorFactory.getInstance().createDocument(textInRange);
      result.setReadOnly(!originalDocument.isWritable());
      result.putUserData(ORIGINAL_DOCUMENT, originalDocument);
      return result;
    }

    protected void onCopyChanged(DocumentEvent event, Document original) {
      final int originalOffset = event.getOffset() + myRangeMarker.getStartOffset();
      LOG.assertTrue(originalOffset >= 0);
      if (!original.isWritable()) return;
      final String newText = subText(event.getDocument(), event.getOffset(), event.getNewLength());
      final int originalEnd = originalOffset + event.getOldLength();
      replaceString(original, originalOffset, originalEnd, newText);
    }
  }
}
