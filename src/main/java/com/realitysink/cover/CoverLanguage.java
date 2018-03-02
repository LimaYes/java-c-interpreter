/*
 * Copyright (c) 2016 Gerard Krol
 * Copyright (c) 2018 Tyler Durden (GPG AAB252C6)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.realitysink.cover;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.tools.TruffleProfiler;
import com.realitysink.cover.nodes.*;
import com.realitysink.cover.nodes.call.SLInvokeNode;
import com.realitysink.cover.nodes.controlflow.SLBlockNode;
import com.realitysink.cover.nodes.controlflow.SLFunctionBodyNode;
import com.realitysink.cover.nodes.expression.CoverFunctionLiteralNode;
import com.realitysink.cover.parser.CoverParseException;
import com.realitysink.cover.parser.CoverParser;
import com.realitysink.cover.runtime.SLContext;
import com.realitysink.cover.runtime.SLFunction;

@TruffleLanguage.Registration(name = "Cover", version = "0.1", mimeType = CoverLanguage.MIME_TYPE)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class, DebuggerTags.AlwaysHalt.class})
public final class CoverLanguage extends TruffleLanguage<SLContext> {

    public static final String MIME_TYPE = "application/x-cover";

    public static final CoverLanguage INSTANCE = new CoverLanguage();
    private CoverLanguage() {
    }

    @Override
    protected SLContext createContext(Env env) {
        BufferedReader in = new BufferedReader(new InputStreamReader(env.in()));
        PrintWriter out = new PrintWriter(env.out(), true);
        return new SLContext(env, in, out);
    }

    @Override
    protected CallTarget parse(Source source, Node node, String... argumentNames) throws IOException {
        CoverScope scope = new CoverScope(null);
        try {
            CoverParser parser = new CoverParser(source, scope);
            parser.parse();
        } catch (CoverParseException ex) {
            if (ex.getNode() != null) {
                CoverParser.printTree(ex.getNode(), 1);
            }
            throw new IOException(ex);
        } catch (Throwable ex) {
            throw new IOException(ex);
        }

        SLFunction main = scope.findReference("main").getFunction();


        return main.getCallTarget();
    }

    @Override
    protected Object findExportedSymbol(SLContext context, String globalName, boolean onlyExplicit) {
        return context.getFunctionRegistry().lookup(globalName, false);
    }

    @Override
    protected Object getLanguageGlobal(SLContext context) {
        return context;
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return object instanceof SLFunction;
    }

    @Override
    protected Object evalInContext(Source source, Node node, MaterializedFrame mFrame) throws IOException {
        throw new IllegalStateException("evalInContext not supported");
    }

    public SLContext findContext() {
        CompilerAsserts.neverPartOfCompilation();
        return super.findContext(super.createFindContextNode());
    }
}
