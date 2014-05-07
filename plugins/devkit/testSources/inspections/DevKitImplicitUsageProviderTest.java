/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.inspections;

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection;
import com.intellij.codeInspection.unusedSymbol.UnusedSymbolLocalInspection;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

public class DevKitImplicitUsageProviderTest extends LightCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return PluginPathManager.getPluginHomePathRelative("devkit") + "/testData/inspections/implicitUsage";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myFixture.addClass("package com.intellij.util.xml; public interface DomElement {}");
    myFixture.addClass("package com.intellij.util.xml; public interface DomElementVisitor {}");
    myFixture.addClass("package com.intellij.util.xml; public interface GenericAttributeValue<T> extends DomElement {}");

    myFixture.enableInspections(new UnusedSymbolLocalInspection(), new UnusedDeclarationInspection());
  }

  public void testImplicitUsagesDomElement() {
    myFixture.testHighlighting("ImplicitUsagesDomElement.java");
  }

  public void testImplicitUsagesDomElementVisitor() {
    myFixture.testHighlighting("ImplicitUsagesDomElementVisitor.java");
  }
}
