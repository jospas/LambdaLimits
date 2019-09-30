package com.aws.lambdalimits;


import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import org.junit.Test;

public class TestGoodLambda
{
    /**
     * Tests running a Lambda
     * https://aws.amazon.com/blogs/developer/invoking-aws-lambda-functions-from-java/
     */
    @Test
    public void testGoodLambda() throws LambdaLimitsError
    {
        GoodService goodService = LambdaInvokerFactory.builder()
                .lambdaClient(AWSLambdaClientBuilder.standard()
                        .withCredentials(new ProfileCredentialsProvider("aws-josh")).build())
                .build(GoodService.class);

        while (true)
        {
            String response = goodService.testMe("Test message");
            System.out.println(response);
        }

    }
}
