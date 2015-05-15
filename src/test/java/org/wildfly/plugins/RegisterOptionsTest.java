/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.plugins;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

public class RegisterOptionsTest {

    @Test
    public void textExtendWithNull() {
        File subsystem = new File("foo");
        RegisterOptions o1 = new RegisterOptions().subsystem(subsystem).socketBinding(subsystem);

        RegisterOptions o2 = new RegisterOptions();
        o1.extend(o2);
        Assert.assertEquals(subsystem, o1.getSubsystem());
        Assert.assertEquals(subsystem, o1.getSocketBinding());
    }

    @Test
    public void testExtend() {
        File subsystem = new File("foo");
        RegisterOptions o1 = new RegisterOptions().subsystem(subsystem);

        File subsystem2 = new File("foo2");
        RegisterOptions o2 = new RegisterOptions().subsystem(subsystem2);

        o1.extend(o2);
        Assert.assertEquals(subsystem2, o1.getSubsystem());
    }
}
