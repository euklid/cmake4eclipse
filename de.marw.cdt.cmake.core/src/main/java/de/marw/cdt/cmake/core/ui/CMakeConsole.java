/*******************************************************************************
 * Copyright (c) 2013-2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.IBuildConsoleManager;

/**
 * A console for cmake invocations.
 *
 * @author Martin Weber
 */
public class CMakeConsole extends AbstractConsole {

  private static final String CONSOLE_CONTEXT_MENU_ID = "CMakeConsole"; //$NON-NLS-1$

  @Override
  protected IBuildConsoleManager getConsoleManager() {
    return CUIPlugin.getDefault().getConsoleManager("CMake Console", CONSOLE_CONTEXT_MENU_ID);
  }

}
