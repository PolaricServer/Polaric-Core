/* 
 * Copyright (C) 2025 by Øyvind Hanssen (ohanssen@acm.org)
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

/**
 * Utility class for running external processes.
 * Provides a cleaner alternative to Runtime.getRuntime().exec().
 */
public class ProcessRunner {
    
    /**
     * Result of a process execution.
     */
    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String error;
        
        public ProcessResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
        
        public int getExitCode() {
            return exitCode;
        }
        
        public String getOutput() {
            return output;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * Execute a command and wait for it to complete.
     * 
     * @param command The command to execute (can be a single string or array of arguments)
     * @return ProcessResult containing exit code, output, and error streams
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public static ProcessResult execute(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        return waitForProcess(process);
    }
    
    /**
     * Execute a command with separate arguments and wait for it to complete.
     * 
     * @param cmdarray Array containing the command and its arguments
     * @return ProcessResult containing exit code, output, and error streams
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public static ProcessResult execute(String[] cmdarray) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmdarray);
        return waitForProcess(process);
    }
    
    /**
     * Wait for a process to complete and collect its output.
     * Streams are read concurrently to avoid deadlocks.
     * 
     * @param process The process to wait for
     * @return ProcessResult containing exit code, output, and error streams
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private static ProcessResult waitForProcess(Process process) throws IOException, InterruptedException {
        try {
            // Read streams concurrently to avoid deadlock if buffers fill up
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            
            Thread outputReader = new Thread(() -> {
                try {
                    readStreamIntoBuilder(process.getInputStream(), output);
                } catch (IOException e) {
                    // Ignore - will be caught by main thread
                }
            });
            
            Thread errorReader = new Thread(() -> {
                try {
                    readStreamIntoBuilder(process.getErrorStream(), error);
                } catch (IOException e) {
                    // Ignore - will be caught by main thread
                }
            });
            
            outputReader.start();
            errorReader.start();
            
            int exitCode = process.waitFor();
            
            // Wait for stream readers to complete
            outputReader.join();
            errorReader.join();
            
            return new ProcessResult(exitCode, output.toString(), error.toString());
        } finally {
            // Ensure process resources are cleaned up
            process.destroy();
        }
    }
    
    /**
     * Read all content from an input stream into a StringBuilder.
     * 
     * @param stream The input stream to read from
     * @param builder The StringBuilder to append to
     * @throws IOException if an I/O error occurs
     */
    private static void readStreamIntoBuilder(InputStream stream, StringBuilder builder) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(line);
            }
        }
    }
    
    /**
     * Read all content from an input stream.
     * 
     * @param stream The input stream to read from
     * @return The content as a string
     * @throws IOException if an I/O error occurs
     */
    private static String readStream(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        readStreamIntoBuilder(stream, result);
        return result.toString();
    }
}
