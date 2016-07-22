/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.realitysink.cover.test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.ExecutionEvent;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.Factory;
import com.oracle.truffle.api.interop.ForeignAccess.Factory10;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.LineLocation;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.EventConsumer;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import com.realitysink.cover.SLLanguage;

public class SLDebugTest {
    private Debugger debugger;
    private final LinkedList<Runnable> run = new LinkedList<>();
    private SuspendedEvent suspendedEvent;
    private Throwable ex;
    private ExecutionEvent executionEvent;
    protected PolyglotEngine engine;
    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Before
    public void before() {
        suspendedEvent = null;
        executionEvent = null;
        engine = PolyglotEngine.newBuilder().setOut(out).setErr(err).onEvent(new EventConsumer<ExecutionEvent>(ExecutionEvent.class) {
            @Override
            protected void on(ExecutionEvent event) {
                executionEvent = event;
                debugger = executionEvent.getDebugger();
                performWork();
                executionEvent = null;
            }
        }).onEvent(new EventConsumer<SuspendedEvent>(SuspendedEvent.class) {
            @Override
            protected void on(SuspendedEvent event) {
                suspendedEvent = event;
                performWork();
                suspendedEvent = null;
            }
        }).build();
        run.clear();
    }

    @After
    public void dispose() {
        if (engine != null) {
            engine.dispose();
        }
    }

    private static Source createFactorial() {
        return Source.fromText("function test() {\n" +
                        "  res = fac(2);\n" + "  println(res);\n" +
                        "  return res;\n" +
                        "}\n" +
                        "function fac(n) {\n" +
                        "  if (n <= 1) {\n" +
                        "    return 1;\n" + "  }\n" +
                        "  nMinusOne = n - 1;\n" +
                        "  nMOFact = fac(nMinusOne);\n" +
                        "  res = n * nMOFact;\n" +
                        "  return res;\n" + "}\n",
                        "factorial.sl").withMimeType(SLLanguage.MIME_TYPE);
    }

    private static Source createFactorialWithDebugger() {
        return Source.fromText("function test() {\n" +
                        "  res = fac(2);\n" + "  println(res);\n" +
                        "  return res;\n" +
                        "}\n" +
                        "function fac(n) {\n" +
                        "  if (n <= 1) {\n" +
                        "    return 1;\n" + "  }\n" +
                        "  nMinusOne = n - 1;\n" +
                        "  nMOFact = fac(nMinusOne);\n" +
                        "  debugger;\n" +
                        "  res = n * nMOFact;\n" +
                        "  return res;\n" + "}\n",
                        "factorial.sl").withMimeType(SLLanguage.MIME_TYPE);
    }

    private static Source createInteropComputation() {
        return Source.fromText("function test() {\n" +
                        "}\n" +
                        "function interopFunction(notifyHandler) {\n" +
                        "  executing = true;\n" +
                        "  while (executing == true || executing) {\n" +
                        "    executing = notifyHandler.isExecuting;\n" +
                        "  }\n" +
                        "  return executing;\n" +
                        "}\n",
                        "interopComputation.sl").withMimeType(SLLanguage.MIME_TYPE);
    }

    protected final String getOut() {
        return new String(out.toByteArray());
    }

    protected final String getErr() {
        try {
            err.flush();
        } catch (IOException e) {
        }
        return new String(err.toByteArray());
    }

