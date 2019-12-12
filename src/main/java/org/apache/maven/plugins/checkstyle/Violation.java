package org.apache.maven.plugins.checkstyle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Holds data about a single violation and represents the violation itself.
 */
public class Violation
{

  /**
   * Indicates that a column is not set.
   */
  protected static final String NO_COLUMN = "-1";

  /** The source file name relative to the project's root. */
  private final String source;

  /** File is the absolute file name to the checked file. */
  private final String file;

  private final String line;

  private String column = NO_COLUMN;

  private final String severity;

  private final String message;

  private final String ruleName;

  private final String category;

  // Leaving out column, because there is no CHECKSTYLE:OFF support.

  /**
   * Creates a violation instance without a column set.
   *
   * @param source
   *     the source file name, relative to the project's root.
   * @param file
   *     the absolute file name in which the violation occurred.
   * @param line
   *     the line in the file on which the violation occurred.
   * @param severity
   *     the severity of the violation.
   * @param message
   *     the message from checkstyle for this violation.
   * @param ruleName
   *     the rule name from which this violation was created.
   * @param category
   *     the category of the checkstyle violation.
   */
  public Violation( String source,
                    String file,
                    String line,
                    String severity,
                    String message,
                    String ruleName,
                    String category )
  {
    this.source = Objects.requireNonNull( source );
    this.file = file;
    this.line = line;
    this.severity = Objects.requireNonNull( severity );
    this.message = Objects.requireNonNull( message );
    this.ruleName = Objects.requireNonNull( ruleName );
    this.category = Objects.requireNonNull( category );
  }

  /**
   * Returns the source file name relative to the project's root.
   *
   * @return the source file name relative to the project's root.
   */
  protected String getSource( )
  {
    return source;
  }

  /**
   * Returns the absolute file name to the checked file.
   *
   * @return the absolute file name to the checked file.
   */
  protected String getFile( )
  {
    return file;
  }

  /**
   * Returns the line of in the checked file on which the violation occurred.
   *
   * @return the line of in the checked file on which the violation occurred.
   */
  protected String getLine( )
  {
    return line;
  }

  /**
   * Returns the column in which the violation occurred, if available.
   *
   * @return the column in which the violation occurred, if available. Otherwise returns {@code "-1"}.
   */
  protected String getColumn( )
  {
    return column;
  }

  protected void setColumn( /* Nullable */ String column )
  {
    if ( null == column || column.length() < 1 )
    {
      this.column = NO_COLUMN;
      return;
    }

    this.column = column;
  }

  /**
   * Returns the severity of the current violation.
   *
   * @return the severity of the current violation.
   */
  protected String getSeverity( )
  {
    return severity;
  }

  /**
   * Returns the message produced by checkstyle for the current violation.
   *
   * @return the message produced by checkstyle for the current violation.
   */
  protected String getMessage( )
  {
    return message;
  }

  /**
   * Returns the name of the rule which led to the current violation.
   *
   * @return the name of the rule which led to the current violation.
   */
  protected String getRuleName( )
  {
    return ruleName;
  }

  /**
   * Returns the category of the current violation.
   *
   * @return the category of the current violation.
   */
  protected String getCategory( )
  {
    return category;
  }

  @Override
  public boolean equals( Object other )
  {
    if ( this == other )
    {
      return true;
    }
    if ( !( other instanceof Violation ) )
    {
      return false;
    }
    Violation violation = ( Violation ) other;
    return line.equals( violation.line )
        && Objects.equals( column, violation.column )
        && source.equals( violation.source )
        && file.equals( violation.file )
        && severity.equals( violation.severity )
        && message.equals( violation.message )
        && ruleName.equals( violation.ruleName )
        && category.equals( violation.category );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( source, file, line, column, severity, message, ruleName, category );
  }

  @Override
  public String toString()
  {
    return new StringJoiner( ", ", Violation.class.getSimpleName() + "[", "]" )
        .add( "source='" + source + "'" )
        .add( "file='" + file + "'" )
        .add( "line=" + line )
        .add( "column=" + column )
        .add( "severity='" + severity + "'" )
        .add( "message='" + message + "'" )
        .add( "ruleName='" + ruleName + "'" )
        .add( "category='" + category + "'" )
        .toString();
  }
}
