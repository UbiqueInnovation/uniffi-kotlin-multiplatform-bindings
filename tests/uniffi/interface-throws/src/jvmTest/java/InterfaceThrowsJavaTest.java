/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import interface_throws.Greeter;
import interface_throws.GreeterException;
import interface_throws.Interface_throws_jvmKt;
import interface_throws.RustGreeter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Java sees Kotlin's {@code @Throws} as a {@code throws} clause in the class file. Without it
 * javac refuses to compile a {@code catch} for the (checked) exception at all, so this test only
 * builds if the annotation is generated for the interface *and* for the object implementing it.
 */
public class InterfaceThrowsJavaTest {
    @Test
    public void catchesThroughTheInterface() {
        Greeter greeter = Interface_throws_jvmKt.makeGreeterTrait("Hello");

        try {
            assertEquals("Hello, world!", greeter.greet("world"));
        } catch (GreeterException e) {
            fail("greeting should have succeeded: " + e);
        }

        try {
            greeter.greet("");
            fail("expected a GreeterException");
        } catch (GreeterException e) {
            // expected
        }
    }

    @Test
    public void catchesThroughTheObject() {
        RustGreeter greeter = new RustGreeter("Hello");

        try {
            assertEquals("Hello, world!", greeter.greet("world"));
        } catch (GreeterException e) {
            fail("greeting should have succeeded: " + e);
        }

        try {
            greeter.greet("");
            fail("expected a GreeterException");
        } catch (GreeterException e) {
            // expected
        }
    }
}
