/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.aws.lambdalimits;

import java.io.*;
import java.util.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hyperic.sigar.*;

/**
 * A Lambda that makes a remote HTTPS request and demonstrates
 * inappropriate HttpClient usage to trigger a too many open files error
 */
@SuppressWarnings("unused")
public class BrokenLambda implements RequestHandler<String, String>
{
    private static String id = UUID.randomUUID().toString();
    private static int invokeCount = 0;
    private static final Sigar sigar = new Sigar();
    private static final NetStat netstat = new NetStat();

    private static final List<FileInputStream> testFiles = new ArrayList<>();

    public BrokenLambda()
    {
        try
        {
            SigarLoader loader = new SigarLoader(Sigar.class);
            loader.load();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    /**
     * Fired to demonstrate triggering an out of file handles error
     * through inappropriate use of HttpClient
     * @param context the context of the Lambda execution
     * @return A message with timing
     */
    @Override
    public String handleRequest(String requestMessage, Context context) throws LambdaLimitsError
    {
        long start = System.currentTimeMillis();

        logInvocationStart(context);

        invokeCount++;

        try
        {
            makeRequest(context, "https://www.google.com.au/");
            makeRequest(context, "https://www.google.com/");
//            makeRequest(context, "https://google.com.au/");
//            makeRequest(context, "https://google.com/");
//            makeRequest(context, "https://www.google.co.nz/");
//            makeRequest(context, "https://google.co.nz/");
//            makeRequest(context, "https://www.google.co.za/");
//            makeRequest(context, "https://google.co.za/");
//            makeRequest(context, "https://www.google.fr/");
//            makeRequest(context, "https://google.fr/");
//            makeRequest(context, "https://www.google.de/");
//            makeRequest(context, "https://google.de/");
//
//            makeRequest(context, "https://www.google.co.jp");
//            makeRequest(context, "https://www.google.co.uk");
//            makeRequest(context, "https://www.google.es");
//            makeRequest(context, "https://www.google.ca");
//            makeRequest(context, "https://www.google.it");
//            makeRequest(context, "https://www.google.com.tw");
//            makeRequest(context, "https://www.google.nl");
//            makeRequest(context, "https://www.google.com.br");
//            makeRequest(context, "https://www.google.com.tr");
//            makeRequest(context, "https://www.google.be");
//            makeRequest(context, "https://www.google.com.gr");
//            makeRequest(context, "https://www.google.co.in");
//            makeRequest(context, "https://www.google.com.mx");
//            makeRequest(context, "https://www.google.dk");

            /**
             * This block will reliably replicate the issue of too many open files
             * Once this error state is reached the Lambda is broken and needs to be
             * redeployed
             */
//            for (int i = 0; i < 50; i++)
//            {
//                FileInputStream in = new FileInputStream(File.createTempFile("broken", ".txt"));
//                testFiles.add(in);
//            }

            long timeTaken = System.currentTimeMillis() - start;
            logInvocationEnd(context, timeTaken);

            String message = String.format("BrokenLambda: [%s]\tInvocation: [%d]\tCost: [%d] millis\n%s",
                    id, invokeCount, timeTaken, gatherStats());

            context.getLogger().log(message);

            return message;
        }
        catch (Throwable t)
        {
            String message = String.format("Failed to execute BrokenLambda: [%s] invocation: [%d] cause: [%s]", id, invokeCount, t.toString());
            context.getLogger().log(message);
            throw new LambdaLimitsError(message, t);
        }
    }

    private void makeRequest(Context context, String url) throws IOException
    {
        HttpGet get1 = new HttpGet(url);
        HttpResponse response1 = invokeRESTCall(get1);
        String responseBody = EntityUtils.toString(response1.getEntity());
    }

    private void logInvocationStart(Context context)
    {
        context.getLogger().log(String.format("[INFO] Starting BrokenLambda: [%s] invocation count: [%d]", id, invokeCount));
    }

    private void logInvocationEnd(Context context, long timeTaken)
    {
        context.getLogger().log(String.format("[INFO] Finishing BrokenLambda: [%s] invocation count: [%d] time: [%d] millis", id, invokeCount, timeTaken));
    }

    /**
     * Minimal HttpClient request with poor client and response handling.
     * Note: this technique leaks sockets to replicate the issue
     * @param httpRequest the request to execute
     * @return the HttpResponse, (should be a CloseableHttpResponse!)
     * @throws IOException thrown on failure
     */
    public HttpResponse invokeRESTCall(HttpRequestBase httpRequest) throws IOException
    {
        HttpClient httpClient = HttpClients.createDefault();
        return httpClient.execute(httpRequest);
    }

    /**
     * Gathers system statistics
     * @return the system statistics including open sockets and files
     * @throws SigarException thrown on failure to get socket and file data
     * @throws IOException thrown on failure to get socket and file data
     */
    private String gatherStats() throws SigarException, IOException
    {
        ProcFd procFd = sigar.getProcFd(sigar.getPid());
        procFd.gather(sigar, sigar.getPid());
        netstat.stat(sigar);

        NetConnection[] connections = sigar.getNetConnectionList(NetFlags.CONN_TCP | NetFlags.CONN_SERVER | NetFlags.CONN_CLIENT);

        Map<String, Integer> counts = new TreeMap<>();

        for (NetConnection connection: connections)
        {
            String key = String.format("%s\t%s\t%s",
                    connection.getRemoteAddress(),
                    connection.getTypeString(),
                    connection.getStateString());

            if (!counts.containsKey(key))
            {
                counts.put(key, 1);
            }
            else
            {
                counts.put(key, counts.get(key) + 1);
            }
        }

        StringBuilder builder = new StringBuilder();

        for (String key: counts.keySet())
        {
            builder.append("\t\t").append(key).append("\t").append(counts.get(key)).append("\n");
        }

        return String.format(
            "\tInbound: [%d]" +
            "\tOutbound: [%d]" +
            "\tEstablished: [%d]" +
            "\tTime wait: [%d]" +
            "\tOpen Files: [%d]" +
            "\tMax Open Files: [%d]\n" +
            "\tConnected sockets:\n" +
            "%s" +
            "%s",
            netstat.getAllInboundTotal(),
            netstat.getAllOutboundTotal(),
            netstat.getTcpEstablished(),
            netstat.getTcpTimeWait(),
            procFd.getTotal(),
            sigar.getResourceLimit().getOpenFilesMax(),
            builder.toString(),
            getOpenFiles());
    }

    /**
     * Finds the open files for the current process
     * @return a formatted list of open files and their open count
     */
    public static String getOpenFiles() {
        StringBuilder builder = new StringBuilder();

        Map<String, Integer> files = new TreeMap<>();

        File fds = new File("/proc/self/fd");

        if (fds.exists())
        {
            for (File fd : fds.listFiles())
            {
                try
                {
                    String name = fd.getCanonicalFile().getAbsolutePath();

                    if (!files.containsKey(name))
                    {
                        files.put(name, 1);
                    }
                    else
                    {
                        files.put(name, files.get(name) + 1);
                    }
                }
                catch (IOException ignore)
                {
                }
            }
        }

        builder.append("\tOpen file count: ").append(files.size()).append("\n");

        for (String file: files.keySet())
        {
            builder.append("\t\t").append(file).append(" ").append(files.get(file)).append("\n");
        }

        return builder.toString();
    }


}



