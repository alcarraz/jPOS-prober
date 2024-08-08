/*
 * jpos-prober [https://github.com/alcarraz/jpos-prober]
 *
 * Copyright (C) 2024.  Andrés Alcarraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https:www.gnu.org/licenses/>.
 *
 */

package org.jpos.probe;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jpos.core.annotation.Config;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.q2.Q2;
import org.jpos.q2.QBeanSupport;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Probe extends QBeanSupport {
    
    static final long DEFAULT_TIMEOUT = 30000;
    @Config("message-file")
    String messageFile = "-";
    
    @Config("mux")
    String mux = "probe-mux";
    
    @Config("shutdown")
    boolean shutdown = true;
    
    @Config("timeout")
    long timeout = DEFAULT_TIMEOUT;
    
    @Override
    protected void startService() {
        Thread t = new Thread( () -> {
            try (InputStream input = messageFile.equals("-") ? System.in : new FileInputStream(messageFile); 
                 XMLInputFileChannel channel = new XMLInputFileChannel(input);) 
            {
                while (true) {
                    ISOMsg request = channel.receive();
                    log.debug("sending request", request);
                    ISOMsg response = QMUX.getMUX(mux).request(request, timeout);
                    log.debug("received response", response);
                }
            } catch (NameRegistrar.NotFoundException e) {
                log.error("Mux not found", e);
            } catch (EOFException eof) {
                log.info("End of input reached");
            } catch (ISOException | IOException e) {
                log.error("Probe error", e);
            } finally {
                if (shutdown) getServer().shutdown();
            }
        });
        t.setDaemon(true);
        t.start();
    }


    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options ()
                .addOption("h", "help", false, "Prints this help")
                .addOption("P", "port", true, "Port to connect to")
                .addOption("H", "host", true, "Host to connect to")
                .addOption("pk", "packager", true, "Packager class used to connect to destination")
                .addOption("p", "packager.config", true, "Packager config used to connect to destination")
                .addOption("ch","channel", true, "channel class used to connect to destination")
                .addOption(null, "header", true, "Header of destination channel")
                /* this will be possible if PR  https://github.com/jpos/jPOS/pull/560 
                    is approved and merged
                    .addOption("k", "mux.key", true, "Key used to match responses by the mux")
                 */
                .addOption("t", "timeout", true, "Time to wait for a response")
                .addOption("ct", "channel.timeout", true, "Time to wait for channel connection to be established")
                .addOption("S", "no-shutdown", false, "No auto shutdown")
                .addOption("Q", "force-quit", false, "Force quit after overall timeout")
                .addOption("T", "overall-timeout", true, "Overall timeout to wait if focer-quit, defaults to timeout (-t)")
                .addOption("m", "message-file",true, "where to read messages to send from, default is '-'")
                .addOptionGroup(new OptionGroup()
                    .addOption(new Option("vv", "verbose", false, "Show more"))
                    .addOption(new Option(null, "log.disable-realms", true, "realms to be ignored, only valid on not verbose output"))
                )
                ;
        
        try {
            CommandLine line = parser.parse(options, args, true);
            if (line.hasOption('h')){
                printHelp(options);
                System.exit (0);
            }
            if (line.hasOption("S")) {
                System.setProperty("probe.shutdown", "false");
            }
            for (Option o :line.getOptions()) {
                if(options.hasLongOption(o.getLongOpt())) {
                    System.setProperty("probe." + o.getLongOpt(), o.hasArg() ? o.getValue() : "true");
                }
            }
            Q2.main(line.getArgs());
            if (line.hasOption("Q")){
                Executors.newScheduledThreadPool(1).schedule(() -> {System.exit(1);},
                        Long.parseLong(line.getOptionValue("T", 
                                line.getOptionValue("t", Long.toString(DEFAULT_TIMEOUT)))), 
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            System.exit(1);

        }
    }
    
    private static void printHelp (Options options) throws Exception {
        HelpFormatter helpFormatter = new HelpFormatter ();
        helpFormatter.printHelp (Probe.class.getName (), "Probe options", options, "In addition you can pass following Q2 options:", true);
        Q2.main(new String[]{"-h"});
    }
}

