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

import java.io.*;
import java.util.Map;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Instrument;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import com.oracle.truffle.tools.TruffleProfiler;
import com.realitysink.cover.parser.CoverParser;
import com.realitysink.cover.runtime.SLNull;
import com.realitysink.cover.runtime.SLUndefinedNameException;

public final class CoverMain {

    static boolean skipCompResult = false;
    static PolyglotEngine engine = null;
    static ComputationResult computationResult = new ComputationResult();

    public static ComputationResult getComputationResult() {
        return computationResult;
    }

    /**
     * The main entry point.
     */
    public static void main(String[] args) throws IOException {
        skipCompResult = true;
        computationResult = null;

        Source source;
        if (args.length == 0) {
            source = Source.fromReader(new InputStreamReader(System.in), "<stdin>").withMimeType(CoverLanguage.MIME_TYPE);
        } else {
            source = Source.fromFileName(args[0]).withMimeType(CoverLanguage.MIME_TYPE);
        }
        executeSource(source, System.in, System.out);
    }
    public static ComputationResult executeSource(String source, InputStream in, PrintStream out) throws IOException {
        return executeSource((Source.fromReader(new InputStreamReader(new ByteArrayInputStream(source.getBytes())), "<stdin>").withMimeType(CoverLanguage.MIME_TYPE)), in, out);
    }
    public static ComputationResult executeSource(String source, InputStream in, PrintStream out, int[] storage) throws IOException {
        return executeSource((Source.fromReader(new InputStreamReader(new ByteArrayInputStream(source.getBytes())), "<stdin>").withMimeType(CoverLanguage.MIME_TYPE)), in, out, storage);
    }
    private static ComputationResult executeSource(Source source, InputStream in, PrintStream out) {
        return executeSource(source, in, out, null);
    }

    private synchronized static ComputationResult executeSource(Source source, InputStream in, PrintStream out, int[] storage) {

        // WARNING: This makes initialization quick, but you will not be able to change in/out anymore
        if(engine==null){
            engine = PolyglotEngine.newBuilder().setIn(in).setOut(out).build();
            assert engine.getLanguages().containsKey(CoverLanguage.MIME_TYPE);
        }

        try {
            Value result = engine.eval(source);

            if (result == null) {
                throw new SLException("No function main() defined?");
            } else if (result.get() != SLNull.SINGLETON) {
                out.println("Program exited with code: " + result.get());
            }

        } catch (Throwable ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof UnsupportedSpecializationException) {
                out.println(cause.getMessage());
                cause.printStackTrace();
            } else if (cause instanceof SLUndefinedNameException) {
                out.println(cause.getMessage());
                cause.printStackTrace();
            } else {
                ex.printStackTrace(out);
            }
        }

        engine.dispose();

        if(skipCompResult) return null;
        else return computationResult.copy();
    }
}