    @Test
    public void testBreakpoint() throws Throwable {
        final Source factorial = createFactorial();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                try {
                    assertNull(suspendedEvent);
                    assertNotNull(executionEvent);
                    LineLocation nMinusOne = factorial.createLineLocation(8);
                    debugger.setLineBreakpoint(0, nMinusOne, false);
                    executionEvent.prepareContinue();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        engine.eval(factorial);
        assertExecutedOK();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                // the breakpoint should hit instead
            }
        });
        assertLocation(8, true,
                        "return 1", "n",
                        1L, "nMinusOne",
                        null, "nMOFact",
                        null, "res", null);
        continueExecution();

        Value value = engine.findGlobalSymbol("test").execute();
        assertExecutedOK();
        Assert.assertEquals("2\n", getOut());
        Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
    }

    @Test
    public void testDebuggerBreakpoint() throws Throwable {
        final Source factorial = createFactorialWithDebugger();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                assertNull(suspendedEvent);
                assertNotNull(executionEvent);
            }
        });
        engine.eval(factorial);
        assertExecutedOK();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                // the breakpoint should hit instead
            }
        });
        assertLocation(12, true,
                        "debugger", "n",
                        2L, "nMinusOne",
                        1L, "nMOFact",
                        1L, "res", null);
        continueExecution();

        Value value = engine.findGlobalSymbol("test").execute();
        assertExecutedOK();
        Assert.assertEquals("2\n", getOut());
        Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
    }

    @Test
    public void stepInStepOver() throws Throwable {
        final Source factorial = createFactorial();
        engine.eval(factorial);

        // @formatter:on
        run.addLast(new Runnable() {
            @Override
            public void run() {
                assertNull(suspendedEvent);
                assertNotNull(executionEvent);
                executionEvent.prepareStepInto();
            }
        });

        assertLocation(2, true, "res = fac(2)", "res", null);
        stepInto(1);
        assertLocation(7, true,
                        "n <= 1", "n",
                        2L, "nMinusOne",
                        null, "nMOFact",
                        null, "res", null);
        stepOver(1);
        assertLocation(10, true,
                        "nMinusOne = n - 1", "n",
                        2L, "nMinusOne",
                        null, "nMOFact",
                        null, "res", null);
        stepOver(1);
        assertLocation(11, true,
                        "nMOFact = fac(nMinusOne)", "n",
                        2L, "nMinusOne",
                        1L, "nMOFact",
                        null, "res", null);
        stepOver(1);
        assertLocation(12, true,
                        "res = n * nMOFact", "n", 2L, "nMinusOne",
                        1L, "nMOFact",
                        1L, "res", null);
        stepOver(1);
        assertLocation(13, true,
                        "return res", "n",
                        2L, "nMinusOne",
                        1L, "nMOFact",
                        1L, "res", 2L);
        stepOver(1);
        assertLocation(2, false, "fac(2)", "res", null);
        stepOver(1);
        assertLocation(3, true, "println(res)", "res", 2L);
        stepOut();

        Value value = engine.findGlobalSymbol("test").execute();
        assertExecutedOK();

        Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
    }

    @Test
    public void testPause() throws Throwable {
        final Source interopComp = createInteropComputation();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                assertNull(suspendedEvent);
                assertNotNull(executionEvent);
            }
        });
        engine.eval(interopComp);
        assertExecutedOK();

        final ExecNotifyHandler nh = new ExecNotifyHandler();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                assertNull(suspendedEvent);
                assertNotNull(executionEvent);
                // Do pause after execution has really started
                new Thread() {
                    @Override
                    public void run() {
                        nh.waitTillCanPause();
                        boolean paused = debugger.pause();
                        Assert.assertTrue(paused);
                    }
                }.start();
            }
        });

        run.addLast(new Runnable() {
            @Override
            public void run() {
                // paused
                assertNotNull(suspendedEvent);
                int line = suspendedEvent.getNode().getSourceSection().getLineLocation().getLineNumber();
                Assert.assertTrue("Unexpected line: " + line, 5 <= line && line <= 6);
                final MaterializedFrame frame = suspendedEvent.getFrame();
                String[] expectedIdentifiers = new String[]{"executing"};
                for (String expectedIdentifier : expectedIdentifiers) {
                    FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(expectedIdentifier);
                    Assert.assertNotNull(expectedIdentifier, slot);
                }
                // Assert.assertTrue(debugger.isExecuting());
                suspendedEvent.prepareContinue();
                nh.pauseDone();
            }
        });

        Value value = engine.findGlobalSymbol("interopFunction").execute(nh);

        assertExecutedOK();
        // Assert.assertFalse(debugger.isExecuting());
        Boolean n = value.as(Boolean.class);
        assertNotNull(n);
        assertTrue("Interop computation OK", !n.booleanValue());
    }

    private void performWork() {
        try {
            if (ex == null && !run.isEmpty()) {
                Runnable c = run.removeFirst();
                c.run();
            }
        } catch (Throwable e) {
            ex = e;
        }
    }

    private void stepOver(final int size) {
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareStepOver(size);
            }
        });
    }

    private void stepOut() {
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareStepOut();
            }
        });
    }

    private void continueExecution() {
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareContinue();
            }
        });
    }

    private void stepInto(final int size) {
        run.addLast(new Runnable() {
            public void run() {
                suspendedEvent.prepareStepInto(size);
            }
        });
    }

    private void assertLocation(final int line, final boolean isBefore, final String code, final Object... expectedFrame) {
        run.addLast(new Runnable() {
            public void run() {
                assertNotNull(suspendedEvent);
                Assert.assertEquals(line, suspendedEvent.getNode().getSourceSection().getLineLocation().getLineNumber());
                Assert.assertEquals(code, suspendedEvent.getNode().getSourceSection().getCode());
                Assert.assertEquals(isBefore, suspendedEvent.isHaltedBefore());
                final MaterializedFrame frame = suspendedEvent.getFrame();

                Assert.assertEquals(expectedFrame.length / 2, frame.getFrameDescriptor().getSize());

                for (int i = 0; i < expectedFrame.length; i = i + 2) {
                    String expectedIdentifier = (String) expectedFrame[i];
                    Object expectedValue = expectedFrame[i + 1];
                    FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(expectedIdentifier);
                    Assert.assertNotNull(slot);
                    Assert.assertEquals(expectedValue, frame.getValue(slot));
                }
                run.removeFirst().run();
            }
        });
    }

    private void assertExecutedOK() throws Throwable {
        Assert.assertTrue(getErr(), getErr().isEmpty());
        if (ex != null) {
            if (ex instanceof AssertionError) {
                throw ex;
            } else {
                throw new AssertionError("Error during execution", ex);
            }
        }
        assertTrue("Assuming all requests processed: " + run, run.isEmpty());
    }

    private static class ExecNotifyHandler implements TruffleObject {

        private final ExecNotifyHandlerForeign nhf = new ExecNotifyHandlerForeign(this);
        private final ForeignAccess access = ForeignAccess.create(null, nhf);
        private final Object pauseLock = new Object();
        private boolean canPause;
        private volatile boolean pauseDone;

        @Override
        public ForeignAccess getForeignAccess() {
            return access;
        }

        private void waitTillCanPause() {
            synchronized (pauseLock) {
                while (!canPause) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException iex) {
                    }
                }
            }
        }

        void setCanPause() {
            synchronized (pauseLock) {
                canPause = true;
                pauseLock.notifyAll();
            }
        }

        private void pauseDone() {
            pauseDone = true;
        }

        boolean isPauseDone() {
            return pauseDone;
        }

    }

    private static class ExecNotifyHandlerForeign implements Factory10, Factory {

        private final ExecNotifyHandler nh;

        ExecNotifyHandlerForeign(ExecNotifyHandler nh) {
            this.nh = nh;
        }

        @Override
        public CallTarget accessIsNull() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessIsExecutable() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessIsBoxed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessHasSize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessGetSize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessUnbox() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessRead() {
            return Truffle.getRuntime().createCallTarget(new ExecNotifyReadNode(nh));
        }

        @Override
        public CallTarget accessWrite() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessExecute(int i) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessInvoke(int i) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessNew(int i) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CallTarget accessMessage(Message msg) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean canHandle(TruffleObject to) {
            return (to instanceof ExecNotifyHandler);
        }

    }

    private static class ExecNotifyReadNode extends RootNode {

        private final ExecNotifyHandler nh;

        ExecNotifyReadNode(ExecNotifyHandler nh) {
            super(SLLanguage.class, null, null);
            this.nh = nh;
        }

        @Override
        public Object execute(VirtualFrame vf) {
            nh.setCanPause();
            return !nh.isPauseDone();
        }

    }
}
