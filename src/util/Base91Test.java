/* 
 * Copyright (C) 2009-2025 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package no.polaric.core.util;

import java.util.Arrays;

/**
 * Test program for Base91 encoding/decoding.
 * Verifies that the implementation matches the basE91 algorithm with APRS alphabet.
 */
public class Base91Test {
    
    public static void main(String[] args) {
        boolean allPassed = true;
        
        // Test 1: Simple text encoding
        System.out.println("Test 1: Text encoding 'Hello'");
        byte[] test1 = "Hello".getBytes();
        String encoded1 = Base91.encode(test1);
        System.out.println("  Encoded: " + encoded1);
        byte[] decoded1 = Base91.decode(encoded1);
        boolean test1Pass = Arrays.equals(test1, decoded1);
        System.out.println("  Decoded: " + new String(decoded1));
        System.out.println("  Round-trip: " + (test1Pass ? "PASS" : "FAIL"));
        allPassed &= test1Pass;
        
        // Test 2: Binary data
        System.out.println("\nTest 2: Binary data {1,2,3,4,5}");
        byte[] test2 = {1, 2, 3, 4, 5};
        String encoded2 = Base91.encode(test2);
        System.out.println("  Encoded: " + encoded2);
        byte[] decoded2 = Base91.decode(encoded2);
        boolean test2Pass = Arrays.equals(test2, decoded2);
        System.out.println("  Decoded: " + Arrays.toString(decoded2));
        System.out.println("  Round-trip: " + (test2Pass ? "PASS" : "FAIL"));
        allPassed &= test2Pass;
        
        // Test 3: Empty data
        System.out.println("\nTest 3: Empty data");
        byte[] test3 = new byte[0];
        String encoded3 = Base91.encode(test3);
        byte[] decoded3 = Base91.decode(encoded3);
        boolean test3Pass = Arrays.equals(test3, decoded3);
        System.out.println("  Encoded: '" + encoded3 + "'");
        System.out.println("  Round-trip: " + (test3Pass ? "PASS" : "FAIL"));
        allPassed &= test3Pass;
        
        // Test 4: Single byte
        System.out.println("\nTest 4: Single byte {42}");
        byte[] test4 = {42};
        String encoded4 = Base91.encode(test4);
        System.out.println("  Encoded: " + encoded4);
        byte[] decoded4 = Base91.decode(encoded4);
        boolean test4Pass = Arrays.equals(test4, decoded4);
        System.out.println("  Decoded: " + Arrays.toString(decoded4));
        System.out.println("  Round-trip: " + (test4Pass ? "PASS" : "FAIL"));
        allPassed &= test4Pass;
        
        // Test 5: APRS text
        System.out.println("\nTest 5: APRS text");
        byte[] test5 = "APRS".getBytes();
        String encoded5 = Base91.encode(test5);
        System.out.println("  Encoded: " + encoded5);
        byte[] decoded5 = Base91.decode(encoded5);
        boolean test5Pass = Arrays.equals(test5, decoded5);
        System.out.println("  Decoded: " + new String(decoded5));
        System.out.println("  Round-trip: " + (test5Pass ? "PASS" : "FAIL"));
        allPassed &= test5Pass;
        
        // Test 6: Longer data
        System.out.println("\nTest 6: Longer text");
        byte[] test6 = "The quick brown fox jumps over the lazy dog".getBytes();
        String encoded6 = Base91.encode(test6);
        System.out.println("  Encoded: " + encoded6);
        byte[] decoded6 = Base91.decode(encoded6);
        boolean test6Pass = Arrays.equals(test6, decoded6);
        System.out.println("  Decoded: " + new String(decoded6));
        System.out.println("  Round-trip: " + (test6Pass ? "PASS" : "FAIL"));
        allPassed &= test6Pass;
        
        // Test 7: All zeros
        System.out.println("\nTest 7: All zeros {0,0,0,0}");
        byte[] test7 = {0, 0, 0, 0};
        String encoded7 = Base91.encode(test7);
        System.out.println("  Encoded: " + encoded7);
        byte[] decoded7 = Base91.decode(encoded7);
        boolean test7Pass = Arrays.equals(test7, decoded7);
        System.out.println("  Decoded: " + Arrays.toString(decoded7));
        System.out.println("  Round-trip: " + (test7Pass ? "PASS" : "FAIL"));
        allPassed &= test7Pass;
        
        // Test 8: All 0xFF
        System.out.println("\nTest 8: All 0xFF");
        byte[] test8 = {(byte)0xFF, (byte)0xFF, (byte)0xFF};
        String encoded8 = Base91.encode(test8);
        System.out.println("  Encoded: " + encoded8);
        byte[] decoded8 = Base91.decode(encoded8);
        boolean test8Pass = Arrays.equals(test8, decoded8);
        System.out.println("  Decoded: " + Arrays.toString(decoded8));
        System.out.println("  Round-trip: " + (test8Pass ? "PASS" : "FAIL"));
        allPassed &= test8Pass;
        
        // Test 9: Verify expected encodings match the reference implementation
        System.out.println("\nTest 9: Verify specific encodings");
        // These values are from our test of the C reference implementation
        boolean test9Pass = true;
        
        String hello = Base91.encode("Hello".getBytes());
        if (!hello.equals(":J^#_NA")) {
            System.out.println("  'Hello' encoding mismatch: expected ':J^#_NA', got '" + hello + "'");
            test9Pass = false;
        }
        
        String aprs = Base91.encode("APRS".getBytes());
        if (!aprs.equals("7y$Y4")) {
            System.out.println("  'APRS' encoding mismatch: expected '7y$Y4', got '" + aprs + "'");
            test9Pass = false;
        }
        
        if (test9Pass) {
            System.out.println("  All specific encodings match: PASS");
        } else {
            System.out.println("  Specific encodings: FAIL");
        }
        allPassed &= test9Pass;
        
        System.out.println("\n" + (allPassed ? "ALL TESTS PASSED" : "SOME TESTS FAILED"));
        System.exit(allPassed ? 0 : 1);
    }
}
