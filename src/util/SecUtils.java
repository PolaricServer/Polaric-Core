/* 
 * Copyright (C) 2009-2026 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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
import java.io.*;
import java.util.*;
import java.security.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.*;



/**
 * Some utilities related to security.
 */
 
public class SecUtils
{

   private static SecureRandom _rand = null;

    static {
       try {
          // Use platform default SecureRandom (NativePRNG on Linux, Windows-PRNG on Windows)
          // This provides better entropy than the deprecated SHA1PRNG algorithm
          _rand = new SecureRandom();
       }
       catch (Exception e) {
          System.out.println("*** SecUtils: Couldnt create random generator");
       }
    }

 
    /**
     * Generate a random key. 
     */   
    public final static byte[] getRandom(int size)
    {
        if (size < 1)
            return null; 
        
        byte[] k = new byte[size];
        _rand.nextBytes(k);
        return k;
    }
    

    /**
     * Compute a hash from the text. Text can be given as
     * an array of bytes, a string or both. A string will be converted
     * to bytes using the UTF-8 encoding before computing the hash.
     */   
    public final static byte[] _digest ( byte[] bytes, String txt, String algo )
    {
        try{
            MessageDigest dig = MessageDigest.getInstance(algo);
            if (bytes != null) 
                dig.update(bytes);
            if (txt != null)
                dig.update(txt.getBytes("UTF-8"));
            return dig.digest();
        }
        catch (Exception e) {
            System.out.println("*** SecUtils: Cannot generate message digest: "+e);
            return null;
        }
    }

    
    /**
     * Computes MD5 hash.
     * @deprecated MD5 is cryptographically broken and should not normally be used for security purposes.
     * Use xDigest() for SHA-256 instead. This method is retained for compatibility
     * with legacy systems that require MD5.
     */
    @Deprecated
    public final static byte[] digest( byte[] bytes, String txt )
        { return _digest(bytes, txt, "MD5"); }
    
    
    
    
    /** Compute SHA-256 hash. */
    public final static byte[] xDigest( byte[] bytes, String txt )
        { return _digest(bytes, txt, "SHA-256"); }
        
      

      
    /**
     * Compute a HMAC SHA256 from data and a key.
     * @return The hmac represented as a byte array.
     */
    public final static byte[] hmac(String data, byte[] key)
    {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HMAC_SHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(data.getBytes("UTF-8"));
        }
        catch (Exception e) {
            System.out.println("*** SecUtils: Cannot generate HMAC: "+e);
            return null;
        }
    }
    
    
    /**
     * Compute a HMAC SHA256 from data and a key.
     * @return The hmac represented as a byte array.
     * @deprecated base64 encoded string should be decoded to binary format.
     *
     * NOTE: key is a string, it is typically Base64 encoded. This is kept for compatibility.
     * It should be decoded to binary format to ensure max entropy. Also consider using a KDF since some 
     * keys may not be random. 
     */
    @Deprecated
    public final static byte[] hmac(String data, String key)
    {   try {
            return hmac(data, key.getBytes("UTF-8"));
        }
        catch (Exception e) {
            return null;
        }
    }
    
    

        
    /**
     * Compute a hash from the text, represented as a hexadecimal string.
     * @deprecated This method uses MD5 which is cryptographically broken. Use xDigestHex() instead.
     */ 
    @Deprecated
    public final static String digestHex(String txt)
        {return b2hex(digest(null, txt));}
        
        
    /**
     * Compute a (SHA256) hash from the text, represented as a hexadecimal string. 
     */
    public final static String xDigestHex(String txt)
        {return b2hex(xDigest(null, txt));}

        
        
    /**
     * Base 64 encoded digest.
     * Returns n first characters of digest, encoded using
     * the Base 64 method.
     * @deprecated This method uses MD5 which is cryptographically broken. Use xDigestB64() instead.
     */
    @Deprecated
    public final static String digestB64(String txt, int n)
    {
       String d = b64encode(digest(null, txt));
       return d.substring(0,n); 
    }
    
    
    /**
     * Base 64 encoded digest (SHA256).
     * Returns n first characters of digest, encoded using
     * the Base 64 method.
     */
    public final static String xDigestB64(String txt, int n)
    {
       String d = b64encode(xDigest(null, txt));
       return d.substring(0,n); 
    }
    


    public final static String xDigestB91(String txt, int n)
    {
        String d = Base91.encode(xDigest(null, txt));
        return d.substring(0,n);
    }

    
     
    /**
     * Base 64 encoded HMAC SHA256.
     * Returns n first characters of digest, encoded using
     * the Base 64 method.
     */
    public final static String hmacB64(String txt, byte[] key, int n)
    {
       String d = b64encode(hmac(txt, key));
       return d.substring(0,n); 
    }
    
         
    /**
     * Base 64 encoded HMAC SHA256.
     * @deprecated
     */
    @Deprecated 
    public final static String hmacB64(String txt, String key, int n) {
        try {
            return hmacB64(txt, key.getBytes("UTF-8"), n);
        }
        catch (Exception e) {
            return null;
        }
    }
     
     
    /**
     * Base 91 encoded HMAC SHA256.
     */
    public final static String hmacB91(String txt, String key, int n)
    {
        String d = Base91.encode(hmac(txt, key));
        return d.substring(0,n);
    }



    public final static String b64encode(byte[] x) 
    {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(x);
    }
    

    
    public final static byte[] b64decode(String txt) 
    {
        Base64.Decoder decoder = Base64.getDecoder();
        return decoder.decode(txt);
    }
    
    

    public final static String b91encode(byte[] x)
    {
        return Base91.encode(x);
    }
    

    public final static byte[] b91decode(String x)
    {
        return Base91.decode(x);
    }



    /**
     * Hexadecimal representation of a byte array.
     */
    public final static String b2hex (byte[] bytes)
    {	
	StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(convertDigit((int) (bytes[i] >> 4)));
            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
        }
        return (sb.toString());
    }     
        
        
    /**
     * [Private] Convert the specified value (0 .. 15) to the corresponding
     * hexadecimal digit.
     *
     * @param value Value to be converted
     */
    private final static char convertDigit(int value) {

        value &= 0x0f;
        if (value >= 10)
            return ((char) (value - 10 + 'a'));
        else
            return ((char) (value + '0'));

    }  
    
    
    
    /**
     * Escape a text-string for use as a part of a regular expression. 
     */
    public static String escape4regex(String x) {
        return x.replaceAll("([\\$\\^\\*\\+\\?\\.\\(\\)\\[\\]\\{\\}\\\\])", "\\\\$1");
    }
    
    
    
    /**
     * Generate a key from a low entropy secret like a password. 
     */
    public static SecretKey pbkdf2(String password, String salt, int iterations, String algorithm)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
    
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterations, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec)
            .getEncoded(), algorithm);
        return secret;
    }
    
    
    /**
     * Generate an AES256 key from a low entropy secret like a password. 
     */
    public static SecretKey pbkdf2_aes(String password, String salt, int iterations)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        return pbkdf2(password, salt, iterations, "AES");
    }
    
    
}

