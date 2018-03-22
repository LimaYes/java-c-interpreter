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
package com.realitysink.cover.builtins;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.realitysink.cover.ComputationResult;
import com.realitysink.cover.CoverMain;
import com.realitysink.cover.nodes.CoverScope;
import com.realitysink.cover.nodes.CoverType;
import com.realitysink.cover.nodes.CoverTypedExpressionNode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@NodeInfo(shortName = "check_pow")
@NodeChildren({@NodeChild("a"), @NodeChild("b"), @NodeChild("c"), @NodeChild("d")})
public abstract class CoverCheckPowBuiltin extends CoverTypedExpressionNode {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Specialization
    public long checkpow(long a, long b, long c, long d) {
        ByteBuffer hashinp = ByteBuffer.allocate(12 * 4);
        hashinp.order(ByteOrder.LITTLE_ENDIAN);

        hashinp.putInt((int) a).putInt((int) b).putInt((int) c).putInt((int) d);
        ComputationResult r = CoverMain.getComputationResult();

        for (int i = 0; i < 8; i++)
            hashinp.putInt(r.personalized_ints[i]);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");

            byte[] thedigest = md.digest(hashinp.array());

            // Now we have to "decrapify" the endianness on the hash to match xel_miner! Lets just
            // form a consensus about that this is the correct way to do :)

            for (int i = 0; i < thedigest.length; i += 4) {
                byte tmp;
                // swap 0 and 3
                tmp = thedigest[i];
                thedigest[i] = thedigest[i + 3];
                thedigest[i + 3] = tmp;
                // swap 1 and 2
                tmp = thedigest[i + 1];
                thedigest[i + 1] = thedigest[i + 2];
                thedigest[i + 2] = tmp;
            }

            r.powHash = thedigest;
            for (int i = 0; i < 16; i++) {
                if (thedigest[i] > r.targetWas[i])
                    return 0;
                else if (thedigest[i] < r.targetWas[i])
                    return 1;    // POW Solution Found
            }
            return 0; // Hashes are the same lol, you should have better played the lottery
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public CoverType getType() {
        return CoverType.SIGNED_INT;
    }
}
