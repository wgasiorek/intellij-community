// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ui.popup;

import com.intellij.ide.ActivityTracker;
import com.intellij.ide.DataManager;
import com.intellij.model.Pointer;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.ui.ListUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Function;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * Use this class to make various hints like QuickDocumentation, ShowImplementations, etc.
 * respond to the selection change in the original component like ProjectView, various GoTo popups, etc.
 *
 * @author gregsh
 */
public abstract class HintUpdateSupply {
  private static final Key<HintUpdateSupply> HINT_UPDATE_MARKER = Key.create("HINT_UPDATE_MARKER");

  private @Nullable JBPopup myHint;
  private JComponent myComponent;

  public static @Nullable HintUpdateSupply getSupply(@NotNull JComponent component) {
    return (HintUpdateSupply)component.getClientProperty(HINT_UPDATE_MARKER);
  }

  public static void hideHint(@NotNull JComponent component) {
    HintUpdateSupply supply = getSupply(component);
    if (supply != null) supply.hideHint();
  }

  public static void installSimpleHintUpdateSupply(@NotNull JComponent component) {
    installHintUpdateSupply(component, o -> o instanceof PsiElement ? (PsiElement)o : null);
  }

  public static void installDataContextHintUpdateSupply(@NotNull JComponent component) {
    installHintUpdateSupply(component, o -> {
      ActivityTracker.getInstance().inc();
      return o instanceof PsiElement ? (PsiElement)o :
             o instanceof Pointer<?> p && p.dereference() instanceof PsiElement e ? e :
             CommonDataKeys.PSI_ELEMENT.getData(DataManager.getInstance().getDataContext(component));
    });
  }

  public static void installHintUpdateSupply(final @NotNull JComponent component, final Function<Object, ? extends PsiElement> provider) {
    HintUpdateSupply supply = new HintUpdateSupply(component) {
      @Override
      protected @Nullable PsiElement getPsiElementForHint(@Nullable Object selectedValue) {
        return provider.fun(selectedValue);
      }
    };
    if (component instanceof JList) supply.installListListener((JList)component);
    if (component instanceof JTree) supply.installTreeListener((JTree)component);
    if (component instanceof JTable) supply.installTableListener((JTable)component);
  }

  protected HintUpdateSupply(@NotNull JComponent component) {
    installSupply(component);
  }

  public HintUpdateSupply(@NotNull JBTable table) {
    installSupply(table);
    installTableListener(table);
  }

  public HintUpdateSupply(@NotNull Tree tree) {
    installSupply(tree);
    installTreeListener(tree);
  }

  public HintUpdateSupply(@NotNull JBList list) {
    installSupply(list);
    installListListener(list);
  }

  protected void installTableListener(final @NotNull JTable table) {
    ListSelectionListener listener = new ListSelectionListener() {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        if (!shouldUpdateHint()) return;

        int selected = ((ListSelectionModel)e.getSource()).getLeadSelectionIndex();
        int rowCount = table.getRowCount();
        if (selected == -1 || rowCount == 0) return;

        PsiElement element = getPsiElementForHint(table.getValueAt(Math.min(selected, rowCount - 1), 0));
        if (element != null && element.isValid()) {
          updateHint(element);
        }
      }
    };
    table.getSelectionModel().addListSelectionListener(listener);
    table.getColumnModel().getSelectionModel().addListSelectionListener(listener);
  }

  protected void installTreeListener(final @NotNull JTree tree) {
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(final TreeSelectionEvent e) {
        if (!shouldUpdateHint()) return;

        TreePath path = tree.getSelectionPath();
        if (path != null) {
          final PsiElement psiElement = getPsiElementForHint(path.getLastPathComponent());
          if (psiElement != null && psiElement.isValid()) {
            updateHint(psiElement);
          }
        }
      }
    });
  }

  protected void installListListener(@NotNull JList list) {
    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        if (!shouldUpdateHint()) return;

        Object[] selectedValues = ((JList<?>)e.getSource()).getSelectedValues();
        if (selectedValues.length != 1) return;

        PsiElement element = getPsiElementForHint(selectedValues[0]);
        if (element != null && element.isValid()) {
          updateHint(element);
        }
      }
    });
  }

  protected abstract @Nullable PsiElement getPsiElementForHint(@Nullable Object selectedValue);

  private void installSupply(@NotNull JComponent component) {
    component.putClientProperty(HINT_UPDATE_MARKER, this);
    myComponent = component;
  }

  public void registerHint(JBPopup hint) {
    hideHint();
    myHint = hint;
    Disposer.register(hint, () -> myHint = null);
  }

  public void hideHint() {
    if (isHintVisible(myHint)) {
      myHint.cancel();
    }

    myHint = null;
  }

  public void updateHint(PsiElement element) {
    if (!isHintVisible(myHint)) return;

    PopupUpdateProcessorBase updateProcessor = myHint.getUserData(PopupUpdateProcessorBase.class);
    if (updateProcessor != null) {
      updateProcessor.updatePopup(element);
    }
  }

  public boolean shouldUpdateHint() {
    return isHintVisible(myHint) && !isSelectedByMouse(myComponent);
  }

  @Contract("!null->true")
  private static boolean isHintVisible(JBPopup hint) {
    return hint != null && hint.isVisible();
  }

  private static boolean isSelectedByMouse(@NotNull JComponent c) {
    return Boolean.TRUE.equals(c.getClientProperty(ListUtil.SELECTED_BY_MOUSE_EVENT));
  }
}
