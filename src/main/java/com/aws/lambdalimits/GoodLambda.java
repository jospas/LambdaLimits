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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hyperic.sigar.*;

import java.io.IOException;
import java.util.UUID;

/**
 * A Lambda that makes a remote HTTPS request and demonstrates
 * appropriate HttpClient usage
 */
@SuppressWarnings("unused")
public class GoodLambda implements RequestHandler<String, String>
{
    private static String id = UUID.randomUUID().toString();
    private static int invokeCount = 0;
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final Sigar sigar = new Sigar();
    private static final NetStat netstat = new NetStat();

    public GoodLambda()
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
     * Fired to demonstrate appropriate use of HttpClient
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
            HttpGet get1 = new HttpGet("https://www.google.com.au/");
            CloseableHttpResponse response1 = invokeRESTCall(get1);
            System.out.println(response1.getStatusLine());
            response1.close();

            long timeTaken = System.currentTimeMillis() - start;
            logInvocationEnd(context, timeTaken);

            String message = String.format("GoodLambda: [%s]\tInvocation: [%d]\tCost: [%d] millis\n%s",
                    id, invokeCount, timeTaken, gatherStats());

            context.getLogger().log(message);

            return message;
        }
        catch (Throwable t)
        {
            String message = String.format("Failed to execute GoodLambda: [%s] invocation: [%d] cause: [%s]", id, invokeCount, t.toString());
            context.getLogger().log(message);
            throw new LambdaLimitsError(message, t);
        }
    }

    private void logInvocationStart(Context context)
    {
        context.getLogger().log(String.format("[INFO] Starting GoodLambda: [%s] invocation count: [%d]", id, invokeCount));
    }

    private void logInvocationEnd(Context context, long timeTaken)
    {
        context.getLogger().log(String.format("[INFO] Finishing GoodLambda: [%s] invocation count: [%d] time: [%d] millis", id, invokeCount, timeTaken));
    }

    public CloseableHttpResponse invokeRESTCall(HttpRequestBase httpRequest) throws IOException
    {
        return httpClient.execute(httpRequest);
    }

    private String gatherStats() throws SigarException
    {
        ProcFd procFd = sigar.getProcFd(sigar.getPid());
        procFd.gather(sigar, 5L);
        netstat.stat(sigar);

        return String.format(
                "\tInbound: [%d]" +
                "\tOutbound: [%d]" +
                "\tClose Wait: [%d]" +
                "\tClose: [%d]" +
                "\tOpen Files: [%d]" +
                "\tMax Open Files: [%d]",
                netstat.getAllInboundTotal(),
                netstat.getAllOutboundTotal(),
                netstat.getTcpCloseWait(),
                netstat.getTcpClose(),
                procFd.getTotal(),
                sigar.getResourceLimit().getOpenFilesMax());
    }

}



