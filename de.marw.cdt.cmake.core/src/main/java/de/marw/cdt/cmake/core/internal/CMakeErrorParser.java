/*******************************************************************************
 * Copyright (c) 2013-2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import de.marw.cdt.cmake.core.CdtPlugin;

/**
 * @author Martin Weber
 */
public class CMakeErrorParser extends OutputStream {

  public static final String CMAKE_PROBLEM_MARKER_ID = CdtPlugin.PLUGIN_ID + ".problem"; //$NON-NLS-1$

  /**
   * Taken from cmMessenger.cxx#printMessagePreamble: <code>
   *
   * <pre>
    if (t == cmake::FATAL_ERROR) {
    msg << "CMake Error";
  } else if (t == cmake::INTERNAL_ERROR) {
    msg << "CMake Internal Error (please report a bug)";
  } else if (t == cmake::LOG) {
    msg << "CMake Debug Log";
  } else if (t == cmake::DEPRECATION_ERROR) {
    msg << "CMake Deprecation Error";
  } else if (t == cmake::DEPRECATION_WARNING) {
    msg << "CMake Deprecation Warning";
  } else if (t == cmake::AUTHOR_WARNING) {
    msg << "CMake Warning (dev)";
  } else if (t == cmake::AUTHOR_ERROR) {
    msg << "CMake Error (dev)";
  } else {
    msg << "CMake Warning";
   * </pre>
   *
   * <code><br>
   * NOTE: That does not handle message-types MESSAGE or WARNING,
   *
   */
  // message start markers...
  private static final String START_DERROR = "CMake Deprecation Error";
  private static final String START_DWARNING = "CMake Deprecation Warning";
  private static final String START_ERROR = "CMake Error";
  private static final String START_ERROR_DEV = "CMake Error (dev)";
  private static final String START_IERROR = "CMake Internal Error (please report a bug)";
  private static final String START_LOG = "CMake Debug Log";
  private static final String START_WARNING = "CMake Warning";
  private static final String START_WARNING_DEV = "CMake Warning (dev)";
  private static final String START_STATUS = "--";
  /** to terminate on the output of 'message("message test")' */
  private static final String START_MSG_SIMPLE = "\\R\\R";
  /** Start of a new error message, also ending the previous message. */
  private static final Pattern PTN_MSG_START;

  /** Name of the named-capturing group that holds a file name. */
  private static final String GP_FILE = "FilE";
  /** Name of the named-capturing group that holds a line number. */
  private static final String GP_LINE = "LinenO";

  /** patterns used to extract file-name and line number information */
  private static final Pattern[] PTN_LOCATION;

  static {
    String ptn = "^" + String.join("|", START_DERROR, START_DWARNING, Pattern.quote(START_ERROR_DEV), START_ERROR,
        Pattern.quote(START_IERROR), START_LOG, Pattern.quote(START_WARNING_DEV), START_WARNING, START_STATUS,
        START_MSG_SIMPLE);
    PTN_MSG_START = Pattern.compile(ptn);
    PTN_LOCATION = new Pattern[] { Pattern.compile("(?m)^ at (?<" + GP_FILE + ">.+):(?<" + GP_LINE + ">\\d+).*$"),
        Pattern.compile("(?m)^ in (?<" + GP_FILE + ">.+):(?<" + GP_LINE + ">\\d+).*$"),
        Pattern.compile("(?m)^:\\s.+$"), };
  }

  ////////////////////////////////////////////////////////////////////
  // the source root of the project being built
  private final IContainer srcPath;
  private final OutputStream os;

  private final StringBuilder buffer;

  /** <code>true</code> if a start-of-message is in the buffer */
  private boolean somSeen;

  /**
   * @param srcFolder
   *          the source root of the project being built
   * @param outputStream
   *          the OutputStream to write to or {@code null}
   */
  public CMakeErrorParser(IContainer srcFolder, OutputStream outputStream) {
    this.srcPath = Objects.requireNonNull(srcFolder);
    this.os = outputStream;
    buffer = new StringBuilder(512);
  }

  /**
   * Deletes all CMake error markers on the specified project.
   *
   * @param project
   *          the project where to remove the error markers.
   * @throws CoreException
   */
  public static void deleteErrorMarkers(IProject project) throws CoreException {
    project.deleteMarkers(CMAKE_PROBLEM_MARKER_ID, false, IResource.DEPTH_INFINITE);
  }

  private void processBuffer(boolean isEOF) {
    Matcher matcher = PTN_MSG_START.matcher("");
    for (;;) {
      matcher.reset(buffer);
      if (matcher.find()) {
        if (!somSeen) {
          somSeen = true;
          // no Start Of Message in buffer yet, discard leading chars
          buffer.delete(0, matcher.start());
          return;
        }
        // Start Of Message is present in the buffer
        String classification = matcher.group();
        int start = matcher.end();
        // get start of next message
        if (matcher.find() || isEOF) {
          int end = isEOF ? buffer.length() : matcher.start();
          String fullMessage = buffer.substring(0, end);
          System.err.println("-###" + fullMessage.trim() + "\n###-");
          String content = buffer.substring(start, end);
          // buffer contains a complete message
          processMessage(classification, content, fullMessage);
          buffer.delete(0, end);
        } else {
          break;
        }
      } else {
        // nothing found in buffer
        return;
      }
    }
  }

