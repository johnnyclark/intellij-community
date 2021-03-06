/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.xdebugger.frame;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class XValueChildrenList {
  public static final XValueChildrenList EMPTY = new XValueChildrenList(Collections.<String>emptyList(), Collections.<XValue>emptyList());
  private List<String> myNames;
  private List<XValue> myValues;
  private boolean myAlreadySorted;

  public XValueChildrenList(int initialCapacity) {
    myNames = new ArrayList<String>(initialCapacity);
    myValues = new ArrayList<XValue>(initialCapacity);
  }

  public XValueChildrenList() {
    myNames = new ArrayList<String>();
    myValues = new ArrayList<XValue>();
  }

  public static XValueChildrenList singleton(String name, @NotNull XValue value) {
    return new XValueChildrenList(Collections.singletonList(name), Collections.singletonList(value));
  }

  private XValueChildrenList(List<String> names, List<XValue> values) {
    myNames = names;
    myValues = values;
  }

  public void add(@NonNls String name, @NotNull XValue value) {
    myNames.add(name);
    myValues.add(value);
  }

  public int size() {
    return myNames.size();
  }

  public String getName(int i) {
    return myNames.get(i);
  }

  public XValue getValue(int i) {
    return myValues.get(i);
  }

  public boolean isAlreadySorted() {
    return myAlreadySorted;
  }

  public void setAlreadySorted(boolean alreadySorted) {
    myAlreadySorted = alreadySorted;
  }
}
