package com.aws.lambdalimits;


import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import org.junit.Test;

import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;

public class TestBrokenLambda
{
    /**
     * Tests running a Lambda until it fails
     * https://aws.amazon.com/blogs/developer/invoking-aws-lambda-functions-from-java/
     */
    @Test
    public void testBrokenLambda() throws LambdaLimitsError
    {
        BrokenService brokenService = LambdaInvokerFactory.builder()
                .lambdaClient(AWSLambdaClientBuilder.standard()
                        .withCredentials(new ProfileCredentialsProvider("aws-josh")).build())
                .build(BrokenService.class);

        while (true)
        {
            String response = brokenService.breakMe("Test message");
            System.out.println(response);
        }

    }
}
