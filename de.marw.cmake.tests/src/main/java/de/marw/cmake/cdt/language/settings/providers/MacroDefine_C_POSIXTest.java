/*******************************************************************************
 * Copyright (c) 2015 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cmake.cdt.language.settings.providers;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

import de.marw.cmake.cdt.language.settings.providers.ToolArgumentParsers.MacroDefine_C_POSIX;

/**
 * @author Martin Weber
 */
public class MacroDefine_C_POSIXTest {

  private MacroDefine_C_POSIX testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new MacroDefine_C_POSIX();
  }

  /**
   * Test method for
   * {@link ToolArgumentParsers.MacroDefine_C_POSIX#processArgument}.
   */
  @Test
  public final void testProcessArgument() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;

    final IPath cwd= new Path("");
    int len;
    // -DFOO
    String name = "FOO";
    String arg = "-D" + name;

    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    assertEquals(2 + name.length(), len);
    // -D   FOO
    entries.clear();
    arg= "-D   " + name;
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", "", parsed.getValue());
    assertEquals(2 + name.length() + 3, len);
  }

  /**
   * Test method for
   * {@link ToolArgumentParsers.MacroDefine_C_POSIX#processArgument}.
   */
  @Test
  public final void testProcessArgument_Value() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");

    final String name = "FOO";
    String val = "noWhiteSpace";

    // -DFOO=noWhiteSpace
    entries.clear();
    assertEquals(2 + name.length() + 1 + val.length(),
        testee.processArgument(entries, cwd, "-D" + name + "=" + val + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D   FOO=noWhiteSpace
    entries.clear();
    assertEquals(2 + name.length() + 1 + 3 + val.length(),
        testee.processArgument(entries, cwd, "-D   " + name + "=" + val + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());

    //----------------------------------------
    val = "Wh it e s ap ac ";
    // -D   'FOO=Wh it e s ap ac '
    entries.clear();
    assertEquals(
        2 + name.length() + 1 + 2 + 3 + val.length(),
        testee.processArgument(entries, cwd, "-D   " + "'" + name + "=" + val + "'"
            + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D   "FOO=Wh it e s ap ac "
    entries.clear();
    assertEquals(
        2 + name.length() + 1 + 2 + 3 + val.length(),
        testee.processArgument(entries, cwd, "-D   " + "\"" + name + "=" + val
            + "\"" + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
  }

  /**
   * Test method for
   * {@link ToolArgumentParsers.MacroDefine_C_POSIX#processArgument}.
   */
  @Test
  public final void testProcessArgument_Value_CharLiteral() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");
    int len;
    String val, arg;

    final String name = "FOO";

    //----------------------------------------
    //  -DFOO='noWhiteSpace'
    val = "'noWhiteSpace'";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -D   FOO='noWhiteSpace'
    arg = "-D   " + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //----------------------------------------
    //  -DFOO='noWhite\'escapedQuoteChar' (values with single quotes)
    val = "'noWhite\\'escapedQuoteChar'";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -DFOO='noWhite\\escapedEscapeChar'
    val = "'noWhite\\\\escapedEscapeChar'";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //----------------------------------------
    // -DFOO='Wh it e s ap ac '
    val = "'Wh it e s ap ac '";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    // -D   FOO='Wh it e s ap ac '
    arg= "-D   " + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -DFOO='Wh it e s ap ac \'escaped Quote Char' (values with single quotes)
    val = "'Wh it e s ap ac \\'escaped Quote Char'";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -DFOO='Wh it e s ap ac \\escaped Escape Char'
    val = "'Wh it e s ap ac \\\\escaped Escape Char'";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
  }

  /**
   * Test method for
   * {@link ToolArgumentParsers.MacroDefine_C_POSIX#processArgument}.
   */
  @Test
  public final void testProcessArgument_Value_StringLiteral() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");
    int len;
    String val, arg;

    final String name = "FO$O";
    //----------------------------------------
    //  -DFOO="noWhiteSpace"
    val = "\"noWhiteSpace\"";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -D   FOO="noWhiteSpace"
    arg = "-D   " + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //----------------------------------------
    //  -DFOO="noWhite\"escapedQuoteChar" (values with single quotes)
    val = "\"noWhite\\\"escapedQuoteChar\"";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -DFOO="noWhite\\escapedEscapeChar"
    val = "\"noWhite\\\\escapedEscapeChar\"";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //----------------------------------------
    // -DFOO="Wh it e s ap ac "
    val = "\"Wh it e s ap ac \"";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    // -D   FOO="Wh it e s ap ac "
    arg= "-D   " + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -DFOO="Wh it e s ap ac \"escaped Quote Char" (values with single quotes)
    val = "\"Wh it e s ap ac \\\"escaped Quote Char\"";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);
    //  -DFOO="Wh it e s ap ac \\escaped Escape Char"
    val = "\"Wh it e s ap ac \\\\escaped Escape Char\"";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg + more);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    assertEquals(arg.length(), len);

    //----------------------------------------
    // special case
    //  -DFOO="2(vtkIOMySQL,vtkIOPostgreSQL)"
    val = "\"2(vtkIOMySQL,vtkIOPostgreSQL)\"";
    arg = "-D" + name + "=" + val;
    entries.clear();
    len = testee.processArgument(entries, cwd, arg + " " + arg  + more);
    assertEquals(arg.length(), len);
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
  }

  /**
   * Test method for
   * {@link ToolArgumentParsers.MacroDefine_C_POSIX#processArgument}.
   */
  @Test
  public final void testProcessArgument_MacroWithArgs() {
    final String more = " -g -MMD -MT CMakeFiles/execut1.dir/util1.c.o -MF \"CMakeFiles/execut1.dir/util1.c.o.d\""
        + " -o CMakeFiles/execut1.dir/util1.c.o -c /testprojects/C-subsrc/src/src-sub/main1.c";
    List<ICLanguageSettingEntry> entries = new ArrayList<>();
    ICLanguageSettingEntry parsed;
    final IPath cwd= new Path("");

    // -DFOO=noWhiteSpace
    final String name = "FOO";
    final String args = "(a,b,c)";
    String val = "(a)/((b)+(c))";

    // -DFOO(a,b,c)=(a)/((b)+(c))
    entries.clear();
    assertEquals(2 + name.length() + args.length() + 1 + val.length(),
        testee.processArgument(entries, cwd, "-D" + name + args + "=" + val + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
    // -D   FOO(a,b,c)=(a)/((b)+(c))
    entries.clear();
    assertEquals(
        2 + name.length() + args.length() + 1 + 3 + val.length(),
        testee.processArgument(entries, cwd, "-D   " + name + args + "=" + val
            + more));
    assertEquals("#entries", 1, entries.size());
    parsed = entries.get(0);
    assertEquals("kind", ICSettingEntry.MACRO, parsed.getKind());
    assertEquals("name", name, parsed.getName());
    assertEquals("value", val, parsed.getValue());
  }
}
