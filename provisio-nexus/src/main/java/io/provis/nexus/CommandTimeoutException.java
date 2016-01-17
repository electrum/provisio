/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.nexus;

public class CommandTimeoutException extends CommandFailedException {
  public CommandTimeoutException(Command command) {
    super(command, "did not complete in " + command.getTimeLimit(), null);
  }
}
