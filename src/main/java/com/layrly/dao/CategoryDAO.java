package com.layrly.dao;

import com.layrly.domain.Category;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Category table
 */
public class CategoryDAO extends BaseDAO {
    /**
     * Get all categories
     */
    public List<Category> getAllCategories() throws Exception {
        return executeQuery(dynamoDb -> {
            ScanRequest request = ScanRequest.builder()
                    .tableName("category")
                    .projectionExpression("#name, display_order")
                    .expressionAttributeNames(
                            Map.of("#name", "name")
                    )
                    .build();

            ScanResponse response = dynamoDb.scan(request);

            List<Category> categories = response.items()
                    .stream()
                    .map(item -> new Category(
                            Integer.parseInt(item.get("display_order").n()),
                            item.get("name").s(),
                            Integer.parseInt(item.get("display_order").n())
                    ))
                    .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                    .toList();

            return categories;
        });
    }

    /**
     * Get total count of category
     *
     * @return count of Category
     * @throws Exception if query fails
     */
    public int getTotalCategories() throws Exception {
        return executeQuery(dynamoDb -> {
            int total = 0;
            Map<String, AttributeValue> lastKey = null;

            do {
                ScanRequest.Builder builder = ScanRequest.builder()
                        .tableName("category")
                        .select("COUNT");

                if (lastKey != null && !lastKey.isEmpty()) {
                    builder.exclusiveStartKey(lastKey);
                }

                var response = dynamoDb.scan(builder.build());

                total += response.count();
                lastKey = response.lastEvaluatedKey();

            } while (lastKey != null && !lastKey.isEmpty());

            return total;
        });
    }
}
