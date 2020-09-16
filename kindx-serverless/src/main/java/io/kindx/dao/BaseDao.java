package io.kindx.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;

import java.util.function.Supplier;

public class BaseDao {

    private static final int OP_WRITE_RETRIES = 3;

    protected DynamoDBMapper dynamoDbMapper;
    protected AmazonDynamoDB amazonDynamoDB;


    public BaseDao(AmazonDynamoDB amazonDynamoDB) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.dynamoDbMapper = new DynamoDBMapper(amazonDynamoDB);
    }

    public <T> void save(Supplier<T> supplier) {
        optimisticWrite(() -> dynamoDbMapper.save(supplier.get()));
    }

    public <T> void forceSave(Supplier<T> supplier) {
        optimisticWrite(() -> forceSave(supplier.get()));
    }

    public <T> void forceSave(T data) {
        dynamoDbMapper.save(data,  DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
                .build());
    }

    public <T> void delete(T data) {
        dynamoDbMapper.delete(data, DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
                .build());
    }

    protected void executeTransactionWrite(Supplier<TransactionWriteRequest> supplier ) {
       optimisticWrite(() -> dynamoDbMapper.transactionWrite(supplier.get()));
    }

    protected <T> void optimisticWrite(Runnable writeOperation) {
        int retries = OP_WRITE_RETRIES;
        Exception e = null;
        while (retries != 0 ) {
            try {
                writeOperation.run();
                return;
            } catch (ConditionalCheckFailedException
                    | InternalServerErrorException | TransactionCanceledException ex) {
                retries--;
                e = ex;
            }
        }
        throw new RuntimeException("Could not write data." + e.getMessage(), e);
    }
}
