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
     * 
     * @param process The process to wait for
     * @return ProcessResult containing exit code, output, and error streams
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private static ProcessResult waitForProcess(Process process) throws IOException, InterruptedException {
        int exitCode = process.waitFor();
        
        String output = readStream(process.getInputStream());
        String error = readStream(process.getErrorStream());
        
        return new ProcessResult(exitCode, output, error);
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (result.length() > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
        }
        return result.toString();
    }
}
