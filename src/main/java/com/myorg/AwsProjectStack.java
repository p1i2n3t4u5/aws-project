package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class AwsProjectStack extends Stack {
	
	private static String imageBucket = "cdk-rekn-imagebucket";
	
    public AwsProjectStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AwsProjectStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // example resource
        // final Queue queue = Queue.Builder.create(this, "AwsProjectQueue")
        //         .visibilityTimeout(Duration.seconds(300))
        //         .build();
        
//        Bucket bucket = Bucket.Builder.create(this, imageBucket)
//                .versioned(true)
//                .encryption(BucketEncryption.KMS_MANAGED)
//                .build();

        Bucket bucket = Bucket.Builder.create(this, imageBucket).versioned(true).removalPolicy(RemovalPolicy.DESTROY)
				.autoDeleteObjects(true).build();
		CfnOutput.Builder.create(this, "MyBucketOutput").description("The Name of the bucket")
				.value(bucket.getBucketName()).build();
		
		Role role = Role.Builder.create(this, "Role")
		        .assumedBy(new ServicePrincipal("ec2.amazonaws.com")).build();
		
		PolicyStatement policyStatement = new PolicyStatement();
		policyStatement.setEffect(Effect.ALLOW);
		policyStatement.addActions("rekognition:*", "logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents");
		policyStatement.addAllResources();
		role.addToPolicy(policyStatement);
		
//		 const table = new dynamodb.Table(this, "cdk-rekn-imagetable", {
//	            partitionKey: { name: "Image", type: dynamodb.AttributeType.STRING },
//	            removalPolicy: cdk.RemovalPolicy.DESTROY,
//	        });
//	        new cdk.CfnOutput(this, "Table", { value: table.tableName });
//		
		TableProps tableProps;
		Attribute partitionKey = Attribute.builder().name("Image").type(AttributeType.STRING).build();
		tableProps = TableProps.builder().tableName("cdk-rekn-imagetable").partitionKey(partitionKey)
				// The default removal policy is RETAIN, which means that cdk destroy will not
				// attempt to delete
				// the new table, and it will remain in your account until manually deleted. By
				// setting the policy to
				// DESTROY, cdk destroy will delete the table (even if it has data in it)
				.removalPolicy(RemovalPolicy.DESTROY).build();
		Table dynamodbTable = new Table(this, "cdk-rekn-imagetable", tableProps);
		
    }
}