  /**
   * @param classification
   *          message classification string
   * @param content
   *          message content, which is parsed according to the classification
   * @param fullMessage
   *          the complete message, including the classification
   */
  private void processMessage(String classification, String content, String fullMessage) {
    MarkerCreator creator;
    switch (classification) {
    case START_DERROR:
      creator = new MC_DError();
      break;
    case START_DWARNING:
      creator = new MC_DWarning();
      break;
    case START_ERROR:
      creator = new MC_Error();
      break;
    case START_ERROR_DEV:
      creator = new MC_ErrorDev();
      break;
    case START_IERROR:
      creator = new MC_IError();
      break;
    case START_WARNING:
      creator = new MC_Warning();
      break;
    case START_WARNING_DEV:
      creator = new MC_WarningDev();
      break;
    default:
      return; // ignore message
    }

    try {
      creator.createMarker(fullMessage, content);
    } catch (CoreException e) {
      CdtPlugin.getDefault().getLog()
          .log(new Status(IStatus.WARNING, CdtPlugin.PLUGIN_ID, "CMake output error parsing failed", e));
    }
  }

  @Override
  public void write(int c) throws IOException {
    if (os != null)
      os.write(c);
    buffer.append(new String(new byte[] { (byte) c }));
    processBuffer(false);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (os != null)
      os.write(b, off, len);
    buffer.append(new String(b, off, len));
    processBuffer(false);
  }

  @Override
  public void flush() throws IOException {
    if (os != null)
      os.flush();
  }

  @Override
  public void close() throws IOException {
    if (os != null)
      os.close();
    // process remaining bytes
    processBuffer(true);
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  /**
   * Generic marker creator.
   *
   * @author Martin Weber
   */
  private abstract class MarkerCreator {

    /**
     * Gets the message classification that this object handles.
     */
    abstract String getClassification();

    /**
     * @return the severity of the problem, see {@link IMarker} for acceptable severity values
     */
    abstract int getSeverity();

    /**
     * Creates the {@link IMarker marker object} that reflects the message.
     *
     * @param fullMessage
     *          the complete message, including the classification
     * @param content
     *          the message, without the classification
     * @throws CoreException
     */
    public void createMarker(String fullMessage, String content) throws CoreException {
      for (Pattern ptn : PTN_LOCATION) {
        final Matcher matcher = ptn.matcher(content);
        if (matcher.find()) {
          String filename = null;
          try {
            filename = matcher.group(GP_FILE);
          } catch (IllegalArgumentException expected) {
          }
          IMarker marker = createBasicMarker(filename, getSeverity(), fullMessage.trim());
          try {
            String lineno = matcher.group(GP_LINE);
            Integer lineNumber = Integer.parseInt(lineno);
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
          } catch (IllegalArgumentException expected) {
          }
          break;
        }
      }
    }

    /**
     * Creates a basic problem marker which should be enhanced with more problem information (e.g. severity, file name,
     * line number exact message).
     *
     * @param fileName
     *          the file where the problem occurred, relative to the source-root or <code>null</code> to denote just the
     *          current project being build
     * @param severity
     *          the severity of the problem, see {@link IMarker} for acceptable severity values
     * @param fullMessage
     *          the complete message, including the classification
     * @throws CoreException
     */
    protected final IMarker createBasicMarker(String fileName, int severity, String fullMessage) throws CoreException {
      // cmake reports the file relative to source entry
      IResource owner = fileName == null ? srcPath : srcPath.getFile(new Path(fileName));
      IMarker marker = owner.createMarker(CMAKE_PROBLEM_MARKER_ID);
      marker.setAttribute(IMarker.MESSAGE, fullMessage);
      marker.setAttribute(IMarker.SEVERITY, severity);
      marker.setAttribute(IMarker.LOCATION, CMakeErrorParser.class.getName());
      return marker;
    }
  } // MarkerCreator

  private class MC_DError extends MarkerCreator {
    @Override
    String getClassification() {
      return START_DERROR;
    }

    @Override
    int getSeverity() {
      return IMarker.SEVERITY_ERROR;
    };
  }

  private class MC_DWarning extends MarkerCreator {
    @Override
    String getClassification() {
      return START_DWARNING;
    }

    @Override
    int getSeverity() {
      return IMarker.SEVERITY_WARNING;
    };
  }

  private class MC_Error extends MarkerCreator {

    @Override
    String getClassification() {
      return START_ERROR;
    }

    @Override
    int getSeverity() {
      return IMarker.SEVERITY_ERROR;
    };
  }

  private class MC_ErrorDev extends MarkerCreator {
    @Override
    String getClassification() {
      return START_ERROR_DEV;
    }

    @Override
    int getSeverity() {
      return IMarker.SEVERITY_ERROR;
    };
  }

  private class MC_IError extends MarkerCreator {
    @Override
    String getClassification() {
      return START_IERROR;
    }

    @Override
    int getSeverity() {
      return IMarker.SEVERITY_ERROR;
    };
  }

  private class MC_Warning extends MarkerCreator {
    @Override
    String getClassification() {
      return START_WARNING;
    }

    @Override
    int getSeverity() {
      return IMarker.SEVERITY_WARNING;
    };
  }

  private class MC_WarningDev extends MarkerCreator {
    @Override
    String getClassification() {
      return START_WARNING_DEV;
    }

    @Override
    int getSeverity() {
      return IMarker.SEVERITY_WARNING;
    };
  }

}
